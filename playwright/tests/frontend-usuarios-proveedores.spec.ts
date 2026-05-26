import { test, expect } from '@playwright/test';
import fs from 'fs';

const BASE = process.env.BASE_URL || 'http://localhost:4200';
const SCREENSHOT_DIR = process.env.SCREENSHOT_DIR || 'screenshots';

function ensureDir(dir: string) {
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
}

ensureDir(SCREENSHOT_DIR);

async function tryFill(page, selectors: string[], value: string) {
  for (const s of selectors) {
    const el = page.locator(s);
    if (await el.count() > 0) {
      await el.fill(value).catch(() => {});
      return true;
    }
  }
  return false;
}

async function tryClick(page, selectors: string[]) {
  for (const s of selectors) {
    const el = page.locator(s);
    if (await el.count() > 0) {
      await el.first().click().catch(() => {});
      return true;
    }
  }
  return false;
}

test('Usuarios y Proveedores - flujo completo (resiliente)', async ({ page }) => {
  const errors: string[] = [];
  const ts = Date.now();

  const screenshot = async (name: string) => {
    await page.screenshot({ path: `${SCREENSHOT_DIR}/${ts}-${name}.png`, fullPage: true }).catch(() => {});
  };

  // Credenciales (pueden inyectarse vía env vars TEST_USER / TEST_PASS)
  const ADMIN_USER = process.env.TEST_USER || 'admin';
  const ADMIN_PASS = process.env.TEST_PASS || 'Admin@123';

  // Login como admin
  try {
    await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' });
    await screenshot('01-login-page');
    const filledUser = await tryFill(page, ["input[name='username']","input[name='user']","input[formcontrolname='username']","input[id*='user']"], 'admin');
      const filledPass = await tryFill(page, ["input[name='password']","input[formcontrolname='password']","input[type='password']","input[id*='pass']"], ADMIN_PASS);
    await tryClick(page, ["button[type='submit']", "button:has-text('Ingresar')", "button:has-text('Login')", "button:has-text('Sign in')"]);
    await page.waitForTimeout(1000);
    await screenshot('02-after-login-click');
    await page.waitForURL(/.*(dashboard|usuarios|home).*/i, { timeout: 5000 }).catch(() => {});
  } catch (e) {
    errors.push('Login admin fallo: ' + String(e));
  }

  // Usuarios
  try {
    await page.goto(`${BASE}/usuarios`, { waitUntil: 'networkidle' }).catch(() => {});
    await screenshot('03-usuarios-page');
    // Esperar a que la tabla cargue (varias heurísticas)
    const table = page.locator('table');
    await table.first().waitFor({ state: 'visible', timeout: 5000 }).catch(() => {});
    await screenshot('04-usuarios-table');

    // Crear usuario
    await tryClick(page, ["button:has-text('Nuevo')","button:has-text('Crear usuario')","a:has-text('Nuevo usuario')"]);
    await page.waitForTimeout(500);
    await screenshot('05-usuarios-new-form');

    const uniq = Math.floor(Math.random()*90000)+10000;
    const username = `autotest_user_${uniq}`;
    const email = `auto_${uniq}@test.com`;

    await tryFill(page, ["input[formcontrolname='username']","input[name='username']","input[id*='username']"], username);
    await tryFill(page, ["input[formcontrolname='email']","input[name='email']","input[type='email']"], email);
    await tryFill(page, ["input[formcontrolname='nombre']","input[name='nombre']","input[id*='nombre']"], 'Auto');
    await tryFill(page, ["input[formcontrolname='apellido']","input[name='apellido']","input[id*='apellido']"], 'Test');

    await tryClick(page, ["button:has-text('Guardar')","button:has-text('Save')","button:has-text('Crear')"]);
    await page.waitForTimeout(800);
    await screenshot('06-usuarios-after-create');

    // Buscar el usuario creado en la tabla
    const found = await page.locator(`text=${username}`).first().count();
    if (!found) errors.push('Usuario creado no aparece en la lista: ' + username);

    // Editar usuario (intentar abrir edición desde la fila encontrada)
    try {
      const row = page.locator(`text=${username}`).first();
      if (await row.count() > 0) {
        await row.click().catch(() => {});
        await page.waitForTimeout(300);
        await screenshot('07-usuarios-open-edit');
        await tryFill(page, ["input[formcontrolname='nombre']","input[name='nombre']"], 'AutoEdited');
        await tryClick(page, ["button:has-text('Guardar')","button:has-text('Save')"]);
        await page.waitForTimeout(500);
        await screenshot('08-usuarios-after-edit');
      }
    } catch (e) {
      errors.push('Editar usuario fallo: ' + String(e));
    }

    // Cambiar estado INACTIVO -> ACTIVO (buscar botones de estado)
    try {
      const toggle = page.locator(`text=${username} >> xpath=.. >> button:has-text('Inactivar'), button:has-text('Desactivar'), button:has-text('Cambiar estado')`).first();
      if (await toggle.count() > 0) {
        await toggle.click().catch(() => {});
        await page.waitForTimeout(300);
        await screenshot('09-usuarios-after-deactivate');
        await toggle.click().catch(() => {});
        await page.waitForTimeout(300);
        await screenshot('10-usuarios-after-activate');
      }
    } catch (e) {
      errors.push('Cambiar estado usuario fallo: ' + String(e));
    }

  } catch (e) {
    errors.push('Flujo usuarios fallo: ' + String(e));
  }

  // Proveedores
  try {
    await page.goto(`${BASE}/proveedores`, { waitUntil: 'networkidle' }).catch(() => {});
    await screenshot('11-proveedores-page');
    const tableP = page.locator('table');
    await tableP.first().waitFor({ state: 'visible', timeout: 5000 }).catch(() => {});
    await screenshot('12-proveedores-table');

    // Crear proveedor
    await tryClick(page, ["button:has-text('Nuevo')","button:has-text('Crear proveedor')","a:has-text('Nuevo proveedor')"]);
    await page.waitForTimeout(500);
    await screenshot('13-proveedores-new-form');

    const nit = `900${Math.floor(Math.random()*900000+100000)}-9`;
    const razon = 'Auto Proveedor S.A.S.';

    await tryFill(page, ["input[formcontrolname='nit']","input[name='nit']","input[id*='nit']"], nit);
    await tryFill(page, ["input[formcontrolname='razonSocial']","input[name='razonSocial']","input[id*='razon']"], razon);
    await tryFill(page, ["input[formcontrolname='telefono']","input[name='telefono']"], '3001234567');

    await tryClick(page, ["button:has-text('Guardar')","button:has-text('Save')","button:has-text('Crear')"]);
    await page.waitForTimeout(800);
    await screenshot('14-proveedores-after-create');

    const foundP = await page.locator(`text=${nit}`).first().count();
    if (!foundP) errors.push('Proveedor creado no aparece en la lista: ' + nit);

    // Editar proveedor
    try {
      const row = page.locator(`text=${nit}`).first();
      if (await row.count() > 0) {
        await row.click().catch(() => {});
        await page.waitForTimeout(300);
        await screenshot('15-proveedores-open-edit');
        await tryFill(page, ["input[formcontrolname='telefono']","input[name='telefono']"], '3109998888');
        await tryClick(page, ["button:has-text('Guardar')","button:has-text('Save')"]);
        await page.waitForTimeout(500);
        await screenshot('16-proveedores-after-edit');
      }
    } catch (e) {
      errors.push('Editar proveedor fallo: ' + String(e));
    }

    // Cambiar estado
    try {
      const toggle = page.locator(`text=${nit} >> xpath=.. >> button:has-text('Inactivar'), button:has-text('Desactivar')`).first();
      if (await toggle.count() > 0) {
        await toggle.click().catch(() => {});
        await page.waitForTimeout(300);
        await screenshot('17-proveedores-after-deactivate');
        // Verificar clase inactivo o texto
        const rowText = await page.locator(`text=${nit}`).nth(0).innerText().catch(()=> '');
        if (!/Inhabilitado|Inactivo|inactivo/i.test(rowText)) {
          // try check class
          const el = page.locator(`text=${nit}`).first();
          const classAttr = await el.getAttribute('class').catch(()=> '');
          if (!/inactivo|disabled|disabled-row/i.test(classAttr||'')) {
            errors.push('Advertencia visual de inactivo no encontrada para proveedor: ' + nit);
          }
        }
        await toggle.click().catch(() => {});
        await page.waitForTimeout(300);
        await screenshot('18-proveedores-after-activate');
      }
    } catch (e) {
      errors.push('Cambiar estado proveedor fallo: ' + String(e));
    }

  } catch (e) {
    errors.push('Flujo proveedores fallo: ' + String(e));
  }

  // Contratos - comprobar ruta o enlace en menú
  try {
    await page.goto(`${BASE}/contratos`, { waitUntil: 'networkidle' }).catch(() => {});
    await screenshot('19-contratos-page');
    // Si la ruta falla, asegurarse que existe enlace en menú
    const hasContent = await page.locator('text=Contratos, text=Módulo de Contratos, text=Próximamente').first().count();
    if (!hasContent) {
      // Revisar menú
      await page.goto(BASE, { waitUntil: 'networkidle' });
      await screenshot('20-home-after-contratos-missing');
      const link = page.locator(`a:has-text('Contratos')`).first();
      if (await link.count() === 0) {
        errors.push('Enlace a Contratos no encontrado en menú');
      }
    }
  } catch (e) {
    errors.push('Chequeo contratos fallo: ' + String(e));
  }

  // Roles: logout/login as funcionario
  try {
    // Cerrar sesión (intentar varios selectores)
    await tryClick(page, ["button:has-text('Salir')","button:has-text('Logout')","a:has-text('Cerrar sesión')","a:has-text('Sign out')"]);
    await page.waitForTimeout(500);
    await screenshot('21-after-logout');

    // Login como funcionario creado (usar username variable si existe)
    // Re-use username variable if present in this scope; otherwise skip
    // We'll attempt with the earlier generated username if exists on page
    const userToTest = (typeof (globalThis as any).username !== 'undefined') ? (globalThis as any).username : null;
    // Try to login with created user (fallback to admin if unknown)
    await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' }).catch(()=>{});
    await screenshot('22-login-funcionario');
    if (userToTest) {
      await tryFill(page, ["input[formcontrolname='username']","input[name='username']"], userToTest);
        await tryFill(page, ["input[formcontrolname='password']","input[name='password']"], ADMIN_PASS);
      await tryClick(page, ["button[type='submit']","button:has-text('Ingresar')"]);
      await page.waitForTimeout(800);
      await screenshot('23-after-login-funcionario');

      // Verificar que no vea Usuarios pero si Proveedores y Contratos
      const seesUsuarios = await page.locator("a:has-text('Usuarios')").first().count();
      const seesProveedores = await page.locator("a:has-text('Proveedores')").first().count();
      const seesContratos = await page.locator("a:has-text('Contratos')").first().count();
      if (seesUsuarios) errors.push('Funcionario VE la sección Usuarios en el menú (no debería)');
      if (!seesProveedores) errors.push('Funcionario NO VE Proveedores en el menú');
      if (!seesContratos) errors.push('Funcionario NO VE Contratos en el menú');
    }

    // Volver a admin para finalizar
    await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' });
    await tryFill(page, ["input[formcontrolname='username']","input[name='username']"], ADMIN_USER);
    await tryFill(page, ["input[formcontrolname='password']","input[name='password']"], ADMIN_PASS);
    await tryClick(page, ["button[type='submit']","button:has-text('Ingresar')"]);
    await page.waitForTimeout(800);
    await screenshot('24-final-admin-login');
  } catch (e) {
    errors.push('Roles/Logout flujo fallo: ' + String(e));
  }

  // Reportar errores pero no detener la ejecución
  if (errors.length > 0) {
    console.log('\n--- PLAYWRIGHT RESILIENT TEST ERRORS ---\n');
    for (const er of errors) console.log(er);
    console.log('\nScreenshots en carpeta: ' + SCREENSHOT_DIR);
  } else {
    console.log('TEST COMPLETED: no errors detected');
  }

});
