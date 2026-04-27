package com.distribuidos.contrato_service.repository;

import com.distribuidos.contrato_service.model.Contract;
import com.distribuidos.contrato_service.model.ContractStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContractRepository extends JpaRepository<Contract, UUID> {

    Optional<Contract> findByContractNumber(String contractNumber);

    @Query(value = "SELECT * FROM contratos WHERE eliminado = false ORDER BY CAST(numero_contrato AS INTEGER) DESC LIMIT 1", nativeQuery = true)
    Optional<Contract> findTopByOrderByContractNumberDesc();

    boolean existsByContractNumber(String contractNumber);

    boolean existsByContractNumberAndIdNot(String contractNumber, UUID id);

    Page<Contract> findByDeletedFalse(Pageable pageable);

    Page<Contract> findByCreatedByUserIdAndDeletedFalse(UUID userId, Pageable pageable);

    Page<Contract> findByStatusAndDeletedFalse(ContractStatus status, Pageable pageable);

    List<Contract> findBySupplierIdAndStatusInAndDeletedFalse(UUID supplierId, List<ContractStatus> statuses);

    @Query("SELECT c FROM Contract c WHERE c.deleted = false AND " +
            "(:status IS NULL OR c.status = :status) AND " +
            "(:search IS NULL OR LOWER(c.contractNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.object) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Contract> findAllWithFilters(@Param("status") ContractStatus status,
                                      @Param("search") String search,
                                      Pageable pageable);

    @Query("SELECT c FROM Contract c WHERE c.deleted = false AND " +
            "c.createdByUserId = :userId AND " +
            "(:status IS NULL OR c.status = :status)")
    Page<Contract> findByUserIdWithFilters(@Param("userId") UUID userId,
                                           @Param("status") ContractStatus status,
                                           Pageable pageable);

    long countBySupplierIdAndStatusInAndDeletedFalse(UUID supplierId, List<ContractStatus> statuses);
}