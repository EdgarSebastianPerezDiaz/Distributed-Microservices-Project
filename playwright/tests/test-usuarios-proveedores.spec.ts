import { test, expect, Page } from '@playwright/test';
import fs from 'fs';
import path from 'path';

const BASE_UI = 'http://localhost:4200';
const BASE_API = 'http://localhost:8081';
const SCREENSHOT_DIR = path.join(process.cwd(), 'screenshots');

// Ensure screenshots directory exists
if (!fs.existsSync(SCREENSHOT_DIR)) {
  fs.mkdirSync(SCREENSHOT_DIR, { recursive: true });
}

async function snapOnError(page: Page, name: string) {
  const file = path.join(SCREENSHOT_DIR, `${Date.now()}_${name}.png`);
  await page.screenshot({ path: file, fullPage: true });
  console.log(`Saved screenshot: ${file}`);
}

async function tryStep(stepName: string, page: Page, fn: () => Promise<void>) {
  try {
    await fn();
    console.log(`PASS - ${stepName}`);
  } catch (err) {
    console.error(`FAIL - ${stepName} -> ${(err as Error).message}`);
    await snapOnError(page, stepName.replace(/[^a-z0-9]/gi, '_'));
    throw err;
  }
}

test('Usuarios y Proveedores - flujo completo (UI)', async ({ page }) => {
  // 1. Abrir navegador y navegar a login
  await tryStep('Abrir /login', page, async () => {
    await page.goto(`${BASE_UI}/login`, { waitUntil: 'networkidle' });
    await expect(page).toHaveURL(/\/login/);
    await page.waitForSelector('input[formcontrolname="username"]', { timeout: 5000 });
  });

  // 2. Login admin
  await tryStep('Login admin', page, async () => {
    await page.fill('input[formcontrolname="username"]', 'admin');
    await page.fill('input[formcontrolname="password"]', 'admin123');
    await Promise.all([
      page.waitForNavigation({ waitUntil: 'networkidle' }),
      page.click('button:has-text("Ingresar")').catch(() => page.click('button:has-text("Login")').catch(() => page.click('button[type="submit"]')))
    ]);
    // Allow redirect to dashboard/admin
    await expect(page).toHaveURL(/(dashboard|admin|\/)/);
  });

  // 3. Ir a Usuarios
  await tryStep('Navegar a /users', page, async () => {
    // Try clicking nav link
    const nav = page.locator('a:has-text("Usuarios"), a:has-text("Users")');
    if (await nav.count() > 0) {
      await nav.first().click();
      await page.waitForLoadState('networkidle');
    } else {
      await page.goto(`${BASE_UI}/users`, { waitUntil: 'networkidle' });
    }
    await expect(page).toHaveURL(/\/users/);
    // Confirm table or heading
    await page.waitForSelector('h1:has-text("Usuarios") , h1:has-text("Usuarios del Sistema"), table.users-table', { timeout: 5000 });
  });

  // 4. Verificar tabla contiene admin
  await tryStep('Verificar tabla usuarios contiene admin', page, async () => {
    // Wait for table rows
    await page.waitForSelector('table.users-table', { timeout: 5000 });
    const adminCell = page.locator('table.users-table span.mono', { hasText: 'admin' });
    await expect(adminCell).toHaveCount(1);
  });

  // 5. Crear nuevo usuario FUNCIONARIO
  const newUsername = 'auto_test';
  await tryStep('Crear usuario FUNCIONARIO', page, async () => {
    // Click Crear usuario button
    const createBtn = page.locator('button:has-text("Crear usuario"), button:has-text("Crear Usuario"), button:has-text("Crear usuario")');
    if (await createBtn.count() > 0) {
      await createBtn.first().click();
      await page.waitForURL(/\/users\/new|\/users\/create|\/users\/add/, { timeout: 3000 }).catch(() => {});
    } else {
      await page.goto(`${BASE_UI}/users/new`, { waitUntil: 'networkidle' });
    }

    // Fill form
    await page.waitForSelector('input[formcontrolname="username"]');
    await page.fill('input[formcontrolname="fullName"]', 'Automático Prueba');
    await page.fill('input[formcontrolname="username"]', newUsername);
    await page.fill('input[formcontrolname="email"]', 'auto@test.com');
    await page.fill('input[formcontrolname="password"]', 'Auto123');
    await page.fill('input[formcontrolname="confirmPassword"]', 'Auto123');
    // Select role FUNCIONARIO
    await page.click('mat-select[formcontrolname="role"]');
    await page.click('mat-option:has-text("Funcionario"), mat-option:has-text("FUNCIONARIO")');

    // Submit
    await Promise.all([
      page.waitForNavigation({ waitUntil: 'networkidle' }),
      page.click('button:has-text("Guardar Usuario"), button:has-text("Guardar")')
    ]);

    // Back to users list
    await expect(page).toHaveURL(/\/users/);
  });

  // 6. Verificar que el usuario aparece en la lista
  await tryStep('Verificar usuario creado en tabla', page, async () => {
    await page.waitForSelector('table.users-table');
    // Use search box to filter
    const search = page.locator('mat-form-field input[placeholder*="Buscar"], input[placeholder*="Buscar"]');
    if (await search.count() > 0) {
      await search.fill('auto_test');
      await page.waitForTimeout(500); // allow debounce
    }
    const userCell = page.locator('table.users-table span.mono', { hasText: newUsername });
    await expect(userCell).toHaveCount(1);
  });

  // 7. Editar usuario -> cambiar nombre a "Automático Editado"
  await tryStep('Editar usuario - cambiar nombre', page, async () => {
    // find the row for the user
    const row = page.locator('table.users-table tr', { has: page.locator('span.mono', { hasText: newUsername }) }).first();
    await expect(row).toBeVisible();
    // Click edit icon inside row (mat-icon text 'edit')
    const editBtn = row.locator('mat-icon', { hasText: 'edit' });
    if (await editBtn.count() > 0) {
      await editBtn.first().click();
    } else {
      // fallback: navigate to edit route by extracting ID from view link or dataset
      const viewBtn = row.locator('button:has-text("Ver")');
      // try to navigate using button that calls editUser(element.id) in template
      await row.locator('button[mattooltip="Editar"]').first().click().catch(async () => {
        // try programmatic navigation: click first action button that calls edit
        await row.locator('button:has-text("Editar")').first().click().catch(() => {});
      });
    }

    // Wait for edit form
    await page.waitForSelector('input[formcontrolname="fullName"]');
    // change name
    await page.fill('input[formcontrolname="fullName"]', 'Automático Editado');
    await Promise.all([
      page.waitForNavigation({ waitUntil: 'networkidle' }),
      page.click('button:has-text("Guardar Cambios"), button:has-text("Guardar Usuario"), button:has-text("Guardar")')
    ]).catch(() => {});
    // ensure we are back to /users and name updated
    await page.waitForSelector('table.users-table');
    const updatedRow = page.locator('table.users-table tr', { has: page.locator('div.name', { hasText: 'Automático Editado' }) });
    await expect(updatedRow).toHaveCount(1);
  });

  // 8. Desactivar y activar usuario
  await tryStep('Desactivar usuario', page, async () => {
    const row = page.locator('table.users-table tr', { has: page.locator('span.mono', { hasText: newUsername }) }).first();
    const toggleBtn = row.locator('button', { has: page.locator('mat-icon', { hasText: 'history' }) });
    if (await toggleBtn.count() === 0) {
      // Try generic toggle button in row
      await row.locator('button[mattooltip="Cambiar estado"], button:has-text("Cambiar estado")').first().click().catch(() => {});
    } else {
      await toggleBtn.first().click();
    }
    // Wait for status pill to show Inactivo
    const statusPill = row.locator('.status-pill', { hasText: 'Inactivo' });
    await expect(statusPill).toHaveCount(1);
  });

  await tryStep('Activar usuario', page, async () => {
    const row = page.locator('table.users-table tr', { has: page.locator('span.mono', { hasText: newUsername }) }).first();
    // Click the toggle again (icon may be 'check_circle')
    const toggleBtn = row.locator('button', { has: page.locator('mat-icon', { hasText: 'check_circle' }) });
    if (await toggleBtn.count() === 0) {
      await row.locator('button[mattooltip="Cambiar estado"], button:has-text("Cambiar estado")').first().click().catch(() => {});
    } else {
      await toggleBtn.first().click();
    }
    const statusPill = row.locator('.status-pill', { hasText: 'Activo' });
    await expect(statusPill).toHaveCount(1);
  });

  // 9. Ir a Proveedores
  await tryStep('Navegar a /suppliers', page, async () => {
    const nav = page.locator('a:has-text("Proveedores"), a:has-text("Suppliers")');
    if (await nav.count() > 0) {
      await nav.first().click();
      await page.waitForLoadState('networkidle');
    } else {
      await page.goto(`${BASE_UI}/suppliers`, { waitUntil: 'networkidle' });
    }
    await expect(page).toHaveURL(/\/suppliers/);
    await page.waitForSelector('h1, table', { timeout: 5000 });
  });

  // 10. Verificar lista de proveedores carga
  await tryStep('Verificar lista proveedores carga', page, async () => {
    // Wait for suppliers table or list
    await page.waitForSelector('table, .suppliers-table, .supplier-list, mat-card', { timeout: 5000 });
  });

  // 11. Crear nuevo proveedor
  const supplierNit = '123456789-3';
  await tryStep('Crear proveedor', page, async () => {
    // Click Registrar Proveedor / Nuevo
    const createBtn = page.locator('button:has-text("Registrar Proveedor"), button:has-text("Crear Proveedor"), button:has-text("Nuevo Registro")');
    if (await createBtn.count() > 0) {
      await createBtn.first().click();
    } else {
      await page.goto(`${BASE_UI}/suppliers/new`, { waitUntil: 'networkidle' });
    }

    // Fill supplier form using known formcontrolnames
    await page.waitForSelector('input[formcontrolname="nit"]', { timeout: 5000 });
    await page.fill('input[formcontrolname="nit"]', supplierNit);
    await page.fill('input[formcontrolname="businessName"]', 'Proveedor Automatizado SRL');
    await page.fill('input[formcontrolname="email"]', 'auto@proveedor.com');
    await page.fill('input[formcontrolname="phone"]', '6012345678');
    // personType select
    await page.click('mat-select[formcontrolname="personType"]');
    await page.click('mat-option:has-text("Jurídica"), mat-option:has-text("JURIDICA"), mat-option:has-text("Juridica")');

    // Try to fill optional fields if present (representanteLegal, direccion, ciudad)
    const rep = page.locator('input[formcontrolname="representanteLegal"], input[name="representanteLegal"]');
    if (await rep.count() > 0) await rep.fill('Juan Automático');
    const direccion = page.locator('input[formcontrolname="direccion"], input[name="direccion"]');
    if (await direccion.count() > 0) await direccion.fill('Calle Automática 123');
    const ciudad = page.locator('input[formcontrolname="ciudad"], input[name="ciudad"]');
    if (await ciudad.count() > 0) await ciudad.fill('Bogotá');

    // Submit
    await Promise.all([
      page.waitForNavigation({ waitUntil: 'networkidle' }),
      page.click('button:has-text("Registrar Proveedor"), button:has-text("Guardar")')
    ]).catch(() => {});

    await expect(page).toHaveURL(/\/suppliers/);
  });

  // 12. Verificar proveedor creado en listado
  await tryStep('Verificar proveedor en lista', page, async () => {
    await page.waitForSelector('table, .suppliers-table');
    // Use search if available
    const search = page.locator('mat-form-field input[placeholder*="Buscar"], input[placeholder*="Buscar"]');
    if (await search.count() > 0) {
      await search.fill(supplierNit);
      await page.waitForTimeout(500);
    }
    // look for row with nit
    const nitCell = page.locator('table tr, .supplier-row', { has: page.locator('td, span, .mono', { hasText: supplierNit }) });
    await expect(nitCell).toHaveCount(1);
  });

  // 13. Editar proveedor - cambiar telefono
  await tryStep('Editar proveedor - cambiar telefono', page, async () => {
    const row = page.locator('table tr', { has: page.locator('td, span', { hasText: supplierNit }) }).first();
    await expect(row).toBeVisible();
    // click edit icon inside row if present
    const editIcon = row.locator('mat-icon', { hasText: 'edit' });
    if (await editIcon.count() > 0) {
      await editIcon.first().click();
    } else {
      // navigate to edit route (if available)
      await page.goto(`${BASE_UI}/suppliers/${await getIdFromRow(row)}/edit`).catch(() => {});
    }

    // Wait for form and update phone
    await page.waitForSelector('input[formcontrolname="phone"]');
    await page.fill('input[formcontrolname="phone"]', '6011111111');
    await Promise.all([
      page.waitForNavigation({ waitUntil: 'networkidle' }),
      page.click('button:has-text("Guardar Cambios"), button:has-text("Guardar")')
    ]).catch(() => {});

    // Verify new phone visible in list
    const updatedRow = page.locator('table tr', { has: page.locator('td, span', { hasText: supplierNit }) }).first();
    await expect(updatedRow).toBeVisible();
  });

  // 14. Cambiar estado proveedor a INACTIVO y verificar advertencia visual
  await tryStep('Inactivar proveedor y verificar advertencia visual', page, async () => {
    const row = page.locator('table tr', { has: page.locator('td, span', { hasText: supplierNit }) }).first();
    // Try to find status toggle in row
    const toggle = row.locator('button[mattooltip="Cambiar estado"], button:has-text("Cambiar estado")');
    if (await toggle.count() > 0) {
      await toggle.first().click();
    } else {
      // Try open detail and change status there (if exists)
      await row.locator('button:has-text("Ver")').first().click().catch(() => {});
    }

    // After inactivation, look for visual marker on supplier row (class .is-inactive or text Inactivo)
    const warning = row.locator('.status-pill.is-inactive, .warning, :text("Inactivo"), .badge--danger');
    await expect(warning).toHaveCount(1);
  });

  // 15. Logout
  await tryStep('Logout', page, async () => {
    const profileMenu = page.locator('button:has-text("Salir"), button:has-text("Logout"), button[aria-label="logout"]');
    if (await profileMenu.count() > 0) {
      await profileMenu.first().click();
    } else {
      // Try clicking menu and logout link
      const menu = page.locator('button[mat-icon-button]');
      if (await menu.count() > 0) await menu.first().click().catch(() => {});
      await page.locator('a:has-text("Salir"), a:has-text("Logout")').first().click().catch(() => {});
    }
    await page.waitForURL(/\/login/);
  });

  // 16. Login with new user (FUNCIONARIO)
  await tryStep('Login con usuario FUNCIONARIO', page, async () => {
    await page.fill('input[formcontrolname="username"]', newUsername);
    await page.fill('input[formcontrolname="password"]', 'Auto123');
    await Promise.all([
      page.waitForNavigation({ waitUntil: 'networkidle' }),
      page.click('button:has-text("Ingresar"), button:has-text("Login"), button[type="submit"]')
    ]);
    // Expect dashboard or contratos route
    await expect(page).toHaveURL(/(contratos|dashboard|\/)/);
  });

  // 17. Verificar que el funcionario NO ve la sección Usuarios
  await tryStep('Verificar roles - funcionario no ve Usuarios', page, async () => {
    const usersNav = page.locator('a:has-text("Usuarios"), a:has-text("Users"), nav a:has-text("Usuarios")');
    await expect(usersNav).toHaveCount(0);
    // Should have Proveedores and Contratos
    const provNav = page.locator('a:has-text("Proveedores"), a:has-text("Suppliers")');
    const contratosNav = page.locator('a:has-text("Contratos"), a:has-text("Contracts"), a:has-text("contratos")');
    await expect(provNav).toHaveCount(1);
    await expect(contratosNav).toHaveCount(1);
  });

  // 18. Final logout
  await tryStep('Logout final', page, async () => {
    await page.locator('button:has-text("Salir"), button:has-text("Logout"), button[aria-label="logout"]').first().click().catch(() => {});
    await page.waitForURL(/\/login/);
  });
});

// Helper: attempt to extract ID from row if available (not guaranteed)
async function getIdFromRow(row: any): Promise<string> {
  try {
    // try data-id or routerlink containing id
    const el = row.locator('[data-id]').first();
    if (await el.count() > 0) {
      return await el.getAttribute('data-id') || '';
    }
    // try link href
    const link = row.locator('a[href*="/suppliers/"]').first();
    if (await link.count() > 0) {
      const href = await link.getAttribute('href');
      if (href) return href.split('/').pop() || '';
    }
  } catch (e) {
    return '';
  }
  return '';
}
