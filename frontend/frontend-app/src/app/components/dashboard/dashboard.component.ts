import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../../services/auth';
import { User, UserRole } from '../../models/auth.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, MatCardModule, MatIconModule, MatButtonModule],
  template: `
    <div class="dashboard-container">
      <div class="welcome-section">
        <h1>Bienvenido, {{ currentUser?.fullName || currentUser?.username }}!</h1>
        <p class="role-badge">{{ roleLabel(currentUser?.role) }}</p>
      </div>

      <div class="cards-grid">
        <mat-card class="dashboard-card" *ngIf="isAdmin()">
          <mat-card-header>
            <mat-icon class="card-icon admin-color">manage_accounts</mat-icon>
            <mat-card-title>Gestión de Usuarios</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <p>Administra usuarios del sistema, asigna roles y permisos.</p>
          </mat-card-content>
          <mat-card-actions>
            <button mat-stroked-button routerLink="/users" color="primary">
              Acceder
              <mat-icon>arrow_forward</mat-icon>
            </button>
          </mat-card-actions>
        </mat-card>

        <mat-card class="dashboard-card">
          <mat-card-header>
            <mat-icon class="card-icon supplier-color">apartment</mat-icon>
            <mat-card-title>Gestión de Proveedores</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <p>Consulta y administra el registro de proveedores registrados.</p>
          </mat-card-content>
          <mat-card-actions>
            <button
              mat-stroked-button
              routerLink="/suppliers"
              color="accent"
            >
              Acceder
              <mat-icon>arrow_forward</mat-icon>
            </button>
          </mat-card-actions>
        </mat-card>

        <mat-card class="dashboard-card" *ngIf="isAdmin() || isFuncionario()">
          <mat-card-header>
            <mat-icon class="card-icon contract-color">description</mat-icon>
            <mat-card-title>Gestión de Contratos</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <p>Gestiona los contratos del sistema.</p>
          </mat-card-content>
          <mat-card-actions>
            <button mat-stroked-button routerLink="/contratos" disabled>
              Próximamente
              <mat-icon>schedule</mat-icon>
            </button>
          </mat-card-actions>
        </mat-card>

        <mat-card class="dashboard-card" *ngIf="isAdmin() || isAuditor()">
          <mat-card-header>
            <mat-icon class="card-icon audit-color">gavel</mat-icon>
            <mat-card-title>Panel de Auditoría</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <p>Revisa los registros de auditoría del sistema.</p>
          </mat-card-content>
          <mat-card-actions>
            <button mat-stroked-button routerLink="/auditoria" disabled>
              Próximamente
              <mat-icon>schedule</mat-icon>
            </button>
          </mat-card-actions>
        </mat-card>
      </div>
    </div>
  `,
  styles: `
    .dashboard-container {
      padding: 2rem;
      max-width: 1200px;
      margin: 0 auto;
    }

    .welcome-section {
      margin-bottom: 3rem;
      text-align: center;

      h1 {
        font-size: 2rem;
        color: #333;
        margin-bottom: 0.5rem;
      }

      .role-badge {
        display: inline-block;
        background: #edbb02;
        color: #333;
        padding: 0.5rem 1rem;
        border-radius: 20px;
        font-weight: 600;
      }
    }

    .cards-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
      gap: 2rem;

      mat-card {
        display: flex;
        flex-direction: column;
      }

      mat-card-header {
        display: flex;
        align-items: center;
        gap: 1rem;
        margin-bottom: 1rem;

        .card-icon {
          font-size: 2rem;
          width: 2rem;
          height: 2rem;

          &.admin-color {
            color: #1976d2;
          }

          &.supplier-color {
            color: #edbb02;
          }

          &.contract-color {
            color: #388e3c;
          }

          &.audit-color {
            color: #f57c00;
          }
        }

        mat-card-title {
          font-size: 1.2rem;
          margin: 0;
        }
      }

      mat-card-content {
        flex: 1;
        padding: 0 !important;

        p {
          color: #666;
          line-height: 1.5;
        }
      }

      mat-card-actions {
        padding-top: 1rem;
        margin-top: 1rem;
        border-top: 1px solid #f0f0f0;

        button {
          width: 100%;
          display: flex;
          justify-content: space-between;
          align-items: center;
        }
      }
    }
  `,
})
export class DashboardComponent implements OnInit {
  currentUser: User | null = null;
  readonly UserRole = UserRole;

  constructor(private authService: AuthService) {}

  ngOnInit() {
    this.currentUser = this.authService.getCurrentUser();
  }

  isAdmin(): boolean {
    return this.authService.hasRole(UserRole.ADMINISTRADOR);
  }

  isFuncionario(): boolean {
    return this.authService.hasRole(UserRole.FUNCIONARIO);
  }

  isAuditor(): boolean {
    return this.authService.hasRole(UserRole.AUDITOR);
  }

  roleLabel(role: UserRole | null | undefined): string {
    const map: Record<UserRole, string> = {
      [UserRole.ADMINISTRADOR]: 'Administrador del Sistema',
      [UserRole.FUNCIONARIO]: 'Funcionario',
      [UserRole.AUDITOR]: 'Auditor',
    };
    return role ? map[role] : 'Usuario';
  }
}
