package com.fapshi.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate(ObjectMapper objectMapper) {
        RestTemplate restTemplate = new RestTemplate();

        // Utilise l'objectMapper configuré ci-dessous
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper);
        restTemplate.getMessageConverters().add(0, converter);

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(30000);
        restTemplate.setRequestFactory(factory);

        return restTemplate;
    }

    @Bean
    @Primary // Priorité à cette instance pour toute l'application
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // --- CORRECTIF POUR LES DATES ---
        mapper.registerModule(new JavaTimeModule()); 
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // --------------------------------
        
        return mapper;
    }
}