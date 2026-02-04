package com.fapshi.backend.service;

import com.fapshi.backend.entity.StatusCheck;
import com.fapshi.backend.repository.StatusCheckRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service pour l'historique des vérifications de statut (polling).
 */
@Service
public class StatusCheckService {

    @Autowired
    private StatusCheckRepository statusCheckRepository;

    public StatusCheck save(StatusCheck check) {
        return statusCheckRepository.save(check);
    }
}