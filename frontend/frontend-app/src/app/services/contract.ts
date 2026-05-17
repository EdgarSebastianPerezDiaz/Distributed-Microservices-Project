import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  ContractCreateRequest,
  ContractResponse,
  ContractUpdateRequest,
  PaginatedContractResponse,
} from '../models/contract.model';

@Injectable({
  providedIn: 'root',
})
export class ContractService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  createContract(request: ContractCreateRequest): Observable<ContractResponse> {
    return this.http.post<ContractResponse>(`${this.apiUrl}/api/contracts`, request);
  }

  getContractById(contractId: string): Observable<ContractResponse> {
    return this.http.get<ContractResponse>(`${this.apiUrl}/api/contracts/${contractId}`);
  }

  updateContract(contractId: string, request: ContractUpdateRequest): Observable<ContractResponse> {
    return this.http.put<ContractResponse>(`${this.apiUrl}/api/contracts/${contractId}`, request);
  }

  downloadContractPdf(contractId: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/api/contracts/${contractId}/pdf`, {
      responseType: 'blob',
    });
  }

  getContracts(page: number = 0, size: number = 10, search?: string): Observable<PaginatedContractResponse> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (search) {
      params = params.set('search', search);
    }

    return this.http.get<PaginatedContractResponse>(`${this.apiUrl}/api/contracts`, { params });
  }
}