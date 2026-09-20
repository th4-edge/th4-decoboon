package com.th4.edge.decoboon.customer.context;

import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.configure.CustomerGradeLevel;
import com.th4.edge.decoboon.customer.configure.PortraitStartWindow;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerGrade;
import com.th4.edge.decoboon.customer.model.CustomerPortrait;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Setting;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 客户分级计算上下文。
 *
 * <p>分级是派生值，不落库、不接受前端写入。依据冻结规则：
 * 总分 = 需求明确(0-30) + 预算匹配(0-25) + 时间紧迫(0-20) + 互动深度(0-15)
 * + 跟进时效(0-10) - 负向扣分；A ≥ 75、B 50–74、C &lt; 50，已流失客户恒为 C。
 * 输出必须可解释，每个维度带 weight/score/reason。</p>
 */
@Note("客户分级计算上下文")
@Component
public class CustomerGradeContext {

    /** 冻结默认值（可由 Company.setting.gradeConfig 覆盖）。 */
    public static final BigDecimal DEFAULT_HIGH_BUDGET = new BigDecimal("200000");
    public static final BigDecimal DEFAULT_STANDARD_BUDGET = new BigDecimal("100000");
    public static final int DEFAULT_A_THRESHOLD = 75;
    public static final int DEFAULT_B_THRESHOLD = 50;
    public static final int DEFAULT_W_DEMAND = 30;
    public static final int DEFAULT_W_BUDGET = 25;
    public static final int DEFAULT_W_URGENCY = 20;
    public static final int DEFAULT_W_ENGAGEMENT = 15;
    public static final int DEFAULT_W_RECENCY = 10;

    /** 画像关键项数量（需求明确维度）。 */
    private static final int DEMAND_KEY_FIELDS = 5;

    /**
     * 计算客户分级。
     *
     * @param customer     客户主档
     * @param portrait     客户画像，可为空
     * @param hasMeasure   是否存在有效量房事实
     * @param followCount  累计跟进次数
     * @param lastFollowAt 最近跟进时间，可为空
     * @param tagKeys      命中标签 key 集合（用于负向扣分），可为空
     */
    public CustomerGrade compute(Customer customer,
                                 CustomerPortrait portrait,
                                 boolean hasMeasure,
                                 int followCount,
                                 LocalDateTime lastFollowAt,
                                 Set<String> tagKeys) {
        Config config = resolveConfig(customer == null ? null : customer.getCompany());
        List<CustomerGrade.Item> breakdown = new ArrayList<>();

        if (customer != null && customer.getStatus() == CustStatus.Lost) {
            breakdown.add(new CustomerGrade.Item("stage", "客户阶段", 0, 0, "客户已流失，直接判为 C"));
            return new CustomerGrade(CustomerGradeLevel.C, 0, LocalDateTime.now(), breakdown);
        }

        int score = 0;

        // 1. 需求明确（0-30）
        int demand = 0;
        StringBuilder demandReason = new StringBuilder();
        int portraitFilled = 0;
        if (portrait != null) {
            if (portrait.getPurpose() != null) portraitFilled++;
            if (portrait.getHouseCondition() != null) portraitFilled++;
            if (portrait.getStartWindow() != null) portraitFilled++;
            if (portrait.getStyle() != null) portraitFilled++;
            if (portrait.getDecisionRole() != null) portraitFilled++;
        }
        demand += Math.min(portraitFilled * 6, config.wDemand);
        if (hasMeasure) {
            demand = Math.min(demand + 12, config.wDemand);
            demandReason.append("已完成量房；");
        }
        demandReason.append("画像完成 ").append(portraitFilled).append("/").append(DEMAND_KEY_FIELDS).append(" 项");
        breakdown.add(new CustomerGrade.Item("demand", "需求明确", config.wDemand, demand, demandReason.toString()));
        score += demand;

        // 2. 预算匹配（0-25）
        int budgetScore;
        String budgetReason;
        BigDecimal budget = customer == null ? null : customer.getBudget();
        if (budget == null) {
            budgetScore = 0;
            budgetReason = "未填写装修预算";
        } else if (budget.compareTo(config.highBudget) >= 0) {
            budgetScore = config.wBudget;
            budgetReason = "预算 " + budget.stripTrailingZeros().toPlainString() + "，属于高预算区间";
        } else if (budget.compareTo(config.standardBudget) >= 0) {
            budgetScore = Math.round(config.wBudget * 0.72f);
            budgetReason = "预算 " + budget.stripTrailingZeros().toPlainString() + "，属于达标区间";
        } else {
            budgetScore = Math.round(config.wBudget * 0.4f);
            budgetReason = "预算 " + budget.stripTrailingZeros().toPlainString() + "，低于达标区间";
        }
        breakdown.add(new CustomerGrade.Item("budget", "预算匹配", config.wBudget, budgetScore, budgetReason));
        score += budgetScore;

        // 3. 时间紧迫（0-20）
        PortraitStartWindow startWindow = portrait == null ? null : portrait.getStartWindow();
        int urgencyScore;
        String urgencyReason;
        if (startWindow == null) {
            urgencyScore = 0;
            urgencyReason = "未填写计划开工时间";
        } else {
            switch (startWindow) {
                case WithinOneMonth -> { urgencyScore = config.wUrgency; urgencyReason = "计划 1 个月内开工"; }
                case OneToThreeMonths -> { urgencyScore = Math.round(config.wUrgency * 0.7f); urgencyReason = "计划 1-3 个月内开工"; }
                case ThreeToSixMonths -> { urgencyScore = Math.round(config.wUrgency * 0.4f); urgencyReason = "计划 3-6 个月内开工"; }
                case HalfYearPlus -> { urgencyScore = Math.round(config.wUrgency * 0.15f); urgencyReason = "计划半年以上开工"; }
                default -> { urgencyScore = 0; urgencyReason = "计划开工时间待定"; }
            }
        }
        breakdown.add(new CustomerGrade.Item("urgency", "时间紧迫", config.wUrgency, urgencyScore, urgencyReason));
        score += urgencyScore;

        // 4. 互动深度（0-15）
        int visits = customer == null || customer.getVisitCount() == null ? 0 : customer.getVisitCount();
        int visitScore = Math.min(visits * 2, 6);
        int wechatScore = customer != null && Boolean.TRUE.equals(customer.getWechatAdded()) ? 3 : 0;
        int measureScore = hasMeasure ? 6 : 0;
        int engagement = Math.min(visitScore + wechatScore + measureScore, config.wEngagement);
        breakdown.add(new CustomerGrade.Item("engagement", "互动深度", config.wEngagement, engagement,
                "到店 " + visits + " 次；" + (wechatScore > 0 ? "已加微信；" : "未加微信；") + (hasMeasure ? "已完成量房" : "未量房")));
        score += engagement;

        // 5. 跟进时效（0-10）
        int recencyScore;
        String recencyReason;
        if (lastFollowAt == null) {
            recencyScore = 0;
            recencyReason = "尚无跟进记录";
        } else {
            long days = Math.max(0, ChronoUnit.DAYS.between(lastFollowAt.toLocalDate(), LocalDateTime.now().toLocalDate()));
            if (days <= 3) { recencyScore = config.wRecency; }
            else if (days <= 7) { recencyScore = Math.round(config.wRecency * 0.6f); }
            else if (days <= 15) { recencyScore = Math.round(config.wRecency * 0.2f); }
            else { recencyScore = 0; }
            recencyReason = "最近跟进 " + days + " 天前";
        }
        if (followCount > 0) {
            recencyReason = recencyReason + "；累计跟进 " + followCount + " 次";
        }
        breakdown.add(new CustomerGrade.Item("recency", "跟进时效", config.wRecency, recencyScore, recencyReason));
        score += recencyScore;

        // 6. 负向扣分
        int penalty = 0;
        List<String> penaltyReasons = new ArrayList<>();
        if (tagKeys != null) {
            if (tagKeys.contains("risk:budgetUnreal")) { penalty += 10; penaltyReasons.add("预算不实"); }
            if (tagKeys.contains("risk:competitor")) { penalty += 10; penaltyReasons.add("同行/竞品"); }
            if (tagKeys.contains("risk:noShow")) { penalty += 10; penaltyReasons.add("爽约"); }
            if (tagKeys.contains("risk:followOverdue")) { penalty += 5; penaltyReasons.add("跟进逾期"); }
        }
        if (penalty > 0) {
            breakdown.add(new CustomerGrade.Item("penalty", "负向扣分", 0, -penalty, "命中：" + String.join("、", penaltyReasons)));
            score -= penalty;
        }

        int normalized = Math.max(0, Math.min(100, score));
        CustomerGradeLevel level = normalized >= config.aThreshold
                ? CustomerGradeLevel.A
                : normalized >= config.bThreshold ? CustomerGradeLevel.B : CustomerGradeLevel.C;
        return new CustomerGrade(level, normalized, LocalDateTime.now(), breakdown);
    }

