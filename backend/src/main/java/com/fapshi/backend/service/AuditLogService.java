package com.fapshi.backend.service;

import com.fapshi.backend.entity.AuditLog;
import com.fapshi.backend.entity.Transaction;
import com.fapshi.backend.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    public AuditLog save(AuditLog auditLog) {
        return auditLogRepository.save(auditLog);
    }

    public void log(Long userId, String username, String action, String details, Transaction transaction) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUserId(userId);
        auditLog.setUsername(username);
        auditLog.setAction(action);
        auditLog.setDetails(details);
        auditLog.setTransaction(transaction);
        auditLogRepository.save(auditLog);
    }
}
