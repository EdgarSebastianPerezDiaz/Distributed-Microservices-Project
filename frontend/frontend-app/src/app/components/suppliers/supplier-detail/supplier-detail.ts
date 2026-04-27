import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { SupplierService } from '../../../services/supplier';
import { AuthService } from '../../../services/auth';
import { Supplier, SupplierStatus, PersonType } from '../../../models/supplier.model';
import { UserRole } from '../../../models/auth.model';

@Component({
  selector: 'app-supplier-detail',
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatDividerModule
  ],
  templateUrl: './supplier-detail.html',
  styleUrl: './supplier-detail.scss',
})
export class SupplierDetailComponent implements OnInit {
  supplier: Supplier | null = null;
  loading = false;
  supplierId: string | null = null;
  error: string | null = null;
  
  SupplierStatus = SupplierStatus;
  PersonType = PersonType;
  UserRole = UserRole;

  constructor(
    private supplierService: SupplierService,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    this.route.params.subscribe(params => {
      if (params['id']) {
        this.supplierId = params['id'];
        this.loadSupplier(params['id']);
      }
    });
  }

  loadSupplier(id: string) {
    this.loading = true;
    this.supplierService.getSupplierById(id).subscribe({
      next: (supplier) => {
        this.supplier = supplier;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading supplier:', error);
        this.error = 'Error al cargar el proveedor';
        this.loading = false;
      }
    });
  }

  editSupplier() {
    if (this.supplierId) {
      this.router.navigate(['/suppliers', this.supplierId, 'edit']);
    }
  }

  goBack() {
    this.router.navigate(['/suppliers']);
  }

  isAdmin(): boolean {
    return this.authService.hasRole(UserRole.ADMINISTRADOR);
  }
}
