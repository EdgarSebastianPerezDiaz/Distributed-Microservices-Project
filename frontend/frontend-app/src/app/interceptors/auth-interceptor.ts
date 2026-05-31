import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth';
import { catchError, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Excepciones para endpoints que no necesitan token
  // Only exclude login endpoint; registration should go through auth flow if needed
  const excludedUrls = ['/api/auth/login'];
  const shouldExclude = excludedUrls.some(url => req.url.includes(url));

  if (!shouldExclude) {
    const token = authService.getToken();
    
    if (token) {
      // Clone la solicitud y agregue el header de autorización
      req = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    }
  }

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // Manejar errores 401 (token expirado o inválido)
      if (error.status === 401) {
        console.warn('Token inválido o expirado. Redirigiendo al login...');
        authService.logout();
        router.navigate(['/login'], { 
          queryParams: { returnUrl: router.routerState.snapshot.url }
        });
      }
      
      return throwError(() => error);
    })
  );
};
