from fastapi import Header, HTTPException  ##Perrmite leer ecabezados HTTP
import jwt
from jwt import InvalidTokenError
from app.config import JWT_SECRET, JWT_ALGORITHM

def decode_token(authorization: str = Header(default="")): ##funcij que recibe el token
    if not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Token requerido") ## se verifica que el token sea correcto

    token = authorization.replace("Bearer ", "").strip()

    try:
        payload = jwt.decode(token, JWT_SECRET, algorithms=[JWT_ALGORITHM])
        return payload
    except InvalidTokenError:
        raise HTTPException(status_code=401, detail="Token inválido")

def require_read_access(payload=Header(default=None), authorization: str = Header(default="")): ##Valid a qe el usuario tenga permiso de lectura del historial
    user = decode_token(authorization)
    roles = user.get("role")

    if isinstance(roles, str):
        roles = [roles]

    if not roles or not any(r in ["ADMINISTRADOR", "AUDITOR"] for r in roles):
        raise HTTPException(status_code=403, detail="No autorizado para consultar auditoría")

    return user

def require_internal_post(authorization: str = Header(default="")): ##Proteje la tabla auditoria solo permite registrar eventos a los usuarios con roles permitidos
    user = decode_token(authorization)
    roles = user.get("roles")

    if isinstance(roles, str):
        roles = [roles]

    if not roles or not any(r in ["ADMINISTRADOR", "FUNCIONARIO"] for r in roles):
        raise HTTPException(status_code=403, detail="No autorizado para registrar eventos")

    return user