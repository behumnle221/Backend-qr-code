package com.fapshi.backend.service;

import com.fapshi.backend.entity.Vendeur;
import com.fapshi.backend.repository.VendeurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service pour les Vendeurs : inscription, recherche, gestion solde.
 */
@Service
public class VendeurService {

    @Autowired
    private VendeurRepository vendeurRepository;

    public Vendeur save(Vendeur vendeur) {
        return vendeurRepository.save(vendeur);
    }

    public Optional<Vendeur> findById(Long id) {
        return vendeurRepository.findById(id);
    }

    public List<Vendeur> findAll() {
        return vendeurRepository.findAll();
    }

    public Optional<Vendeur> findByTelephone(String telephone) {
        return vendeurRepository.findByTelephone(telephone);
    }
    
    // ⬅️ NOUVELLE MÉTHODE AJOUTÉE
    public Optional<Vendeur> findByEmail(String email) {
        return vendeurRepository.findByEmail(email);
    }
}