from dotenv import load_dotenv  ##importa la funcion que lee .env
import os

##Carga del archivo .env para obtener la configuracion de la coneccion con mongo
load_dotenv()

##lee texto
APP_NAME = os.getenv("APP_NAME", "servicio-auditoria")  ## Lee APP_NAME si no existe usa el otro valor
APP_VERSION = os.getenv("APP_VERSION", "1.0.0")

##La clave que viene desde java 
MONGODB_URL = os.getenv("MONGODB_URL", "mongodb://localhost:27017")
DATABASE_NAME = os.getenv("DATABASE_NAME", "auditoria_db")
COLLECTION_NAME = os.getenv("COLLECTION_NAME", "eventos")

JWT_SECRET = os.getenv("JWT_SECRET", "mi-clave-super-secreta-para-jwt-de-512-bits-minimo-requerido-para-firmar-tokens-seguros-en-el-sistema-de-contratos-uptc-2026")
JWT_ALGORITHM = os.getenv("JWT_ALGORITHM", "HS512")

EUREKA_ENABLED = os.getenv("EUREKA_ENABLED", "false").lower() == "true"
EUREKA_SERVER = os.getenv("EUREKA_SERVER", "http://localhost:8761/eureka")
SERVICE_HOST = os.getenv("SERVICE_HOST", "localhost")
SERVICE_PORT = int(os.getenv("SERVICE_PORT", "8000")) ##Convierte el puerto a entero