package com.th4.edge.decoboon.material.context;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 平台行业标准主材下单单据基线（L1，只读、版本化）。
 *
 * <p>依据真实装企在用单据抽象（EV-314：麦鸿装饰 20 张品类子单据 → 17 个品类基线）。
 * 基线 = 标准字段集 + 标准行骨架 + 标准单位与换算 + 标准默认值 + 签名位；
 * 企业复制后成为自己的模板（L2），在受控白名单内个性化。</p>
 *
 * <p>P0 只提供 {@code Tile}（瓷砖）一类基线（T12-BIZ-20261015-003）；
 * 其余 16 类按 BR-044 §二 的清单在 P1 铺开。基线不落企业表，企业复制后保存为自己的模板版本。</p>
 */
@Component
public class MaterialSheetBaseline {

    public static final int VERSION = 1;

    /** 单据类型编码：瓷砖。 */
    public static final String TYPE_TILE = "Tile";

    /** 瓷砖标准行骨架：8 个安装位置（真实单据原文）。 */
    private static final String TILE_ROWS = """
            ["客厅地砖","厨房地砖","阳台地砖","厨房墙砖","阳台墙砖","卫生间地砖","卫生间墙砖（干）","卫生间墙砖（湿）"]
            """.trim();

    /** 瓷砖字段配置：单据头 + 行字段（受控白名单，企业可改显隐/必填/顺序/别名）。 */
    private static final String TILE_FIELDS = """
            {"version":1,
             "header":[
               {"key":"manufacturerName","label":"厂家","visible":true,"required":false,"order":1},
               {"key":"measureAt","label":"测量时间","visible":true,"required":true,"order":2},
               {"key":"deliverAt","label":"配送时间","visible":true,"required":false,"order":3},
               {"key":"installAt","label":"安装/送货时间","visible":true,"required":false,"order":4},
               {"key":"remark","label":"备注","visible":true,"required":false,"order":5}],
             "line":[
               {"key":"locationName","label":"安装位置","visible":true,"required":true,"order":1},
               {"key":"brand","label":"品牌","visible":true,"required":false,"order":2},
               {"key":"model","label":"型号","visible":true,"required":false,"order":3},
               {"key":"spec","label":"规格","visible":true,"required":false,"order":4},
               {"key":"color","label":"颜色/花色","visible":true,"required":false,"order":5},
               {"key":"size","label":"尺寸","visible":true,"required":false,"order":6},
               {"key":"quantity","label":"数量","visible":true,"required":true,"order":7},
               {"key":"unit","label":"单位","visible":true,"required":true,"order":8},
               {"key":"standardIncluded","label":"标配含","visible":true,"required":false,"order":9},
               {"key":"craft","label":"工艺","visible":false,"required":false,"order":10},
               {"key":"glass","label":"玻璃","visible":false,"required":false,"order":11},
               {"key":"remark","label":"备注","visible":true,"required":false,"order":12}]}
            """.trim();

    /** 瓷砖默认规则：规格→单片面积换算（"实际瓷砖面积含退补货"）、默认规格、默认单位。 */
    private static final String TILE_RULES = """
            {"realAreaIncludingReturn":true,
             "lossRequired":true,
             "defaultUnit":"Sheet",
             "unitConversions":{"800*800":0.64,"400*800":0.32,"400*400":0.16,"300*600":0.18,
                                "300*300":0.09,"750*1500":1.125,"600*1200":0.72},
             "backfill":{"enabled":true,"note":"补货内容/地址/联系人/到货时间"},
             "returnGoods":{"enabled":true,"note":"退货内容/地址/联系人/退货时间"}}
            """.trim();

    /** 默认签名位：项目经理测量与安装时间确认、设计师确认（DEC-MP-07 当场确认不作强制双签）。 */
    private static final String DEFAULT_SIGNATURES = """
            {"pm":{"label":"项目经理确认","required":true},"designer":{"label":"设计师确认","required":false}}
            """.trim();

    private static final String DEFAULT_NUMBER_RULE = "{type}-{yyyyMMdd}-{seq}";

    /** 平台基线预设。 */
    public record Preset(String code,
                         String name,
                         String fieldConfig,
                         String rowTemplateConfig,
                         String ruleConfig,
                         String signatureConfig,
                         String numberRule) {
    }

    /** 全部平台基线：P0 仅瓷砖；其余 16 类在 P1 追加（BR-044 §二）。 */
    public List<Preset> presets() {
        return List.of(tile());
    }

    /** 瓷砖基线。 */
    public Preset tile() {
        return new Preset(TYPE_TILE, "瓷砖（地砖/墙砖）", TILE_FIELDS, TILE_ROWS, TILE_RULES,
                DEFAULT_SIGNATURES, DEFAULT_NUMBER_RULE);
    }

    /** 按编码取基线；未实现的品类返回空（P1 铺开）。 */
    public Preset find(String code) {
        return presets().stream()
                .filter(preset -> preset.code().equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }
}
