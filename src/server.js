import { openDatabase } from './db/index.js';
import { createApp } from './app.js';
const demoAuth=process.env.DEMO_AUTH==='true',nodeEnv=process.env.NODE_ENV??'development';
if(demoAuth && nodeEnv==='production') throw new Error('Demo authentication is prohibited in production');
const port=Number(process.env.PORT??3000);
if(!Number.isSafeInteger(port) || port<1 || port>65535) throw new Error('Invalid PORT');
const db=openDatabase();
const {server,marketplace}=createApp({db,demoAuth,nodeEnv,metaSecret:process.env.META_APP_SECRET,metaVerifyToken:process.env.META_VERIFY_TOKEN});
marketplace.tick();
const timer=setInterval(()=>{
  try { marketplace.tick(); } catch { console.error(JSON.stringify({event:'dispatch_tick_failed'})); }
},1000);
timer.unref();
server.listen(port,process.env.HOST??'127.0.0.1',()=>console.log(JSON.stringify({event:'listening',port,demoAuth})));
let stopping=false;
function shutdown() {
  if(stopping) return; stopping=true; clearInterval(timer);
  server.close(()=>{db.close();process.exit(0);});
  setTimeout(()=>{server.closeAllConnections();},5000).unref();
}
process.on('SIGTERM',shutdown);process.on('SIGINT',shutdown);
