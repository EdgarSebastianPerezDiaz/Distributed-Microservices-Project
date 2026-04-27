/**
 * Modelos de Datos para el Frontend
 * Frontend-Dev1 - Angular 18+
 */

/**
 * Respuesta de Login
 */
export interface LoginResponse {
  token: string;
  type: string;
  user: User;
}

/**
 * Modelo de Usuario
 */
export interface User {
  id: string;
  username: string;
  email: string;
  fullName: string;
  role: string;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
}

/**
 * Solicitud de Login
 */
export interface LoginRequest {
  username: string;
  password: string;
}

/**
 * Solicitud de Registro
 */
export interface RegisterRequest {
  username: string;
  password: string;
  email: string;
  fullName: string;
  role: string;
}

/**
 * Respuesta de Operación
 */
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp?: string;
}

/**
 * Token Decodificado
 */
export interface DecodedToken {
  sub: string;
  username: string;
  role: string;
  exp: number;
  iat?: number;
}

/**
 * Estado de Autenticación
 */
export interface AuthState {
  isAuthenticated: boolean;
  user: User | null;
  token: string | null;
  loading: boolean;
  error: string | null;
}
