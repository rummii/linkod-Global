import test from 'node:test';
import assert from 'node:assert/strict';
import { createHmac } from 'node:crypto';
import { mkdtempSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { openDatabase } from '../src/db/index.js';
import { seed } from '../src/seed.js';
import { Marketplace } from '../src/services/marketplace.js';
import { distanceKm } from '../src/services/geo.js';
import { verifySignature, persistLeads } from '../src/services/meta.js';
import { createApp } from '../src/app.js';
const customer={id:1,role:'customer'},provider={id:2,role:'provider'},provider2={id:3,role:'provider'};
const input={category_id:1,address:'Demo address, Manila',lat:14.5995,lng:120.9842};
function fixture(t) {
  const db=openDatabase(':memory:');let now=Date.now();seed(db,now);
  const market=new Marketplace(db,()=>now);t.after(()=>db.close());
  return {db,market,advance:ms=>{now+=ms;}};
}
test('pricing comes from catalogue and duplicate request returns same job',t=>{
  const {market}=fixture(t);
  const job=market.createJob(customer,{...input,price_minor:1},'booking-1');
  assert.equal(job.price_minor,150000);assert.equal(job.currency,'PHP');
  assert.equal(market.createJob(customer,input,'booking-1').id,job.id);
  assert.throws(()=>market.createJob(customer,{...input,address:'Changed'},'booking-1'),{status:409});
});
test('only current offered provider can claim job and repeat acceptance fails',t=>{
  const {market,db}=fixture(t);const job=market.createJob(customer,input,'a');
  const offer=market.listOffers(provider)[0];assert.equal(offer.job_id,job.id);
  assert.throws(()=>market.respond(provider2,offer.id,'accept'),{status:403});
  assert.equal(db.prepare('SELECT status FROM offers WHERE id=?').get(offer.id).status,'pending');
  assert.equal(market.respond(provider,offer.id,'accept').provider_id,1);
  assert.throws(()=>market.respond(provider,offer.id,'accept'),{status:409});
});
test('timeout rotates to next provider and expires exhausted queue',t=>{
  const {market,advance,db}=fixture(t);const job=market.createJob(customer,input,'a');
  const old=market.listOffers(provider)[0];advance(120001);
  assert.throws(()=>market.respond(provider,old.id,'accept'),{status:409});
  market.tick();assert.equal(market.listOffers(provider2).length,1);
  advance(120001);market.tick();assert.equal(db.prepare('SELECT status FROM jobs WHERE id=?').get(job.id).status,'expired');
});
test('reject advances dispatch without disclosing the address',t=>{
  const {market}=fixture(t);market.createJob(customer,input,'a');
  const result=market.respond(provider,market.listOffers(provider)[0].id,'reject');
  assert.equal(result.address,undefined);assert.equal(market.listOffers(provider2).length,1);
});
test('busy provider cannot accept a second job',t=>{
  const {market}=fixture(t);market.createJob(customer,input,'a');market.createJob(customer,input,'b');
  const offers=market.listOffers(provider);market.respond(provider,offers[0].id,'accept');
  assert.throws(()=>market.respond(provider,offers[1].id,'accept'),{status:409});
});
test('provider approval cannot be changed by location updates',t=>{
  const {market,db}=fixture(t);db.prepare('UPDATE providers SET approved=0 WHERE id=1').run();
  market.updateProvider(provider,{active:true,lat:input.lat,lng:input.lng,approved:true});
  market.createJob(customer,input,'a');assert.equal(market.listOffers(provider).length,0);
  assert.equal(market.listOffers(provider2).length,1);
});
test('stale GPS excludes providers',t=>{
  const {market,advance}=fixture(t);advance(600001);
  assert.equal(market.createJob(customer,input,'a').status,'expired');
});
test('job transitions enforce ownership and order',t=>{
  const {market,db}=fixture(t);const job=market.createJob(customer,input,'a');
  assert.throws(()=>market.getJob(job.id,provider2),{status:403});
  market.respond(provider,market.listOffers(provider)[0].id,'accept');
  assert.throws(()=>market.transition(provider,job.id,'completed'),{status:409});
  assert.throws(()=>market.transition(customer,job.id,'in_transit'),{status:403});
  for(const status of ['in_transit','in_progress','completed']) assert.equal(market.transition(provider,job.id,status).status,status);
  assert.equal(db.prepare('SELECT count(*) AS n FROM payments').get().n,0);
  assert.equal(db.prepare('SELECT count(*) AS n FROM audit_events WHERE job_id=?').get(job.id).n,6);
});
test('cancellation withdraws offer and blocks late acceptance',t=>{
  const {market}=fixture(t);const job=market.createJob(customer,input,'a');const offer=market.listOffers(provider)[0];
  market.transition(customer,job.id,'cancelled');assert.equal(market.listOffers(provider).length,0);
  assert.throws(()=>market.respond(provider,offer.id,'accept'),{status:409});
});
test('invalid input rolls back creation',t=>{
  const {market,db}=fixture(t);
  for(const lat of [NaN,91,'14.5',null]) assert.throws(()=>market.createJob(customer,{...input,lat},'a'),{status:400});
  assert.throws(()=>market.createJob(provider,input,'a'),{status:403});
  assert.equal(db.prepare('SELECT count(*) AS n FROM jobs').get().n,0);
});
test('offers survive connection restart',t=>{
  const dir=mkdtempSync(join(tmpdir(),'linkod-'));t.after(()=>rmSync(dir,{recursive:true,force:true}));
  const path=join(dir,'app.db');let now=Date.now();let db=openDatabase(path);seed(db,now);
  let market=new Marketplace(db,()=>now);const job=market.createJob(customer,input,'a');db.close();
  db=openDatabase(path);market=new Marketplace(db,()=>now);now+=120001;market.tick();
  assert.equal(market.listOffers(provider2)[0].job_id,job.id);db.close();
});
test('geographic distance handles dateline and antipodes',()=>{
  assert.equal(distanceKm(0,0,0,0),0);
  assert.ok(distanceKm(0,179.99,0,-179.99)<3);
  assert.ok(Number.isFinite(distanceKm(0,0,0,180)));
});
const lead={object:'page',entry:[{id:'page-1',changes:[{field:'leadgen',value:{leadgen_id:'lead-1',form_id:'form-1'}}]}]};
test('raw-body signature verification rejects malformed input without throwing',()=>{
  const raw=Buffer.from(JSON.stringify(lead)),secret='test-secret';
  const signature='sha256='+createHmac('sha256',secret).update(raw).digest('hex');
  assert.equal(verifySignature(raw,signature,secret),true);
  for(const value of [undefined,'sha256=x','sha256='+'a'.repeat(64)]) assert.equal(verifySignature(raw,value,secret),false);
  assert.equal(verifySignature(Buffer.from('changed'),signature,secret),false);
});
test('lead inbox is idempotent and malformed batch is atomic',t=>{
  const {db}=fixture(t);assert.equal(persistLeads(db,lead),1);assert.equal(persistLeads(db,lead),0);
  const bad={object:'page',entry:[{changes:[{field:'leadgen',value:{leadgen_id:'new'}},{field:'leadgen',value:{}}]}]};
  assert.throws(()=>persistLeads(db,bad),{status:400});
  assert.equal(db.prepare('SELECT count(*) AS n FROM webhook_inbox').get().n,1);
});
test('HTTP authentication, booking, payments fail closed, webhook durability',async t=>{
  const {db}=fixture(t);const {server}=createApp({db,demoAuth:true,metaSecret:'secret'});
  await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
  t.after(()=>new Promise(resolve=>{server.close(resolve);server.closeAllConnections();}));
  const base=`http://127.0.0.1:${server.address().port}`;
  assert.equal((await fetch(base+'/health')).status,200);
  assert.equal((await fetch(base+'/api/jobs')).status,401);
  const response=await fetch(base+'/api/jobs',{method:'POST',headers:{authorization:'Bearer demo-customer','idempotency-key':'http-a'},body:JSON.stringify(input)});
  assert.equal(response.status,201);const job=await response.json();
  assert.equal((await fetch(base+`/api/jobs/${job.id}/payment`,{method:'POST',headers:{authorization:'Bearer demo-customer'}})).status,503);
  const raw=JSON.stringify(lead),signature='sha256='+createHmac('sha256','secret').update(raw).digest('hex');
  assert.equal((await fetch(base+'/api/webhooks/meta',{method:'POST',body:raw,headers:{'x-hub-signature-256':'sha256=x'}})).status,401);
  assert.equal((await fetch(base+'/api/webhooks/meta',{method:'POST',body:raw,headers:{'x-hub-signature-256':signature}})).status,200);
  assert.equal(db.prepare('SELECT count(*) AS n FROM webhook_inbox').get().n,1);
});
test('production rejects demo authentication; default auth is closed',async t=>{
  const {db}=fixture(t);assert.throws(()=>createApp({db,demoAuth:true,nodeEnv:'production'}));
  const {server}=createApp({db});await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
  t.after(()=>new Promise(resolve=>{server.close(resolve);server.closeAllConnections();}));
  const response=await fetch(`http://127.0.0.1:${server.address().port}/api/jobs`,{headers:{authorization:'Bearer demo-customer'}});
  assert.equal(response.status,401);
});
