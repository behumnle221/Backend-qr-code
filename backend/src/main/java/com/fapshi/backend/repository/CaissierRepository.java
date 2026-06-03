package com.fapshi.backend.repository;

import com.fapshi.backend.entity.Caissier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CaissierRepository extends JpaRepository<Caissier, Long> {

    List<Caissier> findByVendeurId(Long vendeurId);

    boolean existsByEmail(String email);

    // Total ventes toutes périodes
    @Query("SELECT COALESCE(SUM(q.montant), 0) FROM QRCode q WHERE q.caissier.id = :id AND q.estUtilise = true")
    BigDecimal sumVentesByCaissierId(@Param("id") Long id);

    // Total ventes depuis une date
    @Query("SELECT COALESCE(SUM(q.montant), 0) FROM QRCode q WHERE q.caissier.id = :id AND q.estUtilise = true AND q.dateCreation >= :debut")
    BigDecimal sumVentesByCaissierIdSince(@Param("id") Long id, @Param("debut") LocalDateTime debut);

    // Nombre total de QR générés
    @Query("SELECT COUNT(q) FROM QRCode q WHERE q.caissier.id = :id")
    int countQrByCaissierId(@Param("id") Long id);

    // Nombre de QR payés dans la période
    @Query("SELECT COUNT(q) FROM QRCode q WHERE q.caissier.id = :id AND q.estUtilise = true AND q.dateCreation >= :debut")
    int countQrPayesByCaissierIdSince(@Param("id") Long id, @Param("debut") LocalDateTime debut);
}
