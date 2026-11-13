// Requires the optional Playwright harness in .local/browser-test (see smoke-swagger.cjs).
const path = require('node:path');
const fs = require('node:fs');
const assert = require('node:assert/strict');
const {chromium} = require(path.join(__dirname,'../.local/browser-test/node_modules/playwright'));
const base='http://127.0.0.1:8080';
(async()=>{
  const executablePath=['C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe','C:/Program Files/Google/Chrome/Application/chrome.exe'].find(fs.existsSync);
  const browser=await chromium.launch({headless:true,executablePath});
  try {
    const page=await browser.newPage({viewport:{width:1440,height:1000}});
    const errors=[],failed=[];
    page.on('pageerror',e=>errors.push(e.message));
    page.on('response',r=>{if(r.status()>=400)failed.push(`${r.status()} ${r.url()}`);});
    // The redesigned JSP interface must work entirely without third-party CDNs.
    await page.route('**/*',route=>new URL(route.request().url()).origin===base?route.continue():route.abort());
    async function visit(route){const r=await page.goto(base+route);assert.equal(r.status(),200,route);await page.locator('h1').waitFor();}
    async function screenshot(name){if(!await page.locator('.modal.show').count()) await page.locator('.modal-backdrop').waitFor({state:'hidden'});await page.evaluate(async()=>{await Promise.all(document.getAnimations().map(a=>a.finished.catch(()=>{})));});await page.screenshot({path:path.join(__dirname,`../.local/ui-${name}.png`),fullPage:true,animations:'disabled'});}
    async function noOverflow(label){assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),`${label}: horizontal page overflow`);}
    await visit('/');await screenshot('home-desktop');await noOverflow('Home desktop');
    await visit('/register.jsp');await screenshot('register-desktop');
    await visit('/login.jsp');await page.locator('#username').fill('demo');await page.locator('#password').fill('Demo123!');
    await page.getByRole('button',{name:'Show or hide password'}).click();assert.equal(await page.locator('#password').getAttribute('type'),'text');
    await page.getByRole('button',{name:'Show or hide password'}).click();
    await page.getByRole('button',{name:'Login',exact:true}).click();await page.waitForURL('**/user/userDashboard.jsp');await screenshot('dashboard-desktop');
    await visit('/user/bookTicket.jsp');
    await page.locator('#departureStation').selectOption('1');await page.locator('#arrivalStation').selectOption('2');
    await page.getByRole('button',{name:'Swap departure and destination'}).click();assert.equal(await page.locator('#departureStation').inputValue(),'2');
    await page.getByRole('button',{name:'Swap departure and destination'}).click();
    await page.locator('#arrivalStation').selectOption('1');assert.equal(await page.locator('#arrivalStation').evaluate(e=>e.validity.valid),false);
    await page.locator('#arrivalStation').selectOption('2');
    await page.getByRole('button',{name:'Tomorrow',exact:true}).click();
    await screenshot('search-desktop');await page.getByRole('button',{name:'Find trains',exact:false}).click();await page.waitForURL('**/user/searchJourney');
    await page.locator('.journey-card').first().waitFor();await screenshot('results-desktop');
    await page.locator('[data-sort-journeys]').selectOption('fare');
    const fares=await page.locator('[data-journey-list]>.journey-card').evaluateAll(cards=>cards.map(c=>Number(c.dataset.fare)));
    assert.deepEqual(fares,[...fares].sort((a,b)=>a-b));
    await page.locator('.journey-card').filter({hasText:'TrackEase Demo Express'}).getByRole('button').click();
    await page.locator('.modal.show').waitFor();await screenshot('booking-desktop');
    await page.locator('.modal.show').getByRole('button',{name:'Go back'}).click();
    await page.setViewportSize({width:390,height:844});await screenshot('results-mobile');await noOverflow('Results mobile');
    await page.locator('.journey-card').filter({hasText:'TrackEase Demo Express'}).getByRole('button').click();await page.locator('.modal.show').waitFor();
    await screenshot('booking-mobile');await noOverflow('Booking mobile');
    await page.locator('.modal.show').getByRole('button',{name:'Go back'}).click();
    await visit('/user/bookTicket.jsp');await screenshot('search-mobile');await noOverflow('Search mobile');
    await page.getByRole('button',{name:'Toggle navigation'}).click();assert.equal(await page.getByRole('button',{name:'Toggle navigation'}).getAttribute('aria-expanded'),'true');
    await page.getByRole('link',{name:'My tickets',exact:true}).first().click();await page.waitForURL('**/user/tickets');
    await screenshot('tickets-mobile');await noOverflow('Tickets mobile');
    await page.getByRole('button',{name:'Cancelled',exact:true}).click();
    for(const card of await page.locator('[data-ticket-status]:visible').all()) assert.equal(await card.getAttribute('data-ticket-status'),'CANCELLED');
    await page.getByRole('button',{name:'All tickets',exact:true}).click();
    await page.locator('.ticket-history-card').first().getByRole('link',{name:'View ticket'}).click();await page.locator('.ticket-card').waitFor();
    await screenshot('ticket-mobile');await noOverflow('Ticket mobile');
    await page.setViewportSize({width:1440,height:1000});await screenshot('ticket-desktop');
    await visit('/user/tickets');await screenshot('tickets-desktop');
    await page.getByRole('button',{name:'Logout',exact:false}).click();await page.waitForURL(/login/);
    await page.locator('#username').fill('admin');await page.locator('#password').fill('Admin123!');await page.getByRole('button',{name:'Login',exact:true}).click();await page.waitForURL('**/admin/adminDashboard.jsp');await screenshot('admin-desktop');
    for(const route of ['/admin/train','/admin/station','/admin/journey','/admin/tickets']){await visit(route);await noOverflow(route);}
    await page.locator('[data-booking-search]').fill('no-matching-booking-xyz');assert.equal(await page.locator('[data-booking-row]:visible').count(),0);
    await page.locator('[data-booking-search]').fill('');assert.ok(await page.locator('[data-booking-row]:visible').count()>0);
    await visit('/admin/train');await page.getByRole('button',{name:/Add.*Train/i}).first().click();await page.locator('.modal.show').waitFor();await screenshot('admin-train-modal');
    await page.locator('.modal.show [data-bs-dismiss="modal"]').first().click();
    await page.setViewportSize({width:390,height:844});await noOverflow('Admin trains mobile');await screenshot('admin-mobile');
    await page.getByRole('button',{name:'Toggle navigation'}).click();await page.getByRole('button',{name:'Logout',exact:false}).click();
    await screenshot('login-mobile');await noOverflow('Login mobile');await visit('/register.jsp');await screenshot('register-mobile');await noOverflow('Register mobile');
    await visit('/');await screenshot('home-mobile');await noOverflow('Home mobile');
    assert.deepEqual(errors,[],'No browser JavaScript errors');assert.deepEqual(failed,[],'No failed local assets/pages');
    console.log('PASS: responsive desktop/mobile UI, offline assets, authentication, search controls, sorting, booking modal, ticket filters, ticket detail and admin pages');
  } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exit(1);});
