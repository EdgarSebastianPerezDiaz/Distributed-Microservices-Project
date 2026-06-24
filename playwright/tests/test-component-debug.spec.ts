import { test, expect } from '@playwright/test';

test('Check users component data binding', async ({ page }) => {
  // Setup response listener
  page.on('response', async (response) => {
    if (response.url().includes('/api/auth/users')) {
      const text = await response.text();
      console.log(`\n📤 GET /api/auth/users Response:
Status: ${response.status()}
Body length: ${text.length}
Is Array: ${text.trim().startsWith('[')}
First 200 chars: ${text.substring(0, 200)}`);
      
      // Parse and inspect
      try {
        const json = JSON.parse(text);
        if (Array.isArray(json)) {
          console.log(`✓ Response is array with ${json.length} users`);
          console.log(`  First user: ${json[0]?.username || 'N/A'}`);
        } else if (json.content) {
          console.log(`✓ Response is object with 'content' property`);
          console.log(`  Content length: ${json.content.length}`);
          console.log(`  TotalElements: ${json.totalElements}`);
        } else {
          console.log(`⚠️ Response structure unknown:`, Object.keys(json));
        }
      } catch (e) {
        console.log(`❌ Failed to parse JSON: ${e}`);
      }
    }
  });
  
  // Navigate and login
  await page.goto('http://localhost:4200/login');
  await page.fill('input[formcontrolname="username"]', 'admin');
  await page.fill('input[formcontrolname="password"]', 'admin123');
  await page.click('button[type="submit"]');
  await page.waitForURL('**/admin**');
  
  // Navigate to users
  await page.click('button:has-text("Gestionar Usuarios")');
  await page.waitForURL('**/users**');
  await page.waitForLoadState('networkidle', { timeout: 10000 }).catch(() => {});
  await page.waitForTimeout(1500);
  
  // Check component state
  console.log('\n🔍 Component State in DOM:');
  
  const componentData = await page.evaluate(() => {
    const tableCell = document.querySelector('mat-cell');
    const loading = document.querySelector('mat-spinner');
    const table = document.querySelector('table');
    const rows = document.querySelectorAll('table tbody tr');
    
    return {
      hasSpinner: !!loading,
      hasTable: !!table,
      rowCount: rows.length,
      hasAnyContent: !!tableCell,
      tableClasses: table?.className || 'N/A'
    };
  });
  
  console.log(`
  Spinner visible: ${componentData.hasSpinner}
  Table exists: ${componentData.hasTable}
  Table rows count: ${componentData.rowCount}
  Has content cells: ${componentData.hasAnyContent}
  Table classes: ${componentData.tableClasses}`);
  
  // Check pagination text
  const paginationText = await page.textContent('mat-paginator');
  console.log(`\n📊 Pagination Text: ${paginationText?.trim()}`);
  
  // Check if *ngIf conditions would evaluate to true
  const hiddenElements = await page.evaluate(() => {
    const spinner = document.querySelector('mat-spinner');
    const table = document.querySelector('table');
    
    return {
      spinnerVisible: spinner ? getComputedStyle(spinner).display !== 'none' : 'N/A',
      tableVisible: table ? getComputedStyle(table).display !== 'none' : 'N/A'
    };
  });
  
  console.log(`
  Spinner computed display: ${hiddenElements.spinnerVisible}
  Table computed display: ${hiddenElements.tableVisible}`);
  
  // Try accessing Angular's view for debugging
  console.log('\n🎯 Attempting to inspect Angular component...');
  const userContent = await page.textContent('.users-table');
  console.log(`Users table text content: ${userContent?.substring(0, 100) || 'EMPTY'}`);
  
  // Final screenshot
  await page.screenshot({ path: 'test-results/component-state-debug.png' });
  console.log('\n✅ Component state debug completed');
});
