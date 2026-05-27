import { test, expect } from '@playwright/test';

test('Monitor component subscription lifecycle', async ({ page }) => {
  // Intercept and log all requests/responses
  const events: any[] = [];
  
  page.on('response', async (response) => {
    if (response.url().includes('/api/auth/')) {
      try {
        const status = response.status();
        let bodyPreview = '';
        if (status === 200) {
          const text = await response.text();
          bodyPreview = text.substring(0, 100);
        }
        
        events.push({
          timestamp: new Date().toISOString(),
          type: 'API_RESPONSE',
          url: response.url().split('?')[0],
          status: status,
          bodyPreview: bodyPreview
        });
        
        console.log(`[${new Date().toISOString()}] API ${response.status()} ${response.url().split('?')[0]}`);
      } catch (e) {
        console.log(`[ERROR] Could not process response`);
      }
    }
  });
  
  page.on('console', (msg) => {
    const text = msg.text();
    if (text.includes('Error loading users') || text.includes('error')) {
      events.push({
        timestamp: new Date().toISOString(),
        type: 'CONSOLE_ERROR',
        message: text
      });
      console.log(`[CONSOLE] ${text}`);
    }
  });
  
  // Login
  await page.goto('http://localhost:4200/login');
  await page.fill('input[formcontrolname="username"]', 'admin');
  await page.fill('input[formcontrolname="password"]', 'admin123');
  await page.click('button[type="submit"]');
  await page.waitForURL('**/admin**', { timeout: 15000 });
  
  // Navigate to users
  console.log('\n👥 Navigating to Users...');
  events.length = 0;
  
  await page.click('button:has-text("Gestionar Usuarios")');
  await page.waitForURL('**/users**', { timeout: 10000 });
  
  // Log events up to this point
  console.log(`\n📋 Events before waiting: ${events.length}`);
  for (const evt of events) {
    console.log(`  - [${evt.type}] ${evt.url || evt.message}`);
  }
  
  // Now wait progressively and check component state at each point
  for (let wait = 1; wait <= 5; wait++) {
    console.log(`\n⏰ Waiting ${wait} second(s)...`);
    await page.waitForTimeout(1000);
    
    const state = await page.evaluate(() => {
      const spinner = document.querySelector('mat-spinner');
      const table = document.querySelector('table');
      const rows = table ? table.querySelectorAll('tbody tr').length : 0;
      const paginationText = document.querySelector('mat-paginator')?.textContent || 'N/A';
      
      return {
        spinnerVisible: spinner ? window.getComputedStyle(spinner).display !== 'none' : false,
        tableExists: !!table,
        rowCount: rows,
        paginationText: paginationText.substring(0, 100)
      };
    });
    
    console.log(`  Spinner: ${state.spinnerVisible}, Table: ${state.tableExists}, Rows: ${state.rowCount}`);
    
    // Check if we're done
    if (!state.spinnerVisible && state.rowCount > 0) {
      console.log(`\n✅ Table loaded successfully! Found ${state.rowCount} rows`);
      break;
    }
  }
  
  // Final state
  const finalState = await page.evaluate(() => {
    const spinner = document.querySelector('mat-spinner');
    const table = document.querySelector('table');
    const matRows = document.querySelectorAll('mat-row').length;
    const htmlRows = table ? table.querySelectorAll('tbody tr').length : 0;
    
    return {
      spinnerVisible: spinner ? window.getComputedStyle(spinner).display !== 'none' : false,
      tableExists: !!table,
      matRows: matRows,
      htmlRows: htmlRows
    };
  });
  
  console.log(`\n📊 Final State:
    Spinner visible: ${finalState.spinnerVisible}
    Table exists: ${finalState.tableExists}
    Material rows: ${finalState.matRows}
    HTML rows: ${finalState.htmlRows}`);
  
  // Take screenshot
  await page.screenshot({ path: 'test-results/subscription-lifecycle-final.png' });
  
  console.log('\n✅ Lifecycle monitoring completed');
});
