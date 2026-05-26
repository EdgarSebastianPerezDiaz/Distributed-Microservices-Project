import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError, Subject } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { Supplier, SupplierPageResponse, SupplierStatus, PersonType } from '../models/supplier.model';

@Injectable({
  providedIn: 'root',
})
export class SupplierService {
  private apiUrl = environment.apiUrl;
  public refresh$ = new Subject<void>();
  // Optional temporary store for newly created suppliers to show immediately in UI
  public tempSuppliers: Supplier[] = [];

  constructor(private http: HttpClient) {}

  /**
   * Obtener lista de proveedores con paginación
   * Accesible para ADMIN y FUNCIONARIO (vista)
   */
  getSuppliers(
    page: number = 0,
    pageSize: number = 10,
    search?: string,
    status?: SupplierStatus | 'ALL',
    personType?: PersonType | 'ALL'
  ): Observable<SupplierPageResponse> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', pageSize.toString());
    
    if (search) {
      params = params.set('search', search);
    }

    if (status && status !== 'ALL') {
      params = params.set('status', status);
    }

    if (personType && personType !== 'ALL') {
      params = params.set('personType', personType);
    }

    return this.http.get<SupplierPageResponse>(
      `${this.apiUrl}/api/suppliers`,
      { params }
    ).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Obtener proveedor por ID
   */
  getSupplierById(id: string): Observable<Supplier> {
    return this.http.get<Supplier>(
      `${this.apiUrl}/api/suppliers/${id}`
    ).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Crear nuevo proveedor (solo ADMIN)
   */
  createSupplier(supplier: Supplier): Observable<Supplier> {
    return this.http.post<Supplier>(`${this.apiUrl}/api/suppliers`, supplier).pipe(
      tap((created: Supplier) => {
        this.refresh$.next();
        if (created) {
          this.tempSuppliers.push(created);
        }
      }),
      catchError(this.handleError)
    );
  }

  /**
   * Actualizar proveedor (solo ADMIN)
   */
  updateSupplier(id: string, supplier: Supplier): Observable<Supplier> {
    return this.http.put<Supplier>(`${this.apiUrl}/api/suppliers/${id}`, supplier).pipe(
      tap(() => this.refresh$.next()),
      catchError(this.handleError)
    );
  }

  /**
   * Eliminar proveedor (solo ADMIN)
   */
  deleteSupplier(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/api/suppliers/${id}`).pipe(
      tap(() => this.refresh$.next()),
      catchError(this.handleError)
    );
  }

  /**
   * Activar/Habilitar proveedor (solo ADMIN)
   */
  enableSupplier(id: string): Observable<Supplier> {
    return this.http.patch<Supplier>(`${this.apiUrl}/api/suppliers/${id}/estado`, { estado: 'HABILITADO' }).pipe(
      tap(() => this.refresh$.next()),
      catchError(this.handleError)
    );
  }

  /**
   * Desactivar/Inhabilitar proveedor (solo ADMIN)
   */
  disableSupplier(id: string): Observable<Supplier> {
    return this.http.patch<Supplier>(`${this.apiUrl}/api/suppliers/${id}/estado`, { estado: 'INHABILITADO' }).pipe(
      tap(() => this.refresh$.next()),
      catchError(this.handleError)
    );
  }

  /**
   * Cambiar estado del proveedor (genérico)
   */
  changeStatus(id: string, status: 'HABILITADO' | 'INHABILITADO'): Observable<Supplier> {
    return this.http.patch<Supplier>(
      `${this.apiUrl}/api/suppliers/${id}/estado`,
      { estado: status }
    ).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Validar que un NIT sea único
   */
  validateNit(nit: string): Observable<{ available: boolean }> {
    return this.http.get<{ available: boolean }>(
      `${this.apiUrl}/api/suppliers/validate/nit`,
      { params: new HttpParams().set('nit', nit) }
    ).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Validar que un email sea único
   */
  validateEmail(email: string): Observable<{ available: boolean }> {
    return this.http.get<{ available: boolean }>(
      `${this.apiUrl}/api/suppliers/validate/email`,
      { params: new HttpParams().set('email', email) }
    ).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Manejar errores HTTP
   */
  private handleError(error: HttpErrorResponse) {
    console.error('Supplier service error:', error);
    return throwError(() => error);
  }
}
