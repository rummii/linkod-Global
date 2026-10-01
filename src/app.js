import { readFileSync } from 'node:fs';
import { createServer } from 'node:http';
import { randomUUID } from 'node:crypto';
import { Marketplace } from './services/marketplace.js';
import { persistLeads, verifySignature } from './services/meta.js';
import { DisabledPayments } from './services/payments.js';
import { HttpError, requireCondition } from './errors.js';

async function readBody(req) {
  const chunks=[]; let size=0;
  for await(const chunk of req) {
    size+=chunk.length;
    if(size>65536) throw new HttpError(413,'Request body too large');
    chunks.push(chunk);
  }
  return Buffer.concat(chunks);
}
function parse(raw) {
  try {
    const body=JSON.parse(raw.toString('utf8'));
    requireCondition(body && typeof body==='object' && !Array.isArray(body),400,'Expected JSON object');
    return body;
  } catch(error) { if(error instanceof HttpError) throw error; throw new HttpError(400,'Invalid JSON'); }
}

export function createApp({db,demoAuth=false,nodeEnv='development',metaSecret='',metaVerifyToken='',authenticate,clock=Date.now}) {
  requireCondition(!(demoAuth && nodeEnv==='production'),500,'Demo authentication cannot run in production');
  const marketplace=new Marketplace(db,clock),payments=new DisabledPayments();
  const limits=new Map();
  const server=createServer(async(req,res)=>{
    const requestId=randomUUID();
    res.setHeader('Content-Type','application/json'); res.setHeader('X-Request-Id',requestId);
    res.setHeader('X-Content-Type-Options','nosniff'); res.setHeader('Cache-Control','no-store');
    const send=(status,body)=>{res.writeHead(status);res.end(JSON.stringify(body));};
    try {
      const url=new URL(req.url,'http://localhost'),path=url.pathname,method=req.method;
      const assets={'/':['index.html','text/html; charset=utf-8'],'/app.js':['app.js','text/javascript; charset=utf-8'],'/style.css':['style.css','text/css; charset=utf-8'],'/favicon.svg':['favicon.svg','image/svg+xml'],'/mobile.js':['mobile.js','text/javascript; charset=utf-8'],'/mobile.css':['mobile.css','text/css; charset=utf-8'],'/leaflet.js':['leaflet.js','text/javascript; charset=utf-8'],'/leaflet.css':['leaflet.css','text/css; charset=utf-8'],'/workspace':['workspace.html','text/html; charset=utf-8'],'/workspace.js':['workspace.js','text/javascript; charset=utf-8'],'/workspace.css':['workspace.css','text/css; charset=utf-8']};
      if(method==='GET' && assets[path]) {
        const [file,type]=assets[path];
        res.setHeader('Content-Type',type);
        res.setHeader('Content-Security-Policy',"default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https://tile.openstreetmap.org; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'");
        res.writeHead(200);return res.end(readFileSync(new URL('../public/'+file,import.meta.url)));
      }
      const address=req.socket.remoteAddress,now=clock();
      if(limits.size>10000) limits.clear();
      const bucket=limits.get(address);
      if(!bucket || bucket.until<=now) limits.set(address,{count:1,until:now+60000});
      else { bucket.count++; requireCondition(bucket.count<=120,429,'Too many requests'); }
      if(method==='GET' && path==='/health') return send(200,{status:'ok',service:'linkod-global',version:'0.1.0'});
      if(method==='GET' && path==='/api/categories') return send(200,db.prepare('SELECT id,slug,name,base_price_minor,currency FROM categories WHERE enabled=1').all());
      if(path==='/api/webhooks/meta' && method==='GET') {
        requireCondition(metaVerifyToken && url.searchParams.get('hub.mode')==='subscribe' && url.searchParams.get('hub.verify_token')===metaVerifyToken,403,'Verification failed');
        const challenge=url.searchParams.get('hub.challenge'); requireCondition(challenge,400,'Missing challenge');
        res.setHeader('Content-Type','text/plain');res.writeHead(200);return res.end(challenge);
      }
      if(path==='/api/webhooks/meta' && method==='POST') {
        requireCondition(metaSecret,503,'Meta webhook is not configured');
        const raw=await readBody(req);
        requireCondition(verifySignature(raw,req.headers['x-hub-signature-256'],metaSecret),401,'Invalid webhook signature');
        const inserted=persistLeads(db,parse(raw),clock()); // durable write before ACK
        return send(200,{received:true,inserted});
      }
      let actor;
      if(authenticate) actor=await authenticate(req);
      else if(demoAuth) {
        // This allowlist is strictly a local development convenience.
        const identities={'Bearer demo-customer':1,'Bearer demo-provider':2,'Bearer demo-provider-2':3,'Bearer demo-admin':4};
        const id=identities[req.headers.authorization];
        if(id) actor=db.prepare('SELECT id,role FROM users WHERE id=?').get(id);
      }
      requireCondition(actor && Number.isSafeInteger(actor.id) && ['customer','provider','admin'].includes(actor.role),401,'Authentication required');
      if(method==='GET' && path==='/api/jobs') {
        const jobs=actor.role==='admin'?db.prepare('SELECT * FROM jobs ORDER BY created_at DESC LIMIT 100').all():
          actor.role==='customer'?db.prepare('SELECT * FROM jobs WHERE customer_id=? ORDER BY created_at DESC LIMIT 100').all(actor.id):
          db.prepare('SELECT j.* FROM jobs j JOIN providers p ON j.provider_id=p.id WHERE p.user_id=? ORDER BY j.created_at DESC LIMIT 100').all(actor.id);
        return send(200,jobs);
      }
      if(method==='POST' && path==='/api/jobs') return send(201,marketplace.createJob(actor,parse(await readBody(req)),req.headers['idempotency-key']));
      if(method==='GET' && path==='/api/providers/me/offers') return send(200,marketplace.listOffers(actor));
      if(method==='PATCH' && path==='/api/providers/me/location') return send(200,marketplace.updateProvider(actor,parse(await readBody(req))));
      if(method==='GET' && path==='/api/admin/webhook-inbox') {
        requireCondition(actor.role==='admin',403,'Admin role required');
        return send(200,db.prepare('SELECT id,source,external_id,status,attempts,received_at FROM webhook_inbox ORDER BY received_at DESC LIMIT 100').all());
      }
      let match=path.match(/^\/api\/offers\/([^/]+)\/(accept|reject)$/);
      if(method==='POST' && match) return send(200,marketplace.respond(actor,match[1],match[2]));
      match=path.match(/^\/api\/jobs\/([^/]+)$/);
      if(method==='GET' && match) return send(200,marketplace.getJob(match[1],actor));
      if(method==='PATCH' && match) return send(200,marketplace.transition(actor,match[1],parse(await readBody(req)).status));
      match=path.match(/^\/api\/jobs\/([^/]+)\/payment$/);
      if(method==='POST' && match) { marketplace.getJob(match[1],actor); await payments.authorize(); }
      throw new HttpError(404,'Route not found');
    } catch(error) {
      if(!(error instanceof HttpError)) console.error(JSON.stringify({requestId,event:'request_failed',type:error.name}));
      if(!res.headersSent) send(error.status??500,{error:error instanceof HttpError?error.message:'Internal server error',requestId});
      else res.end();
    }
  });
  server.requestTimeout=15000; server.headersTimeout=10000;
  return {server,marketplace};
}
