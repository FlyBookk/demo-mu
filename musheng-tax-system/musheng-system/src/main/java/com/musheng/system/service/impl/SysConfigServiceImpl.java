package com.musheng.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.musheng.system.entity.SysConfig;
import com.musheng.system.mapper.SysConfigMapper;
import com.musheng.common.service.SysConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 系统配置服务实现
 *
 * @author wanhua
 * 21:35 2026年03月21日
 */
@Service
@Slf4j
public class SysConfigServiceImpl implements SysConfigService {

    @Autowired
    private SysConfigMapper sysConfigMapper;

    @Override
    public String getValue(String key) {
        return sysConfigMapper.selectValueByKey(key);
    }

    @Override
    public void updateValue(String key, String value) {
        sysConfigMapper.update(null,
                new LambdaUpdateWrapper<SysConfig>()
                        .eq(SysConfig::getConfigKey, key)
                        .set(SysConfig::getConfigValue, value));
        log.info("更新系统配置: {} = {}", key, value);
    }
}
