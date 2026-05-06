import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatSelectModule } from '@angular/material/select';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatMenuModule } from '@angular/material/menu';
import { MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { FormsModule } from '@angular/forms';
import { SupplierService } from '../../../services/supplier';
import { AuthService } from '../../../services/auth';
import { Supplier, SupplierStatus, PersonType } from '../../../models/supplier.model';
import { UserRole } from '../../../models/auth.model';

@Component({
  selector: 'app-supplier-list',
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    MatCardModule,
    MatSelectModule,
    MatPaginatorModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
    MatProgressSpinnerModule,
    MatMenuModule,
    MatDialogModule,
    MatTooltipModule
  ],
  templateUrl: './supplier-list.html',
  styleUrl: './supplier-list.scss',
})
export class SupplierListComponent implements OnInit {
  displayedColumns: string[] = ['nit', 'businessName', 'email', 'phone', 'personType', 'status', 'actions'];
  suppliers: Supplier[] = [];
  loading = false;
  
  // Pagination
  totalElements = 0;
  pageSize = 10;
  currentPage = 0;
  
  // Search
  searchTerm = '';
  
  SupplierStatus = SupplierStatus;
  PersonType = PersonType;
  UserRole = UserRole;

  constructor(
    private supplierService: SupplierService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit() {
    this.loadSuppliers();
  }

  loadSuppliers() {
    this.loading = true;
    this.supplierService.getSuppliers(this.currentPage, this.pageSize, this.searchTerm).subscribe({
      next: (response) => {
        this.suppliers = response.content;
        this.totalElements = response.totalElements;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading suppliers:', error);
        this.loading = false;
      }
    });
  }

  onPageChange(event: PageEvent) {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadSuppliers();
  }

  onSearch() {
    this.currentPage = 0;
    this.loadSuppliers();
  }

  clearSearch() {
    this.searchTerm = '';
    this.currentPage = 0;
    this.loadSuppliers();
  }

  viewSupplier(id: string | undefined) {
    if (id) {
      this.router.navigate(['/suppliers', id]);
    }
  }

  editSupplier(id: string | undefined) {
    if (id) {
      this.router.navigate(['/suppliers', id, 'edit']);
    }
  }

  createSupplier() {
    this.router.navigate(['/suppliers/new']);
  }

  toggleSupplierStatus(supplier: Supplier) {
    if (!supplier.id) return;

    if (supplier.status === SupplierStatus.HABILITADO) {
      this.disableSupplier(supplier.id);
    } else {
      this.enableSupplier(supplier.id);
    }
  }

  enableSupplier(id: string) {
    this.supplierService.enableSupplier(id).subscribe({
      next: () => {
        this.loadSuppliers();
      },
      error: (error) => {
        console.error('Error enabling supplier:', error);
      }
    });
  }

  disableSupplier(id: string) {
    this.supplierService.disableSupplier(id).subscribe({
      next: () => {
        this.loadSuppliers();
      },
      error: (error) => {
        console.error('Error disabling supplier:', error);
      }
    });
  }

  deleteSupplier(id: string | undefined) {
    if (!id) return;
    
    if (confirm('¿Estás seguro de que deseas eliminar este proveedor?')) {
      this.supplierService.deleteSupplier(id).subscribe({
        next: () => {
          this.loadSuppliers();
        },
        error: (error) => {
          console.error('Error deleting supplier:', error);
        }
      });
    }
  }

  isAdmin(): boolean {
    return this.authService.hasRole(UserRole.ADMINISTRADOR);
  }
}
