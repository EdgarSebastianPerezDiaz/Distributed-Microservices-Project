package com.distribuidos.contrato_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class ContratoServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ContratoServiceApplication.class, args);
    }
    
    /**
     * Bean RestTemplate para llamadas HTTP síncronas
     * FIX HC-3: Para llamadas a audit-service
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
