package com.hcb.controller.admin;

import com.hcb.security.HcbUserDetails;
import com.hcb.service.FileStorageService;
import com.hcb.service.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/admin/settings")
@RequiredArgsConstructor
public class AdminSettingController {

    private final SettingService settingService;
    private final FileStorageService fileStorageService;

    @GetMapping
    public String viewSettings(Model model) {
        Map<String, String> settings = settingService.getAllSettings();
        model.addAttribute("settings", settings);
        return "admin/settings/list";
    }

    @PostMapping
    public String saveSettings(@RequestParam Map<String, String> formParams,
                               @RequestParam(value = "qrCodeFile", required = false) MultipartFile qrCodeFile,
                               @RequestParam(value = "removeCustomQr", required = false, defaultValue = "false") boolean removeCustomQr,
                               @AuthenticationPrincipal HcbUserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        Long adminId = userDetails != null ? userDetails.getId() : null;

        // 1. Handle QR Code file upload
        if (qrCodeFile != null && !qrCodeFile.isEmpty()) {
            try {
                String storedQrFilename = fileStorageService.storeProductImage(qrCodeFile);
                settingService.updateSetting("qr_code_image", storedQrFilename, adminId);
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Failed to upload QR code: " + e.getMessage());
                return "redirect:/admin/settings";
            }
        } else if (removeCustomQr) {
            String currentQr = settingService.getSetting("qr_code_image", "");
            if (!currentQr.isEmpty()) {
                try {
                    fileStorageService.deleteProductImage(currentQr);
                } catch (Exception ignored) {}
                settingService.updateSetting("qr_code_image", "", adminId);
            }
        }

        // 2. Boolean checkboxes don't submit if unchecked, so we handle known booleans
        String[] booleanKeys = {
                "site_maintenance_mode",
                "admin_notification_enabled",
                "customer_registration_enabled",
                "guest_checkout_enabled",
                "delivery_pincode_restriction_enabled"
        };

        for (String bKey : booleanKeys) {
            String val = formParams.getOrDefault(bKey, "false");
            settingService.updateSetting(bKey, val, adminId);
        }

        // 3. Update other settings
        for (Map.Entry<String, String> entry : formParams.entrySet()) {
            String key = entry.getKey();
            if (key.equals("_csrf") || key.equals("removeCustomQr") || isBooleanKey(key, booleanKeys)) {
                continue;
            }
            settingService.updateSetting(key, entry.getValue().trim(), adminId);
        }

        redirectAttributes.addFlashAttribute("successMessage", "System settings have been updated successfully.");
        return "redirect:/admin/settings";
    }

    private boolean isBooleanKey(String key, String[] booleanKeys) {
        for (String bKey : booleanKeys) {
            if (bKey.equals(key)) return true;
        }
        return false;
    }
}
