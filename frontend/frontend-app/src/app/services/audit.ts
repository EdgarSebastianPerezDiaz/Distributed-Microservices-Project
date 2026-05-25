import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import {
  AuditEventApiResponse,
  AuditEventFilters,
  AuditEventListResponse,
  AuditEventResponse,
} from '../models/audit.model';

@Injectable({
  providedIn: 'root',
})
export class AuditService {
  private readonly apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getEvents(filters: AuditEventFilters): Observable<AuditEventListResponse> {
    let params = new HttpParams();

    if (filters.tipo_evento) {
      params = params.set('tipo_evento', filters.tipo_evento);
    }
    if (filters.entidad_tipo) {
      params = params.set('entidad_tipo', filters.entidad_tipo);
    }
    if (filters.usuario) {
      params = params.set('usuario', filters.usuario);
    }
    if (filters.fecha_desde) {
      params = params.set('fecha_desde', filters.fecha_desde);
    }
    if (filters.fecha_hasta) {
      params = params.set('fecha_hasta', filters.fecha_hasta);
    }
    if (typeof filters.limit === 'number') {
      params = params.set('limit', filters.limit.toString());
    }
    if (typeof filters.offset === 'number') {
      params = params.set('offset', filters.offset.toString());
    }

    return this.http
      .get<{ total: number; offset: number; limit: number; items: AuditEventApiResponse[] }>(
        `${this.apiUrl}/api/eventos`,
        { params },
      )
      .pipe(
        map((response) => ({
        total: response.total ?? 0,
        offset: response.offset ?? 0,
        limit: response.limit ?? 0,
        data: (response.items ?? []).map((item) => this.mapEvent(item)),
        })),
      );
  }

  private mapEvent(item: AuditEventApiResponse): AuditEventResponse {
    return {
      _id: item.id,
      eventId: item.id,
      eventType: item.tipo_evento as AuditEventResponse['eventType'],
      entityType: item.entidad_tipo,
      entityId: item.entidad_id,
      performedBy: item.usuario_id,
      previousData: this.safeParseJson(item.estado_anterior),
      newData: this.safeParseJson(item.estado_nuevo),
      ipAddress: item.metadata && typeof item.metadata === 'object' && 'ip' in item.metadata ? String(item.metadata['ip']) : undefined,
      userAgent: item.metadata && typeof item.metadata === 'object' && 'userAgent' in item.metadata ? String(item.metadata['userAgent']) : null,
      statusCode: undefined,
      errorMessage: item.motivo || null,
      timestamp: item.fecha,
    };
  }

  private safeParseJson(value: string | null | undefined): Record<string, unknown> | null {
    if (!value) {
      return null;
    }

    try {
      const parsed = JSON.parse(value);
      return parsed && typeof parsed === 'object' ? (parsed as Record<string, unknown>) : null;
    } catch {
      return null;
    }
  }
}