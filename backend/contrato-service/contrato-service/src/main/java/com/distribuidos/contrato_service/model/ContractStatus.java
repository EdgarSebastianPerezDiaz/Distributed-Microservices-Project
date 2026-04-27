package com.distribuidos.contrato_service.model;

public enum ContractStatus {
    EN_PREPARACION,  // Estado inicial - editable
    PUBLICADO,       // Contrato publicado
    ADJUDICADO,      // Adjudicado
    EN_EJECUCION,    // En ejecución
    FINALIZADO,      // Finalizado
    CANCELADO        // Cancelado (desde cualquier estado)
}