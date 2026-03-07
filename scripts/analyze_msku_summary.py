#!/usr/bin/env python3
"""精简汇总：MSKU单价分配规律的最终结论"""
import json
from collections import defaultdict

with open("scripts/settlement_analysis.json", "r", encoding="utf-8") as f:
    results = json.load(f)

# 统计同站点同周期内单价是否相同
same_count = 0
diff_count = 0
for r in results:
    prices = set(round(item["unit_price"], 4) for item in r["items"])
    if len(prices) > 1:
        diff_count += 1
    else:
        same_count += 1

print(f"总结算单数: {len(results)}")
print(f"单价相同: {same_count}")
print(f"单价不同: {diff_count}")
print()

# 按产品型号分组，看型号对单价的影响
print("=" * 60)
print("按产品型号(第2段)分组的平均单价")
print("=" * 60)
model_data = defaultdict(list)
for r in results:
    for item in r["items"]:
        parts = item["msku"].split("-")
        model = parts[1] if len(parts) > 1 else "?"
        model_data[model].append(item["unit_price"])

for model in sorted(model_data.keys()):
    prices = model_data[model]
    avg = sum(prices) / len(prices)
    print(f"  {model:8s}: 平均={avg:6.2f}, 范围=[{min(prices):.2f}, {max(prices):.2f}], 记录数={len(prices)}")

# 按变体编号(第3段)分组
print()
print("=" * 60)
print("按变体编号(第3段)分组的平均单价")
print("=" * 60)
variant_data = defaultdict(list)
for r in results:
    for item in r["items"]:
        parts = item["msku"].split("-")
        variant = parts[2] if len(parts) > 2 else "?"
        # 简化分类：提取变体的"基础类型"
        if variant.startswith("20") and len(variant) > 2:
            base = "20x系列"
        elif variant.startswith("2") and len(variant) > 1 and variant[1:].isdigit() and int(variant) >= 20:
            base = "2x大号系列"
        elif variant in ("2", "5", "6", "7", "0", "1", "3", "8"):
            base = f"基础变体({variant})"
        else:
            base = f"其他({variant})"
        variant_data[base].append(item["unit_price"])

for base in sorted(variant_data.keys()):
    prices = variant_data[base]
    avg = sum(prices) / len(prices)
    print(f"  {base:20s}: 平均={avg:6.2f}, 范围=[{min(prices):.2f}, {max(prices):.2f}], 记录数={len(prices)}")

# 关键发现：同一站点同一周期内，按变体类型分组看单价差异
print()
print("=" * 60)
print("关键发现：变体编号长度与单价的关系")
print("=" * 60)
short_variant = []  # 1位变体 (2,5,6,7等)
long_variant = []   # 2位以上变体 (20,25,26,206,209等)
for r in results:
    for item in r["items"]:
        parts = item["msku"].split("-")
        variant = parts[2] if len(parts) > 2 else ""
        if len(variant) == 1:
            short_variant.append(item["unit_price"])
        elif len(variant) >= 2:
            long_variant.append(item["unit_price"])

if short_variant:
    print(f"  短变体(1位): 平均={sum(short_variant)/len(short_variant):.2f}, 记录数={len(short_variant)}")
if long_variant:
    print(f"  长变体(2位+): 平均={sum(long_variant)/len(long_variant):.2f}, 记录数={len(long_variant)}")
if short_variant and long_variant:
    ratio = (sum(long_variant)/len(long_variant)) / (sum(short_variant)/len(short_variant))
    print(f"  长/短比例: {ratio:.2f}")

# 同一站点同一周期内的单价比例分析
print()
print("=" * 60)
print("同站点同周期内：短变体 vs 长变体的单价比例")
print("=" * 60)
ratios = []
for r in results:
    short_prices = []
    long_prices = []
    for item in r["items"]:
        parts = item["msku"].split("-")
        variant = parts[2] if len(parts) > 2 else ""
        if len(variant) == 1:
            short_prices.append(item["unit_price"])
        elif len(variant) >= 2:
            long_prices.append(item["unit_price"])
    if short_prices and long_prices:
        avg_short = sum(short_prices) / len(short_prices)
        avg_long = sum(long_prices) / len(long_prices)
        if avg_short > 0:
            ratio = avg_long / avg_short
            ratios.append(ratio)

if ratios:
    print(f"  周期数: {len(ratios)}")
    print(f"  平均比例: {sum(ratios)/len(ratios):.2f}")
    print(f"  比例范围: [{min(ratios):.2f}, {max(ratios):.2f}]")
    print(f"  中位数: {sorted(ratios)[len(ratios)//2]:.2f}")

# 最终结论
print()
print("=" * 60)
print("最终结论")
print("=" * 60)
print(f"""
108份结算单分析结果：
- {diff_count}份结算单中同站点同周期内MSKU单价不同（占{diff_count*100/len(results):.0f}%）
- {same_count}份结算单中同站点同周期内MSKU单价相同（占{same_count*100/len(results):.0f}%）

单价差异的核心规律：
- 变体编号长度决定了单价档位
- 短变体(1位数: 2,5,6,7等) → 低单价档
- 长变体(2位+: 20,25,26,206,209,268等) → 高单价档
- 长变体单价约为短变体的 {sum(ratios)/len(ratios):.1f} 倍

这说明：
1. 单价分摊不是简单的等比分摊（同一站点同一周期内MSKU单价不同）
2. 变体编号反映了产品规格/尺寸，不同规格的产品成本不同
3. 财务在推导时会根据MSKU的变体特征手动调整单价

建议方案：
- 系统先按等比分摊计算初始单价
- 展示可编辑列表，财务可手动调整各MSKU单价
- 调整后自动重算金额
- 这与当前需求文档中的需求5已经一致
""")
