import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../services/auth';
import { ContractService } from '../../../services/contract';
import { ContractResponse } from '../../../models/contract.model';
import { UserRole } from '../../../models/auth.model';

@Component({
  selector: 'app-contract-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatDividerModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <div class="detail-page">
      <nav class="crumbs crumbs--muted" aria-label="Breadcrumb">
        <a routerLink="/dashboard">Inicio</a>
        <span class="crumbs__sep">›</span>
        <a routerLink="/contratos">Contratos</a>
        <span class="crumbs__sep">›</span>
        <span class="crumbs__strong">Detalle</span>
      </nav>

      <div class="loading-state" *ngIf="loading">
        <mat-spinner diameter="44"></mat-spinner>
      </div>

      <mat-card class="elevated-card" *ngIf="!loading && contract; else errorBlock">
        <mat-card-content>
          <header class="header">
            <div>
              <div class="kicker">Detalle del Contrato</div>
              <h1>{{ contract.contractNumber }}</h1>
              <p>{{ contract.supplierBusinessName || 'Sin nombre' }}</p>
            </div>
            <span class="status-badge" [ngClass]="statusTone(contract.status)">{{ statusLabel(contract.status) }}</span>
          </header>

          <div class="summary-grid">
            <div class="summary-item">
              <span class="summary-label">Proveedor</span>
              <strong>{{ contract.supplierBusinessName || 'Sin nombre' }}</strong>
            </div>
            <div class="summary-item">
              <span class="summary-label">NIT</span>
              <strong>{{ contract.supplierNit || contract.supplierId }}</strong>
            </div>
            <div class="summary-item">
              <span class="summary-label">Presupuesto</span>
              <strong>{{ formatCurrency(contract.budget) }}</strong>
            </div>
          </div>

          <mat-divider></mat-divider>

          <div class="detail-grid">
            <div>
              <div class="field-label">Objeto</div>
              <p class="object-preview">{{ contract.object }}</p>
            </div>
            <div>
              <div class="field-label">Fechas</div>
              <p><strong>Inicio:</strong> {{ contract.startDate }}</p>
              <p><strong>Fin:</strong> {{ contract.endDate }}</p>
            </div>
            <div>
              <div class="field-label">Creado</div>
              <p>{{ contract.createdAt ? (contract.createdAt | date:'short') : '-' }}</p>
            </div>
            <div>
              <div class="field-label">Actualizado</div>
              <p>{{ contract.updatedAt ? (contract.updatedAt | date:'short') : '-' }}</p>
            </div>
          </div>

          <div class="actions">
            <button mat-flat-button color="primary" type="button" (click)="exportPdf()">
              <mat-icon>picture_as_pdf</mat-icon>
              Exportar PDF
            </button>
            <button mat-stroked-button type="button" routerLink="/contratos">
              <mat-icon>arrow_back</mat-icon>
              Volver
            </button>
            <button mat-stroked-button type="button" *ngIf="isFuncionario()" [routerLink]="['/contratos', contract.id, 'edit']">
              <mat-icon>edit</mat-icon>
              Editar
            </button>
          </div>
        </mat-card-content>
      </mat-card>

      <ng-template #errorBlock>
        <mat-card class="elevated-card">
          <mat-card-content>
            <div class="empty-state empty-state--error" *ngIf="errorMessage">
              <mat-icon>error_outline</mat-icon>
              <h3>No fue posible cargar el contrato</h3>
              <p>{{ errorMessage }}</p>
            </div>
          </mat-card-content>
        </mat-card>
      </ng-template>
    </div>
  `,
  styles: [`
    .detail-page { padding: 24px; max-width: 1180px; margin: 0 auto; }
    .loading-state { min-height: 280px; display: grid; place-items: center; }
    .header { display:flex; justify-content:space-between; gap:16px; align-items:flex-start; margin-bottom:20px; }
    .kicker { text-transform:uppercase; letter-spacing:.12em; color:#7c7c7c; font-size:.78rem; margin-bottom:8px; }
    h1 { margin:0; font-size:2rem; }
    .header p { margin:8px 0 0; color:#666; }
    .summary-grid { display:grid; grid-template-columns:repeat(3, minmax(0,1fr)); gap:14px; margin:18px 0; }
    .summary-item { background:#f7f7f8; border-radius:12px; padding:14px; }
    .summary-label, .field-label { display:block; font-size:.78rem; text-transform:uppercase; letter-spacing:.08em; color:#7a7a7a; margin-bottom:8px; }
    .detail-grid { display:grid; grid-template-columns:repeat(2, minmax(0,1fr)); gap:18px; margin-top:18px; }
    .object-preview { white-space:pre-wrap; line-height:1.5; }
    .actions { display:flex; gap:12px; justify-content:flex-end; margin-top:24px; flex-wrap:wrap; }
    @media (max-width: 900px) { .summary-grid, .detail-grid { grid-template-columns:1fr; } .header { flex-direction:column; } .actions { justify-content:flex-start; } }
  `],
})
export class ContractDetailComponent implements OnInit {
  contract: ContractResponse | null = null;
  loading = false;
  errorMessage = '';
  contractId = '';
  readonly UserRole = UserRole;

  constructor(
    private contractService: ContractService,
    private authService: AuthService,
    private route: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    this.contractId = this.route.snapshot.paramMap.get('id') || '';

    if (!this.contractId) {
      this.errorMessage = 'No se pudo identificar el contrato.';
      return;
    }

    this.loadContract();
  }

  loadContract(): void {
    this.loading = true;
    this.errorMessage = '';

    this.contractService.getContractById(this.contractId).subscribe({
      next: (contract) => {
        this.contract = contract;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading contract detail:', error);
        this.contract = null;
        this.loading = false;
        this.errorMessage = 'No fue posible cargar el detalle del contrato. Intenta de nuevo.';
      },
    });
  }

  exportPdf(): void {
    if (!this.contract) {
      return;
    }

    this.contractService.downloadContractPdf(this.contract.id).subscribe({
      next: (pdfBlob) => {
        const url = window.URL.createObjectURL(pdfBlob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = `contrato-${this.contract?.contractNumber || this.contract?.id}.pdf`;
        anchor.click();
        window.URL.revokeObjectURL(url);
      },
      error: (error) => {
        console.error('Error exporting contract PDF:', error);
        this.errorMessage = 'No fue posible generar el PDF del contrato.';
      },
    });
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

  isFuncionario(): boolean {
    return this.authService.hasRole(UserRole.FUNCIONARIO);
  }
}