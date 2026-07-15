import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';

export interface RegistroRequest {
  nombreCompleto: string;
  email: string;
  password: string;
}

export interface OlvidarPasswordRequest {
  email: string;
}

export interface RestablecerPasswordRequest {
  token: string;
  nuevaPassword: string;
}

interface RespuestaMensaje {
  mensaje: string;
}

// Endpoints de /api/auth/* que no son login/refresh/logout (ver
// AuthService para esos): crear cuenta, pedir recuperacion de
// contrasena, fijar una contrasena nueva con el token del enlace de
// recuperacion.
@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private http = inject(HttpClient);
  private base = `${environment.apiBaseUrl}/auth`;

  registrar(datos: RegistroRequest): Observable<RespuestaMensaje> {
    return this.http.post<RespuestaMensaje>(`${this.base}/registro`, datos);
  }

  olvidarPassword(datos: OlvidarPasswordRequest): Observable<RespuestaMensaje> {
    return this.http.post<RespuestaMensaje>(`${this.base}/olvidar-password`, datos);
  }

  restablecerPassword(datos: RestablecerPasswordRequest): Observable<RespuestaMensaje> {
    return this.http.post<RespuestaMensaje>(`${this.base}/restablecer-password`, datos);
  }
}
