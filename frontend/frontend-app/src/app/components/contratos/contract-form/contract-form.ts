import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatOptionModule } from '@angular/material/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { AuthService } from '../../../services/auth';
import { ContractService } from '../../../services/contract';
import { SupplierService } from '../../../services/supplier';
import { ContractCreateRequest, ContractResponse } from '../../../models/contract.model';
import { Supplier } from '../../../models/supplier.model';
import { User } from '../../../models/auth.model';

@Component({
  selector: 'app-contract-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatOptionModule,
    MatProgressSpinnerModule,
    MatSelectModule,
  ],
  templateUrl: './contract-form.html',
  styleUrl: './contract-form.scss',
})
export class ContractFormComponent implements OnInit {
  contractForm!: FormGroup;
  loading = false;
  suppliersLoading = false;
  currentUser: User | null = null;
  availableSuppliers: Supplier[] = [];
  message: { type: 'success' | 'error'; text: string } | null = null;

  constructor(
    private formBuilder: FormBuilder,
    private contractService: ContractService,
    private supplierService: SupplierService,
    private authService: AuthService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();
    this.initializeForm();
    this.loadSuppliers();
  }

  initializeForm(): void {
    this.contractForm = this.formBuilder.group(
      {
        supplierId: ['', Validators.required],
        object: ['', [Validators.required, Validators.minLength(200), Validators.maxLength(2000)]],
        budget: ['', [Validators.required, Validators.min(0.01)]],
        startDate: ['', [Validators.required, this.startDateValidator]],
        endDate: ['', [Validators.required]],
      },
      { validators: [this.dateRangeValidator] },
    );
  }

  get f(): { [key: string]: AbstractControl } {
    return this.contractForm.controls;
  }

  loadSuppliers(): void {
    this.suppliersLoading = true;

    this.supplierService.getSuppliers(0, 1000).subscribe({
      next: (response) => {
        const enabledStatuses = new Set(['ACTIVO', 'HABILITADO']);
        this.availableSuppliers = response.content
          .filter((supplier) => enabledStatuses.has(String(supplier.status).toUpperCase()))
          .sort((a, b) => a.businessName.localeCompare(b.businessName));
        this.suppliersLoading = false;
      },
      error: (error) => {
        console.error('Error loading suppliers:', error);
        this.availableSuppliers = [];
        this.suppliersLoading = false;
        this.message = {
          type: 'error',
          text: 'No se pudieron cargar los proveedores habilitados. Intenta nuevamente.',
        };
      },
    });
  }

  onSubmit(): void {
    this.message = null;

    if (this.contractForm.invalid) {
      this.contractForm.markAllAsTouched();
      return;
    }

    const request: ContractCreateRequest = {
      supplierId: this.f['supplierId'].value,
      object: String(this.f['object'].value).trim(),
      budget: Number(this.f['budget'].value),
      startDate: this.f['startDate'].value,
      endDate: this.f['endDate'].value,
    };

    this.loading = true;

    this.contractService.createContract(request).subscribe({
      next: (response: ContractResponse) => {
        this.loading = false;
        this.message = {
          type: 'success',
          text: `Contrato ${response.contractNumber} creado correctamente para ${response.supplierBusinessName || 'el proveedor seleccionado'}. Se está descargando el PDF.`,
        };
        this.downloadContractPdf(response);
        this.contractForm.reset();
      },
      error: (error) => {
        this.loading = false;
        this.message = {
          type: 'error',
          text: this.resolveErrorMessage(error),
        };
      },
    });
  }

  private downloadContractPdf(contract: ContractResponse): void {
    this.contractService.downloadContractPdf(contract.id).subscribe({
      next: (pdfBlob: Blob) => {
        const fileName = `contrato-${contract.contractNumber || contract.id}.pdf`;
        const url = window.URL.createObjectURL(pdfBlob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = fileName;
        anchor.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.message = {
          type: 'error',
          text: 'El contrato fue creado, pero no se pudo descargar el PDF automáticamente. Vuelve a intentarlo en unos segundos.',
        };
      },
    });
  }

  backToContracts(): void {
    this.router.navigate(['/contratos']);
  }

  private startDateValidator = (control: AbstractControl): ValidationErrors | null => {
    const value = control.value;

    if (!value) {
      return null;
    }

    const selectedDate = new Date(`${value}T00:00:00`);
    if (Number.isNaN(selectedDate.getTime())) {
      return null;
    }

    const today = new Date();
    today.setHours(0, 0, 0, 0);

    return selectedDate < today ? { pastDate: true } : null;
  };

  private dateRangeValidator = (control: AbstractControl): ValidationErrors | null => {
    const startDate = control.get('startDate')?.value;
    const endDate = control.get('endDate')?.value;

    if (!startDate || !endDate) {
      return null;
    }

    const start = new Date(`${startDate}T00:00:00`);
    const end = new Date(`${endDate}T00:00:00`);

    if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) {
      return null;
    }

    return end < start ? { dateRange: true } : null;
  };

  private resolveErrorMessage(error: any): string {
    if (error?.status === 0) {
      return 'No se pudo conectar con el servicio de contratos. Verifica que el backend esté disponible.';
    }

    const backendMessage =
      error?.error?.message ||
      error?.error?.error ||
      error?.message ||
      'No fue posible crear el contrato.';

    return `No fue posible crear el contrato: ${backendMessage}`;
  }
}