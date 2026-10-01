// Dedicated local-only demo launcher. Never enables demo identities in production.
if (process.env.NODE_ENV === 'production') throw new Error('Demo launcher cannot run in production');
process.env.HOST=process.argv.includes('--lan')?'0.0.0.0':'127.0.0.1';
process.env.DEMO_AUTH='true';
process.env.DB_PATH ??= './data/linkod-mobile-demo.db';
const {openDatabase}=await import('./db/index.js');
const {seed}=await import('./seed.js');
const db=openDatabase();seed(db);
// Demo providers can demonstrate every catalogue service. Refresh GPS each launch.
db.exec('INSERT OR IGNORE INTO provider_skills(provider_id,category_id) SELECT p.id,c.id FROM providers p CROSS JOIN categories c WHERE p.user_id IN (2,3)');
db.prepare('UPDATE providers SET lat=14.5995,lng=120.9842,last_seen=?,active=1 WHERE user_id IN (2,3)').run(Date.now());
db.close();
await import('./server.js');

if(process.argv.includes('--lan')) {
 const {networkInterfaces}=await import('node:os');
 for(const entries of Object.values(networkInterfaces())) for(const entry of entries??[]) if(entry.family==='IPv4'&&!entry.internal) console.log('Phone demo: http://'+entry.address+':'+(process.env.PORT??3000));
 console.log('Use only on a trusted local network. Demo roles have no passwords.');
}
