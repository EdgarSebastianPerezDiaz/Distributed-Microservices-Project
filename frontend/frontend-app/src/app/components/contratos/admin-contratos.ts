import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth';
import { User, UserRole } from '../../models/auth.model';

@Component({
  selector: 'app-admin-contratos',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatButtonModule, MatIconModule, RouterModule],
  template: `
    <div class="container">
      <mat-card class="welcome-card">
        <mat-card-header>
          <mat-card-title>Panel de Administración</mat-card-title>
          <mat-card-subtitle>Administración del Sistema</mat-card-subtitle>
        </mat-card-header>

        <mat-card-content>
          <div class="summary-grid">
            <div class="summary-item">
              <span class="summary-label">Usuario</span>
              <strong>{{ currentUser?.fullName || currentUser?.username }}</strong>
            </div>
            <div class="summary-item">
              <span class="summary-label">Rol</span>
              <strong>{{ currentUser?.role }}</strong>
            </div>
            <div class="summary-item">
              <span class="summary-label">Correo</span>
              <strong>{{ currentUser?.email }}</strong>
            </div>
          </div>

          <hr>

          <p>
            Como administrador puedes gestionar usuarios, revisar registros y acceder al listado de contratos.
          </p>

          <div class="feature-grid">
            <div class="feature-card">
              <mat-icon>supervisor_account</mat-icon>
              <span>Gestión de usuarios</span>
            </div>
            <div class="feature-card">
              <mat-icon>visibility</mat-icon>
              <span>Revisión y auditoría</span>
            </div>
            <div class="feature-card">
              <mat-icon>description</mat-icon>
              <span>Acceso al listado de contratos</span>
            </div>
          </div>
        </mat-card-content>

        <mat-card-actions>
          <button mat-raised-button color="primary" routerLink="/contratos/list">
            <mat-icon>list</mat-icon>
            Ver lista de contratos
          </button>
          <button mat-stroked-button routerLink="/users">
            <mat-icon>people</mat-icon>
            Gestionar Usuarios
          </button>
          <button mat-raised-button (click)="logout()">
            <mat-icon>logout</mat-icon>
            Cerrar Sesión
          </button>
        </mat-card-actions>
      </mat-card>
    </div>
  `,
  styles: [`
    .container { min-height:100vh; display:flex; align-items:center; justify-content:center; padding:24px; background:linear-gradient(135deg,#edbb02 0%,#f5f7fa 100%);} 
    .welcome-card { width:100%; max-width:920px; border-radius:12px; }
    mat-card-header { text-align:center; }
    .summary-grid { display:grid; grid-template-columns:repeat(3,1fr); gap:16px; margin-bottom:20px; }
    .summary-item { background:#f5f7fa; padding:12px; border-left:4px solid #edbb02; border-radius:8px; }
    .feature-grid { display:grid; grid-template-columns:repeat(3,1fr); gap:14px; margin-top:16px; }
    .feature-card { padding:14px; border-radius:8px; background:#fff; border:1px solid #e5e7eb; display:flex; gap:8px; align-items:center; }
    mat-card-actions { display:flex; gap:12px; justify-content:flex-end; padding:16px; }
  `]
})
export class AdminContratosComponent implements OnInit {
  currentUser: User | null = null;
  UserRole = UserRole;

  constructor(private authService: AuthService) {}

  ngOnInit() {
    this.currentUser = this.authService.getCurrentUser();
  }

  logout() { this.authService.logout(); }
}
