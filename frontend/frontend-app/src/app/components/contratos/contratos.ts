import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
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
    RouterModule,
  ],
  templateUrl: './contratos.html',
  styleUrl: './contratos.scss',
})
export class ContratosComponent implements OnInit {
  currentUser: User | null = null;

  constructor(private authService: AuthService) {}

  ngOnInit() {
    this.currentUser = this.authService.getCurrentUser();
  }

  isFuncionario(): boolean {
    return this.authService.hasRole(UserRole.FUNCIONARIO);
  }

  isAdministrador(): boolean {
    return this.authService.hasRole(UserRole.ADMINISTRADOR);
  }
}
