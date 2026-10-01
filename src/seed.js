import { pathToFileURL } from 'node:url';
import { openDatabase, transaction } from './db/index.js';
export function seed(db,now=Date.now()) {
  transaction(db,()=>{
    const user=db.prepare('INSERT OR IGNORE INTO users(id,role,name) VALUES(?,?,?)');
    for(const row of [[1,'customer','Demo Customer'],[2,'provider','Demo Provider'],[3,'provider','Demo Provider 2'],[4,'admin','Demo Admin']]) user.run(...row);
    const category=db.prepare('INSERT OR IGNORE INTO categories(id,slug,name,base_price_minor,currency) VALUES(?,?,?,?,?)');
    for(const row of [[1,'plumbing','Plumbing',150000,'PHP'],[2,'electrical','Electrical',180000,'PHP'],[3,'aircon','Aircon Cleaning',120000,'PHP']]) category.run(...row);
    const provider=db.prepare('INSERT OR IGNORE INTO providers(id,user_id,active,approved,lat,lng,last_seen) VALUES(?,?,1,1,?,?,?)');
    provider.run(1,2,14.5995,120.9842,now); provider.run(2,3,14.6095,120.9942,now);
    for(const id of [1,2]) db.prepare('INSERT OR IGNORE INTO provider_skills VALUES(?,1)').run(id);
  });
}
if(process.argv[1] && import.meta.url===pathToFileURL(process.argv[1]).href) {
  if(process.env.NODE_ENV==='production') throw new Error('Demo seed is prohibited in production');
  const db=openDatabase();seed(db);db.close();console.log('Seeded local demo users, providers and PHP service prices.');
}
