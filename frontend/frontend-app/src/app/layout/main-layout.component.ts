import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  NavigationEnd,
  Router,
  RouterModule,
  RouterOutlet,
} from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { filter } from 'rxjs/operators';
import { AuthService } from '../services/auth';
import { User, UserRole } from '../models/auth.model';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    RouterOutlet,
    MatSidenavModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatListModule,
    MatMenuModule,
    MatDividerModule,
  ],
  templateUrl: './main-layout.component.html',
  styleUrl: './main-layout.component.scss',
})
export class MainLayoutComponent implements OnInit {
  currentUser: User | null = null;
  shellToolbarLabel = 'Portal de Compras';
  readonly UserRole = UserRole;

  constructor(
    private authService: AuthService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();
    this.authService.currentUser$.subscribe((u) => {
      this.currentUser = u;
    });
    this.updateToolbarLabel(this.router.url);
    this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe((e) => this.updateToolbarLabel(e.urlAfterRedirects));
  }

  private updateToolbarLabel(rawUrl: string): void {
    const url = (rawUrl || '').split('?')[0];
    if (url.startsWith('/users')) {
      this.shellToolbarLabel = 'Usuarios del Sistema';
    } else if (url.startsWith('/suppliers')) {
      this.shellToolbarLabel = 'Vista de Proveedores';
    } else if (url.startsWith('/contratos')) {
      this.shellToolbarLabel = 'Gestión de Contratos';
    } else if (url.startsWith('/auditoria')) {
      this.shellToolbarLabel = 'Panel de Auditoría Institucional';
    } else if (url.startsWith('/dashboard')) {
      this.shellToolbarLabel = 'Inicio';
    } else {
      this.shellToolbarLabel = 'Portal de Compras';
    }
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

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
