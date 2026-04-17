from pydantic import BaseModel, Field
from typing import Optional, Dict, Any, Literal
from datetime import datetime  ##Importacion para fecha
from uuid import UUID    ##Importacion ppara id

#Tipo de etidad
EntidadTipo=Literal["CONTRATO","PROVEEDOR","USUARIO"]

#Tipo de operacion 
OperacionTipo = Literal["CREATE","UPDATE","DELETE"]


## Define los tipos personalizados
TipoEvento = Literal[ 
    "CREAR_CONTRATO",
    "CAMBIAR_ESTADO",
    "MODIFICAR_CONTRATO",
    "ANULAR",
    "MODIFICAR_PROVEEDOR",
    "CREAR_PROVEEDOR",
    "CAMBIAR_ESTADO_PROVEEDOR",
    "CREAR_USUARIO",
    "MODIFICAR_USUARIO"
]
#rOLES VALIDOS

RolUsuario=Literal["ADMINISTRADOR","FUNCIONARIO","AUDITOR"]


##Define que campos debe tener el JSON al realizar un nuevo registro
class EventoCreate(BaseModel):
    entidad_tipo:EntidadTipo=Field(...,description="Contratp, Proveedor o Usuario")
    entidad_id: str = Field(..., min_length=1, description="ID Único del recurso afectado (UUID)")

    #Tipo de operacion
    tipo_evento:TipoEvento =Field(...,description="Tipo de evento")

    descripcion: str = Field(
        min_length=5,
        max_length=500,
        description="Descripcion del cambio realizado a las emas tablas"
    )

    estado_anterior: Optional[str]=Field(default=None,description="Estado anterior")
    estado_nuevo: Optional[str]=Field(default=None,description="Estado Nuevo")

    motivo:Optional[str]=Field(default=None,max_length=300,description="Motivo")

    #Usuario que hizo el cambio
    usuario_id: UUID =Field(...,description="ID del usuario que ejecuto la accion")
    usuario_nombre: str = Field(min_length=2, max_length=120,description="Nombre usuario")
    usuario_rol:RolUsuario=Field(...,description="Rol del ejecutor")

    #Fecha del evento
    fecha: Optional[datetime]=Field(default=None,description="Fecha")

    version:int=Field(
        ge=1,
        description="version"
    )

    metadata: Optional[Dict[str,Any]]=Field(default=None,description="Datos adicionales")

    contrato_id: Optional[UUID]=Field(default=None,description="ID contrato")


class EventoResponse(BaseModel):
    id: str
    entidad_tipo: str
    entidad_id: str
    operacion: str
    tipo_evento: str
    descripcion: str
    estado_anterior: Optional[str] = None
    estado_nuevo: Optional[str] = None
    motivo: Optional[str] = None
    usuario_id: str
    usuario_nombre: str
    usuario_rol:str
    fecha: datetime
    version: int
    metadata: Optional[Dict[str, Any]] = None
    contrato_id:Optional[str]=None
##Respuesta de resumen de cada item
class ResumenItem(BaseModel):
    fecha_dia: str
    tipo_evento: str
    total: int