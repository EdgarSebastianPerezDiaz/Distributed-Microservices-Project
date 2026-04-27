package com.distribuidos.contrato_service.config;


import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class FeignClientConfig {
    
    private static final Logger log = LoggerFactory.getLogger(FeignClientConfig.class);
    
    @Bean
    public RequestInterceptor jwtTokenInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                try {
                    // Obtener el JWT token del request context
                    ServletRequestAttributes requestAttributes = 
                        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                    
                    if (requestAttributes != null) {
                        HttpServletRequest request = requestAttributes.getRequest();
                        String authHeader = request.getHeader("Authorization");
                        
                        if (authHeader != null && authHeader.startsWith("Bearer ")) {
                            template.header("Authorization", authHeader);
                            log.debug("Added JWT token to Feign request: {}", template.url());
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to add JWT token to Feign request: {}", e.getMessage());
                }
            }
        };
    }
}