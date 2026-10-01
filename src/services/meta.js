import { createHmac, timingSafeEqual } from 'node:crypto';
import { transaction } from '../db/index.js';
import { requireCondition } from '../errors.js';
export function verifySignature(raw,header,secret) {
  if(!secret || typeof header!=='string' || !/^sha256=[a-f0-9]{64}$/.test(header)) return false;
  const expected=createHmac('sha256',secret).update(raw).digest();
  return timingSafeEqual(expected,Buffer.from(header.slice(7),'hex'));
}
export function persistLeads(db,payload,now=Date.now()) {
  requireCondition(payload.object==='page' && Array.isArray(payload.entry),400,'Invalid Meta webhook');
  return transaction(db,()=>{
    let inserted=0;
    for(const entry of payload.entry) {
      requireCondition(entry && typeof entry==='object' && (!entry.changes || Array.isArray(entry.changes)),400,'Invalid Meta entry');
      for(const change of entry.changes??[]) {
        if(change?.field!=='leadgen') continue;
        const id=change.value?.leadgen_id;
        requireCondition(typeof id==='string' && id.length>0 && id.length<=128,400,'Invalid lead ID');
        inserted+=Number(db.prepare(`INSERT OR IGNORE INTO webhook_inbox(source,external_id,payload,available_at,received_at)
          VALUES('meta',?,?,?,?)`).run(id,JSON.stringify({page_id:entry.id,...change.value}),now,now).changes);
      }
    }
    return inserted;
  });
}
