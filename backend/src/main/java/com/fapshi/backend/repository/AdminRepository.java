package com.fapshi.backend.repository;

import com.fapshi.backend.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository pour les Admins.
 * Operations CRUD de base.
 */
@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {
}