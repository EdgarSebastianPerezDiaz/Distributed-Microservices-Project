from motor.motor_asyncio import AsyncIOMotorClient
from pymongo import ASCENDING, DESCENDING    ##Importa constantes para definir el orden de los indices 
from app.config import MONGODB_URL, DATABASE_NAME, COLLECTION_NAME

client = AsyncIOMotorClient(MONGODB_URL) ##Crea la conexion con mongo 
db = client[DATABASE_NAME] ##Selecciona e cliente que va a tarbajar con la bd
eventos_collection = db[COLLECTION_NAME] ##Selecciona la coleccion de eventos que va a guardar los json

async def create_indexes(): ##Indices para celerar consultas
    await eventos_collection.create_index([("contrato_id", ASCENDING)])
    await eventos_collection.create_index([("fecha", DESCENDING)])
    await eventos_collection.create_index([("tipo_evento", ASCENDING)])
    await eventos_collection.create_index([("usuario_id", ASCENDING)])