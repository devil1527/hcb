package com.hcb.service.impl;

import com.hcb.model.entity.Setting;
import com.hcb.model.entity.User;
import com.hcb.repository.SettingRepository;
import com.hcb.repository.UserRepository;
import com.hcb.service.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettingServiceImpl implements SettingService {

    private final SettingRepository settingRepository;
    private final UserRepository userRepository;

    @Override
    public String getSetting(String key, String defaultValue) {
        return settingRepository.findBySettingKey(key)
                .map(Setting::getSettingValue)
                .orElse(defaultValue);
    }

    @Override
    public int getIntSetting(String key, int defaultValue) {
        try {
            return Integer.parseInt(getSetting(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public boolean getBooleanSetting(String key, boolean defaultValue) {
        return Boolean.parseBoolean(getSetting(key, String.valueOf(defaultValue)));
    }

    @Override
    public BigDecimal getBigDecimalSetting(String key, BigDecimal defaultValue) {
        try {
            return new BigDecimal(getSetting(key, defaultValue.toString()));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @Override
    @Transactional
    public void updateSetting(String key, String value, Long updatedByUserId) {
        User updater = updatedByUserId != null ? userRepository.findById(updatedByUserId).orElse(null) : null;
        Setting setting = settingRepository.findBySettingKey(key)
                .orElse(Setting.builder()
                        .settingKey(key)
                        .settingType("STRING")
                        .build());
        setting.setSettingValue(value);
        setting.setUpdatedBy(updater);
        settingRepository.save(setting);
    }

    @Override
    public Map<String, String> getAllSettings() {
        Map<String, String> map = new HashMap<>();
        settingRepository.findAll().forEach(s -> map.put(s.getSettingKey(), s.getSettingValue()));
        return map;
    }
}
