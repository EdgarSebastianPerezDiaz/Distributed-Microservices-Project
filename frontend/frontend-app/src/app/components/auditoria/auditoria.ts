import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-auditoria',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatTableModule,
    RouterModule
  ],
  template: `
    <div class="container">
      <mat-card class="welcome-card">
        <mat-card-header>
          <mat-card-title>Panel de Auditoría</mat-card-title>
          <mat-card-subtitle>Registro y Monitoreo de Eventos del Sistema</mat-card-subtitle>
        </mat-card-header>
        
        <mat-card-content>
          <p>Usuario: <strong>{{ currentUser?.username }}</strong></p>
          <p>Rol: <strong>{{ currentUser?.role }}</strong></p>
          <p>Email: <strong>{{ currentUser?.email }}</strong></p>
          
          <hr style="margin: 20px 0;">
          
          <p>
            Como <strong>AUDITOR</strong>, tienes acceso a información de auditoría del sistema:
          </p>
          
          <ul>
            <li>Registro de todas las acciones de usuarios</li>
            <li>Cambios en datos sensibles</li>
            <li>Accesos al sistema (logins/logouts)</li>
            <li>Operaciones sobre contratos y proveedores</li>
            <li>Generación de reportes de auditoría</li>
          </ul>
          
          <p style="margin-top: 20px; color: #666; font-size: 12px;">
            <em>Este es un componente placeholder. La funcionalidad completa será implementada en la siguiente fase.</em>
          </p>
        </mat-card-content>
        
        <mat-card-actions>
          <button mat-raised-button color="primary" routerLink="/suppliers">
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
      padding: 20px;
      max-width: 800px;
      margin: 0 auto;
    }
    
    .welcome-card {
      margin-top: 20px;
    }
    
    mat-card-title {
      font-size: 24px;
      margin-bottom: 10px;
    }
    
    mat-card-subtitle {
      color: #666;
    }
    
    p {
      margin: 10px 0;
      line-height: 1.6;
    }
    
    ul {
      margin-left: 20px;
      line-height: 1.8;
    }
    
    mat-card-actions {
      display: flex;
      gap: 10px;
      justify-content: flex-end;
      padding: 20px;
    }
    
    button {
      margin-left: 10px;
    }
  `]
})
export class AuditoriaComponent implements OnInit {
  currentUser: any = null;

  constructor(private authService: AuthService) {}

  ngOnInit() {
    this.currentUser = this.authService.getCurrentUser();
  }

  logout() {
    this.authService.logout();
  }
}
