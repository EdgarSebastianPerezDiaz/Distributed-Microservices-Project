import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ContractCreateRequest, ContractResponse } from '../models/contract.model';

@Injectable({
  providedIn: 'root',
})
export class ContractService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  createContract(request: ContractCreateRequest): Observable<ContractResponse> {
    return this.http.post<ContractResponse>(`${this.apiUrl}/api/contracts`, request);
  }
}