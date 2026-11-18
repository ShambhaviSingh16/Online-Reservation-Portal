// Local-only catalog migration checks. Requires the optional Playwright harness.
const path = require('node:path'), fs = require('node:fs'), assert = require('node:assert/strict');
const {chromium} = require(path.join(__dirname, '../.local/browser-test/node_modules/playwright'));
const base = 'http://127.0.0.1:8080';
(async () => {
  const executablePath = ['C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe', 'C:/Program Files/Google/Chrome/Application/chrome.exe'].find(fs.existsSync);
  const browser = await chromium.launch({headless:true, executablePath});
  try {
    const context = await browser.newContext(), page = await context.newPage(), errors = [];
    page.setDefaultNavigationTimeout(90000); // First JSP compilation can be slow on a local cold start.
    page.on('pageerror', e => errors.push(e.message));
    async function api(url, method = 'GET', data, expected = 200) {
      const headers = {};
      if (method !== 'GET') headers['X-CSRF-Token'] = (await (await context.request.get(base + '/api/auth/csrf')).json()).token;
      const response = await context.request.fetch(base + url, {method, data, headers});
      assert.equal(response.status(), expected, method + ' ' + url + ': ' + await response.text());
      return response.json();
    }
    await api('/api/auth/login', 'POST', {username:'admin', password:'Admin123!'});
    const tag = Date.now().toString(36).toUpperCase(), code = 'J' + tag;
    await page.goto(base + '/admin/station');
    await page.locator('[data-bs-target="#addStationModal"]').first().click();
    const stationModal = page.locator('#addStationModal');
    for (const [name, value] of Object.entries({stationCode:code, stationName:'JPA Stop ' + tag, city:'Kanpur', state:'UP'}))
      await stationModal.locator('[name=' + name + ']').fill(value);
    await stationModal.getByRole('button', {name:'Add Station', exact:true}).click();
    await page.waitForURL('**/admin/station?success=*');
    const station = (await api('/api/admin/stations')).find(s => s.stationCode === code);
    assert.ok(station, 'Servlet-created station visible through JPA API');
    assert.ok((await api('/api/stations')).some(s => s.stationId === station.stationId));
    await page.goto(base + '/admin/train');
    await page.locator('[data-bs-target="#addTrainModal"]').first().click();
    const trainModal = page.locator('#addTrainModal');
    for (const [name, value] of Object.entries({trainNumber:code, trainName:'JPA Train ' + tag, totalSeats:'80'}))
      await trainModal.locator('[name=' + name + ']').fill(value);
    await trainModal.getByRole('button', {name:'Add Train', exact:true}).click();
    await page.waitForURL('**/admin/train?success=*');
    const train = (await api('/api/admin/trains')).find(t => t.trainNumber === code);
    assert.ok(train, 'Servlet-created train visible through JPA API');
    const trainInput = {trainNumber:code, trainName:'Edited ' + tag, totalSeats:120};
    const stationInput = {stationCode:code, stationName:'Edited ' + tag, city:'Lucknow', state:'UP'};
    await api('/api/admin/trains/' + train.trainId, 'PUT', trainInput);
    await api('/api/admin/stations/' + station.stationId, 'PUT', stationInput);
    assert.equal((await api('/api/admin/trains')).find(t => t.trainId === train.trainId).totalSeats, 120);
    assert.equal((await api('/api/stations')).find(s => s.stationId === station.stationId).city, 'Lucknow');
    for (const type of ['trains', 'stations']) {
      const result = await api('/api/admin/catalog?' + new URLSearchParams({type, q:tag, filter:'active', page:'0', size:'1'}));
      assert.equal(result.total, 1); assert.equal(result.items.length, 1);
      await page.goto(base + '/admin/' + (type === 'trains' ? 'train' : 'station'));
      assert.ok((await page.locator('table').textContent()).includes('Edited ' + tag), 'JSP sees API edit');
    }
    await api('/api/admin/trains', 'POST', trainInput, 409);
    await api('/api/admin/stations', 'POST', stationInput, 409);
    await api('/api/admin/trains/' + train.trainId, 'DELETE');
    await api('/api/admin/stations/' + station.stationId, 'DELETE');
    await api('/api/admin/trains/' + train.trainId, 'PUT', trainInput, 409);
    await api('/api/admin/stations/' + station.stationId, 'PUT', stationInput, 409);
    assert.ok(!(await api('/api/stations')).some(s => s.stationId === station.stationId));
    for (const type of ['trains', 'stations']) {
      assert.equal((await api('/api/admin/catalog?' + new URLSearchParams({type, q:tag, filter:'archived'}))).total, 1);
    }
    assert.deepEqual(errors, []);
    console.log('PASS: JPA-backed train/station creation through browser forms, API edits visible in JSPs, public station choices, paginated search, duplicate conflicts, archival and archived-edit rejection.');
  } finally { await browser.close(); }
})().catch(e => { console.error(e); process.exit(1); });
