package com.fapshi.backend.repository;

import com.fapshi.backend.entity.Caissier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface CaissierRepository extends JpaRepository<Caissier, Long> {
    List<Caissier> findByVendeurId(Long vendeurId);
    boolean existsByEmail(String email);

    @Query("SELECT COALESCE(SUM(q.montant), 0) FROM QRCode q WHERE q.caissier.id = :caissierId AND q.estUtilise = true")
    BigDecimal sumVentesByCaissierId(@Param("caissierId") Long caissierId);

    @Query("SELECT COUNT(q) FROM QRCode q WHERE q.caissier.id = :caissierId")
    int countQrByCaissierId(@Param("caissierId") Long caissierId);
}
