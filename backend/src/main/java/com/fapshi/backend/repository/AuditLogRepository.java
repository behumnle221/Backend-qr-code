package com.fapshi.backend.repository;

import com.fapshi.backend.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByTransactionId(Long transactionId);
    List<AuditLog> findByUserId(Long userId);
}
