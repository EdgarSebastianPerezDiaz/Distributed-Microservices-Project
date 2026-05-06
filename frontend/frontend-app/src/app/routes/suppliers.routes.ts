import type { Routes } from '@angular/router';
import { roleGuard } from '../guards/role-guard';
import { UserRole } from '../models/auth.model';

export const suppliersRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('../components/suppliers/supplier-list/supplier-list').then(
        (m) => m.SupplierListComponent,
      ),
  },
  {
    path: 'new',
    canActivate: [roleGuard],
    data: { roles: [UserRole.ADMINISTRADOR] },
    loadComponent: () =>
      import('../components/suppliers/supplier-form/supplier-form').then(
        (m) => m.SupplierFormComponent,
      ),
  },
  {
    path: ':id/edit',
    canActivate: [roleGuard],
    data: { roles: [UserRole.ADMINISTRADOR] },
    loadComponent: () =>
      import('../components/suppliers/supplier-form/supplier-form').then(
        (m) => m.SupplierFormComponent,
      ),
  },
  {
    path: ':id',
    loadComponent: () =>
      import('../components/suppliers/supplier-detail/supplier-detail').then(
        (m) => m.SupplierDetailComponent,
      ),
  },
];
