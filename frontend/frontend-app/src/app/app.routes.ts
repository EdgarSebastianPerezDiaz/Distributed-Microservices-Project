import { Routes } from '@angular/router';
import { authGuard } from './guards/auth-guard';
import { roleGuard } from './guards/role-guard';
import { loginGuard } from './guards/login-guard';
import { UserRole } from './models/auth.model';
import { MainLayoutComponent } from './layout/main-layout.component';

export const routes: Routes = [
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
      import('./components/auth/register/register').then((m) => m.RegisterComponent),
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
          import('./components/users/user-list/user-list').then((m) => m.UserListComponent),
      },
      {
        path: 'users',
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
    redirectTo: '/dashboard',
  },
];
