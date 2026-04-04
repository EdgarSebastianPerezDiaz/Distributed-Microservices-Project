package com.distribuidos.contrato_service.model;

public enum ContractStatus {
    BORRADOR,      // Estado inicial - editable
    ACTIVO,        // Contrato publicado y activo
    EN_EJECUCION,  // En ejecución
    VENCIDO,       // Finalizado por fecha
    ANULADO        // Cancelado por ADMIN
}