    /** 解析企业配置，缺省时使用冻结默认值。 */
    private Config resolveConfig(Company company) {
        Config config = new Config();
        if (company == null) return config;
        Setting setting = company.getSetting();
        if (setting == null || setting.getGradeConfig() == null || setting.getGradeConfig().isBlank()) {
            return config;
        }
        for (String segment : setting.getGradeConfig().split(";")) {
            String[] pair = segment.split("=", 2);
            if (pair.length != 2) continue;
            String key = pair[0].trim();
            String value = pair[1].trim();
            if (value.isEmpty()) continue;
            try {
                switch (key) {
                    case "high" -> config.highBudget = new BigDecimal(value);
                    case "standard" -> config.standardBudget = new BigDecimal(value);
                    case "a" -> config.aThreshold = Integer.parseInt(value);
                    case "b" -> config.bThreshold = Integer.parseInt(value);
                    case "wDemand" -> config.wDemand = Integer.parseInt(value);
                    case "wBudget" -> config.wBudget = Integer.parseInt(value);
                    case "wUrgency" -> config.wUrgency = Integer.parseInt(value);
                    case "wEngagement" -> config.wEngagement = Integer.parseInt(value);
                    case "wRecency" -> config.wRecency = Integer.parseInt(value);
                    default -> { }
                }
            } catch (RuntimeException ignored) {
                // 单项配置非法时忽略该项，保留默认值，不阻断主链
            }
        }
        return config;
    }

    /** 分级配置。 */
    private static final class Config {
        private BigDecimal highBudget = DEFAULT_HIGH_BUDGET;
        private BigDecimal standardBudget = DEFAULT_STANDARD_BUDGET;
        private int aThreshold = DEFAULT_A_THRESHOLD;
        private int bThreshold = DEFAULT_B_THRESHOLD;
        private int wDemand = DEFAULT_W_DEMAND;
        private int wBudget = DEFAULT_W_BUDGET;
        private int wUrgency = DEFAULT_W_URGENCY;
        private int wEngagement = DEFAULT_W_ENGAGEMENT;
        private int wRecency = DEFAULT_W_RECENCY;
    }
}
