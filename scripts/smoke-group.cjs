// Optional Playwright harness: npm install --prefix .local/browser-test playwright
const path=require('node:path'),fs=require('node:fs'),assert=require('node:assert/strict');
const {chromium}=require(path.join(__dirname,'../.local/browser-test/node_modules/playwright'));
const base='http://127.0.0.1:8080';
(async()=>{
  const browser=await chromium.launch({headless:true,executablePath:['C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe','C:/Program Files/Google/Chrome/Application/chrome.exe'].find(fs.existsSync)});
  try {
    const page=await browser.newPage({viewport:{width:1440,height:1000}});const errors=[];
    page.on('pageerror',e=>errors.push(e.message));
    await page.goto(base+'/login.jsp');await page.locator('#username').fill('demo');await page.locator('#password').fill('Demo123!');
    await page.getByRole('button',{name:'Login',exact:true}).click();await page.waitForURL('**/user/userDashboard.jsp');
    await page.goto(base+'/user/bookTicket.jsp');await page.locator('#departureStation').selectOption('1');await page.locator('#arrivalStation').selectOption('2');
    await page.getByRole('button',{name:'Tomorrow',exact:true}).click();await page.getByRole('button',{name:'Find trains'}).click();
    await page.locator('.journey-card').filter({hasText:'TrackEase Demo Express'}).getByRole('button').click();
    const modal=page.locator('.modal.show');await modal.waitFor();
    const journeyId=Number(await modal.locator('input[name=journeyId]').inputValue());
    const requestId=await modal.locator('input[name=bookingRequestId]').inputValue();
    await modal.locator('[data-passenger-count]').selectOption('2');
    assert.equal(await modal.locator('input[name=passengerName]').count(),2);
    await modal.locator('input[name=passengerName]').nth(0).fill('Asha Rao');await modal.locator('input[name=passengerName]').nth(1).fill('Ravi Shah');
    assert.match(await modal.locator('[data-passenger-summary]').textContent(),/2 passengers/);
    const unitFare=Number(await modal.locator('[data-group-passengers]').getAttribute('data-unit-fare'));
    assert.equal(await modal.locator('[data-group-total]').textContent(),new Intl.NumberFormat('en-IN',{style:'currency',currency:'INR'}).format(unitFare*2));
    // Changing the count must retain existing names rather than silently erasing them.
    await modal.locator('[data-passenger-count]').selectOption('3');await modal.locator('[data-passenger-count]').selectOption('2');
    assert.equal(await modal.locator('input[name=passengerName]').nth(1).inputValue(),'Ravi Shah');
    await modal.locator('input[name=passengerName]').nth(1).fill('');
    await modal.getByRole('button',{name:'Confirm booking'}).click();assert.ok(page.url().endsWith('/user/searchJourney'));
    await modal.locator('input[name=passengerName]').nth(1).fill('Ravi Shah');
    await modal.getByRole('button',{name:'Confirm booking'}).click();await page.waitForURL('**/user/bookingConfirmation.jsp?bookingId=*');
    const groupUrl=page.url();assert.equal(new URL(groupUrl).searchParams.get('bookingId'),requestId);
    assert.equal(await page.locator('[data-group-ticket]').count(),2);
    assert.match(await page.locator('.group-ticket-list').textContent(),/Asha Rao/);assert.match(await page.locator('.group-ticket-list').textContent(),/Ravi Shah/);
    await page.screenshot({path:path.join(__dirname,'../.local/group-booking-desktop.png'),fullPage:true});
    await page.setViewportSize({width:390,height:844});assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1));
    await page.screenshot({path:path.join(__dirname,'../.local/group-booking-mobile.png'),fullPage:true});
    await page.emulateMedia({media:'print'});await page.pdf({path:path.join(__dirname,'../.local/group-booking-print.pdf'),format:'A4'});await page.emulateMedia({media:'screen'});
    const token=(await (await page.request.get(base+'/api/auth/csrf')).json()).token;
    const headers={'X-CSRF-Token':token};
    const get=await page.request.get(base+'/api/bookings/group/'+requestId);assert.equal(get.status(),200);const group=await get.json();
    assert.deepEqual(group.tickets.map(t=>t.passengerName),['Asha Rao','Ravi Shah']);
    assert.equal(new Set(group.tickets.map(t=>`${t.coachNumber}-${t.seatNumber}`)).size,2);
    const retry=await page.request.post(base+'/api/bookings/group',{headers,data:{journeyId,requestId,passengerNames:['Asha Rao','Ravi Shah'],joinWaitingList:false}});
    assert.equal(retry.status(),201);assert.deepEqual((await retry.json()).tickets.map(t=>t.ticketId),group.tickets.map(t=>t.ticketId));
    const wrong=await page.request.post(base+'/api/bookings/group',{headers,data:{journeyId,requestId,passengerNames:['Other Passenger']}});assert.equal(wrong.status(),400);
    const invalid=await page.request.post(base+'/api/bookings/group',{headers,data:{journeyId,requestId:require('node:crypto').randomUUID(),passengerNames:Array(7).fill('Passenger')}});assert.equal(invalid.status(),400);
    const noCsrf=await page.request.post(base+'/api/bookings/group',{data:{journeyId,requestId,passengerNames:['Asha Rao','Ravi Shah']}});assert.equal(noCsrf.status(),403);
    await page.goto(base+'/user/ticketConfirmation.jsp?ticketId='+group.tickets[1].ticketId);assert.match(await page.locator('.ticket-details').textContent(),/Ravi Shah/);
    await page.getByRole('link',{name:'View all passengers in this booking'}).click();await page.waitForURL('**/user/bookingConfirmation.jsp?bookingId=*');
    const cancel=await page.request.delete(base+'/api/bookings/'+group.tickets[0].ticketId,{headers});assert.equal(cancel.status(),200);
    const after=await (await page.request.get(base+'/api/bookings/group/'+requestId)).json();assert.deepEqual(after.tickets.map(t=>t.status),['CANCELLED','CONFIRMED']);
    // Leave the local demo inventory as it was; the cancelled records remain for inspection.
    assert.equal((await page.request.delete(base+'/api/bookings/'+group.tickets[1].ticketId,{headers})).status(),200);
    await page.goto(base+'/user/tickets');assert.match(await page.locator('body').textContent(),/Passenger:.*Ravi Shah/s);
    assert.deepEqual(errors,[],'No browser errors');
    await page.request.post(base+'/api/auth/logout',{headers});
    assert.equal((await page.request.get(base+'/api/bookings/group/'+requestId)).status(),401);
    const anon=await page.goto(groupUrl);assert.equal(anon.status(),200);assert.match(page.url(),/login/);
    const adminToken=(await (await page.request.get(base+'/api/auth/csrf')).json()).token;
    assert.equal((await page.request.post(base+'/api/auth/login',{headers:{'X-CSRF-Token':adminToken},data:{username:'admin',password:'Admin123!'}})).status(),200);
    assert.equal((await page.request.get(base+'/api/bookings/group/'+requestId)).status(),404);
    assert.equal((await page.goto(groupUrl)).status(),404);
    console.log('PASS: two named passengers in one UI submission, total fare, distinct seats, desktop/mobile/print summary, API retry/validation/CSRF, ownership, individual cancellation and ticket history');
  } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exit(1);});
