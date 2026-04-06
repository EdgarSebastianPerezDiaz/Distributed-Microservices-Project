package com.distribuidos.proveedor_service.repository;

import com.distribuidos.proveedor_service.model.Supplier;
import com.distribuidos.proveedor_service.model.SupplierStatus;
import com.distribuidos.proveedor_service.model.PersonType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, UUID> {
    
    Optional<Supplier> findByNit(String nit);
    
    Optional<Supplier> findByEmail(String email);
    
    boolean existsByNit(String nit);
    
    boolean existsByEmail(String email);
    
    boolean existsByNitAndIdNot(String nit, UUID id);
    
    boolean existsByEmailAndIdNot(String email, UUID id);
    
    Page<Supplier> findByStatus(SupplierStatus status, Pageable pageable);
    
    Page<Supplier> findByPersonType(PersonType personType, Pageable pageable);
    
    @Query("SELECT s FROM Supplier s WHERE " +
           "(:status IS NULL OR s.status = :status) AND " +
           "(:personType IS NULL OR s.personType = :personType) AND " +
           "(:search IS NULL OR LOWER(s.businessName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(s.nit) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Supplier> findAllWithFilters(@Param("status") SupplierStatus status,
                                      @Param("personType") PersonType personType,
                                      @Param("search") String search,
                                      Pageable pageable);
}