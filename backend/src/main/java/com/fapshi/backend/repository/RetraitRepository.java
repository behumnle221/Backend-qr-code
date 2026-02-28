package com.fapshi.backend.repository;

import com.fapshi.backend.entity.Retrait;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RetraitRepository extends JpaRepository<Retrait, Long> {
    
    // Récupérer tous les retraits d'un vendeur
    List<Retrait> findByVendeurId(Long vendeurId);
    
    // Récupérer les retraits par statut
    List<Retrait> findByStatut(String statut);
    
    // Récupérer le dernier retrait d'un vendeur (pour vérifier l'écart 5h)
    @Query("SELECT r FROM Retrait r WHERE r.vendeur.id = :vendeurId ORDER BY r.dateCreation DESC LIMIT 1")
    Optional<Retrait> findLastRetraitByVendeur(@Param("vendeurId") Long vendeurId);
    
    // Récupérer les retraits SUCCESS et PENDING après une date donnée
    @Query("SELECT r FROM Retrait r WHERE r.vendeur.id = :vendeurId AND r.statut IN ('SUCCESS', 'PENDING') AND r.dateCreation >= :dateLimit")
    List<Retrait> findRecentRetaits(@Param("vendeurId") Long vendeurId, @Param("dateLimit") java.time.LocalDateTime dateLimit);
    
    // Récupérer les retraits avec pagination
    Page<Retrait> findByVendeurIdOrderByDateCreationDesc(Long vendeurId, Pageable pageable);
}
