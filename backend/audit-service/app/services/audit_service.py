from datetime import datetime, timezone #3trabaja con la hora local
from typing import Optional
from bson import ObjectId
from app.database import eventos_collection

def serialize_document(doc: dict) -> dict: ##Convierte un documento para devolverlo a JSON
    return {
           "id": str(doc["_id"]),
        "entidad_tipo": doc.get("entidad_tipo"),
        "entidad_id": doc.get("entidad_id"),
        "operacion": doc.get("operacion"),
        "tipo_evento": doc["tipo_evento"],
        "descripcion": doc.get("descripcion"),
        "estado_anterior": doc.get("estado_anterior"),
        "estado_nuevo": doc.get("estado_nuevo"),
        "motivo": doc.get("motivo"),
        "usuario_id": str(doc["usuario_id"]),
        "usuario_nombre": doc["usuario_nombre"],
        "rol_usuario": doc.get("rol_usuario"),
        "fecha": doc["fecha"],
        "version": doc.get("version"),
        "metadata": doc.get("metadata", {}),
        "contrato_id": str(doc["contrato_id"]) if doc.get("contrato_id") else None
    }

async def create_event(event_data: dict): ## Recibe los datos validados y los guarda en mongo
    document = {
          "entidad_tipo": event_data.get("entidad_tipo"),
        "entidad_id": str(event_data.get("entidad_id", "")),
        "operacion": event_data.get("operacion"),
        "descripcion": event_data.get("descripcion"),
        "rol_usuario": event_data.get("rol_usuario"),
        "version": event_data.get("version"),
        "tipo_evento": event_data["tipo_evento"],
        "estado_anterior": event_data.get("estado_anterior"),
        "estado_nuevo": event_data.get("estado_nuevo"),
        "motivo": event_data.get("motivo"),
        "usuario_id": str(event_data["usuario_id"]),
        "usuario_nombre": event_data["usuario_nombre"],
        "fecha": event_data.get("fecha") or datetime.now(timezone.utc),
        "metadata": event_data.get("metadata", {}),
        "contrato_id": str(event_data["contrato_id"]) if event_data.get("contrato_id") else None
    }

    result = await eventos_collection.insert_one(document)
    document["_id"] = result.inserted_id
    return serialize_document(document)

async def get_events(   ##Recibe los parametros posibles si no recibe ninguno solo devuelve los establecidos
   contrato_id: Optional[str] = None,
    entidad_tipo: Optional[str] = None,
    tipo_evento: Optional[str] = None,
    usuario: Optional[str] = None,
    fecha_desde: Optional[datetime] = None,
    fecha_hasta: Optional[datetime] = None,
    offset: int = 0,
    limit: int = 20
):
    query = {}

    if contrato_id:
        query["contrato_id"] = contrato_id

    if entidad_tipo:                  
        query["entidad_tipo"] = entidad_tipo

    if tipo_evento:
        query["tipo_evento"] = tipo_evento

    if usuario:
        query["usuario_nombre"] = {"$regex": usuario, "$options": "i"}

    if fecha_desde or fecha_hasta:
        query["fecha"] = {}
        if fecha_desde:
            query["fecha"]["$gte"] = fecha_desde
        if fecha_hasta:
            query["fecha"]["$lte"] = fecha_hasta

    cursor = (
        eventos_collection
        .find(query)
        .sort("fecha", -1)
        .skip(offset)
        .limit(limit)
    )

    results = [] ##Itera y serializa cada documento 
    async for doc in cursor:
        results.append(serialize_document(doc))

    total = await eventos_collection.count_documents(query)

    return { ## La respuesta que se muestra en psotman
        "total": total,
        "offset": offset,
        "limit": limit,
        "items": results
    }

async def get_resumen( ## Construye un rango de fechas 
    fecha_desde: Optional[datetime] = None,
    fecha_hasta: Optional[datetime] = None
):
    match_stage = {}

    if fecha_desde or fecha_hasta:
        match_stage["fecha"] = {}
        if fecha_desde:
            match_stage["fecha"]["$gte"] = fecha_desde
        if fecha_hasta:
            match_stage["fecha"]["$lte"] = fecha_hasta

    pipeline = []

    if match_stage:
        pipeline.append({"$match": match_stage})

    pipeline.extend([
        {
            "$group": {
                "_id": {
                    "fecha_dia": {
                        "$dateToString": {
                            "format": "%Y-%m-%d",
                            "date": "$fecha"
                        }
                    },
                    "tipo_evento": "$tipo_evento"
                },
                "total": {"$sum": 1}
            }
        },
        {
            "$project": {
                "_id": 0,
                "fecha_dia": "$_id.fecha_dia",
                "tipo_evento": "$_id.tipo_evento",
                "total": 1
            }
        },
        {
            "$sort": {
                "fecha_dia": 1,
                "tipo_evento": 1
            }
        }
    ])

    cursor = eventos_collection.aggregate(pipeline)
    results = []

    async for doc in cursor:
        results.append(doc)

    return results