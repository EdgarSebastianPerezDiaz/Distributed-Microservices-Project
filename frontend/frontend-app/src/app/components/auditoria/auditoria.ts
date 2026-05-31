import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { User } from '../../models/auth.model';
import {
  AuditEntityType,
  AuditEventFilters,
  AuditEventResponse,
  AuditEventType,
} from '../../models/audit.model';
import { AuditService } from '../../services/audit';

@Component({
  selector: 'app-auditoria',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    RouterModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatSelectModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './auditoria.html',
  styleUrl: './auditoria.scss',
})
export class AuditoriaComponent implements OnInit {
  currentUser: User | null = null;
  loading = false;
  detailLoading = false;
  errorMessage = '';

  events: AuditEventResponse[] = [];
  selectedEvent: AuditEventResponse | null = null;

  readonly pageSize = 12;
  total = 0;
  offset = 0;

  filters = {
    searchTerm: '',
    entityType: 'ALL',
    eventType: 'ALL',
    userId: '',
    startDate: '',
    endDate: '',
  };

  readonly entityTypeOptions: AuditEntityType[] = ['USER', 'SUPPLIER', 'PROVEEDOR', 'CONTRACT', 'SYSTEM'];
  readonly eventTypeOptions: AuditEventType[] = [
    'LOGIN',
    'LOGOUT',
    'CREATE',
    'UPDATE',
    'DELETE',
    'TRANSITION_STATE',
    'ERROR',
    'CREAR_CONTRATO',
    'CAMBIAR_ESTADO',
    'MODIFICAR_CONTRATO',
    'ELIMINAR_CONTRATO',
    'ANULAR',
    'MODIFICAR_PROVEEDOR',
    'CREAR_PROVEEDOR',
    'CAMBIAR_ESTADO_PROVEEDOR',
    'CREAR_USUARIO',
    'MODIFICAR_USUARIO',
  ];

  constructor(
    private authService: AuthService,
    private auditService: AuditService,
  ) {}

  ngOnInit() {
    this.currentUser = this.authService.getCurrentUser();
    this.loadEvents();
  }

