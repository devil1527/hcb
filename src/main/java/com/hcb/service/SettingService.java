package com.hcb.service;

import java.math.BigDecimal;
import java.util.Map;

public interface SettingService {

    String getSetting(String key, String defaultValue);

    int getIntSetting(String key, int defaultValue);

    boolean getBooleanSetting(String key, boolean defaultValue);

    BigDecimal getBigDecimalSetting(String key, BigDecimal defaultValue);

    void updateSetting(String key, String value, Long updatedByUserId);

    Map<String, String> getAllSettings();
}
