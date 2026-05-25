export type AuditEventType =
  | 'LOGIN'
  | 'LOGOUT'
  | 'CREATE'
  | 'UPDATE'
  | 'DELETE'
  | 'TRANSITION_STATE'
  | 'ERROR'
  | 'CREAR_CONTRATO'
  | 'CAMBIAR_ESTADO'
  | 'MODIFICAR_CONTRATO'
  | 'ELIMINAR_CONTRATO'
  | 'ANULAR'
  | 'MODIFICAR_PROVEEDOR'
  | 'CREAR_PROVEEDOR'
  | 'CAMBIAR_ESTADO_PROVEEDOR'
  | 'CREAR_USUARIO'
  | 'MODIFICAR_USUARIO';

export type AuditEntityType = 'USER' | 'SUPPLIER' | 'PROVEEDOR' | 'CONTRACT' | 'SYSTEM';

export interface AuditEventResponse {
  _id?: string;
  eventId?: string;
  eventType: AuditEventType;
  entityType: AuditEntityType;
  entityId?: string;
  operation?: string;
  description?: string;
  userRole?: string;
  performedBy?: string;
  previousData?: Record<string, unknown> | null;
  newData?: Record<string, unknown> | null;
  ipAddress?: string;
  userAgent?: string | null;
  statusCode?: number;
  errorMessage?: string | null;
  timestamp: string;
}

export interface AuditEventApiResponse {
  id: string;
  entidad_tipo: AuditEntityType;
  entidad_id: string;
  operacion: string;
  tipo_evento: string;
  descripcion: string;
  estado_anterior?: string | null;
  estado_nuevo?: string | null;
  motivo?: string | null;
  usuario_id: string;
  usuario_nombre: string;
  usuario_rol: string;
  fecha: string;
  version: number;
  metadata?: Record<string, unknown> | null;
  contrato_id?: string | null;
}

export interface AuditEventListResponse {
  total: number;
  limit: number;
  offset: number;
  data: AuditEventResponse[];
}

export interface AuditEventFilters {
  tipo_evento?: string;
  entidad_tipo?: AuditEntityType;
  usuario?: string;
  fecha_desde?: string;
  fecha_hasta?: string;
  limit?: number;
  offset?: number;
}