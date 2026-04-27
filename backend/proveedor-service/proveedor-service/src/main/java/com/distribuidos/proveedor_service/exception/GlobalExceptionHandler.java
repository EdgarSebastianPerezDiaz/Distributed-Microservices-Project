package com.distribuidos.proveedor_service.exception;


import com.distribuidos.proveedor_service.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(SupplierNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSupplierNotFound(
            SupplierNotFoundException ex,
            HttpServletRequest request) {
        
        log.error("Supplier not found: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .codigo(ex.getCodigo())
                .mensaje(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(DuplicateSupplierException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateSupplier(
            DuplicateSupplierException ex,
            HttpServletRequest request) {
        
        log.error("Duplicate supplier data: {}", ex.getMessage());
        
        List<Map<String, String>> detalles = new ArrayList<>();
        Map<String, String> detalle = new HashMap<>();
        detalle.put("campo", ex.getField());
        detalle.put("error", ex.getMessage());
        detalles.add(detalle);
        
        ErrorResponse error = ErrorResponse.builder()
                .codigo(ex.getCodigo())
                .mensaje("Duplicate value: " + ex.getField())
                .detalles(detalles)
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    
    @ExceptionHandler(SupplierHasActiveContractsException.class)
    public ResponseEntity<ErrorResponse> handleSupplierHasActiveContracts(
            SupplierHasActiveContractsException ex,
            HttpServletRequest request) {
        
        log.error("Supplier has active contracts: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .codigo(ex.getCodigo())
                .mensaje(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        
        List<Map<String, String>> detalles = new ArrayList<>();
        
        ex.getBindingResult().getAllErrors().forEach(error -> {
            Map<String, String> detalle = new HashMap<>();
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            detalle.put("campo", fieldName);
            detalle.put("error", errorMessage);
            detalles.add(detalle);
        });
        
        log.error("Validation error: {}", detalles);
        
        ErrorResponse error = ErrorResponse.builder()
                .codigo("VAL_001")
                .mensaje("Validation failed")
                .detalles(detalles)
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {
        
        log.error("Access denied: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .codigo("AUTH_001")
                .mensaje("Access denied - You don't have permission to perform this action")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {
        
        log.error("Unexpected error: ", ex);
        
        ErrorResponse error = ErrorResponse.builder()
                .codigo("SYS_001")
                .mensaje("An unexpected error occurred: " + ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}