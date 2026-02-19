package com.fapshi.backend.repository;

import com.fapshi.backend.entity.ResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResetTokenRepository extends JpaRepository<ResetToken, Long> {

    Optional<ResetToken> findByToken(String token);
    void deleteByUserId(Long userId);  // Pour nettoyer les anciens codes
}