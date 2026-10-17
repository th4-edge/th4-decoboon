package com.th4.edge.decoboon.material.assistant;

import org.springframework.stereotype.Component;

import java.util.Locale;

/** 材料问题意图识别：规则基线、可解释、无模型调用。 */
@Component
public class MaterialAssistantIntentResolver {

    public MaterialAssistantIntent resolve(String rawQuestion) {
        String question = normalize(rawQuestion);
        if (question.isBlank()) return MaterialAssistantIntent.UNKNOWN;

        if (containsAny(question, "自动审批", "自动付款", "自动删除", "改权限", "修改权限",
                "帮我审批", "帮我付款", "删除数据", "写代码", "天气", "股票", "权限改成", "改成财务")) {
            return MaterialAssistantIntent.OUT_OF_SCOPE;
        }
        if (containsAny(question, "权限", "越权", "跨企业", "跨租户", "跨项目", "数据范围",
                "谁能查看", "谁能看", "能看到", "看不到")) {
            return MaterialAssistantIntent.PERMISSION;
        }
        boolean sheetLike = question.contains("作业单") || question.contains("请购单") || question.contains("单据");
        if (sheetLike && question.contains("采购单")
                || question.contains("两张单") || question.contains("单据边界")
                || (sheetLike && containsAny(question, "价格", "账期", "对厂家", "对财务"))) {
            return MaterialAssistantIntent.SHEET_VS_PURCHASE;
        }
        if (containsAny(question, "字段没填", "没填", "必填", "不能提交", "不能编辑",
                "不能取消", "直接取消", "已锁定", "锁定", "下一步", "卡住", "无法继续")
                || (sheetLike && containsAny(question, "状态", "提交请购", "请购", "取消", "编辑"))) {
            return MaterialAssistantIntent.SHEET_STATUS;
        }
        if (containsAny(question, "字段", "是什么意思", "代表什么", "什么含义", "字段含义")
                && !containsAny(question, "没填", "必填", "提交", "测量", "配送", "安装", "到场",
                "价格", "成本", "利润", "结算", "暂估", "应付", "未定价", "无价")) {
            return MaterialAssistantIntent.FIELD_DICTIONARY;
        }
        if (containsAny(question, "材料体检", "材料闭环", "缺什么", "材料风险")) {
            return MaterialAssistantIntent.PROJECT_HEALTH;
        }
        if (containsAny(question, "测量", "配送", "安装", "拉群", "复尺", "日期", "时间",
                "要求到场", "实际到场", "建单时间", "下单时间")) {
            return MaterialAssistantIntent.TIME_SEMANTICS;
        }
        if (containsAny(question, "选材", "已选材", "未选材", "选材确认", "交付财务", "选择主材", "进入采购", "可采购", "采购条件")) {
            return MaterialAssistantIntent.SELECTION_GATE;
        }
        if (containsAny(question, "增减项", "减项", "增项", "退货", "补货", "冲减", "换材料", "换材")) {
            return MaterialAssistantIntent.CHANGE;
        }
        if (containsAny(question, "到场确认", "到货", "收货", "到场")
                && !containsAny(question, "未定价", "无价", "成本", "利润", "结算价")) {
            return MaterialAssistantIntent.RECEIPT;
        }
        if (containsAny(question, "结算撤销", "月结", "逐单", "重复结算", "结算")
                && !containsAny(question, "结算价")) {
            return MaterialAssistantIntent.SETTLEMENT;
        }
        if (containsAny(question, "供应商", "经销商", "合作方", "采购供应商", "厂家")
                && !containsAny(question, "结算价", "成本", "利润", "未定价", "暂估")) {
            return MaterialAssistantIntent.SUPPLIER;
        }
        if (containsAny(question, "结算价", "成本", "利润", "毛利", "毛利率", "未定价", "无价", "暂估",
                "P1", "P2", "P3", "价格", "应付", "实际成本", "预计成本")) {
            return MaterialAssistantIntent.PRICE_COST;
        }
        if (containsAny(question, "结算", "月结", "逐单", "重复结算", "撤销")) {
            return MaterialAssistantIntent.SETTLEMENT;
        }
        if (containsAny(question, "到场", "到货", "收到", "收货", "部分到场", "尾量", "验收", "账期")) {
            return MaterialAssistantIntent.RECEIPT;
        }
        if (containsAny(question, "供应商", "经销商", "厂家", "合作方", "采购供应商")) {
            return MaterialAssistantIntent.SUPPLIER;
        }
        if (containsAny(question, "材料体检", "项目材料", "材料进度", "材料闭环", "缺什么",
                "材料风险", "项目.*下一步", "项目.*材料")) {
            return MaterialAssistantIntent.PROJECT_HEALTH;
        }
        return MaterialAssistantIntent.UNKNOWN;
    }

    private boolean containsAny(String question, String... keywords) {
        for (String keyword : keywords) {
            if (keyword.contains(".*")) {
                if (question.matches(".*" + keyword + ".*")) return true;
            } else if (question.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }
}
