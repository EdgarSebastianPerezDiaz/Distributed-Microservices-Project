import { Routes } from '@angular/router';
import { authGuard } from './guards/auth-guard';
import { roleGuard } from './guards/role-guard';
import { loginGuard } from './guards/login-guard';
import { LoginComponent } from './components/auth/login/login';
import { UserListComponent } from './components/users/user-list/user-list';
import { UserFormComponent } from './components/users/user-form/user-form';
import { UserDetailComponent } from './components/users/user-detail/user-detail';
import { SupplierListComponent } from './components/suppliers/supplier-list/supplier-list';
import { SupplierFormComponent } from './components/suppliers/supplier-form/supplier-form';
import { SupplierDetailComponent } from './components/suppliers/supplier-detail/supplier-detail';
import { ContratosComponent } from './components/contratos/contratos';
import { ContractFormComponent } from './components/contratos/contract-form/contract-form';
import { ContractEditComponent } from './components/contratos/contract-edit/contract-edit';
import { ContractListComponent } from './components/contratos/contract-list/contract-list';
import { AuditoriaComponent } from './components/auditoria/auditoria';
import { UserRole } from './models/auth.model';
import { MainLayoutComponent } from './layout/main-layout.component';

export const routes: Routes = [
  {
    path: '',
    redirectTo: '/login',
    pathMatch: 'full'
  },
  {
    path: 'login',
    canActivate: [loginGuard],
    loadComponent: () =>
      import('./components/auth/login/login').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    canActivate: [loginGuard],
    loadComponent: () =>
      import('./components/auth/login/login').then((m) => m.LoginComponent),
  },
  {
    path: '',
    component: MainLayoutComponent,
    canActivate: [authGuard],
    children: [
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full',
      },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./components/dashboard/dashboard.component').then((m) => m.DashboardComponent),
      },
      {
        path: 'users',
        canActivate: [roleGuard],
        data: { roles: [UserRole.ADMINISTRADOR] },
        loadChildren: () =>
          import('./routes/users.routes').then((m) => m.usersRoutes),
      },
      {
        path: 'usuarios',
        canActivate: [roleGuard],
        data: { roles: [UserRole.ADMINISTRADOR] },
        loadChildren: () =>
          import('./routes/users.routes').then((m) => m.usersRoutes),
      },
      {
        path: 'suppliers',
        loadChildren: () =>
          import('./routes/suppliers.routes').then((m) => m.suppliersRoutes),
      },
      {
        path: 'contratos',
        canActivate: [roleGuard],
        data: { roles: [UserRole.ADMINISTRADOR, UserRole.FUNCIONARIO] },
        loadComponent: () =>
          import('./components/contratos/contratos').then((m) => m.ContratosComponent),
      },
      {
        path: 'admin',
        redirectTo: 'users',
        pathMatch: 'full',
      },
      {
        path: 'contratos/list',
        canActivate: [authGuard, roleGuard],
        data: { roles: [UserRole.ADMINISTRADOR, UserRole.FUNCIONARIO] },
        component: ContractListComponent,
      },
      {
        path: 'contratos/new',
        canActivate: [authGuard, roleGuard],
        data: { roles: [UserRole.FUNCIONARIO] },
        component: ContractFormComponent,
      },
      {
        path: 'contratos/:id/edit',
        canActivate: [authGuard, roleGuard],
        data: { roles: [UserRole.FUNCIONARIO] },
        component: ContractEditComponent,
      },
      {
        path: 'contratos/:id',
        canActivate: [authGuard, roleGuard],
        data: { roles: [UserRole.ADMINISTRADOR, UserRole.FUNCIONARIO] },
        loadComponent: () =>
          import('./components/contratos/contract-detail/contract-detail').then(
            (m) => m.ContractDetailComponent,
          ),
      },
      {
        path: 'auditoria',
        canActivate: [roleGuard],
        data: {
          roles: [UserRole.AUDITOR, UserRole.ADMINISTRADOR],
        },
        loadComponent: () =>
          import('./components/auditoria/auditoria').then((m) => m.AuditoriaComponent),
      },
      {
        path: 'access-denied',
        loadComponent: () =>
          import('./components/users/user-list/user-list').then((m) => m.UserListComponent),
      },
    ],
  },
  {
    path: '**',
    redirectTo: '/login',
  },
];
