package com.distribuidos.contrato_service.config;


import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@Configuration
public class FeignClientConfig {
    
    @Bean
    public RequestInterceptor jwtTokenInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                var authentication = SecurityContextHolder.getContext().getAuthentication();
                
                if (authentication instanceof JwtAuthenticationToken) {
                    Jwt jwt = ((JwtAuthenticationToken) authentication).getToken();
                    String tokenValue = jwt.getTokenValue();
                    template.header("Authorization", "Bearer " + tokenValue);
                    log.debug("Added JWT token to Feign request: {}", template.url());
                } else {
                    log.warn("No JWT token found in security context for Feign request: {}", template.url());
                }
            }
            
            private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FeignClientConfig.class);
        };
    }
}