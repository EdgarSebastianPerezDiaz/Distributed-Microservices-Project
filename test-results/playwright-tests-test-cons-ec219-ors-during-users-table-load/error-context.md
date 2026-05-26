# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: playwright\tests\test-console-errors.spec.ts >> Check console errors during users table load
- Location: playwright\tests\test-console-errors.spec.ts:3:5

# Error details

```
Error: page.goto: net::ERR_CONNECTION_REFUSED at http://localhost:4200/login
Call log:
  - navigating to "http://localhost:4200/login", waiting until "load"

```

# Test source

```ts
  1   | import { test, expect } from '@playwright/test';
  2   | 
  3   | test('Check console errors during users table load', async ({ page }) => {
  4   |   const consoleErrors: string[] = [];
  5   |   const apiErrors: any[] = [];
  6   |   
  7   |   page.on('console', (msg) => {
  8   |     if (msg.type() === 'error') {
  9   |       consoleErrors.push(msg.text());
  10  |       console.log(`[CONSOLE ERROR] ${msg.text()}`);
  11  |     } else if (msg.type() === 'warn') {
  12  |       console.log(`[CONSOLE WARN] ${msg.text()}`);
  13  |     }
  14  |   });
  15  |   
  16  |   page.on('response', async (response) => {
  17  |     if (response.url().includes('/api/auth/users')) {
  18  |       const status = response.status();
  19  |       let text = '';
  20  |       try {
  21  |         text = await response.text();
  22  |       } catch (e) {
  23  |         text = 'Could not read response';
  24  |       }
  25  |       
  26  |       apiErrors.push({
  27  |         url: response.url(),
  28  |         status,
  29  |         method: response.request().method(),
  30  |         bodyLength: text.length
  31  |       });
  32  |       
  33  |       console.log(`[API] ${response.request().method()} ${response.url()} -> ${status}`);
  34  |     }
  35  |   });
  36  |   
  37  |   // Navigate
> 38  |   await page.goto('http://localhost:4200/login');
      |              ^ Error: page.goto: net::ERR_CONNECTION_REFUSED at http://localhost:4200/login
  39  |   await page.fill('input[formcontrolname="username"]', 'admin');
  40  |   await page.fill('input[formcontrolname="password"]', 'admin123');
  41  |   await page.click('button[type="submit"]');
  42  |   await page.waitForURL('**/admin**', { timeout: 15000 });
  43  |   
  44  |   consoleErrors.length = 0;
  45  |   apiErrors.length = 0;
  46  |   
  47  |   // Navigate to users
  48  |   console.log('\n👥 Navigating to Users page...');
  49  |   await page.click('button:has-text("Gestionar Usuarios")');
  50  |   await page.waitForURL('**/users**', { timeout: 10000 });
  51  |   
  52  |   // Wait for loads
  53  |   await page.waitForLoadState('networkidle', { timeout: 10000 }).catch(() => {
  54  |     console.log('networkidle timeout');
  55  |   });
  56  |   await page.waitForTimeout(2000);
  57  |   
  58  |   console.log(`\n📋 Total console errors: ${consoleErrors.length}`);
  59  |   for (const err of consoleErrors) {
  60  |     console.log(`  - ${err}`);
  61  |   }
  62  |   
  63  |   console.log(`\n📊 API calls made: ${apiErrors.length}`);
  64  |   for (const call of apiErrors) {
  65  |     console.log(`  - ${call.method} ${call.url.split('?')[1]?.split('&')[0] || ''} -> ${call.status}`);
  66  |   }
  67  |   
  68  |   // Check spinner status
  69  |   const spinnerVisible = await page.evaluate(() => {
  70  |     const spinner = document.querySelector('mat-spinner');
  71  |     if (!spinner) return { exists: false };
  72  |     
  73  |     const style = getComputedStyle(spinner);
  74  |     return {
  75  |       exists: true,
  76  |       display: style.display,
  77  |       visibility: style.visibility,
  78  |       opacity: style.opacity
  79  |     };
  80  |   });
  81  |   
  82  |   console.log(`\n🔄 Spinner status:`, spinnerVisible);
  83  |   
  84  |   // Check if there's a loading state still
  85  |   const loadingState = await page.evaluate(() => {
  86  |     // Try to find the component instance
  87  |     const appUserList = document.querySelector('app-user-list');
  88  |     if (!appUserList) return { componentFound: false };
  89  |     
  90  |     return {
  91  |       componentFound: true,
  92  |       innerHTML: appUserList.innerHTML.substring(0, 200)
  93  |     };
  94  |   });
  95  |   
  96  |   console.log('\nComponent found:', loadingState.componentFound);
  97  |   if (loadingState.componentFound) {
  98  |     console.log('Component HTML preview:', loadingState.innerHTML);
  99  |   }
  100 |   
  101 |   // Get page title/heading
  102 |   const heading = await page.textContent('h1');
  103 |   console.log(`\nPage heading: ${heading}`);
  104 |   
  105 |   // Final screenshot
  106 |   await page.screenshot({ path: 'test-results/console-errors-debug.png' });
  107 |   console.log('\n✅ Console error debug completed');
  108 | });
  109 | 
```