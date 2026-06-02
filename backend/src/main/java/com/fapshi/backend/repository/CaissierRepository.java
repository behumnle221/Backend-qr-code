package com.fapshi.backend.repository;

import com.fapshi.backend.entity.Caissier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaissierRepository extends JpaRepository<Caissier, Long> {
    List<Caissier> findByVendeurId(Long vendeurId);
    boolean existsByEmail(String email);
}
