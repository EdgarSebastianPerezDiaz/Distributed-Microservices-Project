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
import { ContractListComponent } from './components/contratos/contract-list/contract-list';
import { AuditoriaComponent } from './components/auditoria/auditoria';
import { UserRole } from './models/auth.model';

export const routes: Routes = [
  {
    path: '',
    redirectTo: '/login',
    pathMatch: 'full'
  },
  {
    path: 'login',
    canActivate: [loginGuard],
    component: LoginComponent
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    component: UserListComponent // Temporal - será reemplazado con componente dashboard
  },
  {
    path: 'users',
    canActivate: [authGuard, roleGuard],
    data: { roles: [UserRole.ADMINISTRADOR] },
    children: [
      {
        path: '',
        component: UserListComponent
      },
      {
        path: 'new',
        component: UserFormComponent
      },
      {
        path: ':id/edit',
        component: UserFormComponent
      },
      {
        path: ':id',
        component: UserDetailComponent
      }
    ]
  },
  {
    path: 'suppliers',
    canActivate: [authGuard],
    children: [
      {
        path: '',
        component: SupplierListComponent
      },
      {
        path: 'new',
        canActivate: [roleGuard],
        data: { roles: [UserRole.ADMINISTRADOR] },
        component: SupplierFormComponent
      },
      {
        path: ':id/edit',
        canActivate: [roleGuard],
        data: { roles: [UserRole.ADMINISTRADOR] },
        component: SupplierFormComponent
      },
      {
        path: ':id',
        component: SupplierDetailComponent
      }
    ]
  },
  {
    path: 'contratos',
    canActivate: [authGuard, roleGuard],
    data: { roles: [UserRole.ADMINISTRADOR, UserRole.FUNCIONARIO] },
    component: ContratosComponent
  },
  {
    path: 'contratos/list',
    canActivate: [authGuard, roleGuard],
    data: { roles: [UserRole.ADMINISTRADOR, UserRole.FUNCIONARIO] },
    component: ContractListComponent
  },
  {
    path: 'contratos/new',
    canActivate: [authGuard, roleGuard],
    data: { roles: [UserRole.FUNCIONARIO] },
    component: ContractFormComponent
  },
  {
    path: 'auditoria',
    canActivate: [authGuard, roleGuard],
    data: { roles: [UserRole.AUDITOR] },
    component: AuditoriaComponent
  },
  {
    path: 'access-denied',
    component: UserListComponent // Temporal
  },
  {
    path: '**',
    redirectTo: '/login'
  }
];
