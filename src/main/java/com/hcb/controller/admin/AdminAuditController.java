package com.hcb.controller.admin;

import com.hcb.model.entity.AuditLog;
import com.hcb.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping({"/admin/audit", "/admin/audit-logs"})
@RequiredArgsConstructor
public class AdminAuditController {

    private final AuditService auditService;

    @GetMapping
    public String listAuditLogs(@RequestParam(value = "page", defaultValue = "0") int page,
                                @RequestParam(value = "size", defaultValue = "25") int size,
                                Model model) {
        Page<AuditLog> auditPage = auditService.getAllLogs(PageRequest.of(page, size));
        model.addAttribute("auditPage", auditPage);
        return "admin/audit/list";
    }
}
