package com.fapshi.backend.service;

import com.fapshi.backend.entity.AangaraaPayRequest;
import com.fapshi.backend.repository.AangaraaPayRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service pour gérer les requêtes envoyées à l'API Aangaraa-Pay.
 */
@Service
public class AangaraaPayRequestService {

    @Autowired
    private AangaraaPayRequestRepository requestRepository;

    public AangaraaPayRequest save(AangaraaPayRequest request) {
        return requestRepository.save(request);
    }

    public Optional<AangaraaPayRequest> findById(Long id) {
        return requestRepository.findById(id);
    }

    public Optional<AangaraaPayRequest> findByExternalTransactionId(String externalId) {
        return requestRepository.findByExternalTransactionId(externalId);
    }
}