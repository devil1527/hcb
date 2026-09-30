package com.hcb.service.impl;

import com.hcb.service.DeliveryService;
import com.hcb.service.SettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryServiceImpl implements DeliveryService {

    private final SettingService settingService;

    @Override
    public boolean isPincodeDeliverable(String pincode) {
        if (!isRestrictionEnabled()) {
            return true;
        }

        if (pincode == null) {
            return false;
        }

        String trimmed = pincode.trim();
        if (!trimmed.matches("^[1-9]\\d{5}$")) {
            return false;
        }

        try {
            int code = Integer.parseInt(trimmed);
            int min = Integer.parseInt(getMinPincode());
            int max = Integer.parseInt(getMaxPincode());
            return code >= min && code <= max;
        } catch (NumberFormatException e) {
            log.warn("Failed to parse pincode range for validation: {}", trimmed);
            return false;
        }
    }

    @Override
    public String getPincodeErrorMessage(String pincode) {
        return "We currently only deliver within " + getDeliveryRegionName() + 
                " (PIN codes " + getMinPincode() + " – " + getMaxPincode() + 
                "). We are expanding to other locations soon!";
    }

    @Override
    public boolean isRestrictionEnabled() {
        return settingService.getBooleanSetting("delivery_pincode_restriction_enabled", true);
    }

    @Override
    public String getMinPincode() {
        return settingService.getSetting("delivery_pincode_min", "411001");
    }

    @Override
    public String getMaxPincode() {
        return settingService.getSetting("delivery_pincode_max", "411090");
    }

    @Override
    public String getDeliveryRegionName() {
        return settingService.getSetting("delivery_city_name", "Pune & PCMC");
    }
}
