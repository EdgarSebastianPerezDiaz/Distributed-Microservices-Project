import { test, expect, Page } from '@playwright/test';

const BASE_UI = 'http://localhost:4200';

test('Debug: Verificar por qué login no se envía', async ({ page }) => {
  console.log('\n=== NAVEGANDO A LOGIN ===');
  await page.goto(`${BASE_UI}/login`, { waitUntil: 'networkidle' });
  
  // Espiar todas las requests
  const requestsLog: string[] = [];
  page.on('request', request => {
    requestsLog.push(`REQUEST: ${request.method()} ${request.url()}`);
  });

  console.log('✓ Página cargada');
  await page.waitForSelector('input[formcontrolname="username"]', { timeout: 5000 });

  console.log('\n=== RELLENANDO FORM ===');
  const usernameInput = page.locator('input[formcontrolname="username"]');
  const passwordInput = page.locator('input[formcontrolname="password"]');
  const button = page.locator('button[type="submit"]');
  
  console.log('✓ Inputs encontrados');
  
  // Ver estado del form
  const formState = await page.evaluate(() => {
    const username = (document.querySelector('input[formcontrolname="username"]') as HTMLInputElement)?.value;
    const password = (document.querySelector('input[formcontrolname="password"]') as HTMLInputElement)?.value;
    const button = document.querySelector('button[type="submit"]') as HTMLButtonElement;
    return { username, password, buttonDisabled: button?.disabled, buttonText: button?.textContent };
  });
  
  console.log('Estado inicial del form:', formState);
  
  // Llenar inputs
  await usernameInput.fill('admin');
  await passwordInput.fill('admin123');  // ✓ CONTRASEÑA CORRECTA
  
  // Esperar a que el botón se habilite (si estaba deshabilitado)
  await page.waitForTimeout(500);
  
  const formStateAfter = await page.evaluate(() => {
    const button = document.querySelector('button[type="submit"]') as HTMLButtonElement;
    return { buttonDisabled: button?.disabled, buttonText: button?.textContent };
  });
  
  console.log('Estado después de llenar:', formStateAfter);
  
  // Verificar si el botón está habilitado
  const isEnabled = await button.isEnabled();
  console.log(`✓ Botón habilitado: ${isEnabled}`);
  
  if (!isEnabled) {
    console.error('❌ BOTÓN NO ESTÁ HABILITADO!');
    const html = await page.locator('button[type="submit"]').evaluate(el => (el as HTMLElement).outerHTML);
    console.log('HTML del botón:', html);
    throw new Error('Button is not enabled');
  }

  console.log('\n=== HACIENDO CLICK EN BOTÓN ===');
  
  // Escuchar errores de red
  let networkError = false;
  page.on('requestfailed', request => {
    console.error(`❌ REQUEST FAILED: ${request.url()} - ${request.failure()?.errorText}`);
    networkError = true;
  });

  // Capturar console errors
  page.on('console', msg => {
    if (msg.type() === 'error') {
      console.error(`[CONSOLE ERROR] ${msg.text()}`);
    } else if (msg.type() === 'warn') {
      console.warn(`[CONSOLE WARN] ${msg.text()}`);
    }
  });

  // Click
  const clickSuccess = await button.click().then(() => {
    console.log('✓ Click ejecutado');
    return true;
  }).catch(err => {
    console.error('❌ Error al hacer click:', err);
    return false;
  });

  if (!clickSuccess) {
    throw new Error('Failed to click button');
  }

  // Esperar un poco y ver qué requests se hicieron
  await page.waitForTimeout(2000);
  
  console.log('\n=== REQUESTS REGISTRADAS ===');
  const loginRequests = requestsLog.filter(r => r.includes('/api/auth/login'));
  if (loginRequests.length === 0) {
    console.error('❌ NO SE ENVIÓ /api/auth/login');
    console.log('Todos los requests:');
    requestsLog.forEach(r => console.log('  ' + r));
  } else {
    console.log('✓ Login request enviado');
    loginRequests.forEach(r => console.log('  ' + r));
  }

  // Esperar a ver si hay navegación
  console.log('\n=== ESPERANDO NAVEGACIÓN ===');
  try {
    await page.waitForNavigation({ timeout: 3000 }).catch(() => {});
  } catch {}
  
  const currentUrl = page.url();
  console.log('URL actual:', currentUrl);
  
  if (currentUrl.includes('/login')) {
    console.error('❌ SEGUIMOS EN /login - LOGIN FALLÓ');
  } else {
    console.log('✓ Navegamos fuera de /login');
  }
});
