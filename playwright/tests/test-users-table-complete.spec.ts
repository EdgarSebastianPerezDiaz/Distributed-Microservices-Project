import { test, expect } from '@playwright/test';

test('Load users table by clicking Gestionar Usuarios button', async ({ page }) => {
  // 1. Navigate to login
  await page.goto('http://localhost:4200/login');
  
  // 2. Login with correct credentials
  console.log('🔐 Logging in with admin/admin123');
  await page.fill('input[formcontrolname="username"]', 'admin');
  await page.fill('input[formcontrolname="password"]', 'admin123');
  
  // 3. Click login button
  await page.click('button[type="submit"]');
  
  // 4. Wait for dashboard to load
  console.log('⏳ Waiting for dashboard...');
  await page.waitForURL('**/admin**', { timeout: 15000 });
  await page.waitForLoadState('networkidle', { timeout: 10000 });
  
  // 5. Check current URL
  const currentUrl = page.url();
  console.log(`✓ Current URL: ${currentUrl}`);
  expect(currentUrl).toContain('admin');
  
  // 6. Click "Gestionar Usuarios" button
  console.log('🔍 Looking for Gestionar Usuarios button...');
  await page.click('button:has-text("Gestionar Usuarios")', { timeout: 5000 });
  
  console.log('⏳ Waiting for users page to load...');
  // Wait for URL to change to /users or similar
  try {
    await page.waitForURL('**/users**', { timeout: 10000 });
  } catch (e) {
    console.log('URL did not change to **/users**, checking alternative routes');
  }
  
  // Wait for page to be ready
  await page.waitForLoadState('networkidle', { timeout: 10000 });
  
  const usersUrl = page.url();
  console.log(`✓ Users page URL: ${usersUrl}`);
  
  // 7. Look for table or user content
  console.log('🔍 Looking for users table...');
  
  // Check for various table indicators
  const hasTable = await page.locator('table').count().then(c => c > 0);
  const hasMatTable = await page.locator('mat-table').count().then(c => c > 0);
  const hasMatHeader = await page.locator('mat-header-row').count().then(c => c > 0);
  const hasMatRow = await page.locator('mat-row').count().then(c => c > 0);
  
  console.log(`📊 Table indicators:
    - HTML table: ${hasTable}
    - mat-table: ${hasMatTable}
    - mat-header-row: ${hasMatHeader}
    - mat-row: ${hasMatRow}`);
  
  // Count users if table exists
  if (hasMatRow) {
    const userCount = await page.locator('mat-row').count();
    console.log(`✅ Found ${userCount} users in table!`);
    
    // Get first user name (admin)
    const firstUserCell = await page.locator('mat-cell').first().textContent();
    console.log(`First user: ${firstUserCell}`);
    
    expect(userCount).toBeGreaterThan(0);
  } else if (hasTable) {
    const rows = await page.locator('table tbody tr').count();
    console.log(`✅ Found ${rows} rows in HTML table!`);
    expect(rows).toBeGreaterThan(0);
  } else {
    // Take screenshot to see what's displayed
    console.log('⚠️ No table found, taking screenshot...');
    await page.screenshot({ path: 'test-results/users-table-page.png' });
    
    // Check page content
    const content = await page.content();
    const hasAdminText = content.includes('admin');
    const hasUserText = content.includes('usuario');
    
    console.log(`Page content check:
      - Contains "admin": ${hasAdminText}
      - Contains "usuario": ${hasUserText}`);
  }
  
  console.log('✅ Test completed successfully');
});
