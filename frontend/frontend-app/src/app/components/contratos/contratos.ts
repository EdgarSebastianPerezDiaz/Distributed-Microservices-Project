import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth';
import { UserRole } from '../../models/auth.model';

@Component({
  selector: 'app-contratos',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    RouterModule
  ],
  template: `
    <div class="container">
      <div class="hero-card">
        <div class="hero-copy">
          <span class="eyebrow">Módulo de contratos</span>
          <h1>Consulta y crea contratos desde una sola interfaz</h1>
          <p>
            Desde aquí puedes revisar el listado de contratos registrados, buscar por proveedor o número y abrir el formulario de creación.
          </p>
          <div class="actions">
            <button mat-raised-button color="primary" routerLink="/contratos" *ngIf="isFuncionario()">
              <mat-icon>table_view</mat-icon>
              Ver lista de contratos
            </button>
            <button mat-stroked-button routerLink="/contratos/new" *ngIf="isFuncionario()">
              <mat-icon>add_circle</mat-icon>
              Crear contrato
            </button>
          </div>
        </div>
        <div class="hero-panel">
          <div class="panel-item">
            <mat-icon>description</mat-icon>
            <span>Listado paginado</span>
          </div>
          <div class="panel-item">
            <mat-icon>search</mat-icon>
            <span>Búsqueda por texto</span>
          </div>
          <div class="panel-item">
            <mat-icon>verified_user</mat-icon>
            <span>Acceso por rol</span>
          </div>
        </div>
      </div>
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

    .hero-card {
      width: 100%;
      max-width: 1100px;
      display: grid;
      grid-template-columns: 1.2fr 0.8fr;
      gap: 24px;
      align-items: stretch;
      box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
      border-radius: 12px;
      background: white;
      padding: 32px;
    }

    .hero-copy {
      display: flex;
      flex-direction: column;
      justify-content: center;
    }

    .eyebrow {
      display: inline-block;
      margin-bottom: 14px;
      text-transform: uppercase;
      letter-spacing: 0.08em;
      color: #9a7c00;
      font-weight: 700;
      font-size: 12px;
    }

    h1 {
      margin: 0 0 14px;
      color: #1f2933;
      font-size: 40px;
      color: #1f2933;
      font-weight: 600;
    }

    p {
      margin: 0;
      line-height: 1.7;
      color: #4a5568;
      max-width: 56ch;
    }

    .actions {
      display: flex;
      gap: 12px;
      flex-wrap: wrap;
      margin-top: 24px;
    }

    .hero-panel {
      display: grid;
      gap: 14px;
      align-content: center;
      padding: 18px;
      border-radius: 10px;
      background: linear-gradient(180deg, #f8fafc 0%, #eef2f7 100%);
      border: 1px solid #e5e7eb;
    }

    .panel-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 16px;
      border-radius: 8px;
      background: white;
      color: #1f2933;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);

      mat-icon {
        color: #edbb02;
      }
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

    @media (max-width: 900px) {
      .hero-card {
        grid-template-columns: 1fr;
      }

      h1 {
        font-size: 30px;
      }
    }

    @media (max-width: 768px) {
      .container {
        padding: 12px;
      }

      .hero-card {
        padding: 20px;
      }

      .actions {
        flex-direction: column;
      }

      .actions button {
        width: 100%;
      }
    }
  `]
})
export class ContratosComponent {
  UserRole = UserRole;

  constructor(private authService: AuthService) {}

  isFuncionario(): boolean {
    return this.authService.hasRole(UserRole.FUNCIONARIO);
  }

  logout() {
    this.authService.logout();
  }
}
