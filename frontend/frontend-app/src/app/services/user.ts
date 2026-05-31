import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError, Subject } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { User, PaginatedResponse, UserStatus } from '../models/auth.model';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  private apiUrl = environment.apiUrl;
  // Subject to notify components to refresh lists after mutations
  public refresh$ = new Subject<void>();
  // Temporal in-memory store for newly created users
  public tempUsers: User[] = [];

  constructor(private http: HttpClient) {}

  /**
   * Obtener lista de usuarios con paginación (solo ADMIN)
   */
  getUsers(
    page: number = 0,
    pageSize: number = 10,
    search?: string,
    role?: string,
    active?: UserStatus | 'ALL'
  ): Observable<PaginatedResponse<User>> {
    // Try server-side pagination first (if backend supports it)
    let params = new HttpParams()
      .set('page', String(page))
      .set('size', String(pageSize));

    if (search) params = params.set('search', search);
    if (role && role !== 'ALL') params = params.set('rol', role);
    if (active && active !== 'ALL') params = params.set('estado', String(active));

    return this.http.get<any>(`${this.apiUrl}/api/auth/users`, { params }).pipe(
      map((response) => {
        // If backend returns a paginated response with `content`, use it directly
        if (response && Array.isArray(response.content)) {
          return response as PaginatedResponse<User>;
        }

        // If backend returned a plain array, fall back to client-side filtering + pagination
        const list: User[] = Array.isArray(response) ? response : [];
        const searchLower = search?.trim().toLowerCase() || '';

        const filtered = list.filter((user) => {
          const matchesSearch = !searchLower ||
            user.fullName?.toLowerCase().includes(searchLower) ||
            user.username.toLowerCase().includes(searchLower) ||
            user.email.toLowerCase().includes(searchLower);

          const matchesRole = !role || role === 'ALL' || user.role === role;
          const matchesActive = !active || active === 'ALL' ||
            (active === UserStatus.ACTIVO ? user.active === true : user.active === false);

          return matchesSearch && matchesRole && matchesActive;
        });

        const start = page * pageSize;
        const paged = filtered.slice(start, start + pageSize);

        return {
          content: paged,
          totalElements: filtered.length,
          totalPages: Math.ceil(filtered.length / pageSize),
          currentPage: page,
          pageSize,
        } as PaginatedResponse<User>;
      }),
      catchError(this.handleError)
    );
  }

  /**
   * Obtener usuario por ID (solo ADMIN)
   */
  getUserById(id: string): Observable<User> {
    return this.http.get<User>(
      `${this.apiUrl}/api/auth/users/${id}`
    ).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Crear nuevo usuario (solo ADMIN)
   */
  createUser(user: User): Observable<User> {
    return this.http.post<User>(`${this.apiUrl}/api/auth/register`, user).pipe(
      tap((created: User) => {
        this.refresh$.next();
        if (created) {
          this.tempUsers.push(created);
        }
      }),
      catchError(this.handleError)
    );
  }

  /**
   * Actualizar usuario (solo ADMIN)
   */
  updateUser(id: string, user: User): Observable<User> {
    return this.http.put<User>(`${this.apiUrl}/api/auth/users/${id}`, user).pipe(
      tap(() => this.refresh$.next()),
      catchError(this.handleError)
    );
  }

  /**
   * Eliminar usuario (solo ADMIN)
   */
  deleteUser(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/api/auth/users/${id}`).pipe(
      tap(() => this.refresh$.next()),
      catchError(this.handleError)
    );
  }

  /**
   * Activar usuario (cambiar estado a activo)
   */
  activateUser(id: string): Observable<User> {
    return this.http.patch<User>(`${this.apiUrl}/api/auth/users/${id}/estado`, { estado: true }).pipe(
      tap(() => this.refresh$.next()),
      catchError(this.handleError)
    );
  }

  /**
   * Desactivar usuario (cambiar estado a inactivo)
   */
  deactivateUser(id: string): Observable<User> {
    return this.http.patch<User>(`${this.apiUrl}/api/auth/users/${id}/estado`, { estado: false }).pipe(
      tap(() => this.refresh$.next()),
      catchError(this.handleError)
    );
  }

  /**
   * Validar que un username sea único
   */
  validateUsername(username: string): Observable<{ available: boolean }> {
    return this.http.get<{ available: boolean }>(
      `${this.apiUrl}/api/auth/users/validate/username`,
      { params: new HttpParams().set('username', username) }
    ).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Validar que un email sea único
   */
  validateEmail(email: string): Observable<{ available: boolean }> {
    return this.http.get<{ available: boolean }>(
      `${this.apiUrl}/api/auth/users/validate/email`,
      { params: new HttpParams().set('email', email) }
    ).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Manejar errores HTTP
   */
  private handleError(error: HttpErrorResponse) {
    console.error('User service error:', error);
    console.error('Error status:', error.status);
    console.error('Error message:', error.message);
    console.error('Error statusText:', error.statusText);
    return throwError(() => error);
  }
}
