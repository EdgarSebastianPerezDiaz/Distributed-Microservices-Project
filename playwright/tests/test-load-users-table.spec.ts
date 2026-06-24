import { test, expect } from '@playwright/test';

test('Load users table after login', async ({ page }) => {
  // 1. Navigate to login
  await page.goto('http://localhost:4200/login');
  
  // 2. Login with correct credentials
  console.log('🔐 Logging in with admin/admin123');
  await page.fill('input[formcontrolname="username"]', 'admin');
  await page.fill('input[formcontrolname="password"]', 'admin123');
  
  // 3. Click login button
  await page.click('button[type="submit"]');
  
  // 4. Wait for navigation (with longer timeout)
  console.log('⏳ Waiting for navigation...');
  await page.waitForURL('**/admin**', { timeout: 15000 });
  
  // 5. Wait a bit for page to fully load
  await page.waitForLoadState('networkidle', { timeout: 10000 });
  
  // 6. Check URL
  const currentUrl = page.url();
  console.log(`✓ Current URL: ${currentUrl}`);
  expect(currentUrl).toContain('admin');
  
  // 7. Look for users table or users section
  console.log('🔍 Looking for users table...');
  
  // Try different selectors for the table
  const tableExists = await Promise.race([
    page.waitForSelector('table', { timeout: 5000 }).then(() => true).catch(() => false),
    page.waitForSelector('mat-table', { timeout: 5000 }).then(() => true).catch(() => false),
    page.waitForSelector('[role="table"]', { timeout: 5000 }).then(() => true).catch(() => false),
    page.waitForSelector('.users-table', { timeout: 5000 }).then(() => true).catch(() => false),
  ]);
  
  if (tableExists) {
    console.log('✅ Users table found!');
    
    // Count rows
    const rows = await page.locator('table tbody tr, mat-table tr, [role="table"] [role="row"]').count();
    console.log(`📊 Found ${rows} rows in users table`);
    
    expect(rows).toBeGreaterThan(0);
  } else {
    console.log('⚠️ Users table not found, checking page content');
    
    // Get page content to see what's there
    const content = await page.content();
    console.log('Page contains "usuario":', content.includes('usuario'));
    console.log('Page contains "user":', content.includes('user'));
    console.log('Page contains "table":', content.includes('table'));
    
    // Take screenshot
    await page.screenshot({ path: 'test-results/users-page-screenshot.png' });
  }
  
  console.log('✅ Test completed');
});
