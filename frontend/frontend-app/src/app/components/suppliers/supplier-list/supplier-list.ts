import { Component, OnDestroy, OnInit } from '@angular/core';
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
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { SupplierService } from '../../../services/supplier';
import { AuthService } from '../../../services/auth';
import { UserRole } from '../../../models/auth.model';
import { Supplier, SupplierStatus, PersonType } from '../../../models/supplier.model';

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
  displayedColumns: string[] = ['nombre', 'nit', 'email', 'telefono', 'estado', 'actions'];
  suppliers: Supplier[] = [];
  loading = false;
  
  // Pagination
  totalElements = 0;
  pageSize = 10;
  currentPage = 0;
  
  // Search and filters
  searchTerm = '';
  statusFilter: SupplierStatus | 'ALL' = 'ALL';
  personTypeFilter: PersonType | 'ALL' = 'ALL';
  private searchTerms$ = new Subject<string>();
  private destroy$ = new Subject<void>();
  
  UserRole = UserRole;
  SupplierStatus = SupplierStatus;
  PersonType = PersonType;
  currentUserRole: UserRole | null = null;

  constructor(
    private supplierService: SupplierService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit() {
    this.currentUserRole = this.authService.getUserRole();
    this.initSearchSubscription();
    this.loadData();

    // Refresh when service signals mutations
    this.supplierService.refresh$.pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.loadSuppliers();
    });
  }

  // Public alias used by templates to load data when filters change
  loadData() {
    this.currentPage = 0;
    this.loadSuppliers();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initSearchSubscription() {
    this.searchTerms$
      .pipe(debounceTime(400), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(() => {
        this.currentPage = 0;
        this.loadSuppliers();
      });
  }

  loadSuppliers() {
    this.loading = true;
    this.supplierService
      .getSuppliers(
        this.currentPage,
        this.pageSize,
        this.searchTerm,
        this.statusFilter,
        this.personTypeFilter
      )
      .subscribe({
        next: (response) => {
          // Robust handling of possible response shapes coming from different backends
          const resp: any = response;
          let items: Supplier[] = [];
          let total = 0;

          if (Array.isArray(resp)) {
            items = resp as Supplier[];
            total = items.length;
          } else if (resp?.content && Array.isArray(resp.content)) {
            items = resp.content as Supplier[];
            total = resp.totalElements ?? resp.content.length;
          } else if (resp?.data && Array.isArray(resp.data)) {
            items = resp.data as Supplier[];
            total = resp.data.length;
          } else if (resp?.items && Array.isArray(resp.items)) {
            // some APIs use 'items'
            items = resp.items as Supplier[];
            total = resp.totalElements ?? resp.items.length;
          } else {
            console.error('Formato de respuesta inesperado en getSuppliers():', resp);
            items = [];
            total = 0;
          }

          // Merge UI-only temporary entries (if any) so newly created suppliers appear immediately.
          const temps = (this.supplierService as any).tempSuppliers || [];
          if (Array.isArray(temps) && temps.length) {
            const visibleTemps = temps.map((t: Supplier) => ({ ...t, __pending: true } as any));
            // Prepend temps and remove duplicates by id (keep first occurrence)
            items = [...visibleTemps, ...items].filter((s, i, self) => self.findIndex(x => x.id === s.id) === i);
            total = Math.max(total || 0, items.length);
          }

          this.suppliers = items;
          this.totalElements = total || 0;
          this.loading = false;
        },
        error: (error) => {
          console.error('Error loading suppliers:', error);
          this.suppliers = [];
          this.totalElements = 0;
          this.loading = false;
        }
      });
  }

  onSearchTermChange(event: Event) {
    const value = (event.target as HTMLInputElement).value;
    this.searchTerm = value;
    this.searchTerms$.next(value);
  }

  onFilterChange() {
    this.currentPage = 0;
    this.loadSuppliers();
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
    this.statusFilter = 'ALL';
    this.personTypeFilter = 'ALL';
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
      this.supplierService.disableSupplier(supplier.id).subscribe({
        next: () => this.loadSuppliers(),
        error: (error) => {
          console.error('Error changing supplier status:', error);
        }
      });
      return;
    }

    this.supplierService.enableSupplier(supplier.id).subscribe({
      next: () => this.loadSuppliers(),
      error: (error) => {
        console.error('Error changing supplier status:', error);
      }
    });
  }

  changeSupplierStatus(id: string, status: SupplierStatus) {
    this.supplierService.changeStatus(id, status).subscribe({
      next: () => {
        this.loadSuppliers();
      },
      error: (error) => {
        console.error('Error changing supplier status:', error);
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

