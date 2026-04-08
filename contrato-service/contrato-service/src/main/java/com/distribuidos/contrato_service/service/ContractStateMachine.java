package com.distribuidos.contrato_service.service;


import com.distribuidos.contrato_service.model.ContractStatus;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class ContractStateMachine {
    
    // Mapa de transiciones válidas 
    private static final Map<ContractStatus, Set<ContractStatus>> VALID_TRANSITIONS = new HashMap<>();
    
    static {
        // BORRADOR → ACTIVO
        VALID_TRANSITIONS.put(ContractStatus.BORRADOR, EnumSet.of(ContractStatus.ACTIVO));
        
        // ACTIVO → EN_EJECUCION, ANULADO
        VALID_TRANSITIONS.put(ContractStatus.ACTIVO, EnumSet.of(ContractStatus.EN_EJECUCION, ContractStatus.ANULADO));
        
        // EN_EJECUCION → VENCIDO, ANULADO
        VALID_TRANSITIONS.put(ContractStatus.EN_EJECUCION, EnumSet.of(ContractStatus.VENCIDO, ContractStatus.ANULADO));
        
        // VENCIDO → {} (no tiene transiciones)
        VALID_TRANSITIONS.put(ContractStatus.VENCIDO, EnumSet.noneOf(ContractStatus.class));
        
        // ANULADO → {} (no tiene transiciones)
        VALID_TRANSITIONS.put(ContractStatus.ANULADO, EnumSet.noneOf(ContractStatus.class));
    }
    
    /**
     * Valida si una transición de estado es permitida
     */
    public boolean isValidTransition(ContractStatus currentStatus, ContractStatus newStatus) {
        if (currentStatus == null || newStatus == null) {
            return false;
        }
        
        Set<ContractStatus> allowed = VALID_TRANSITIONS.get(currentStatus);
        return allowed != null && allowed.contains(newStatus);
    }
    
    /**
     * Obtiene las transiciones permitidas desde un estado
     */
    public Set<ContractStatus> getAllowedTransitions(ContractStatus currentStatus) {
        return VALID_TRANSITIONS.getOrDefault(currentStatus, EnumSet.noneOf(ContractStatus.class));
    }
    
    /**
     * Verifica si un estado es terminal (no tiene transiciones)
     */
    public boolean isTerminalState(ContractStatus status) {
        Set<ContractStatus> transitions = VALID_TRANSITIONS.get(status);
        return transitions == null || transitions.isEmpty();
    }
}
