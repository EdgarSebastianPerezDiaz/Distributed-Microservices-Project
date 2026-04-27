import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatMenuModule } from '@angular/material/menu';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { AuthService } from '../../../services/auth';
import { User, UserRole } from '../../../models/auth.model';

@Component({
  selector: 'app-navbar',
  imports: [
    CommonModule,
    RouterModule,
    MatToolbarModule,
    MatMenuModule,
    MatButtonModule,
    MatIconModule,
    MatSidenavModule,
    MatListModule
  ],
  templateUrl: './navbar.html',
  styleUrl: './navbar.scss',
})
export class NavbarComponent implements OnInit {
  currentUser: User | null = null;
  UserRole = UserRole;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit() {
    this.currentUser = this.authService.getCurrentUser();
    this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
    });
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
