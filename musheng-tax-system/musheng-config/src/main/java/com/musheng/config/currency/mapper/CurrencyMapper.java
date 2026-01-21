package com.musheng.config.currency.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.musheng.config.currency.entity.Currency;
import org.apache.ibatis.annotations.Mapper;

/**
 * Currency Mapper
 */
@Mapper
public interface CurrencyMapper extends BaseMapper<Currency> {
}
