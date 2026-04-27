import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { User, PaginatedResponse } from '../models/auth.model';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getUsers(page: number = 0, pageSize: number = 10, search?: string): Observable<PaginatedResponse<User>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', pageSize.toString());
    
    if (search) {
      params = params.set('search', search);
    }

    return this.http.get<PaginatedResponse<User>>(`${this.apiUrl}/api/users`, { params });
  }

  getUserById(id: string): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/api/users/${id}`);
  }

  createUser(user: User): Observable<User> {
    return this.http.post<User>(`${this.apiUrl}/api/users`, user);
  }

  updateUser(id: string, user: User): Observable<User> {
    return this.http.put<User>(`${this.apiUrl}/api/users/${id}`, user);
  }

  deleteUser(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/api/users/${id}`);
  }

  activateUser(id: string): Observable<User> {
    return this.http.put<User>(`${this.apiUrl}/api/users/${id}/activate`, {});
  }

  deactivateUser(id: string): Observable<User> {
    return this.http.put<User>(`${this.apiUrl}/api/users/${id}/deactivate`, {});
  }

  validateUsername(username: string): Observable<{ available: boolean }> {
    return this.http.get<{ available: boolean }>(`${this.apiUrl}/api/users/validate/username`, {
      params: new HttpParams().set('username', username)
    });
  }

  validateEmail(email: string): Observable<{ available: boolean }> {
    return this.http.get<{ available: boolean }>(`${this.apiUrl}/api/users/validate/email`, {
      params: new HttpParams().set('email', email)
    });
  }
}
