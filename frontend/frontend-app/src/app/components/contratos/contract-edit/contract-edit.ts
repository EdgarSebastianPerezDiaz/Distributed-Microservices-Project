import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../services/auth';
import { ContractService } from '../../../services/contract';
import { ContractResponse, ContractUpdateRequest } from '../../../models/contract.model';

@Component({
  selector: 'app-contract-edit',
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
    MatProgressSpinnerModule,
  ],
  templateUrl: './contract-edit.html',
  styleUrl: './contract-edit.scss',
})
export class ContractEditComponent implements OnInit {
  contractForm!: FormGroup;
  loading = false;
  saving = false;
  contractId = '';
  contract: ContractResponse | null = null;
  message: { type: 'success' | 'error'; text: string } | null = null;

  constructor(
    private formBuilder: FormBuilder,
    private contractService: ContractService,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    this.initializeForm();
    this.contractId = this.route.snapshot.paramMap.get('id') || '';

    if (!this.contractId) {
      this.message = {
        type: 'error',
        text: 'No se pudo identificar el contrato a editar.',
      };
      return;
    }

    this.loadContract();
  }

  initializeForm(): void {
    this.contractForm = this.formBuilder.group({
      budget: ['', [Validators.required, Validators.min(0.01)]],
    });
  }

  get f(): { [key: string]: AbstractControl } {
    return this.contractForm.controls;
  }

  loadContract(): void {
    this.loading = true;
    this.message = null;

    this.contractService.getContractById(this.contractId).subscribe({
      next: (contract) => {
        this.contract = contract;
        this.contractForm.patchValue({
          budget: contract.budget,
        });
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading contract:', error);
        this.message = {
          type: 'error',
          text: this.resolveLoadErrorMessage(error),
        };
        this.loading = false;
      },
    });
  }

  onSubmit(): void {
    this.message = null;

    if (this.contractForm.invalid || !this.contract) {
      this.contractForm.markAllAsTouched();
      return;
    }

    const request: ContractUpdateRequest = {
      budget: Number(this.f['budget'].value),
    };

    this.saving = true;

    this.contractService.updateContract(this.contractId, request).subscribe({
      next: (response) => {
        this.contract = response;
        this.contractForm.patchValue({ budget: response.budget });
        this.saving = false;
        this.message = {
          type: 'success',
          text: `El contrato ${response.contractNumber} fue actualizado correctamente.`,
        };
      },
      error: (error) => {
        this.saving = false;
        this.message = {
          type: 'error',
          text: this.resolveSaveErrorMessage(error),
        };
      },
    });
  }

  backToContracts(): void {
    this.router.navigate(['/contratos']);
  }

  formatCurrency(value: number | undefined): string {
    return new Intl.NumberFormat('es-MX', {
      style: 'currency',
      currency: 'MXN',
      minimumFractionDigits: 2,
    }).format(value || 0);
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

  getObjectPreview(object: string | undefined): string {
    if (!object) {
      return '-';
    }

    return object.length > 220 ? `${object.slice(0, 220)}...` : object;
  }

  getCurrentUserLabel(): string {
    const user = this.authService.getCurrentUser();
    return user?.fullName || user?.username || 'Funcionario';
  }

  private resolveLoadErrorMessage(error: any): string {
    if (error?.status === 404) {
      return 'El contrato que intentas editar no existe o ya no está disponible.';
    }

    if (error?.status === 403) {
      return 'No tienes permisos para ver este contrato.';
    }

    if (error?.status === 401) {
      return 'Tu sesión expiró. Inicia sesión nuevamente.';
    }

    return 'No fue posible cargar el contrato. Intenta de nuevo.';
  }

  private resolveSaveErrorMessage(error: any): string {
    if (error?.status === 422) {
      return error?.error?.mensaje || error?.error?.message || 'El contrato no puede editarse en su estado actual.';
    }

    if (error?.status === 403) {
      return 'No tienes permisos para editar este contrato.';
    }

    if (error?.status === 401) {
      return 'Tu sesión expiró. Inicia sesión nuevamente.';
    }

    const backendMessage = error?.error?.message || error?.error?.error || error?.message || 'No fue posible actualizar el contrato.';
    return `No fue posible actualizar el contrato: ${backendMessage}`;
  }
}