import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AuthService } from '../../../services/auth';
import { ContractService } from '../../../services/contract';
import { ContractResponse, PaginatedContractResponse } from '../../../models/contract.model';
import { User, UserRole } from '../../../models/auth.model';

@Component({
  selector: 'app-contract-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatTableModule,
    MatTooltipModule,
  ],
  templateUrl: './contract-list.html',
  styleUrl: './contract-list.scss',
})
export class ContractListComponent implements OnInit {
  displayedColumns: string[] = ['contractNumber', 'supplier', 'object', 'budget', 'dates', 'status', 'createdAt'];
  contracts: ContractResponse[] = [];
  loading = false;
  errorMessage = '';
  totalElements = 0;
  pageSize = 10;
  currentPage = 0;
  searchTerm = '';
  currentUser: User | null = null;
  UserRole = UserRole;

  constructor(
    private contractService: ContractService,
    private authService: AuthService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();
    this.loadContracts();
  }

  loadContracts(): void {
    this.loading = true;
    this.errorMessage = '';
    this.contractService.getContracts(this.currentPage, this.pageSize, this.searchTerm).subscribe({
      next: (response: PaginatedContractResponse) => {
        this.contracts = response.content;
        this.totalElements = response.totalElements;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading contracts:', error);
        if (error?.status === 403) {
          this.errorMessage = 'No tienes permisos para consultar contratos con este rol.';
        } else if (error?.status === 401) {
          this.errorMessage = 'Tu sesión expiró. Inicia sesión nuevamente.';
        } else {
          this.errorMessage = 'No fue posible cargar los contratos. Intenta de nuevo.';
        }
        this.contracts = [];
        this.totalElements = 0;
        this.loading = false;
      },
    });
  }

  onPageChange(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadContracts();
  }

  onSearch(): void {
    this.currentPage = 0;
    this.loadContracts();
  }

  clearSearch(): void {
    this.searchTerm = '';
    this.currentPage = 0;
    this.loadContracts();
  }

  createContract(): void {
    this.router.navigate(['/contratos/new']);
  }

  viewContract(id: string | undefined): void {
    if (id) {
      this.router.navigate(['/contratos', id]);
    }
  }

  isFuncionario(): boolean {
    return this.authService.hasRole(UserRole.FUNCIONARIO);
  }

  getStatusLabel(status: string): string {
    return status || '-';
  }

  getObjectPreview(object: string): string {
    if (!object) {
      return '-';
    }

    return object.length > 120 ? `${object.slice(0, 120)}...` : object;
  }
}