import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Supplier, SupplierPageResponse } from '../models/supplier.model';

@Injectable({
  providedIn: 'root',
})
export class SupplierService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getSuppliers(page: number = 0, pageSize: number = 10, search?: string): Observable<SupplierPageResponse> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', pageSize.toString());
    
    if (search) {
      params = params.set('search', search);
    }

    return this.http.get<SupplierPageResponse>(`${this.apiUrl}/api/suppliers`, { params });
  }

  getSupplierById(id: string): Observable<Supplier> {
    return this.http.get<Supplier>(`${this.apiUrl}/api/suppliers/${id}`);
  }

  createSupplier(supplier: Supplier): Observable<Supplier> {
    return this.http.post<Supplier>(`${this.apiUrl}/api/suppliers`, supplier);
  }

  updateSupplier(id: string, supplier: Supplier): Observable<Supplier> {
    return this.http.put<Supplier>(`${this.apiUrl}/api/suppliers/${id}`, supplier);
  }

  deleteSupplier(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/api/suppliers/${id}`);
  }

  enableSupplier(id: string): Observable<Supplier> {
    return this.http.put<Supplier>(`${this.apiUrl}/api/suppliers/${id}/enable`, {});
  }

  disableSupplier(id: string): Observable<Supplier> {
    return this.http.put<Supplier>(`${this.apiUrl}/api/suppliers/${id}/disable`, {});
  }

  validateNit(nit: string): Observable<{ available: boolean }> {
    return this.http.get<{ available: boolean }>(`${this.apiUrl}/api/suppliers/validate/nit`, {
      params: new HttpParams().set('nit', nit)
    });
  }

  validateEmail(email: string): Observable<{ available: boolean }> {
    return this.http.get<{ available: boolean }>(`${this.apiUrl}/api/suppliers/validate/email`, {
      params: new HttpParams().set('email', email)
    });
  }
}
