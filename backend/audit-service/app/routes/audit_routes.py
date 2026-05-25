from typing import Optional
from datetime import datetime
from fastapi import APIRouter, Depends, Query, Request, HTTPException
import logging
from app.schemas import EventoCreate
from app.services.audit_service import create_event, get_events, get_resumen
from app.security import require_internal_post, require_read_access

router = APIRouter(tags=["Auditoria"])

@router.get("/health")
async def health(): ##Devuelve el servicio que existe
    return {"status": "ok", "service": "servicio-auditoria"}

@router.post("/eventos", status_code=201)
async def registrar_evento(
    evento: EventoCreate,
    request: Request,
    user=Depends(require_internal_post)
):
    logger = logging.getLogger("audit_routes")
    try:
        auth = request.headers.get("authorization")
        client_host = request.client.host if request.client else "unknown"
        logger.info(
            "POST /eventos desde %s authorization=%s payload=%s",
            client_host,
            auth,
            evento.model_dump(),
        )
    except Exception:
        logger.debug("POST /eventos payload (unable to serialize)")

    try:
        return await create_event(evento.model_dump())
    except Exception:
        logger.exception("Error al crear evento de auditoría")
        raise HTTPException(status_code=500, detail="Error interno al registrar evento")

@router.get("/eventos")
async def listar_eventos(
        contrato_id: Optional[str] = Query(default=None),
    entidad_tipo: Optional[str] = Query(default=None),
    tipo_evento: Optional[str] = Query(default=None),
    usuario: Optional[str] = Query(default=None),
    fecha_desde: Optional[datetime] = Query(default=None),
    fecha_hasta: Optional[datetime] = Query(default=None),
    offset: int = Query(default=0, ge=0),
    limit: int = Query(default=20, ge=1, le=100),
        user=Depends(require_read_access)
):
    rol = user.get("role")

    if(rol== "AUDITOR"): entidad_tipo= "CONTRATO"

    return await get_events(
        contrato_id=contrato_id,
        entidad_tipo=entidad_tipo,
        tipo_evento=tipo_evento,
        usuario=usuario,
        fecha_desde=fecha_desde,
        fecha_hasta=fecha_hasta,
        offset=offset,
        limit=limit
    )

@router.get("/eventos/resumen")
async def resumen_eventos(
    fecha_desde: Optional[datetime] = Query(default=None),
    fecha_hasta: Optional[datetime] = Query(default=None),
        user=Depends(require_read_access)
):
    return await get_resumen(
        fecha_desde=fecha_desde,
        fecha_hasta=fecha_hasta
    )