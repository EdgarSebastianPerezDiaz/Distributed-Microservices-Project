package com.distribuidos.contrato_service.repository;

import com.distribuidos.contrato_service.model.Contract;
import com.distribuidos.contrato_service.model.ContractStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContractStatusHistoryRepository extends JpaRepository<ContractStatusHistory, UUID> {

    List<ContractStatusHistory> findByContractOrderByChangeDateAsc(Contract contract);

    List<ContractStatusHistory> findByContractIdOrderByChangeDateAsc(UUID contractId);

    long countByContractId(UUID contractId);
}