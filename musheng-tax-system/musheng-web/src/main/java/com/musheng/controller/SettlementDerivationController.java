package com.musheng.controller;

import com.musheng.business.settlement.derivation.dto.DerivationRequest;
import com.musheng.business.settlement.derivation.service.SettlementDerivationService;
import com.musheng.business.settlement.derivation.vo.DerivationResultVO;
import com.musheng.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 结算数据推导控制器（按季度）
 *
 * <p>交互流程：选站点 → 选季度 → 输入采购成本 → 推导计算（自动删旧+写入）</p>
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@RestController
@RequestMapping("/v1/settlement-derivation")
@Tag(name = "结算数据推导")
@Slf4j
@CrossOrigin
public class SettlementDerivationController {

    @Autowired
    private SettlementDerivationService settlementDerivationService;

    /**
     * 执行推导计算并写入数据库（自动删除旧数据后插入新数据）
     *
     * @param request 推导请求，包含季度范围和各站点采购成本
     * @return 推导结果
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @PostMapping("/derive")
    @Operation(summary = "执行推导计算并写入（按季度）")
    public Result<DerivationResultVO> derive(@RequestBody DerivationRequest request) {
        try {
            log.info("开始推导计算，季度: {} ~ {}, 站点数: {}",
                    request.getStartQuarter(), request.getEndQuarter(),
                    request.getSiteCosts() != null ? request.getSiteCosts().size() : 0);
            DerivationResultVO result = settlementDerivationService.derive(request);
            return Result.success(result);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("推导计算参数异常: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("推导计算失败", e);
            return Result.error(500, "操作失败，请重试");
        }
    }

    /**
     * 检查指定季度范围是否已有推导数据
     *
     * @param startQuarter 开始季度，如 2025-Q3
     * @param endQuarter 结束季度，如 2025-Q3
     * @return 存在返回 true，否则返回 false
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @GetMapping("/check-existing")
    @Operation(summary = "检查指定季度是否已有推导数据")
    public Result<Boolean> checkExistingData(
            @Parameter(description = "开始季度，如 2025-Q3")
            @RequestParam String startQuarter,
            @Parameter(description = "结束季度，如 2025-Q3")
            @RequestParam String endQuarter) {
        try {
            log.info("检查已有推导数据，季度: {} ~ {}", startQuarter, endQuarter);
            boolean exists = settlementDerivationService.checkExistingData(startQuarter, endQuarter);
            return Result.success(exists);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("检查已有数据参数异常: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("检查已有数据失败", e);
            return Result.error(500, "操作失败，请重试");
        }
    }
}
