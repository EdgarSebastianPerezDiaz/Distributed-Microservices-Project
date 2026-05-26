import { test, expect } from '@playwright/test';

test('Check console errors during users table load', async ({ page }) => {
  const consoleErrors: string[] = [];
  const apiErrors: any[] = [];
  
  page.on('console', (msg) => {
    if (msg.type() === 'error') {
      consoleErrors.push(msg.text());
      console.log(`[CONSOLE ERROR] ${msg.text()}`);
    } else if (msg.type() === 'warn') {
      console.log(`[CONSOLE WARN] ${msg.text()}`);
    }
  });
  
  page.on('response', async (response) => {
    if (response.url().includes('/api/auth/users')) {
      const status = response.status();
      let text = '';
      try {
        text = await response.text();
      } catch (e) {
        text = 'Could not read response';
      }
      
      apiErrors.push({
        url: response.url(),
        status,
        method: response.request().method(),
        bodyLength: text.length
      });
      
      console.log(`[API] ${response.request().method()} ${response.url()} -> ${status}`);
    }
  });
  
  // Navigate
  await page.goto('http://localhost:4200/login');
  await page.fill('input[formcontrolname="username"]', 'admin');
  await page.fill('input[formcontrolname="password"]', 'admin123');
  await page.click('button[type="submit"]');
  await page.waitForURL('**/admin**', { timeout: 15000 });
  
  consoleErrors.length = 0;
  apiErrors.length = 0;
  
  // Navigate to users
  console.log('\n👥 Navigating to Users page...');
  await page.click('button:has-text("Gestionar Usuarios")');
  await page.waitForURL('**/users**', { timeout: 10000 });
  
  // Wait for loads
  await page.waitForLoadState('networkidle', { timeout: 10000 }).catch(() => {
    console.log('networkidle timeout');
  });
  await page.waitForTimeout(2000);
  
  console.log(`\n📋 Total console errors: ${consoleErrors.length}`);
  for (const err of consoleErrors) {
    console.log(`  - ${err}`);
  }
  
  console.log(`\n📊 API calls made: ${apiErrors.length}`);
  for (const call of apiErrors) {
    console.log(`  - ${call.method} ${call.url.split('?')[1]?.split('&')[0] || ''} -> ${call.status}`);
  }
  
  // Check spinner status
  const spinnerVisible = await page.evaluate(() => {
    const spinner = document.querySelector('mat-spinner');
    if (!spinner) return { exists: false };
    
    const style = getComputedStyle(spinner);
    return {
      exists: true,
      display: style.display,
      visibility: style.visibility,
      opacity: style.opacity
    };
  });
  
  console.log(`\n🔄 Spinner status:`, spinnerVisible);
  
  // Check if there's a loading state still
  const loadingState = await page.evaluate(() => {
    // Try to find the component instance
    const appUserList = document.querySelector('app-user-list');
    if (!appUserList) return { componentFound: false };
    
    return {
      componentFound: true,
      innerHTML: appUserList.innerHTML.substring(0, 200)
    };
  });
  
  console.log('\nComponent found:', loadingState.componentFound);
  if (loadingState.componentFound) {
    console.log('Component HTML preview:', loadingState.innerHTML);
  }
  
  // Get page title/heading
  const heading = await page.textContent('h1');
  console.log(`\nPage heading: ${heading}`);
  
  // Final screenshot
  await page.screenshot({ path: 'test-results/console-errors-debug.png' });
  console.log('\n✅ Console error debug completed');
});
