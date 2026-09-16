const fs=require('fs');
const roles=[
 ['boss','18660219918'],['storeManager','13900000001'],['designer','13900000002'],
 ['pm','13900000003'],['finance','13900000004'],['support','13900000005'],
 ['marketer','13900000006'],['teamLeader','13900000007']
];
(async()=>{const out={};for(const [role,phone] of roles){const r=await fetch('http://127.0.0.1:8090/api/auth/session/password-login',{method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify({phone,password:'Maihong123'})});const j=await r.json();if(!j.success)throw new Error(role+' login failed '+JSON.stringify(j));out[role]={phone,token:j.data.token};console.log(role,'ok')}fs.writeFileSync('/private/tmp/maihong-role-tokens.json',JSON.stringify(out,null,2),{mode:0o600});})().catch(e=>{console.error(e);process.exit(1)})
