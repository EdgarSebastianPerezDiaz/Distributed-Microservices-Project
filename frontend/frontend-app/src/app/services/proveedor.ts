import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';

/**
 * Interfaz para Proveedor
 */
export interface Proveedor {
  id?: string;
  nombre?: string;
  nit: string;
  email: string;
  telefono?: string;
  phone?: string;
  direccion?: string;
  tipoPersona?: string;
  personType?: string;
  estado?: string;
  status?: string;
  createdAt?: string;
  updatedAt?: string;
  businessName?: string;
  [key: string]: any;
}

/**
 * Interfaz para respuesta paginada de proveedores
 */
export interface ProveedorPageResponse {
  content: Proveedor[];
  totalElements: number;
  totalPages: number;
  currentPage?: number;
  pageSize?: number;
  pageable?: {
    pageNumber: number;
    pageSize: number;
  };
}

/**
 * Interfaz para cambio de estado
 */
export interface CambioEstado {
  estado: 'ACTIVO' | 'INACTIVO';
  status?: 'ACTIVO' | 'INACTIVO' | 'HABILITADO' | 'INHABILITADO';
}

@Injectable({
  providedIn: 'root',
})
export class ProveedorService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  /**
   * Obtener lista de proveedores con paginación
   * Accesible para ADMIN y FUNCIONARIO (vista)
   * 
   * @param page número de página (0-based)
   * @param pageSize tamaño de página
   * @param search término de búsqueda opcional
   * @returns Observable con lista paginada de proveedores
   */
  getProveedores(page: number = 0, pageSize: number = 10, search?: string): Observable<ProveedorPageResponse> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', pageSize.toString());
    
    if (search) {
      params = params.set('search', search);
    }

    return this.http.get<ProveedorPageResponse>(
      `${this.apiUrl}/api/suppliers`,
      { params }
    ).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Obtener proveedor por ID
   * 
   * @param id identificador del proveedor
   * @returns Observable con detalles del proveedor
   */
  getProveedorById(id: string): Observable<Proveedor> {
    return this.http.get<Proveedor>(
      `${this.apiUrl}/api/suppliers/${id}`
    ).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Crear nuevo proveedor (solo ADMIN/FUNCIONARIO)
   * 
   * @param data datos del proveedor a crear
   * @returns Observable con el proveedor creado
   */
  createProveedor(data: Proveedor): Observable<Proveedor> {
    return this.http.post<Proveedor>(
      `${this.apiUrl}/api/suppliers`,
      data
    ).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Actualizar proveedor (solo ADMIN)
   * 
   * @param id identificador del proveedor
   * @param data datos actualizados del proveedor
   * @returns Observable con el proveedor actualizado
   */
  updateProveedor(id: string, data: Proveedor): Observable<Proveedor> {
    return this.http.put<Proveedor>(
      `${this.apiUrl}/api/suppliers/${id}`,
      data
    ).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Cambiar estado del proveedor (ACTIVO/INACTIVO)
   * Solo ADMIN
   * 
   * @param id identificador del proveedor
   * @param estado nuevo estado (ACTIVO | INACTIVO)
   * @returns Observable con el proveedor actualizado
   */
  cambiarEstado(id: string, estado: 'ACTIVO' | 'INACTIVO' | 'HABILITADO' | 'INHABILITADO'): Observable<Proveedor> {
    // Mapir estados legacy a los enums que acepta el backend
    const statusValue = (estado === 'ACTIVO') ? 'HABILITADO' : (estado === 'INACTIVO' ? 'INHABILITADO' : estado);
    return this.http.patch<Proveedor>(
      `${this.apiUrl}/api/suppliers/${id}/estado`,
      { status: statusValue }
    ).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Desactivar/eliminar proveedor (desactivación lógica)
   * Solo ADMIN
   * 
   * @param id identificador del proveedor
   * @returns Observable completado
   */
  deleteProveedor(id: string): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/api/suppliers/${id}`
    ).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Validar que un NIT sea único
   * 
   * @param nit NIT a validar
   * @returns Observable con resultado de disponibilidad
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
   * 
   * @param email email a validar
   * @returns Observable con resultado de disponibilidad
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
   * 
   * @param error error HTTP recibido
   * @returns Observable con error
   */
  private handleError(error: HttpErrorResponse) {
    console.error('Supplier service error:', error);
    return throwError(() => error);
  }
}
