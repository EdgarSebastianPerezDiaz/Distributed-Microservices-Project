import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { SupplierService } from '../../../services/supplier';
import { Supplier, PersonType, SupplierStatus } from '../../../models/supplier.model';

@Component({
  selector: 'app-supplier-form',
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    MatIconModule
  ],
  templateUrl: './supplier-form.html',
  styleUrl: './supplier-form.scss',
})
export class SupplierFormComponent implements OnInit {
  form!: FormGroup;
  loading = false;
  submitted = false;
  isEditMode = false;
  supplierId: string | null = null;
  error: string | null = null;
  private loadedSupplier: Supplier | null = null;
  
  PersonType = PersonType;
  personTypes = Object.values(PersonType);

  constructor(
    private formBuilder: FormBuilder,
    private supplierService: SupplierService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    this.initializeForm();
    
    this.route.params.subscribe(params => {
      if (params['id']) {
        this.isEditMode = true;
        this.supplierId = params['id'];
        this.loadSupplier(params['id']);
      }
    });
  }

  initializeForm() {
    this.form = this.formBuilder.group({
      nit: ['', [Validators.required, Validators.minLength(8)]],
      businessName: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]],
      phone: ['', []],
      personType: [PersonType.JURIDICA, [Validators.required]]
    });
  }

  loadSupplier(id: string) {
    this.loading = true;
    this.supplierService.getSupplierById(id).subscribe({
      next: (supplier) => {
        this.loadedSupplier = supplier;
        this.form.patchValue({
          nit: supplier.nit,
          businessName: supplier.businessName,
          email: supplier.email,
          phone: supplier.phone,
          personType: supplier.personType
        });
        this.form.get('nit')?.disable();
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading supplier:', error);
        this.error = 'Error al cargar el proveedor';
        this.loading = false;
      }
    });
  }

  get f() {
    return this.form.controls;
  }

  onSubmit() {
    this.submitted = true;
    this.error = null;

    if (this.form.invalid) {
      return;
    }

    this.loading = true;
    const request = this.isEditMode && this.supplierId
      ? this.supplierService.updateSupplier(this.supplierId, {
          businessName: this.f['businessName'].value,
          email: this.f['email'].value,
          phone: this.f['phone'].value,
          status: this.loadedSupplier?.status ?? SupplierStatus.HABILITADO,
        } as Supplier)
      : this.supplierService.createSupplier({
          nit: this.form.getRawValue().nit,
          businessName: this.f['businessName'].value,
          email: this.f['email'].value,
          phone: this.f['phone'].value,
          personType: this.f['personType'].value,
          status: SupplierStatus.HABILITADO,
        });

    request.subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/suppliers']);
      },
      error: (error) => {
        this.loading = false;
        console.error('Error saving supplier:', error);
        this.error = error.error?.message || 'Error al guardar el proveedor';
      }
    });
  }

  onCancel() {
    this.router.navigate(['/suppliers']);
  }
}
