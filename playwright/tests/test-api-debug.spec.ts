import { test, expect } from '@playwright/test';

test('Debug users API call and table loading', async ({ page }) => {
  // Collect all API responses
  const apiResponses: any[] = [];
  
  page.on('response', async (response) => {
    const url = response.url();
    if (url.includes('/api/')) {
      try {
        const text = await response.text();
        apiResponses.push({
          method: response.request().method(),
          url: url,
          status: response.status(),
          body: text.substring(0, 500) // First 500 chars
        });
        console.log(`[API] ${response.request().method()} ${url} -> ${response.status()}`);
      } catch (e) {
        apiResponses.push({
          method: response.request().method(),
          url: url,
          status: response.status(),
          error: 'Could not read body'
        });
      }
    }
  });
  
  // 1. Navigate to login
  await page.goto('http://localhost:4200/login');
  
  // 2. Login
  console.log('\n🔐 Logging in...');
  await page.fill('input[formcontrolname="username"]', 'admin');
  await page.fill('input[formcontrolname="password"]', 'admin123');
  await page.click('button[type="submit"]');
  
  // 3. Wait for dashboard
  await page.waitForURL('**/admin**', { timeout: 15000 });
  
  // Clear API log to only capture users page requests
  apiResponses.length = 0;
  
  // 4. Click Gestionar Usuarios
  console.log('\n👥 Clicking Gestionar Usuarios...');
  await page.click('button:has-text("Gestionar Usuarios")');
  
  // 5. Wait for users page
  await page.waitForURL('**/users**', { timeout: 10000 });
  
  // Wait for potential API calls
  console.log('\n⏳ Waiting for API calls to complete...');
  await page.waitForLoadState('networkidle', { timeout: 10000 }).catch(() => {
    console.log('networkidle timeout (expected if API is slow)');
  });
  
  // Give extra time for any lingering requests
  await page.waitForTimeout(2000);
  
  // 6. Log all API responses
  console.log('\n📊 API Responses Captured:');
  for (const resp of apiResponses) {
    console.log(`\n${resp.method} ${resp.url}`);
    console.log(`Status: ${resp.status}`);
    if (resp.body) {
      console.log(`Response: ${resp.body}`);
    }
    if (resp.error) {
      console.log(`Error: ${resp.error}`);
    }
  }
  
  // 7. Check page content
  console.log('\n🔍 Page Content Analysis:');
  const pageContent = await page.content();
  console.log(`- Page contains "Usuarios del Sistema": ${pageContent.includes('Usuarios del Sistema')}`);
  console.log(`- Page contains "0 total": ${pageContent.includes('0 total')}`);
  console.log(`- Page contains mat-row: ${pageContent.includes('mat-row')}`);
  
  // 8. Get console messages
  const consoleMessages: any[] = [];
  page.on('console', (msg) => {
    if (!msg.text().includes('Navigated to')) {
      consoleMessages.push({
        type: msg.type(),
        text: msg.text()
      });
    }
  });
  
  // 9. Trigger a manual getUsers call to see error
  console.log('\n🔄 Attempting to trigger users API call...');
  await page.evaluate(() => {
    // Try to find the button and click it again
    const buttons = document.querySelectorAll('button');
    for (const btn of buttons) {
      if (btn.textContent?.includes('Crear usuario')) {
        console.log('Found Crear usuario button');
      }
    }
  });
  
  // 10. Take screenshot
  await page.screenshot({ path: 'test-results/users-api-debug.png' });
  
  // 11. Get all errors from page
  const errors = await page.evaluate(() => {
    return (window as any).errors || [];
  });
  console.log(`\n❌ Window errors: ${errors.length}`);
  
  console.log('\n✅ Debug test completed');
});
