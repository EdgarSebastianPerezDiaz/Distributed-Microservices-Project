import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);

  // Excepciones para endpoints que no necesitan token
  const excludedUrls = ['/api/auth/login', '/api/auth/register'];
  const shouldExclude = excludedUrls.some(url => req.url.includes(url));

  if (shouldExclude) {
    return next(req);
  }

  const token = authService.getToken();
  
  if (token) {
    // Clone la solicitud y agregue el header de autorización
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  return next(req);
};
