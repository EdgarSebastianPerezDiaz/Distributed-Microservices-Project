import { test, expect, Page } from '@playwright/test';

const BASE_UI = 'http://localhost:4200';
const BASE_API = 'http://localhost:8081';

async function captureConsole(page: Page) {
  return new Promise<void>((resolve) => {
    page.on('console', msg => {
      console.log(`[BROWSER CONSOLE] ${msg.type()}: ${msg.text()}`);
    });
    page.on('error', error => {
      console.error(`[BROWSER ERROR]:`, error);
    });
    page.on('response', response => {
      console.log(`[RESPONSE] ${response.status()} ${response.url()}`);
    });
    setTimeout(resolve, 100);
  });
}

test('Debug: Test de carga de usuarios - captura detallada', async ({ page }) => {
  // Configurar captura de logs y errores
  await captureConsole(page);
  
  console.log('\n=== PASO 1: Navegar a login ===');
  await page.goto(`${BASE_UI}/login`, { waitUntil: 'networkidle' });
  console.log('✓ Página de login cargada');
  await page.waitForSelector('input[formcontrolname="username"]', { timeout: 5000 });
  console.log('✓ Input de username encontrado');

  console.log('\n=== PASO 2: Realizar login ===');
  await page.fill('input[formcontrolname="username"]', 'admin');
  await page.fill('input[formcontrolname="password"]', 'admin123');
  console.log('✓ Credenciales ingresadas');

  // Espiar las llamadas de red
  let loginResponseReceived = false;
  let getUsersResponseReceived = false;
  let apiErrors: string[] = [];

  page.on('response', async (response) => {
    const url = response.url();
    const status = response.status();
    console.log(`[API] ${response.request().method()} ${url} -> ${status}`);
    
    if (url.includes('/api/auth/login')) {
      loginResponseReceived = true;
      if (status !== 200) {
        const body = await response.text();
        apiErrors.push(`Login failed: ${status} - ${body}`);
        console.error(`  ❌ Login failed: ${body}`);
      } else {
        console.log('  ✓ Login succeeded');
      }
    }
    
    if (url.includes('/api/auth/users')) {
      getUsersResponseReceived = true;
      if (status !== 200) {
        const body = await response.text();
        apiErrors.push(`GetUsers failed: ${status} - ${body}`);
        console.error(`  ❌ GetUsers failed: ${status} - ${body}`);
      } else {
        const body = await response.json();
        console.log(`  ✓ GetUsers succeeded: ${JSON.stringify(body).substring(0, 100)}...`);
      }
    }
  });

  // Hacer click en login
  try {
    await Promise.all([
      page.waitForNavigation({ waitUntil: 'networkidle', timeout: 10000 }),
      page.click('button:has-text("Ingresar")')
        .catch(() => page.click('button:has-text("Login")'))
        .catch(() => page.click('button[type="submit"]'))
    ]);
    console.log('✓ Login button clicked y navegación completada');
  } catch (error) {
    console.error('❌ Error al hacer click en login:', error);
    throw error;
  }

  // Verificar token en localStorage
  console.log('\n=== PASO 3: Verificar token en localStorage ===');
  const token = await page.evaluate(() => localStorage.getItem('token'));
  const user = await page.evaluate(() => localStorage.getItem('user'));
  
  if (token) {
    console.log('✓ Token guardado en localStorage');
    console.log(`  Token: ${token.substring(0, 50)}...`);
  } else {
    console.error('❌ NO HAY TOKEN EN LOCALSTORAGE');
    apiErrors.push('Token not saved in localStorage');
  }

  if (user) {
    console.log('✓ Usuario guardado en localStorage');
    console.log(`  Usuario: ${user}`);
  } else {
    console.error('❌ NO HAY USUARIO EN LOCALSTORAGE');
  }

  // Esperar un poco para que se procese el redirect
  await page.waitForTimeout(2000);

  console.log('\n=== PASO 4: Verificar URL actual ===');
  const url = page.url();
  console.log(`URL actual: ${url}`);
  
  if (url.includes('/login')) {
    console.error('❌ Aún estamos en /login - Login probablemente falló');
    apiErrors.push('Still on login page after login attempt');
  } else if (url.includes('/users')) {
    console.log('✓ Navegamos a /users');
  } else {
    console.log(`⚠ Estamos en: ${url}`);
  }

  console.log('\n=== PASO 5: Intentar navegar a /users manualmente ===');
  try {
    await page.goto(`${BASE_UI}/users`, { waitUntil: 'networkidle', timeout: 10000 });
    console.log('✓ Navegación a /users completada');
  } catch (error) {
    console.error('❌ Error al navegar a /users:', error);
    throw error;
  }

  console.log('\n=== PASO 6: Esperar la tabla de usuarios ===');
  try {
    // Buscar primero si la tabla o empty-state aparecen
    await Promise.race([
      page.waitForSelector('table.users-table', { timeout: 5000 }),
      page.waitForSelector('.empty-state', { timeout: 5000 }),
      page.waitForSelector('.loading-spinner', { timeout: 5000 })
    ]);
    console.log('✓ Uno de los selectores apareció');
    
    // Esperar un poco más para que la tabla se llene
    await page.waitForTimeout(2000);
  } catch (error) {
    console.error('❌ TIMEOUT - ningún selector encontrado:', error);
    
    // Tomar screenshot para debug
    await page.screenshot({ path: 'debug-screenshot.png', fullPage: true });
    console.log('Screenshot guardado como debug-screenshot.png');
    
    // Mostrar el HTML para debug
    const html = await page.content();
    console.log('\n=== HTML DE LA PÁGINA ===');
    console.log(html.substring(0, 1000));
    
    apiErrors.push('No users table/empty-state found');
    throw error;
  }

  console.log('\n=== PASO 7: Verificar contenido de la tabla ===');
  const tableExists = await page.$('table.users-table');
  const emptyStateExists = await page.$('.empty-state');
  
  if (tableExists) {
    console.log('✓ Tabla de usuarios encontrada');
    const rows = await page.$$('table.users-table tbody tr');
    console.log(`  ${rows.length} filas encontradas`);
  } else if (emptyStateExists) {
    console.log('⚠ Empty state encontrado (no hay usuarios)');
  } else {
    console.error('❌ Ni tabla ni empty state encontrados');
  }

  console.log('\n=== RESUMEN DE ERRORES ===');
  if (apiErrors.length === 0) {
    console.log('✓ Sin errores detectados');
  } else {
    console.log('❌ Errores encontrados:');
    apiErrors.forEach(err => console.log(`  - ${err}`));
    throw new Error(`Errores detectados: ${apiErrors.join(', ')}`);
  }
});
