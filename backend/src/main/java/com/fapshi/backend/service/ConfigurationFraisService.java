package com.fapshi.backend.service;

import com.fapshi.backend.entity.ConfigurationFrais;
import com.fapshi.backend.repository.ConfigurationFraisRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service pour gérer la configuration unique des frais de la plateforme.
 */
@Service
public class ConfigurationFraisService {

    @Autowired
    private ConfigurationFraisRepository configRepository;

    public ConfigurationFrais save(ConfigurationFrais config) {
        return configRepository.save(config);
    }

    // Récupère la config (toujours ID = 1)
    public Optional<ConfigurationFrais> getConfig() {
        return configRepository.findById(1L);
    }
}