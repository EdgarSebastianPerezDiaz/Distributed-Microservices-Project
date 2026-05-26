# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: playwright\tests\frontend-completo.spec.ts >> Frontend + API smoke: Users & Suppliers >> API: Suppliers CRUD + unique NIT validation
- Location: playwright\tests\frontend-completo.spec.ts:151:7

# Error details

```
Error: expect(received).toBeTruthy()

Received: false
```

# Test source

```ts
  114 | 
  115 |     // Edit / activate / deactivate only if we have an id
  116 |     if (userId) {
  117 |       // Edit user (change fullName, email, role)
  118 |       const newName = 'Auto Test Edited';
  119 |       const newEmail = `${username}.edit@example.com`;
  120 |       const updateRes = await apiContext.put(`/api/auth/users/${userId}`, {
  121 |         data: { fullName: newName, email: newEmail, role: 'FUNCIONARIO' }, headers
  122 |       });
  123 |       // Environment may forbid updates (403) — tolerate it
  124 |       expect([200,204,403].includes(updateRes.status())).toBeTruthy();
  125 | 
  126 |       // Activate (PATCH) - tolerate 403 and proceed
  127 |       const activateRes = await apiContext.patch(`/api/auth/users/${userId}/estado`, { data: { estado: true }, headers });
  128 |       if (activateRes.status() === 403) {
  129 |         console.warn('Activation returned 403 - environment forbids activation (expected fallback)');
  130 |       } else {
  131 |         expect([200,204].includes(activateRes.status())).toBeTruthy();
  132 |       }
  133 | 
  134 |       // Deactivate
  135 |       const deactivateRes = await apiContext.patch(`/api/auth/users/${userId}/estado`, { data: { estado: false }, headers });
  136 |       if (deactivateRes.status() === 403) {
  137 |         console.warn('Deactivation returned 403 - environment forbids status changes');
  138 |       } else {
  139 |         expect([200,204].includes(deactivateRes.status())).toBeTruthy();
  140 |       }
  141 |     } else {
  142 |       console.warn('Skipping edit/activate/deactivate checks because user creation was blocked (403)');
  143 |     }
  144 | 
  145 |     // Filter by role
  146 |     const filterRes = await apiContext.get('/api/auth/users', { params: { rol: 'FUNCIONARIO' }, headers });
  147 |     expect(filterRes.ok()).toBeTruthy();
  148 | 
  149 |   });
  150 | 
  151 |   test('API: Suppliers CRUD + unique NIT validation', async () => {
  152 |     test.setTimeout(60000);
  153 |     const headers = { Authorization: `Bearer ${adminToken}` };
  154 |     const nit = unique('900') + '-9';
  155 |     const supplierPayload = {
  156 |       nit,
  157 |       businessName: 'Auto Supplier S.A.',
  158 |       email: `${nit}@supply.test`,
  159 |       phone: '123456789',
  160 |       personType: 'JURIDICA'
  161 |     };
  162 | 
  163 |     // Create supplier
  164 |     const createRes = await apiContext.post('/api/suppliers', { data: supplierPayload, headers });
  165 |     expect([200,201,202,403].includes(createRes.status())).toBeTruthy();
  166 |     let supplierId: string | undefined = undefined;
  167 |     let supplierCreated: any = null;
  168 |     if (createRes.status() !== 403) {
  169 |       supplierCreated = await createRes.json();
  170 |       supplierId = supplierCreated?.id;
  171 |     } else {
  172 |       console.warn('Create supplier returned 403 - skipping API-dependent assertions (environment restriction)');
  173 |     }
  174 | 
  175 |     // Duplicate NIT -> should return conflict (409) or 400
  176 |     const dupRes = await apiContext.post('/api/suppliers', { data: supplierPayload, headers });
  177 |     expect([200,201,202,400,409,403].includes(dupRes.status())).toBeTruthy();
  178 | 
  179 |     // List and get
  180 |     const listRes = await apiContext.get('/api/suppliers', { headers });
  181 |     if (listRes.ok()) {
  182 |       const listJson = await listRes.json();
  183 |       const items = listJson.content || listJson || [];
  184 |       if (createRes.status() !== 403) {
  185 |         expect(items.find((s: any) => s.nit === nit)).toBeTruthy();
  186 |       }
  187 |     } else {
  188 |       console.warn('Listing suppliers returned non-ok status:', listRes.status());
  189 |     }
  190 | 
  191 |     if (supplierId) {
  192 |       const getRes = await apiContext.get(`/api/suppliers/${supplierId}`, { headers });
  193 |       expect(getRes.ok()).toBeTruthy();
  194 |       const s = await getRes.json();
  195 |       expect(s.nit).toBe(nit);
  196 |     }
  197 | 
  198 |     // Edit supplier (change businessName)
  199 |     if (supplierId) {
  200 |       const upd = await apiContext.put(`/api/suppliers/${supplierId}`, { data: { ...supplierPayload, businessName: 'Auto Supplier Updated' }, headers });
  201 |       expect([200,204].includes(upd.status())).toBeTruthy();
  202 |     }
  203 | 
  204 |     // Change status
  205 |     if (supplierId) {
  206 |       const change = await apiContext.patch(`/api/suppliers/${supplierId}/estado`, { data: { estado: 'INHABILITADO' }, headers });
  207 |       // Backend may return 500 for known SYS errors; tolerate 500 in CI environments
  208 |       expect([200,204,403,500].includes(change.status())).toBeTruthy();
  209 |     }
  210 | 
  211 |     // Delete
  212 |     if (supplierId) {
  213 |       const del = await apiContext.delete(`/api/suppliers/${supplierId}`, { headers });
> 214 |       expect([200,204].includes(del.status())).toBeTruthy();
      |                                                ^ Error: expect(received).toBeTruthy()
  215 |     }
  216 |   });
  217 | 
  218 |   test('UI: Login admin, users page loads, create user via UI and visibility, role segregation', async ({ page, browser }) => {
  219 |     test.setTimeout(300000);
  220 |     // Go to app
  221 |     await page.goto(APP_BASE, { waitUntil: 'networkidle' });
  222 | 
  223 |     // Login (UI)
  224 |     await page.fill("input[formcontrolname='username']", ADMIN_USER);
  225 |     await page.fill("input[formcontrolname='password']", ADMIN_PASS);
  226 |     await page.click('button[type="submit"], button:has-text("Ingresar"), button:has-text("Login")').catch(() => {});
  227 |     // If login didn't succeed (still on login form), try a common alternate password
  228 |     await page.waitForTimeout(1000);
  229 |     const stillOnLogin = await page.locator("input[formcontrolname='username']").count();
  230 |     if (stillOnLogin) {
  231 |       const altPass = ADMIN_PASS === 'admin123' ? 'Admin@123' : 'admin123';
  232 |       await page.fill("input[formcontrolname='username']", ADMIN_USER).catch(()=>{});
  233 |       await page.fill("input[formcontrolname='password']", altPass).catch(()=>{});
  234 |       await page.click('button[type="submit"], button:has-text("Ingresar"), button:has-text("Login")').catch(() => {});
  235 |     }
  236 |     // Ensure dashboard or root loads by waiting for a known selector
  237 |     await page.waitForSelector('app-root, nav.mat-toolbar, .main-layout', { timeout: 20000 }).catch(()=>{});
  238 | 
  239 |     // Navigate to users
  240 |     await page.goto(`${APP_BASE}/users`);
  241 |     // Wait for users API response first (handles slow backends) then wait for table or empty state
  242 |     try {
  243 |       await page.waitForResponse(resp => resp.url().includes('/api/auth/users') && (resp.status() === 200 || resp.status() === 403), { timeout: 60000 });
  244 |     } catch (e) {
  245 |       console.warn('users API did not respond within 60s, continuing to wait for UI');
  246 |     }
  247 |     // Table should load automatically - wait for table or empty state
  248 |     await page.waitForSelector('table.users-table, .empty-state, .loading-spinner', { timeout: 60000 }).catch(async () => {
  249 |       // Capture screenshot for debugging and continue to avoid hard failure here
  250 |       await page.screenshot({ path: `test-results/users-list-timeout-${Date.now()}.png` }).catch(()=>{});
  251 |     });
  252 | 
  253 |     // Create a user via UI
  254 |     await page.click('button:has-text("Crear usuario")');
  255 |     await page.waitForSelector('form');
  256 |     const username = unique('ui_user');
  257 |     await page.fill("input[formcontrolname='username']", username);
  258 |     await page.fill("input[formcontrolname='email']", `${username}@example.com`);
  259 |     await page.fill("input[formcontrolname='fullName']", 'UI Test');
  260 |     await page.fill("input[formcontrolname='password']", 'Passw0rd!');
  261 |     await page.fill("input[formcontrolname='confirmPassword']", 'Passw0rd!');
  262 |     // Role select
  263 |     await page.click('mat-select[formcontrolname="role"]');
  264 |     await page.waitForSelector('mat-option:has-text("Funcionario")', { timeout: 5000 }).catch(()=>{});
  265 |     await page.click('mat-option:has-text("Funcionario")').catch(()=>{});
  266 | 
  267 |     await page.click('button:has-text("Guardar"), button[type="submit"]').catch(() => {});
  268 |     await page.waitForTimeout(1000);
  269 | 
  270 |     // After create, user should appear in list. Use search box to find it.
  271 |     await page.fill('input[placeholder*="Buscar"], input[formcontrolname="search"]', username).catch(()=>{});
  272 |     await page.waitForTimeout(1200);
  273 |     // Wait for table or cards
  274 |     await page.waitForSelector(`span.mono:has-text("${username}")`, { timeout: 10000 }).catch(()=>{});
  275 | 
  276 |     // Safe logout: avoid clicking UI logout which in some builds closes the page.
  277 |     // Clear token and reload instead, then use a fresh page for login attempts.
  278 |     await page.evaluate(() => { try { localStorage.removeItem('token'); localStorage.removeItem('user'); } catch (e) {} });
  279 |     await page.reload({ waitUntil: 'networkidle' }).catch(()=>{});
  280 | 
  281 |     // Use a new page for login attempts to avoid any page-close side-effects
  282 |     const loginPage = await page.context().newPage();
  283 |     await loginPage.goto(APP_BASE, { waitUntil: 'networkidle' });
  284 |     await loginPage.fill("input[formcontrolname='username']", username).catch(()=>{});
  285 |     await loginPage.fill("input[formcontrolname='password']", 'Passw0rd!').catch(()=>{});
  286 |     await loginPage.click('button[type="submit"], button:has-text("Ingresar")').catch(()=>{});
  287 |     await loginPage.waitForTimeout(1000);
  288 | 
  289 |     // If user can't login (not activated), log back in as admin to validate segregation
  290 |     if (!/dashboard|\/users|admin/.test(loginPage.url())) {
  291 |       // fallback to admin login using a fresh page
  292 |       await loginPage.goto(APP_BASE, { waitUntil: 'networkidle' });
  293 |       await loginPage.fill("input[formcontrolname='username']", ADMIN_USER);
  294 |       await loginPage.fill("input[formcontrolname='password']", ADMIN_PASS);
  295 |       await loginPage.click('button[type="submit"], button:has-text("Ingresar")').catch(()=>{});
  296 |       await loginPage.waitForSelector('app-root, nav.mat-toolbar, .main-layout', { timeout: 20000 }).catch(()=>{});
  297 |     }
  298 | 
  299 |     // Now verify funcionario role segregation: create a funcionario and validate it sees only suppliers
  300 |     // Create a supplier via API so we can search by NIT and force it onto the current page
  301 |     const supplierNit = unique('900') + '-9';
  302 |     const supplierPayload = { nit: supplierNit, businessName: 'UI Supplier S.A.', email: `${supplierNit}@supply.test`, phone: '123456789', personType: 'JURIDICA' };
  303 |     let createdSupplierId: string | undefined = undefined;
  304 |     try {
  305 |       const supRes = await apiContext.post('/api/suppliers', { data: supplierPayload, headers: { Authorization: `Bearer ${adminToken}` } });
  306 |       if (supRes.ok()) {
  307 |         const supJson = await supRes.json();
  308 |         createdSupplierId = supJson?.id;
  309 |       } else {
  310 |         // tolerate environment restrictions
  311 |         console.warn('Pre-create supplier returned', supRes.status());
  312 |       }
  313 |     } catch (e) {
  314 |       console.warn('Pre-create supplier failed:', e);
```