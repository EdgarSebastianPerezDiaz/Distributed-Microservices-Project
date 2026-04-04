package com.distribuidos.proveedor_service.mapper;

import com.distribuidos.proveedor_service.dto.SupplierRequest;
import com.distribuidos.proveedor_service.dto.SupplierResponse; 
import com.distribuidos.proveedor_service.dto.SupplierUpdateRequest;
import com.distribuidos.proveedor_service.model.Supplier;
import com.distribuidos.proveedor_service.model.SupplierStatus;
import org.springframework.stereotype.Component;

@Component
public class SupplierMapper {
    
    public Supplier toEntity(SupplierRequest request) {
        Supplier supplier = new Supplier();
        supplier.setNit(request.getNit());
        supplier.setBusinessName(request.getBusinessName());
        supplier.setEmail(request.getEmail());
        supplier.setPhone(request.getPhone());
        supplier.setPersonType(request.getPersonType());
        supplier.setStatus(SupplierStatus.ACTIVO); // Estado inicial ACTIVO
        return supplier;
    }
    
    public SupplierResponse toResponse(Supplier supplier) {
        return SupplierResponse.builder()
                .id(supplier.getId())
                .nit(supplier.getNit())
                .businessName(supplier.getBusinessName())
                .email(supplier.getEmail())
                .phone(supplier.getPhone())
                .personType(supplier.getPersonType())
                .status(supplier.getStatus())
                .createdAt(supplier.getCreatedAt())
                .updatedAt(supplier.getUpdatedAt())
                .build();
    }
    
    public void updateEntity(Supplier existing, SupplierUpdateRequest request) {
        if (request.getBusinessName() != null) {
            existing.setBusinessName(request.getBusinessName());
        }
        if (request.getEmail() != null) {
            existing.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            existing.setPhone(request.getPhone());
        }
        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }
    }
}