package com.musheng.tiktok.document.controller;

import com.musheng.common.result.Result;
import com.musheng.tiktok.document.entity.TiktokPartyConfig;
import com.musheng.tiktok.document.service.TiktokPartyConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * TK交易方配置接口
 *
 * @author wanhua
 * 21:12 2026年05月14日
 */
@RestController
@RequestMapping("/v1/tiktok/party-config")
@Tag(name = "TK交易方配置")
@Slf4j
@CrossOrigin
public class TiktokPartyConfigController {

    @Autowired
    private TiktokPartyConfigService partyConfigService;

    @Operation(summary = "查询所有站点配置")
    @GetMapping("/list")
    public Result<List<TiktokPartyConfig>> list() {
        return Result.success(partyConfigService.listAll());
    }

    @Operation(summary = "按站点查询配置")
    @GetMapping("/{siteCode}")
    public Result<TiktokPartyConfig> getBySiteCode(@PathVariable String siteCode) {
        return Result.success(partyConfigService.getBySiteCode(siteCode));
    }

    @Operation(summary = "保存或更新配置")
    @PostMapping
    public Result<TiktokPartyConfig> save(@RequestBody TiktokPartyConfig config) {
        return Result.success(partyConfigService.saveOrUpdate(config));
    }

    @Operation(summary = "删除配置")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        partyConfigService.delete(id);
        return Result.success();
    }
}
