// Install the optional local harness with:
// npm install --prefix .local/browser-test --no-audit --no-fund playwright
const path = require('node:path');
const fs = require('node:fs');
const assert = require('node:assert/strict');
const { chromium } = require(path.join(__dirname, '../.local/browser-test/node_modules/playwright'));

(async () => {
  const executablePath = [
    process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE,
    'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
    'C:/Program Files/Google/Chrome/Application/chrome.exe',
  ].find(file => file && fs.existsSync(file));
  const browser = await chromium.launch({ headless: true, ...(executablePath ? { executablePath } : {}) });
  try {
    const page = await browser.newPage({ viewport: { width: 1440, height: 960 } });
    page.setDefaultTimeout(15000);
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    await page.goto('http://127.0.0.1:8080/swagger-ui.html', { waitUntil: 'networkidle' });
    await page.locator('.opblock').first().waitFor();
    const spec = await page.evaluate(() => window.ui.specSelectors.specJson().toJS());
    assert.equal(spec.info.title, 'TrackEase API');
    assert.equal(spec.components.securitySchemes.csrfToken.name, 'X-CSRF-Token');
    assert.equal(spec.components.securitySchemes.sessionCookie.in, 'cookie');
    assert.deepEqual(spec.paths['/api/auth/csrf'].get.security ?? [], []);
    assert.ok(spec.paths['/api/bookings'].post.security[0].csrfToken);

    async function execute(endpoint, method, body, status = 200) {
      const operation = page.locator(`.opblock.opblock-${method.toLowerCase()}`).filter({
        has: page.locator(`.opblock-summary-path[data-path="${endpoint}"]`),
      });
      assert.equal(await operation.count(), 1, `Unique operation ${method} ${endpoint}`);
      if (!(await operation.locator('.opblock-body').isVisible())) await operation.locator('.opblock-summary').click();
      await operation.locator('.opblock-body').waitFor({ state: 'visible' });
      const tryButton = operation.locator('.try-out__btn:not(.reset)');
      await tryButton.waitFor({ state: 'visible' });
      if ((await tryButton.innerText()).trim() === 'Try it out') await tryButton.click();
      await operation.getByRole('button', { name: 'Execute', exact: true }).waitFor({ state: 'visible' });
      if (body) await operation.locator('textarea').fill(JSON.stringify(body, null, 2));
      const responsePromise = page.waitForResponse(response =>
        new URL(response.url()).pathname === endpoint && response.request().method() === method);
      await operation.getByRole('button', { name: 'Execute', exact: true }).click();
      const response = await responsePromise;
      assert.equal(response.status(), status, `${method} ${endpoint}`);
      console.log(`PASS: Swagger Try it out ${method} ${endpoint} -> ${status}`);
      return response.json();
    }

    async function authorize(token) {
      await page.locator('.auth-wrapper .authorize').click();
      const modal = page.locator('.modal-ux');
      const csrf = modal.locator('.auth-container').filter({ hasText: 'csrfToken' });
      const logout = csrf.locator('button').filter({ hasText: /^Logout$/ });
      if (await logout.count()) await logout.click();
      await csrf.locator('input').fill(token);
      await csrf.locator('button').filter({ hasText: /^Authorize$/ }).click();
      await csrf.getByRole('button', { name: 'Close', exact: true }).click();
    }

    const csrf = await execute('/api/auth/csrf', 'GET');
    await execute('/api/auth/login', 'POST', { username: 'demo', password: 'Demo123!' }, 403);
    await authorize(csrf.token);
    const login = await execute('/api/auth/login', 'POST', { username: 'demo', password: 'Demo123!' });
    assert.equal(login.user.username, 'demo');
    assert.notEqual(login.csrfToken, csrf.token);
    await authorize(login.csrfToken);
    const me = await execute('/api/auth/me', 'GET');
    assert.equal(me.username, 'demo');
    await execute('/api/auth/logout', 'POST');
    await execute('/api/auth/me', 'GET', null, 401);
    assert.deepEqual(errors, [], 'No Swagger JavaScript errors');
    await page.goto('http://127.0.0.1:8080/swagger-ui.html', { waitUntil: 'networkidle' });
    await page.locator('.opblock').first().waitFor();
    await page.screenshot({ path: path.join(__dirname, '../.local/swagger-tested.png') });
    console.log(`PASS: Swagger rendered ${Object.keys(spec.paths).length} API paths; browser authentication, CSRF and logout verified.`);
  } finally { await browser.close(); }
})().catch(error => { console.error(error.stack); process.exit(1); });
