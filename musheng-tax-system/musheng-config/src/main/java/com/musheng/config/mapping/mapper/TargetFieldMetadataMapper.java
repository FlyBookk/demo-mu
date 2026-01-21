package com.musheng.config.mapping.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.musheng.config.mapping.entity.TargetFieldMetadata;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 目标字段元数据 Mapper
 */
@Mapper
public interface TargetFieldMetadataMapper extends BaseMapper<TargetFieldMetadata> {

    /**
     * 根据数据类型查询目标字段
     *
     * @param dataType   数据类型
     * @param sourceType 数据源类型（可选）
     * @return 字段列表
     */
    @Select({
        "<script>",
        "SELECT * FROM t_target_field_metadata",
        "WHERE data_type = #{dataType}",
        "<if test='sourceType != null'>",
        "  AND sub_type = #{sourceType}",
        "</if>",
        "<if test='sourceType == null'>",
        "  AND sub_type IS NULL",
        "</if>",
        "ORDER BY sort_order ASC",
        "</script>"
    })
    List<TargetFieldMetadata> selectByDataType(@Param("dataType") String dataType, 
                                                @Param("sourceType") String sourceType);
}
