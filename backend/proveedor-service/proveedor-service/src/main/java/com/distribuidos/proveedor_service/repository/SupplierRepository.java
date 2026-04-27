package com.distribuidos.proveedor_service.repository;

import com.distribuidos.proveedor_service.model.Supplier;
import com.distribuidos.proveedor_service.model.SupplierStatus;
import com.distribuidos.proveedor_service.model.PersonType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    /**
     * Listar proveedores con filtros usando SQL nativo
     * Usa ILIKE para búsqueda case-insensitive
     */
    @Query(value = "SELECT * FROM proveedores s WHERE " +
            "(:status IS NULL OR s.status = CAST(:status AS TEXT)) AND " +
            "(:personType IS NULL OR s.tipo_persona = CAST(:personType AS TEXT)) AND " +
            "(:search IS NULL OR s.razon_social ILIKE CONCAT('%', CAST(:search AS TEXT), '%') OR " +
            "s.nit ILIKE CONCAT('%', CAST(:search AS TEXT), '%')) " +
            "ORDER BY s.razon_social",
            nativeQuery = true)
    List<Supplier> findAllWithFiltersList(@Param("status") String status,
                                          @Param("personType") String personType,
                                          @Param("search") String search);

    /**
     * Contar proveedores con filtros usando SQL nativo
     */
    @Query(value = "SELECT COUNT(*) FROM proveedores s WHERE " +
            "(:status IS NULL OR s.status = CAST(:status AS TEXT)) AND " +
            "(:personType IS NULL OR s.tipo_persona = CAST(:personType AS TEXT)) AND " +
            "(:search IS NULL OR s.razon_social ILIKE CONCAT('%', CAST(:search AS TEXT), '%') OR " +
            "s.nit ILIKE CONCAT('%', CAST(:search AS TEXT), '%'))",
            nativeQuery = true)
    long countAllWithFilters(@Param("status") String status,
                             @Param("personType") String personType,
                             @Param("search") String search);
}