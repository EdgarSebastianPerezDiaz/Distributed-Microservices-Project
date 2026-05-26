import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, throwError } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { LoginRequest, LoginResponse, RegisterRequest, User, UserRole } from '../models/auth.model';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private apiUrl = environment.apiUrl;
  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();
  
  private isAuthenticatedSubject = new BehaviorSubject<boolean>(this.hasToken());
  public isAuthenticated$ = this.isAuthenticatedSubject.asObservable();

  constructor(
    private http: HttpClient,
    private router: Router
  ) {
    this.loadCurrentUser();
  }

  /**
   * Realizar login con credenciales (legacy JWT method)
   */
  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(
      `${this.apiUrl}/api/auth/login`,
      credentials
    ).pipe(
      tap(response => {
        this.saveToken(response.token);
        this.currentUserSubject.next(response.user);
        this.isAuthenticatedSubject.next(true);
        localStorage.setItem('user', JSON.stringify(response.user));
      }),
      catchError((error: HttpErrorResponse) => {
        console.error('Login error:', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Registrar nuevo usuario (solo ADMIN)
   */
  register(payload: RegisterRequest): Observable<User> {
    return this.http.post<User>(
      `${this.apiUrl}/api/auth/register`,
      payload
    ).pipe(
      catchError((error: HttpErrorResponse) => {
        console.error('Register error:', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Cerrar sesión
   */
  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    this.currentUserSubject.next(null);
    this.isAuthenticatedSubject.next(false);
    this.router.navigate(['/login']);
  }

  /**
   * Obtener token JWT del almacenamiento
   */
  getToken(): string | null {
    return localStorage.getItem('token');
  }

  /**
   * Guardar token JWT en almacenamiento
   */
  saveToken(token: string): void {
    localStorage.setItem('token', token);
  }

  /**
   * Verificar si el usuario está autenticado
   */
  isLoggedIn(): boolean {
    return this.hasToken();
  }

  /**
   * Verificar si existe token
   */
  private hasToken(): boolean {
    return !!this.getToken();
  }

  /**
   * Obtener usuario actual
   */
  getCurrentUser(): User | null {
    return this.currentUserSubject.value;
  }

  /**
   * Obtener rol del usuario actual
   */
  getUserRole(): UserRole | null {
    const user = this.getCurrentUser();
    return user ? user.role : null;
  }

  /**
   * Verificar si el usuario tiene un rol específico
   */
  hasRole(role: UserRole | UserRole[]): boolean {
    const userRole = this.getUserRole();
    if (!userRole) return false;
    
    const rolesArray = Array.isArray(role) ? role : [role];
    return rolesArray.includes(userRole);
  }

  /**
   * Cargar usuario actual del almacenamiento local
   */
  private loadCurrentUser(): void {
    const userJson = localStorage.getItem('user');
    if (userJson) {
      try {
        const user = JSON.parse(userJson);
        this.currentUserSubject.next(user);
      } catch (e) {
        console.error('Error parsing user from storage', e);
        localStorage.removeItem('user');
      }
    }
  }
}
