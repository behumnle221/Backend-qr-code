package com.fapshi.backend.service;

import com.fapshi.backend.entity.AangaraaPayResponse;
import com.fapshi.backend.repository.AangaraaPayResponseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service pour stocker et récupérer les réponses de l'API.
 */
@Service
public class AangaraaPayResponseService {

    @Autowired
    private AangaraaPayResponseRepository responseRepository;

    public AangaraaPayResponse save(AangaraaPayResponse response) {
        return responseRepository.save(response);
    }

    public Optional<AangaraaPayResponse> findById(Long id) {
        return responseRepository.findById(id);
    }
}