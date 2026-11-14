// Local-only end-to-end checks. Start the app with SMTP pointing to 127.0.0.1:1025,
// MAIL_PROVIDER=smtp, SMTP_AUTH=false, SMTP_STARTTLS=false and a local test sender.
// The in-process SMTP sink below never connects to external recipients.
const fs=require('node:fs'),path=require('node:path'),net=require('node:net'),assert=require('node:assert/strict');
const {chromium}=require(path.join(__dirname,'../.local/browser-test/node_modules/playwright'));
const base='http://127.0.0.1:8080',messages=[];
const smtp=net.createServer(socket=>{
  let buffer='',data=false,lines=[],recipient='';socket.setEncoding('utf8');socket.write('220 localhost TrackEase test sink\r\n');
  socket.on('data',chunk=>{buffer+=chunk;let index;while((index=buffer.indexOf('\r\n'))>=0){const line=buffer.slice(0,index);buffer=buffer.slice(index+2);
    if(data){if(line==='.') {messages.push({recipient,body:lines.join('\n').replace(/=\n/g,'').replace(/=([0-9A-F]{2})/g,(_,hex)=>String.fromCharCode(parseInt(hex,16)))});data=false;lines=[];socket.write('250 captured locally\r\n');}else lines.push(line.startsWith('..')?line.slice(1):line);continue;}
    if(/^EHLO|^HELO/i.test(line))socket.write('250-localhost\r\n250 8BITMIME\r\n');
    else if(/^RCPT TO:/i.test(line)){recipient=line;socket.write('250 OK\r\n');}
    else if(/^DATA$/i.test(line)){data=true;socket.write('354 End with dot\r\n');}
    else if(/^QUIT$/i.test(line)){socket.end('221 Bye\r\n');}
    else socket.write('250 OK\r\n');
  }});socket.on('error',()=>{});
});
(async()=>{
  await new Promise((resolve,reject)=>smtp.listen(1025,'127.0.0.1',resolve).once('error',reject));
  const browser=await chromium.launch({headless:true,executablePath:['C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe','C:/Program Files/Google/Chrome/Application/chrome.exe'].find(fs.existsSync)});
  try {
    const admin=await browser.newContext({viewport:{width:1440,height:1000}}),user=await browser.newContext({viewport:{width:1440,height:1000}}),other=await browser.newContext();
    const errors=[];const page=await user.newPage(),adminPage=await admin.newPage();for(const p of [page,adminPage])p.on('pageerror',e=>errors.push(e.message));
    async function call(context,url,method='GET',data,status=200){
      const headers={};if(method!=='GET'){const r=await context.request.get(base+'/api/auth/csrf');headers['X-CSRF-Token']=(await r.json()).token;}
      const r=await context.request.fetch(base+url,{method,headers,data});assert.equal(r.status(),status,`${method} ${url}: ${await r.text()}`);return r;
    }
    const tag=Date.now().toString(36),username='feature'+tag,password='FeaturePassword123!',email=username+'@example.test';
    await call(admin,'/api/auth/login','POST',{username:'admin',password:'Admin123!'});
    const mailStatus=await (await call(admin,'/api/admin/email-status')).json();assert.equal(mailStatus.configured,true,'Run the app against the local SMTP sink');assert.equal(mailStatus.provider,'smtp');
    const stations=[];
    for(let i=0;i<12;i++)await call(admin,'/api/admin/stations','POST',{stationCode:('F'+tag+i).toUpperCase(),stationName:'Feature '+tag+' stop '+i,city:'Local',state:'Test'},201);
    stations.push(...(await (await call(admin,'/api/admin/stations')).json()).filter(s=>s.stationName.startsWith('Feature '+tag)).sort((a,b)=>a.stationId-b.stationId));
    await call(admin,'/api/admin/trains','POST',{trainNumber:'F'+tag,trainName:'Feature Route '+tag,totalSeats:10},201);
    const train=(await (await call(admin,'/api/admin/trains')).json()).find(t=>t.trainNumber==='F'+tag);
    const date=new Date();date.setDate(date.getDate()+1);const day=`${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`;
    await call(admin,'/api/admin/journeys','POST',{trainId:train.trainId,departureStationId:stations[0].stationId,arrivalStationId:stations[2].stationId,departureTime:day+'T08:00:00',arrivalTime:day+'T18:00:00',fare:1000,availableSeats:10},201);
    const journey=(await (await call(admin,'/api/admin/journeys')).json()).find(j=>j.trainId===train.trainId),id=journey.journeyId;
    // Configure the route through the actual administrator form.
    await adminPage.goto(base+'/admin/routePlanner.jsp');await adminPage.locator('#plannerJourney').selectOption(String(id));
    await adminPage.getByRole('button',{name:'Add intermediate stop'}).click();const stop=adminPage.locator('[data-stop-row]');
    await stop.locator('[data-station]').selectOption(String(stations[1].stationId));await stop.locator('[data-arrival]').fill(day+'T13:00');await stop.locator('[data-departure]').fill(day+'T13:10');await stop.locator('[data-cumulative-fare]').fill('400');
    const coach=adminPage.locator('[data-coach-row]').first();await coach.locator('[data-travel-class]').selectOption('SLEEPER');await coach.locator('[data-seats]').fill('8');await coach.locator('[data-rac-berths]').fill('1');
    await adminPage.getByRole('button',{name:'Add coach',exact:true}).click();const ac=adminPage.locator('[data-coach-row]').nth(1);await ac.locator('[data-coach]').fill('A1');await ac.locator('[data-travel-class]').selectOption('AC2');await ac.locator('[data-seats]').fill('2');await ac.locator('[data-multiplier]').fill('2');
    await adminPage.getByRole('button',{name:'Save route & coach plan'}).click();await adminPage.locator('[data-feature-message]').filter({hasText:'Route and coaches configured'}).waitFor();
    const plan=await (await call(admin,`/api/journeys/${id}/plan`)).json();assert.equal(plan.stops.length,3);assert.equal(plan.classes.length,2);
    await adminPage.screenshot({path:path.join(__dirname,'../.local/features-route-planner.png'),fullPage:true});
    // Server-side filtering and pagination, including a real second page.
    await adminPage.goto(base+'/admin/catalog.jsp');await adminPage.locator('#resourceType').selectOption('stations');await adminPage.locator('#catalogQuery').fill('Feature '+tag);await adminPage.getByRole('button',{name:'Search',exact:true}).click();
    await adminPage.locator('[data-catalog-count]').filter({hasText:'12 matching records'}).waitFor();await adminPage.getByRole('button',{name:'Next →',exact:true}).click();
    await adminPage.locator('[data-catalog-page]').filter({hasText:'Page 2 of 2'}).waitFor();assert.equal(await adminPage.locator('[data-catalog-body] tr').count(),2);
    await adminPage.screenshot({path:path.join(__dirname,'../.local/features-catalog.png'),fullPage:true});
    await call(user,'/api/auth/register','POST',{username,password,email,fullName:'Feature Passenger'},201);await call(user,'/api/auth/login','POST',{username,password});
    await call(other,'/api/auth/register','POST',{username:username+'b',password,email:username+'b@example.test',fullName:'Other Passenger'},201);await call(other,'/api/auth/login','POST',{username:username+'b',password});
    await call(user,'/api/admin/catalog','GET',undefined,403);assert.equal((await (await call(user,'/api/journeys/provider')).json()).configured,false);
    await page.goto(base+'/user/reservation.jsp?journeyId='+id);await page.locator('[data-plan-from] option').first().waitFor({state:'attached'});await page.locator('[data-plan-to]').selectOption('1');await page.locator('[data-plan-class]').selectOption('SLEEPER');
    await page.waitForFunction(()=>document.querySelector('[data-plan-fare]').textContent.includes('400'));
    await page.locator('[data-passenger-count]').selectOption('2');await page.locator('[name=passengerName]').nth(0).fill('Asha Rao');await page.locator('[name=passengerName]').nth(1).fill('Ravi Shah');
    await page.locator('[data-seat-map][aria-busy=false]').waitFor();
    await page.locator('[data-seat-key="S1:1"]').click();await page.locator('[data-seat-key="S1:3"]').click();
    const heldResponse=page.waitForResponse(r=>r.url().endsWith('/api/bookings/holds')&&r.request().method()==='POST');await page.getByRole('button',{name:'Hold selected seats'}).click();const held=await (await heldResponse).json();assert.ok(held.holdId);
    await page.locator('[data-hold-status]').filter({hasText:'Seats held for'}).waitFor();
    await call(other,'/api/bookings/holds','POST',{journeyId:id,from:0,to:1,travelClass:'SLEEPER',seats:['S1:1']},409);
    await page.screenshot({path:path.join(__dirname,'../.local/features-seat-map-desktop.png'),fullPage:true});await page.setViewportSize({width:390,height:844});
    assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1));await page.screenshot({path:path.join(__dirname,'../.local/features-seat-map-mobile.png'),fullPage:true});
    await page.getByRole('button',{name:'Confirm passenger tickets'}).click();await page.waitForURL('**/user/bookingConfirmation.jsp?bookingId=*');const bookingId=new URL(page.url()).searchParams.get('bookingId');
    const group=await (await call(user,'/api/bookings/group/'+bookingId)).json();assert.equal(group.tickets.length,2);assert.equal(group.tickets[0].journey.fare,400);assert.equal(group.tickets[0].travelClass,'SLEEPER');
    const download=page.waitForEvent('download');await page.getByRole('link',{name:'Download booking PDF'}).click();const pdf=await download;await pdf.saveAs(path.join(__dirname,'../.local/features-booking.pdf'));assert.ok(fs.readFileSync(path.join(__dirname,'../.local/features-booking.pdf')).subarray(0,5).equals(Buffer.from('%PDF-')));
    await call(other,`/api/bookings/${group.tickets[0].ticketId}/pdf`,'GET',undefined,404);
    // A different passenger may use the same berth on the following, non-overlapping leg.
    const nextHold=await (await call(other,'/api/bookings/holds','POST',{journeyId:id,from:1,to:2,travelClass:'SLEEPER',seats:['S1:1']})).json();
    const book=(overrides={})=>({journeyId:id,from:0,to:2,travelClass:'SLEEPER',passengers:['Passenger'],preference:'LOWER',allowRac:false,joinWaitingList:false,requestId:require('node:crypto').randomUUID(),...overrides});
    const segment=await (await call(other,'/api/bookings/advanced','POST',book({from:1,to:2,holdId:nextHold.holdId}))).json();assert.equal(segment.tickets[0].seatNumber,'1');assert.equal(segment.tickets[0].journey.fare,600);
    await call(other,'/api/bookings/holds','POST',{journeyId:id,from:0,to:2,travelClass:'SLEEPER',seats:['S1:1']},409);
    const fill=await (await call(user,'/api/bookings/advanced','POST',book({passengers:['One','Two','Three','Four','Five']}))).json();
    const rac=await (await call(user,'/api/bookings/advanced','POST',book({passengers:['RAC One','RAC Two'],allowRac:true}))).json();assert.deepEqual(rac.tickets.map(t=>t.status),['RAC','RAC']);assert.equal(rac.tickets[0].seatNumber,rac.tickets[1].seatNumber);
    const waiting=await (await call(user,'/api/bookings/advanced','POST',book({allowRac:true,joinWaitingList:true}))).json();assert.equal(waiting.tickets[0].status,'WAITLISTED');
    await call(user,'/api/bookings/'+fill.tickets[0].ticketId,'DELETE');assert.equal((await (await call(user,'/api/bookings/'+rac.tickets[0].ticketId)).json()).status,'CONFIRMED');assert.equal((await (await call(user,'/api/bookings/'+waiting.tickets[0].ticketId)).json()).status,'RAC');
    await page.goto(base+'/user/ticketConfirmation.jsp?ticketId='+rac.tickets[1].ticketId);assert.match(await page.locator('.ticket-header').textContent(),/shared berth/i);await page.screenshot({path:path.join(__dirname,'../.local/features-rac-ticket.png'),fullPage:true});
    // Capture real JavaMail traffic locally: no provider credentials or external addresses are contacted.
    async function waitForMail(predicate){for(let n=0;n<200;n++){const m=messages.find(predicate);if(m)return m;await page.waitForTimeout(200);}throw new Error('Expected email was not captured by local SMTP sink');}
    await waitForMail(m=>m.recipient.includes(email)&&m.body.includes('CONFIRMED'));await waitForMail(m=>m.recipient.includes(email)&&m.body.includes('PROMOTED'));
    // Clear reservations before exercising credential changes; history/audit records remain.
    for(const context of [user,other]){const history=await (await call(context,'/api/bookings/history?size=100')).json();for(const t of history.items)if(t.status!=='CANCELLED')await call(context,'/api/bookings/'+t.ticket_id,'DELETE');}
    await page.goto(base+'/user/security.jsp');await page.locator('#currentPassword').fill(password);await page.locator('[data-generate-recovery] [type=checkbox]').check();await page.getByRole('button',{name:'Generate recovery codes'}).click();await page.locator('[data-recovery-output]').waitFor();const code=(await page.locator('#recoveryCodes').inputValue()).split('\n')[0];
    const anonymous=await browser.newContext(),recoveryPage=await anonymous.newPage();await recoveryPage.goto(base+'/recover.jsp');await recoveryPage.locator('#recoveryUsername').fill(username);await recoveryPage.locator('#recoveryCode').fill(code);await recoveryPage.locator('#newPassword').fill('RecoveredPassword123!');await recoveryPage.getByRole('button',{name:'Reset password',exact:true}).click();await recoveryPage.locator('[data-feature-message]').filter({hasText:'Password reset.'}).waitFor();
    await call(user,'/api/auth/me','GET',undefined,401);await call(anonymous,'/api/auth/login','POST',{username,password},401);await call(anonymous,'/api/auth/login','POST',{username,password:'RecoveredPassword123!'});
    // Email-based reset, including the actual link, page and token consumption.
    await recoveryPage.goto(base+'/recover.jsp');await recoveryPage.locator('#resetUsername').fill(username);await recoveryPage.getByRole('button',{name:'Email reset link'}).click();
    const emailReset=await waitForMail(m=>m.recipient.includes(email)&&m.body.includes('#token='));const token=emailReset.body.match(/#token=([0-9a-f]{64})/)[1];
    await recoveryPage.goto(base+'/reset-password.jsp#token='+token);await recoveryPage.locator('#resetPassword').fill('EmailResetPassword123!');await recoveryPage.getByRole('button',{name:'Reset password',exact:true}).click();await recoveryPage.locator('[data-feature-message]').filter({hasText:'Password reset.'}).waitFor();assert.ok(!recoveryPage.url().includes(token));
    await call(anonymous,'/api/auth/password-reset/complete','POST',{token,newPassword:'ReusePassword123!'},400);await call(anonymous,'/api/auth/login','POST',{username,password:'EmailResetPassword123!'});
    await call(admin,'/api/admin/trains/'+train.trainId,'DELETE');for(const station of stations)await call(admin,'/api/admin/stations/'+station.stationId,'DELETE');
    assert.deepEqual(errors,[],'No new page JavaScript errors');
    console.log('PASS: route/coach admin UI, server-side pagination, class fares, segment reuse, selected-seat holds, overlapping-seat rejection, RAC sharing/promotion, PDF download/ownership, recovery codes/session revocation, real password-reset email captured locally, and single-use reset links.');
  }finally{await browser.close();smtp.close();}
})().catch(e=>{console.error(e);smtp.close();process.exit(1);});
