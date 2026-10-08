package com.th4.edge.decoboon.customer.context;

import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.configure.CustomerRiskLevel;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.quote.model.Quote;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 客户经营派生投影的唯一实现。
 *
 * <p>风险等级、风险原因、下一步动作、停留天数与最近金额的判定只能有一份：
 * 企业负责人客户经营（{@link CustomerOverviewContext}）与设计师「我的客户」
 * 必须走同一套规则，否则同一客户在不同角色视角会出现两种风险与主行动口径
 * （依据 DEC-002 六条冻结映射、BR-005）。</p>
 */
@Component
public class CustomerInsightContext {

    /** 组装只读投影：停留天数、最近报价/签约金额与项目、风险与下一步动作。 */
    public void enrich(Customer customer, LocalDate today, Quote quote, ProjectContract contract) {
        LocalDateTime lastActivity = latest(customer.getLastFollowAt(), customer.getLastVisitAt(),
                customer.getMeasureAt(), customer.getHandedOffAt(), customer.getRestoredAt(), created(customer));
        LocalDate lastDate = lastActivity == null ? null : lastActivity.toLocalDate();
        customer.setIdleDays(lastDate == null ? 0 : (int) Math.max(0, ChronoUnit.DAYS.between(lastDate, today)));
        if (quote != null) {
            customer.setQuoteAmount(value(quote.getAmount()));
        }
        if (contract != null) {
            BigDecimal signed = value(contract.getSignedAmount());
            customer.setSignedAmount(signed.signum() == 0 ? value(contract.getCurrentAmount()) : signed);
            if (contract.getProject() != null) {
                customer.setProjectId(contract.getProject().getProjectId());
                customer.setProjectName(contract.getProject().getName());
            }
        }
        riskAndAction(customer, customer.getIdleDays() == null ? 0 : customer.getIdleDays(), today);
    }

    /** 风险等级 / 风险原因 / 下一步动作。 */
    public void riskAndAction(Customer customer, int idleDays, LocalDate today) {
        CustomerRiskLevel level = CustomerRiskLevel.Normal;
        String reason = null;
        String action = defaultAction(customer);
        if (customer.getStatus() == CustStatus.Dormant) {
            reason = "客户已休眠，暂不进入风险跟进";
            action = "恢复跟进";
        } else if (customer.getStatus() == CustStatus.Lost) {
            reason = "客户已流失";
            action = "查看流失原因";
        } else if (customer.getOwner() == null) {
            level = CustomerRiskLevel.High;
            reason = "未分配负责人";
            action = "先分配负责人";
        } else if (overdue(customer, today)) {
            level = CustomerRiskLevel.High;
            long days = ChronoUnit.DAYS.between(customer.getNextFollowAt().toLocalDate(), today);
            reason = "跟进已逾期 " + days + " 天";
            action = "立即处理逾期跟进";
        } else if (customer.getStatus() == CustStatus.PendingMeasure && customer.getDesigner() == null) {
            level = customer.getMeasureAt() != null && customer.getMeasureAt().toLocalDate().isBefore(today)
                    ? CustomerRiskLevel.High : CustomerRiskLevel.Attention;
            reason = customer.getMeasureAt() != null && customer.getMeasureAt().toLocalDate().isBefore(today)
                    ? "量房时间已过但未指派设计师" : "待量房但未指派设计师";
            action = "指定设计师并确认量房安排";
        } else if (idleDays >= 15 && openStatus(customer.getStatus())) {
            level = CustomerRiskLevel.High;
            reason = "连续 " + idleDays + " 天未推进";
            action = "重新激活客户或评估流失";
        } else if (idleDays >= 7 && openStatus(customer.getStatus())) {
            level = CustomerRiskLevel.Attention;
            reason = "已 " + idleDays + " 天没有新的推进";
            action = "安排下一次跟进";
        }
        customer.setRiskLevel(level);
        customer.setRiskReason(reason);
        customer.setNextAction(action);
    }

