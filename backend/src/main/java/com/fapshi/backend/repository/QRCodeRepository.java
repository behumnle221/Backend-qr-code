package com.fapshi.backend.repository;

import com.fapshi.backend.entity.QRCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository pour les QR Codes générés par les vendeurs.
 */
@Repository
public interface QRCodeRepository extends JpaRepository<QRCode, Long> {

    // Tous les QR codes d'un vendeur
    List<QRCode> findByVendeurId(Long vendeurId);

    // QR codes non encore utilisés
    List<QRCode> findByEstUtiliseFalse();

    // Tous les QR codes d'un vendeur, triés par date de création décroissante 
    List<QRCode> findByVendeurIdOrderByDateCreationDesc(Long vendeurId);
}
   