const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: false });
  const context = await browser.newContext();
  const page = await context.newPage();

  page.on('response', async (response) => {
    try {
      const url = response.url();
      if (url.includes('/api/auth/register') || url.includes('/api/auth/users') || url.includes('/api/auth/login')) {
        const text = await response.text();
        console.log(`RESPONSE ${response.status()} ${url} -> ${text.substring(0, 1000)}`);
      }
    } catch (e) {
      console.error('Response read error', e);
    }
  });

  const BASE_UI = 'http://localhost:4200';
  await page.goto(`${BASE_UI}/login`, { waitUntil: 'networkidle' });

  // login
  await page.fill('input[formcontrolname="username"]', 'admin');
  await page.fill('input[formcontrolname="password"]', 'admin123');
  await Promise.all([
    page.waitForNavigation({ waitUntil: 'networkidle', timeout: 10000 }).catch(() => {}),
    page.click('button:has-text("Ingresar"), button:has-text("Login"), button[type="submit"]').catch(() => {}),
  ]);

  // go to users
  await page.goto(`${BASE_UI}/users`, { waitUntil: 'networkidle' });
  await page.waitForSelector('input[formcontrolname="username"], button:has-text("Crear usuario"), button:has-text("Crear Usuario")', { timeout: 5000 }).catch(() => {});

  // start create user flow
  const ts = Date.now();
  const uname = `trace_user_${ts}`;
  const createBtn = page.locator('button:has-text("Crear usuario"), button:has-text("Crear Usuario"), button:has-text("Crear usuario")');
  if (await createBtn.count() > 0) {
    await createBtn.first().click();
  } else {
    await page.goto(`${BASE_UI}/users/new`, { waitUntil: 'networkidle' });
  }

  await page.waitForSelector('input[formcontrolname="username"]', { timeout: 5000 });
  await page.fill('input[formcontrolname="fullName"]', 'Trace User');
  await page.fill('input[formcontrolname="username"]', uname);
  await page.fill('input[formcontrolname="email"]', `${uname}@test.local`);
  await page.fill('input[formcontrolname="password"]', 'Trace1234');
  await page.fill('input[formcontrolname="confirmPassword"]', 'Trace1234');
  // role select
  try {
    await page.click('mat-select[formcontrolname="role"]');
    await page.click('mat-option:has-text("Funcionario"), mat-option:has-text("FUNCIONARIO")');
  } catch (e) {}

  // submit and wait
  await Promise.all([
    page.waitForNavigation({ waitUntil: 'networkidle', timeout: 10000 }).catch(() => {}),
    page.click('button:has-text("Guardar Usuario"), button:has-text("Guardar")').catch(() => {}),
  ]).catch(() => {});

  console.log('Submitted create for', uname);

  // wait a bit and then trigger a users GET by searching
  await page.waitForTimeout(1000);
  const search = page.locator('mat-form-field input[placeholder*="Buscar"], input[placeholder*="Buscar"]');
  if (await search.count() > 0) {
    await search.fill(uname);
    await page.waitForTimeout(800);
  }

  // force explicit GET by calling API via page.evaluate (optional)
  try {
    const apiRes = await page.evaluate(async (u) => {
      const r = await fetch(`/api/auth/users?page=0&size=20&search=${encodeURIComponent(u)}`);
      const j = await r.json();
      return { status: r.status, body: j };
    }, uname);
    console.log('Fetch via page.evaluate ->', apiRes.status, apiRes.body && apiRes.body.length ? (apiRes.body.length + ' items') : JSON.stringify(apiRes.body).substring(0,200));
  } catch (e) {
    console.error('In-page fetch failed', e);
  }

  console.log('Done. Keep browser open for inspection (10s)');
  await page.waitForTimeout(10000);
  await browser.close();
})();
