package com.distribuidos.contrato_service.model;

public enum ContractStatus {
    EN_PREPARACION,   // Estado inicial (reemplaza BORRADOR)
    PUBLICADO,        // Contrato publicado
    ADJUDICADO,       // Adjudicado a proveedor
    EN_EJECUCION,     // En ejecución
    FINALIZADO,       // Finalizado correctamente
    CANCELADO         // Cancelado (desde cualquier estado)
}