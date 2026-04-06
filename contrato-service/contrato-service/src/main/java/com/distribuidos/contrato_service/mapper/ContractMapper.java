package com.distribuidos.contrato_service.mapper;

import com.distribuidos.contrato_service.dto.ContractRequest;
import com.distribuidos.contrato_service.dto.ContractResponse;
import com.distribuidos.contrato_service.dto.ContractStatusHistoryResponse;
import com.distribuidos.contrato_service.dto.ContractUpdateRequest;
import com.distribuidos.contrato_service.model.Contract;
import com.distribuidos.contrato_service.model.ContractStatus;
import com.distribuidos.contrato_service.model.ContractStatusHistory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ContractMapper {
    
    public Contract toEntity(ContractRequest request, UUID userId) {
        Contract contract = new Contract();
        contract.setSupplierId(request.getSupplierId());
        contract.setObject(request.getObject());
        contract.setBudget(request.getBudget());
        contract.setStartDate(request.getStartDate());
        contract.setEndDate(request.getEndDate());
        contract.setStatus(ContractStatus.BORRADOR);
        contract.setCreatedByUserId(userId);
        contract.setDeleted(false);
        return contract;
    }
    
    public ContractResponse toResponse(Contract contract) {
        return ContractResponse.builder()
                .id(contract.getId())
                .supplierId(contract.getSupplierId())
                .supplierNit(contract.getSupplierNit())
                .supplierBusinessName(contract.getSupplierBusinessName())
                .contractNumber(contract.getContractNumber())
                .object(contract.getObject())
                .budget(contract.getBudget())
                .startDate(contract.getStartDate())
                .endDate(contract.getEndDate())
                .status(contract.getStatus())
                .createdByUserId(contract.getCreatedByUserId())
                .createdAt(contract.getCreatedAt())
                .updatedAt(contract.getUpdatedAt())
                .build();
    }
    
    public void updateEntity(Contract existing, ContractUpdateRequest request) {
        if (request.getObject() != null) {
            existing.setObject(request.getObject());
        }
        if (request.getBudget() != null) {
            existing.setBudget(request.getBudget());
        }
    }
    
    public ContractStatusHistoryResponse toHistoryResponse(ContractStatusHistory history) {
        return ContractStatusHistoryResponse.builder()
                .id(history.getId())
                .previousStatus(history.getPreviousStatus())
                .newStatus(history.getNewStatus())
                .reason(history.getReason())
                .userId(history.getUserId())
                .changeDate(history.getChangeDate())
                .build();
    }
}