import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
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

  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/api/auth/login`, credentials)
      .pipe(
        tap(response => {
          this.saveToken(response.token);
          this.currentUserSubject.next(response.user);
          this.isAuthenticatedSubject.next(true);
          sessionStorage.setItem('user', JSON.stringify(response.user));
        })
      );
  }

  register(payload: RegisterRequest): Observable<User> {
    return this.http.post<User>(`${this.apiUrl}/api/auth/register`, payload);
  }

  logout(): void {
    localStorage.removeItem('token');
    sessionStorage.removeItem('token');
    sessionStorage.removeItem('user');
    this.currentUserSubject.next(null);
    this.isAuthenticatedSubject.next(false);
    
    // Navegar a login después de logout
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem('token') || sessionStorage.getItem('token');
  }

  saveToken(token: string): void {
    localStorage.setItem('token', token);
    sessionStorage.setItem('token', token);
  }

  isLoggedIn(): boolean {
    return this.hasToken();
  }

  private hasToken(): boolean {
    return !!this.getToken();
  }

  getCurrentUser(): User | null {
    return this.currentUserSubject.value;
  }

  getUserRole(): UserRole | null {
    const user = this.getCurrentUser();
    return user ? user.role : null;
  }

  hasRole(role: UserRole | UserRole[]): boolean {
    const userRole = this.getUserRole();
    if (!userRole) return false;
    
    const rolesArray = Array.isArray(role) ? role : [role];
    return rolesArray.includes(userRole);
  }

  private loadCurrentUser(): void {
    // Primero intenta sessionStorage (sesión actual)
    let userJson = sessionStorage.getItem('user');
    
    // Si no está en sessionStorage, busca en localStorage
    if (!userJson) {
      userJson = localStorage.getItem('user');
    }

    if (userJson) {
      try {
        const user = JSON.parse(userJson);
        this.currentUserSubject.next(user);
        // Si estaba en localStorage pero no en sessionStorage, sincronizar
        if (!sessionStorage.getItem('user')) {
          sessionStorage.setItem('user', userJson);
        }
      } catch (e) {
        console.error('Error parsing user from storage', e);
      }
    }
  }
}
