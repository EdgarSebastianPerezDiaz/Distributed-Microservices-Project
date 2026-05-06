import type { Routes } from '@angular/router';

export const usersRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('../components/users/user-list/user-list').then((m) => m.UserListComponent),
  },
  {
    path: 'new',
    loadComponent: () =>
      import('../components/users/user-form/user-form').then((m) => m.UserFormComponent),
  },
  {
    path: ':id/edit',
    loadComponent: () =>
      import('../components/users/user-form/user-form').then((m) => m.UserFormComponent),
  },
  {
    path: ':id',
    loadComponent: () =>
      import('../components/users/user-detail/user-detail').then((m) => m.UserDetailComponent),
  },
];
