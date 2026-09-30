package com.hcb.service;

import com.hcb.model.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AuditService {

    void log(String action, String entityType, Long entityId, String oldValue, String newValue, Long userId);

    List<AuditLog> getRecentLogs();

    Page<AuditLog> getAllLogs(Pageable pageable);
}
