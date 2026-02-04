package com.fapshi.backend.service;

import com.fapshi.backend.entity.ApiCredentials;
import com.fapshi.backend.repository.ApiCredentialsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service pour gérer les clés API (appKey Aangaraa-Pay).
 */
@Service
public class ApiCredentialsService {

    @Autowired
    private ApiCredentialsRepository credentialsRepository;

    public ApiCredentials save(ApiCredentials credentials) {
        return credentialsRepository.save(credentials);
    }

    public Optional<ApiCredentials> findByAppKey(String appKey) {
        return credentialsRepository.findByAppKey(appKey);
    }
}