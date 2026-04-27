from contextlib import asynccontextmanager
from fastapi import FastAPI
from app.config import APP_NAME, APP_VERSION, EUREKA_ENABLED, EUREKA_SERVER, SERVICE_HOST, SERVICE_PORT
from app.database import create_indexes  ##Importacion de la creacion de indices
from app.routes.audit_routes import router  ##Importa los endpoints del microservicio

@asynccontextmanager
async def lifespan(app: FastAPI): ##Definr funciones que controlan el inicio y el final de ejecucion
    await create_indexes()

    if EUREKA_ENABLED:
        try:
            import py_eureka_client.eureka_client as eureka_client
            await eureka_client.init_async(
                eureka_server=EUREKA_SERVER,
                app_name=APP_NAME,
                instance_host=SERVICE_HOST,
                instance_port=SERVICE_PORT
            )
        except Exception as e:
            print(f"No se pudo registrar en Eureka: {e}")

    yield

app = FastAPI(
    title=APP_NAME,
    version=APP_VERSION,
    description="Microservicio de auditoría con FastAPI y MongoDB",
    lifespan=lifespan
)

app.include_router(router)