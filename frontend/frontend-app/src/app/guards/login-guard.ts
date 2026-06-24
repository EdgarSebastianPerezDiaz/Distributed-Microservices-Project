import { inject } from '@angular/core';
import { CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth';

/**
 * Guard que permite acceder al login siempre.
 * La pantalla de login debe ser el punto de entrada inicial incluso si existe una sesión previa.
 */
export const loginGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);

  return true;
};
