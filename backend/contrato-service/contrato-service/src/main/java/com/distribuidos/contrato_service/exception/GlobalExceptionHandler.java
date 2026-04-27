package com.distribuidos.contrato_service.exception;

import com.distribuidos.contrato_service.dto.ErrorResponse;
import feign.FeignException;
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
    
    @ExceptionHandler(ContractNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleContractNotFound(
            ContractNotFoundException ex,
            HttpServletRequest request) {
        
        log.error("Contract not found: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .codigo(ex.getCodigo())
                .mensaje(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(DuplicateContractNumberException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateContractNumber(
            DuplicateContractNumberException ex,
            HttpServletRequest request) {
        
        log.error("Duplicate contract number: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .codigo(ex.getCodigo())
                .mensaje(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    
    @ExceptionHandler(ContractNotEditableException.class)
    public ResponseEntity<ErrorResponse> handleContractNotEditable(
            ContractNotEditableException ex,
            HttpServletRequest request) {
        
        log.error("Contract not editable: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .codigo(ex.getCodigo())
                .mensaje(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }
    
    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStatusTransition(
            InvalidStatusTransitionException ex,
            HttpServletRequest request) {
        
        log.error("Invalid status transition: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .codigo(ex.getCodigo())
                .mensaje(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }
    
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
        
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }
    
    @ExceptionHandler(SupplierInactiveException.class)
    public ResponseEntity<ErrorResponse> handleSupplierInactive(
            SupplierInactiveException ex,
            HttpServletRequest request) {
        
        log.error("Supplier inactive: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .codigo(ex.getCodigo())
                .mensaje(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }
    
    @ExceptionHandler(FeignException.NotFound.class)
    public ResponseEntity<ErrorResponse> handleFeignNotFound(
            FeignException.NotFound ex,
            HttpServletRequest request) {
        
        log.error("Feign client error: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .codigo("FGN_001")
                .mensaje("Supplier service returned 404 - Supplier not found")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }
    
    @ExceptionHandler(FeignException.ServiceUnavailable.class)
    public ResponseEntity<ErrorResponse> handleFeignServiceUnavailable(
            FeignException.ServiceUnavailable ex,
            HttpServletRequest request) {
        
        log.error("Service unavailable: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .codigo("SVC_001")
                .mensaje("Supplier service is temporarily unavailable. Please try again later.")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
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