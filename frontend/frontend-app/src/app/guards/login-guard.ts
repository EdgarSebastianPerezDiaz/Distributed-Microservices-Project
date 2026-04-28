import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth';

/**
 * Guard que impide acceder a /login si ya estás autenticado.
 * Si estás autenticado, redirige al dashboard.
 */
export const loginGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Si ya estás autenticado, redirige a dashboard
  if (authService.isLoggedIn()) {
    router.navigate(['/dashboard']);
    return false;
  }

  // Si no estás autenticado, permite acceder a login
  return true;
};
