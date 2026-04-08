package com.distribuidos.contrato_service.service;

import com.distribuidos.contrato_service.model.ContractStatus;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class ContractStateMachine {

    private static final Map<ContractStatus, Set<ContractStatus>> VALID_TRANSITIONS = new HashMap<>();

    static {
        // EN_PREPARACION → PUBLICADO
        VALID_TRANSITIONS.put(ContractStatus.EN_PREPARACION, EnumSet.of(ContractStatus.PUBLICADO));

        // PUBLICADO → ADJUDICADO
        VALID_TRANSITIONS.put(ContractStatus.PUBLICADO, EnumSet.of(ContractStatus.ADJUDICADO));

        // ADJUDICADO → EN_EJECUCION
        VALID_TRANSITIONS.put(ContractStatus.ADJUDICADO, EnumSet.of(ContractStatus.EN_EJECUCION));

        // EN_EJECUCION → FINALIZADO
        VALID_TRANSITIONS.put(ContractStatus.EN_EJECUCION, EnumSet.of(ContractStatus.FINALIZADO));

        // FINALIZADO → {} (estado terminal)
        VALID_TRANSITIONS.put(ContractStatus.FINALIZADO, EnumSet.noneOf(ContractStatus.class));
    }

    public boolean isValidTransition(ContractStatus currentStatus, ContractStatus newStatus) {
        if (currentStatus == null || newStatus == null) {
            return false;
        }

        // CANCELADO puede venir desde cualquier estado excepto FINALIZADO
        if (newStatus == ContractStatus.CANCELADO && currentStatus != ContractStatus.FINALIZADO) {
            return true;
        }

        Set<ContractStatus> allowed = VALID_TRANSITIONS.get(currentStatus);
        return allowed != null && allowed.contains(newStatus);
    }

    public Set<ContractStatus> getAllowedTransitions(ContractStatus currentStatus) {
        Set<ContractStatus> transitions = VALID_TRANSITIONS.getOrDefault(currentStatus, EnumSet.noneOf(ContractStatus.class));

        if (currentStatus != ContractStatus.FINALIZADO && !transitions.contains(ContractStatus.CANCELADO)) {
            transitions = EnumSet.copyOf(transitions);
            transitions.add(ContractStatus.CANCELADO);
        }

        return transitions;
    }

    public boolean isTerminalState(ContractStatus status) {
        return status == ContractStatus.FINALIZADO;
    }
}