    /**
     * 客户主链唯一主行动（与风险提示是两件事）。
     *
     * <p>依据 BR-001 客户—房屋—报价—项目主链与 SCN-014：
     * 未预约量房→跟进；已预约量房→录入量房；已量房→方案设计；有报价→跟进报价确认；
     * 已签约→合同交接；施工中→查看工地。前端只按 key 映射落地路由，不再自行推导动作。</p>
     */
    public void mainline(Customer customer, boolean measured, boolean measureScheduled) {
        String key;
        String label;
        String hint;
        switch (customer.getStatus()) {
            case Lost -> {
                key = "closed";
                label = "查看流失原因";
                hint = "客户已流失，仅保留历史查看。";
            }
            case Finished -> {
                key = "closed";
                label = "维护老客户关系";
                hint = "项目已完工，可做回访与转介绍。";
            }
            case Serving -> {
                key = "project";
                label = "查看工地";
                hint = "项目在管，进入工地查看阶段与留证。";
            }
            case Signed -> {
                key = "contract";
                label = "推进合同交接";
                hint = "合同已签，推进生效与项目立项。";
            }
            case Quoted -> {
                boolean hasQuote = customer.getQuoteAmount() != null && customer.getQuoteAmount().signum() > 0;
                key = hasQuote ? "quoteDecision" : "quote";
                label = hasQuote ? "跟进报价确认" : "生成报价";
                hint = hasQuote ? "报价已产生，跟进客户决策与调整。" : "方案已就绪，生成客户可见报价。";
            }
            default -> {
                if (measured) {
                    key = "design";
                    label = "进入方案设计";
                    hint = "量房已完成，进入方案设计并录入客户诉求。";
                } else if (measureScheduled) {
                    key = "recordMeasure";
                    label = "录入量房信息";
                    hint = "量房已预约，按计划完成并登记量房结果。";
                } else if (customer.getStatus() == CustStatus.New) {
                    key = "follow";
                    label = "完成首次联系";
                    hint = "尚未建立联系，先完成首次触达并登记跟进。";
                } else {
                    key = "follow";
                    label = "写跟进 / 约定量房";
                    hint = "尚未预约量房，先保持跟进并推进量房预约。";
                }
            }
        }
        customer.setMainlineKey(key);
        customer.setMainlineLabel(label);
        customer.setMainlineHint(hint);
    }

    /** 阶段默认动作（未命中风险分支时使用）。 */
    public String defaultAction(Customer customer) {
        return switch (customer.getStatus()) {
            case Dormant -> "恢复跟进";
            case Lost -> "查看流失原因";
            case Finished -> "维护老客户关系";
            case Signed -> "推进合同和项目交付";
            case Serving -> "同步项目履约进度";
            case Quoted -> "推进客户确认报价";
            case PendingMeasure -> "按计划完成量房";
            case Following, New -> customer.getLastFollowAt() == null ? "完成首次联系"
                    : customer.getNextFollowAt() == null ? "安排下一次跟进" : "按计划完成下一次跟进";
        };
    }

    /**
     * 跟进逾期是否适用：仅在客户预约量房之前成立（{@link CustStatus#New}/{@link CustStatus#Following}）。
     *
     * <p>客户一旦预约量房（进入待量房及以后阶段），销售跟进目标已达成，
     * 后续由量房/设计/报价主链驱动，不再产生“跟进已逾期”风险、标签、通知与计数。</p>
     */
    public static boolean followOverdueEligible(CustStatus status) {
        return status == CustStatus.New || status == CustStatus.Following;
    }

    /**
     * 跟进逾期是否适用（带预约量房事实兜底）。
     *
     * <p>除状态判定外，客户级任一量房约定时间存在即视为已预约量房，
     * 覆盖历史/异常数据里状态仍停在 New/Following 但已约定量房的情况。</p>
     */
    public static boolean followOverdueEligible(Customer customer) {
        if (customer == null || !followOverdueEligible(customer.getStatus())) return false;
        return !measureBooked(customer);
    }

    /** 客户级是否已写入预约量房约定事实。 */
    public static boolean measureBooked(Customer customer) {
        return customer != null && (customer.getMeasureRequestedAt() != null
                || customer.getPendingMeasureAt() != null
                || customer.getMeasureAt() != null);
    }

    /** 跟进是否已逾期（仅预约量房前的客户参与判定）。 */
    public boolean overdue(Customer customer, LocalDate today) {
        return followOverdueEligible(customer) && customer.getNextFollowAt() != null
                && customer.getNextFollowAt().toLocalDate().isBefore(today);
    }

    /** 是否处于"进行中"：已签约后与终态不参与跟进逾期与沉睡判定。 */
    public static boolean openStatus(CustStatus status) {
        return status != CustStatus.Signed && status != CustStatus.Serving
                && status != CustStatus.Finished && status != CustStatus.Lost
                && status != CustStatus.Dormant;
    }

    public static LocalDateTime latest(LocalDateTime... values) {
        LocalDateTime result = null;
        for (LocalDateTime value : values) {
            if (value != null && (result == null || value.isAfter(result))) {
                result = value;
            }
        }
        return result;
    }

    public static LocalDateTime created(Customer customer) {
        return customer.getCreateTime() == null ? null : customer.getCreateTime().toLocalDateTime();
    }

    public static BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