  get displayedEvents(): AuditEventResponse[] {
    const term = this.filters.searchTerm.trim().toLowerCase();
    if (!term) {
      return this.events;
    }

    return this.events.filter((event) => {
      const stack = [
        event.eventId,
        event._id,
        event.entityId,
        event.performedBy,
        event.entityType,
        event.eventType,
        event.errorMessage,
        this.buildDescription(event),
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase();

      return stack.includes(term);
    });
  }

  loadEvents(): void {
    this.loading = true;
    this.errorMessage = '';
    this.selectedEvent = null;

    const request: AuditEventFilters = {
      limit: this.pageSize,
      offset: this.offset,
    };

    if (this.filters.entityType !== 'ALL') {
      request.entidad_tipo = this.filters.entityType as AuditEntityType;
    }
    if (this.filters.eventType !== 'ALL') {
      request.tipo_evento = this.filters.eventType;
    }
    if (this.filters.userId.trim()) {
      request.usuario = this.filters.userId.trim();
    }
    if (this.filters.startDate) {
      request.fecha_desde = this.filters.startDate;
    }
    if (this.filters.endDate) {
      request.fecha_hasta = this.filters.endDate;
    }

    this.auditService.getEvents(request).subscribe({
      next: (response) => {
        this.events = response.data || [];
        this.total = response.total || 0;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading audit events:', error);
        this.events = [];
        this.total = 0;
        this.loading = false;
        this.errorMessage = 'No fue posible cargar los eventos de auditoría. Intenta nuevamente.';
      },
    });
  }

  applyFilters(): void {
    this.offset = 0;
    this.loadEvents();
  }

  clearFilters(): void {
    this.filters = {
      searchTerm: '',
      entityType: 'ALL',
      eventType: 'ALL',
      userId: '',
      startDate: '',
      endDate: '',
    };
    this.offset = 0;
    this.loadEvents();
  }

  exportReport(): void {
    const reportWindow = window.open('', '_blank', 'width=1100,height=800');
    if (!reportWindow) {
      this.errorMessage = 'El navegador bloqueó la ventana emergente necesaria para exportar el informe.';
      return;
    }

    const filtersSummary = [
      this.filters.searchTerm ? `Búsqueda: ${this.filters.searchTerm}` : '',
      this.filters.entityType !== 'ALL' ? `Entidad: ${this.filters.entityType}` : '',
      this.filters.eventType !== 'ALL' ? `Operación: ${this.filters.eventType}` : '',
      this.filters.userId ? `Usuario: ${this.filters.userId}` : '',
      this.filters.startDate ? `Desde: ${this.filters.startDate}` : '',
      this.filters.endDate ? `Hasta: ${this.filters.endDate}` : '',
    ].filter(Boolean);

    const rows = this.displayedEvents
      .map((event) => `
        <tr>
          <td>${this.escapeHtml(this.formatDate(event.timestamp))}</td>
          <td>${this.escapeHtml(event.entityType)}</td>
          <td>${this.escapeHtml(event.eventType)}</td>
          <td>${this.escapeHtml(this.buildDescription(event))}</td>
          <td>${this.escapeHtml(event.performedBy || '-')}</td>
          <td>${this.escapeHtml(event.statusCode ? String(event.statusCode) : '-')}</td>
        </tr>
      `)
      .join('');

    reportWindow.document.write(`
      <!doctype html>
      <html lang="es">
        <head>
          <meta charset="utf-8" />
          <title>Informe de auditoría</title>
          <style>
            body { font-family: Arial, sans-serif; margin: 24px; color: #1f2937; }
            h1 { margin: 0 0 8px; }
            .muted { color: #6b7280; }
            .meta { margin: 16px 0 18px; font-size: 13px; }
            .meta div { margin-bottom: 4px; }
            table { width: 100%; border-collapse: collapse; font-size: 12px; }
            th, td { border: 1px solid #d1d5db; padding: 8px; vertical-align: top; }
            th { background: #f3f4f6; text-align: left; }
            .footer { margin-top: 18px; font-size: 11px; color: #6b7280; }
          </style>
          <script>
            window.onload = () => { window.print(); };
          </script>
        </head>
        <body>
          <h1>Informe de auditoría</h1>
          <div class="muted">Exportado el ${this.escapeHtml(new Date().toLocaleString('es-CO'))}</div>
          <div class="meta">
            ${filtersSummary.length ? `<div><strong>Filtros aplicados:</strong> ${this.escapeHtml(filtersSummary.join(' | '))}</div>` : '<div><strong>Filtros aplicados:</strong> Ninguno</div>'}
            <div><strong>Total de eventos mostrados:</strong> ${this.displayedEvents.length}</div>
          </div>
          <table>
            <thead>
              <tr>
                <th>Fecha/Hora</th>
                <th>Entidad</th>
                <th>Operación</th>
                <th>Descripción</th>
                <th>Usuario</th>
                <th>Estado</th>
              </tr>
            </thead>
            <tbody>
              ${rows || '<tr><td colspan="6">No hay eventos para exportar</td></tr>'}
            </tbody>
          </table>
          <div class="footer">Documento generado automáticamente desde el panel de auditoría.</div>
        </body>
      </html>
    `);
    reportWindow.document.close();
    reportWindow.focus();
  }

  previousPage(): void {
    if (!this.canGoPrevious()) {
      return;
    }
    this.offset = Math.max(this.offset - this.pageSize, 0);
    this.loadEvents();
  }

  nextPage(): void {
    if (!this.canGoNext()) {
      return;
    }
    this.offset += this.pageSize;
    this.loadEvents();
  }

  canGoPrevious(): boolean {
    return this.offset > 0 && !this.loading;
  }

  canGoNext(): boolean {
    return this.offset + this.pageSize < this.total && !this.loading;
  }

  currentPage(): number {
    return Math.floor(this.offset / this.pageSize) + 1;
  }

  formatDate(dateTime: string): string {
    const parsed = new Date(dateTime);
    if (Number.isNaN(parsed.getTime())) {
      return '-';
    }

    return parsed.toLocaleString('es-CO', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  entityTone(entityType: AuditEntityType): string {
    if (entityType === 'CONTRACT') {
      return 'tone-blue';
    }
    if (entityType === 'SUPPLIER' || entityType === 'PROVEEDOR') {
      return 'tone-violet';
    }
    if (entityType === 'USER') {
      return 'tone-amber';
    }
    return 'tone-neutral';
  }

  eventTone(eventType: AuditEventType): string {
    if (eventType === 'CREATE' || eventType === 'LOGIN' || eventType === 'CREAR_PROVEEDOR' || eventType === 'CREAR_CONTRATO' || eventType === 'CREAR_USUARIO') {
      return 'op-create';
    }
    if (eventType === 'UPDATE' || eventType === 'TRANSITION_STATE' || eventType === 'CAMBIAR_ESTADO' || eventType === 'CAMBIAR_ESTADO_PROVEEDOR' || eventType === 'MODIFICAR_CONTRATO' || eventType === 'MODIFICAR_PROVEEDOR' || eventType === 'MODIFICAR_USUARIO') {
      return 'op-update';
    }
    if (eventType === 'DELETE' || eventType === 'ERROR' || eventType === 'LOGOUT' || eventType === 'ELIMINAR_CONTRATO' || eventType === 'ANULAR') {
      return 'op-delete';
    }
    return 'op-generic';
  }

  buildDescription(event: AuditEventResponse): string {
    if (event.description) {
      return event.description;
    }

    if (event.errorMessage) {
      return event.errorMessage;
    }

    const newDataSummary = this.extractSummary(event.newData);
    if (newDataSummary) {
      return newDataSummary;
    }

    const previousDataSummary = this.extractSummary(event.previousData);
    if (previousDataSummary) {
      return previousDataSummary;
    }

    return `Evento ${event.eventType} sobre entidad ${event.entityType}`;
  }

  selectEvent(event: AuditEventResponse): void {
    if (this.selectedEvent?.eventId && this.selectedEvent.eventId === event.eventId) {
      this.selectedEvent = null;
      return;
    }

    this.selectedEvent = event;
  }

  formatJson(value: Record<string, unknown> | null | undefined): string {
    if (!value || Object.keys(value).length === 0) {
      return 'Sin datos';
    }

    return JSON.stringify(value, null, 2);
  }

  private escapeHtml(value: string): string {
    return value
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/\"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  private extractSummary(payload: Record<string, unknown> | null | undefined): string {
    if (!payload) {
      return '';
    }

    const summaryKeys = ['description', 'object', 'contractNumber', 'username', 'businessName'];
    for (const key of summaryKeys) {
      const candidate = payload[key];
      if (typeof candidate === 'string' && candidate.trim()) {
        return candidate;
      }
    }

    return Object.keys(payload).slice(0, 3).join(' · ');
  }
}
