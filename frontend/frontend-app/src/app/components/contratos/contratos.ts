import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth';
import { User, UserRole } from '../../models/auth.model';

@Component({
  selector: 'app-contratos',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    RouterModule
  ],
  template: `
    <div class="container">
      <mat-card class="welcome-card">
        <mat-card-header>
          <mat-card-title>Bienvenido al Módulo de Contratos</mat-card-title>
          <mat-card-subtitle>Gestión de Contratos Administrativos</mat-card-subtitle>
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
            Este módulo permite la gestión integral de contratos.
            Aquí podrás registrar nuevos contratos, consultar sus datos y continuar con la gestión operativa.
          </p>
          
          <div class="feature-grid">
            <div class="feature-card">
              <mat-icon>description</mat-icon>
              <span>Creación de contratos</span>
            </div>
            <div class="feature-card">
              <mat-icon>verified_user</mat-icon>
              <span>Validación contra backend</span>
            </div>
            <div class="feature-card">
              <mat-icon>storage</mat-icon>
              <span>Persistencia en base de datos</span>
            </div>
          </div>

          <p class="helper-text" *ngIf="!isFuncionario()">
            La creación de contratos está habilitada solo para el rol FUNCIONARIO.
          </p>
        </mat-card-content>
        
        <mat-card-actions>
          <button mat-raised-button color="primary" routerLink="/contratos/new" *ngIf="isFuncionario()">
            <mat-icon>add_circle</mat-icon>
            Crear contrato
          </button>
          <button mat-stroked-button routerLink="/suppliers">
            Ver Proveedores
          </button>
          <button mat-raised-button (click)="logout()">
            Cerrar Sesión
          </button>
        </mat-card-actions>
      </mat-card>
    </div>
  `,
  styles: [`
    .container {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 24px;
      background: linear-gradient(135deg, #edbb02 0%, #f5f7fa 100%);
    }

    .welcome-card {
      width: 100%;
      max-width: 920px;
      box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
      border-radius: 12px;
    }

    mat-card-header {
      text-align: center;
      margin-bottom: 12px;
    }

    mat-card-title {
      font-size: 28px;
      margin-bottom: 10px;
      color: #1f2933;
      font-weight: 600;
    }

    mat-card-subtitle {
      color: #738096;
    }

    .summary-grid {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: 16px;
      margin-bottom: 20px;
    }

    .summary-item {
      background: #f5f7fa;
      border-radius: 8px;
      padding: 16px;
      border-left: 4px solid #edbb02;
    }

    .summary-label {
      display: block;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      font-size: 12px;
      color: #738096;
      margin-bottom: 4px;
    }

    hr {
      margin: 24px 0;
      border: 0;
      border-top: 1px solid #e2e8f0;
    }

    p {
      margin: 10px 0;
      line-height: 1.6;
      color: #334155;
    }

    .feature-grid {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: 14px;
      margin-top: 22px;
    }

    .feature-card {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      gap: 10px;
      padding: 18px;
      border-radius: 10px;
      background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
      border: 1px solid #e5e7eb;
      color: #1f2933;

      mat-icon {
        color: #edbb02;
      }
    }

    .helper-text {
      margin-top: 18px;
      padding: 12px 14px;
      border-radius: 8px;
      background: #f5f7fa;
      border-left: 4px solid #edbb02;
      color: #4a5568;
      font-size: 13px;
    }

    mat-card-actions {
      display: flex;
      gap: 10px;
      justify-content: flex-end;
      padding: 20px;
    }

    button[mat-raised-button]:not(:disabled) {
      background-color: #edbb02 !important;
      color: #1f2933 !important;
    }

    button[mat-raised-button]:not(:disabled):hover {
      background-color: #d4a602 !important;
    }

    button[mat-stroked-button] {
      border-color: #edbb02 !important;
      color: #1f2933 !important;
    }

    button mat-icon {
      margin-right: 8px;
    }

    @media (max-width: 768px) {
      .container {
        padding: 12px;
      }

      .summary-grid,
      .feature-grid {
        grid-template-columns: 1fr;
      }

      mat-card-title {
        font-size: 24px;
      }

      mat-card-actions {
        flex-direction: column;
      }

      button[mat-raised-button],
      button[mat-stroked-button] {
        width: 100%;
      }
    }
  `]
})
export class ContratosComponent implements OnInit {
  currentUser: User | null = null;
  UserRole = UserRole;

  constructor(private authService: AuthService) {}

  ngOnInit() {
    this.currentUser = this.authService.getCurrentUser();
  }

  isFuncionario(): boolean {
    return this.authService.hasRole(UserRole.FUNCIONARIO);
  }

  logout() {
    this.authService.logout();
  }
}
