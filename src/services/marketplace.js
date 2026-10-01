import { randomUUID, createHash } from 'node:crypto';
import { transaction } from '../db/index.js';
import { requireCondition } from '../errors.js';
import { coordinates, text, integer } from '../validation.js';
import { nearby } from './geo.js';

export class Marketplace {
  constructor(db, clock = Date.now) { this.db=db; this.clock=clock; }
  audit(jobId, actorId, event) {
    this.db.prepare('INSERT INTO audit_events(job_id,actor_id,event,created_at) VALUES(?,?,?,?)').run(jobId,actorId,event,this.clock());
  }
  getJob(id, actor) {
    const job=this.db.prepare('SELECT * FROM jobs WHERE id=?').get(id);
    requireCondition(job,404,'Job not found');
    const provider=this.db.prepare('SELECT id FROM providers WHERE user_id=?').get(actor.id);
    requireCondition(actor.role==='admin' || (actor.role==='customer' && job.customer_id===actor.id) ||
      (actor.role==='provider' && job.provider_id===provider?.id),403,'Job access denied');
    return job;
  }
  createJob(actor,input,key) {
    requireCondition(actor.role==='customer',403,'Customer role required');
    coordinates(input.lat,input.lng);
    const normalized={category_id:integer(input.category_id,'category'),address:text(input.address,'address',500),
      description:input.description===undefined?'':text(input.description,'description',2000),lat:input.lat,lng:input.lng};
    key=text(key,'Idempotency-Key',128);
    const hash=createHash('sha256').update(JSON.stringify(normalized)).digest('hex');
    return transaction(this.db,()=>{
      const prior=this.db.prepare('SELECT * FROM jobs WHERE customer_id=? AND request_key=?').get(actor.id,key);
      if(prior) { requireCondition(prior.request_hash===hash,409,'Idempotency key reused with different request'); return prior; }
      const category=this.db.prepare('SELECT * FROM categories WHERE id=? AND enabled=1').get(normalized.category_id);
      requireCondition(category,400,'Unknown or disabled category');
      const id=randomUUID(),now=this.clock();
      this.db.prepare(`INSERT INTO jobs(id,customer_id,category_id,address,description,lat,lng,price_minor,currency,request_key,request_hash,created_at,updated_at)
        VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)`).run(id,actor.id,normalized.category_id,normalized.address,normalized.description,
        normalized.lat,normalized.lng,category.base_price_minor,category.currency,key,hash,now,now);
      this.audit(id,actor.id,'created'); this.offerNext(id);
      return this.db.prepare('SELECT * FROM jobs WHERE id=?').get(id);
    });
  }
  // Caller owns transaction. Offers/deadlines are durable; no in-memory dispatch state.
  offerNext(jobId) {
    const job=this.db.prepare('SELECT * FROM jobs WHERE id=?').get(jobId);
    if(!job || !['pending_dispatch','dispatched'].includes(job.status)) return;
    if(this.db.prepare("SELECT id FROM offers WHERE job_id=? AND status='pending'").get(jobId)) return;
    const candidate=nearby(this.db,job,this.clock())[0];
    if(!candidate) {
      this.db.prepare("UPDATE jobs SET status='expired',updated_at=? WHERE id=?").run(this.clock(),jobId);
      this.audit(jobId,null,'dispatch_exhausted'); return;
    }
    this.db.prepare("INSERT INTO offers(id,job_id,provider_id,status,expires_at) VALUES(?,?,?,'pending',?)")
      .run(randomUUID(),jobId,candidate.id,this.clock()+120000);
    this.db.prepare("UPDATE jobs SET status='dispatched',updated_at=? WHERE id=?").run(this.clock(),jobId);
    this.audit(jobId,null,'offered');
  }
  tick() {
    transaction(this.db,()=>{
      const due=this.db.prepare("SELECT * FROM offers WHERE status='pending' AND expires_at<=?").all(this.clock());
      for(const offer of due) {
        this.db.prepare("UPDATE offers SET status='expired' WHERE id=?").run(offer.id);
        this.audit(offer.job_id,null,'offer_expired'); this.offerNext(offer.job_id);
      }
      for(const job of this.db.prepare("SELECT id FROM jobs WHERE status='pending_dispatch'").all()) this.offerNext(job.id);
    });
  }
  listOffers(actor) {
    requireCondition(actor.role==='provider',403,'Provider role required');
    return this.db.prepare(`SELECT o.id,o.job_id,o.expires_at,j.category_id,j.price_minor,j.currency
      FROM offers o JOIN providers p ON p.id=o.provider_id JOIN jobs j ON j.id=o.job_id
      WHERE p.user_id=? AND o.status='pending' AND o.expires_at>?`).all(actor.id,this.clock());
  }
  respond(actor,offerId,decision) {
    requireCondition(actor.role==='provider',403,'Provider role required');
    requireCondition(['accept','reject'].includes(decision),400,'Invalid offer decision');
    return transaction(this.db,()=>{
      const offer=this.db.prepare(`SELECT o.*,p.user_id,p.active,p.approved FROM offers o JOIN providers p ON p.id=o.provider_id WHERE o.id=?`).get(offerId);
      requireCondition(offer,404,'Offer not found');
      requireCondition(offer.user_id===actor.id,403,'Offer belongs to another provider');
      requireCondition(offer.status==='pending' && offer.expires_at>this.clock(),409,'Offer is no longer available');
      if(decision==='accept') {
        requireCondition(offer.active && offer.approved,409,'Provider unavailable');
        const busy=this.db.prepare("SELECT id FROM jobs WHERE provider_id=? AND status IN ('accepted','in_transit','in_progress')").get(offer.provider_id);
        requireCondition(!busy,409,'Provider has an active job');
        const result=this.db.prepare("UPDATE jobs SET status='accepted',provider_id=?,updated_at=? WHERE id=? AND status='dispatched'")
          .run(offer.provider_id,this.clock(),offer.job_id);
        requireCondition(result.changes===1,409,'Job already assigned');
        this.db.prepare("UPDATE offers SET status='accepted' WHERE id=?").run(offerId);
      } else {
        this.db.prepare("UPDATE offers SET status='rejected' WHERE id=?").run(offerId); this.offerNext(offer.job_id);
      }
      this.audit(offer.job_id,actor.id,decision==='accept'?'accepted':'offer_rejected');
      const job=this.db.prepare('SELECT * FROM jobs WHERE id=?').get(offer.job_id);
      return decision==='accept'?job:{id:job.id,status:job.status};
    });
  }
  transition(actor,id,status) {
    const next={accepted:'in_transit',in_transit:'in_progress',in_progress:'completed'};
    return transaction(this.db,()=>{
      const job=this.getJob(id,actor);
      if(status==='cancelled') {
        requireCondition(actor.role==='customer' || actor.role==='admin',403,'Only customer or admin may cancel');
        requireCondition(['pending_dispatch','dispatched','accepted','in_transit'].includes(job.status),409,'Cannot cancel at this stage');
        this.db.prepare("UPDATE offers SET status='cancelled' WHERE job_id=? AND status='pending'").run(id);
      } else {
        requireCondition(actor.role==='provider',403,'Assigned provider required');
        requireCondition(next[job.status]===status,409,'Invalid job transition');
      }
      this.db.prepare('UPDATE jobs SET status=?,updated_at=? WHERE id=?').run(status,this.clock(),id);
      this.audit(id,actor.id,status); return this.db.prepare('SELECT * FROM jobs WHERE id=?').get(id);
    });
  }
  updateProvider(actor,input) {
    requireCondition(actor.role==='provider',403,'Provider role required'); coordinates(input.lat,input.lng);
    requireCondition(typeof input.active==='boolean',400,'active must be boolean');
    const result=this.db.prepare('UPDATE providers SET lat=?,lng=?,active=?,last_seen=? WHERE user_id=?')
      .run(input.lat,input.lng,Number(input.active),this.clock(),actor.id);
    requireCondition(result.changes===1,404,'Provider profile not found');
    return {updated:true};
  }
}
