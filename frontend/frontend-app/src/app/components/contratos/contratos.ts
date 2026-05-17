import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../services/auth';
import { ContractService } from '../../services/contract';
import { ContractResponse } from '../../models/contract.model';
import { ContractListComponent } from './contract-list/contract-list';
import { User, UserRole } from '../../models/auth.model';

@Component({
  selector: 'app-contratos',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    ContractListComponent,
  ],
  templateUrl: './contratos.html',
  styleUrl: './contratos.scss',
})
export class ContratosComponent implements OnInit {
  contracts: ContractResponse[] = [];
  loading = false;
  errorMessage = '';
  searchTerm = '';
  currentUser: User | null = null;
  readonly UserRole = UserRole;

  constructor(
    private contractService: ContractService,
    private authService: AuthService,
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();
    this.loadContracts();
  }

  loadContracts(): void {
    this.loading = true;
    this.errorMessage = '';

    this.contractService.getContracts(0, 12, this.searchTerm.trim()).subscribe({
      next: (response) => {
        this.contracts = response.content;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading contracts:', error);
        this.errorMessage = 'No fue posible cargar los contratos. Intenta de nuevo.';
        this.contracts = [];
        this.loading = false;
      },
    });
  }

  onSearch(): void {
    this.loadContracts();
  }

  clearSearch(): void {
    this.searchTerm = '';
    this.loadContracts();
  }

  isFuncionario(): boolean {
    return this.authService.hasRole(UserRole.FUNCIONARIO);
  }

  isAdministrador(): boolean {
    return this.authService.hasRole(UserRole.ADMINISTRADOR);
  }

  logout(): void {
    this.authService.logout();
  }

  statusLabel(status: string | undefined): string {
    return (status || '-').toUpperCase().replace(/\s+/g, '_');
  }

  statusTone(status: string | undefined): string {
    const normalized = (status || '').toUpperCase();

    if (normalized.includes('FINAL')) {
      return 'tone-done';
    }

    if (normalized.includes('ACTIV')) {
      return 'tone-live';
    }

    return 'tone-prep';
  }

  formatCurrency(value: number | undefined): string {
    return new Intl.NumberFormat('es-MX', {
      style: 'currency',
      currency: 'MXN',
      minimumFractionDigits: 2,
    }).format(value || 0);
  }
}
