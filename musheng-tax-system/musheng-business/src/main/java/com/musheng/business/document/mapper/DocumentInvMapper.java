package com.musheng.business.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.musheng.business.document.entity.DocumentInv;
import org.apache.ibatis.annotations.Mapper;

/**
 * INV发票主表 Mapper 接口
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Mapper
public interface DocumentInvMapper extends BaseMapper<DocumentInv> {
}
