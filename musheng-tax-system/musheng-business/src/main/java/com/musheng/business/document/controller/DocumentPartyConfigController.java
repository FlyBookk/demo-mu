package com.musheng.business.document.controller;

import com.musheng.business.document.dto.DocumentPartyConfigCopyDTO;
import com.musheng.business.document.dto.DocumentPartyConfigDTO;
import com.musheng.business.document.entity.DocumentPartyConfig;
import com.musheng.business.document.service.DocumentPartyConfigService;
import com.musheng.business.document.vo.DocumentPartyConfigVO;
import com.musheng.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * FBA单据交易方配置 Controller
 *
 * <p>提供交易方配置的增删改查及复制接口，按站点（siteCode）进行配置管理。</p>
 *
 * @author wanhua
 * 10:30 2026年03月22日
 */
@RestController
@RequestMapping("/v1/business/document-party-config")
@Tag(name = "FBA单据交易方配置")
@Slf4j
@CrossOrigin
public class DocumentPartyConfigController {

    @Autowired
    private DocumentPartyConfigService documentPartyConfigService;

    /**
     * 查询所有交易方配置列表
     *
     * @return 配置列表
     * @author wanhua
     * 10:30 2026年03月22日
     */
    @GetMapping("/list")
    @Operation(summary = "查询交易方配置列表")
    public Result<List<DocumentPartyConfigVO>> list() {
        return Result.success(documentPartyConfigService.list());
    }

    /**
     * 新增交易方配置
     *
     * @param dto 配置请求参数
     * @return 操作结果
     * @author wanhua
     * 10:30 2026年03月22日
     */
    @PostMapping("/add")
    @Operation(summary = "新增交易方配置")
    public Result<Void> add(@Valid @RequestBody DocumentPartyConfigDTO dto) {
        documentPartyConfigService.add(dto);
        return Result.success();
    }

    /**
     * 修改交易方配置
     *
     * @param dto 配置请求参数（id必填）
     * @return 操作结果
     * @author wanhua
     * 10:30 2026年03月22日
     */
    @PutMapping("/update")
    @Operation(summary = "修改交易方配置")
    public Result<Void> update(@Valid @RequestBody DocumentPartyConfigDTO dto) {
        documentPartyConfigService.update(dto);
        return Result.success();
    }

    /**
     * 删除交易方配置
     *
     * @param id 配置ID
     * @return 操作结果
     * @author wanhua
     * 10:30 2026年03月22日
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除交易方配置")
    public Result<Void> delete(@PathVariable Long id) {
        documentPartyConfigService.delete(id);
        return Result.success();
    }

    /**
     * 按站点代码查询交易方配置
     *
     * @param siteCode 站点代码（US/CA/UK/EU）
     * @return 配置实体
     * @author wanhua
     * 10:30 2026年03月22日
     */
    @GetMapping("/{siteCode}")
    @Operation(summary = "按站点代码查询交易方配置")
    public Result<DocumentPartyConfig> getBySiteCode(@PathVariable String siteCode) {
        return Result.success(documentPartyConfigService.getBySiteCode(siteCode));
    }

    /**
     * 复制配置到目标站点（目标站点已存在则覆盖，不存在则新增）
     *
     * @param dto 复制请求参数（来源ID + 目标站点代码）
     * @return 操作结果
     * @author wanhua
     * 10:30 2026年03月22日
     */
    @PostMapping("/copy")
    @Operation(summary = "复制配置到目标站点")
    public Result<Void> copy(@Valid @RequestBody DocumentPartyConfigCopyDTO dto) {
        documentPartyConfigService.copy(dto.getSourceId(), dto.getTargetSiteCode());
        return Result.success();
    }
}
