export interface LoginRequest {
  username?: string;
  email?: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  user: User;
}

export interface User {
  id?: string;
  username: string;
  email: string;
  fullName?: string;
  password?: string;
  role: UserRole;
  status: UserStatus;
  createdAt?: string;
  updatedAt?: string;
}

export enum UserRole {
  ADMINISTRADOR = 'ADMINISTRADOR',
  FUNCIONARIO = 'FUNCIONARIO',
  AUDITOR = 'AUDITOR'
}

export enum UserStatus {
  ACTIVO = 'ACTIVO',
  INACTIVO = 'INACTIVO'
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
}
