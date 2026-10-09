package com.th4.edge.decoboon.statistic.context;

import com.th4.edge.decoboon.statistic.model.DesignerWorkbenchReport;
import com.th4.edge.tenant.user.model.Employee;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 设计师业务工作台聚合上下文。
 *
 * <p>按领域执行固定数量的聚合 SQL，Java 只组装固定大小的结果；不加载客户、报价、
 * 项目、材料计划或验收实体，不允许循环查库。</p>
 *
 * <p>效率约束（DEC-036）：
 * <ul>
 *   <li>项目状态统计按状态一次分组，不在 15 个相关子查询中重复扫描项目。</li>
 *   <li>设计状态统计先按当前设计师范围聚合设计表，避免对每个房屋重复执行 NOT EXISTS。</li>
 *   <li>材料、增减项、验收均按项目域一次聚合，查询条数固定且不随数据量增长。</li>
 * </ul>
 */
@Component
public class DesignerWorkbenchContext {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final List<String> ACTIVE_PROJECT_STATUSES = List.of(
            "Preparing", "Ongoing", "Suspended", "Accepting");
    private static final List<String> TREND_KEYS = List.of(
            "designerMeasureCompletedDailyCount",
            "designerQuoteSubmittedDailyCount",
            "designerQuoteAcceptedDailyCount",
            "designerContractEffectiveDailyCount"
    );

    private final EntityManager entityManager;

    public DesignerWorkbenchContext(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public DesignerWorkbenchReport build(Employee current) {
        Long comId = current.getCompany().getComId();
        Long empId = current.getEmpId();
        LocalDate today = LocalDate.now(ZONE);
        LocalDate monthStartDate = today.withDayOfMonth(1);
        LocalDate monthEndDate = monthStartDate.plusMonths(1);
        LocalDate previousMonthStartDate = monthStartDate.minusMonths(1);
        LocalDate previousMonthEndDate = monthStartDate;
        LocalDate trendStart = today.minusDays(29);
        LocalDate trendEnd = today;
        LocalDateTime now = LocalDateTime.now(ZONE);

        Object[] propertyRow = customerPropertyMetrics(comId, empId);
        Object[] customerRow = customerMetrics(comId, empId);
        Object[] quoteRow = quoteMetrics(comId, empId, now,
                monthStartDate.atStartOfDay(), monthEndDate.atStartOfDay(),
                previousMonthStartDate.atStartOfDay(), previousMonthEndDate.atStartOfDay());
        Object[] contractRow = contractMetrics(comId, empId, monthStartDate, monthEndDate,
                previousMonthStartDate, previousMonthEndDate);
        Map<String, Integer> projectStatusCounts = projectStatusMetrics(comId, empId);
        Object[] materialRow = materialSelectionMetrics(comId, empId);
        Object[] changeRow = contractChangeMetrics(comId, empId);
        Object[] acceptanceRow = acceptanceMetrics(comId, empId);
        List<Object[]> trendRows = trendMetrics(comId, empId,
                trendStart.atStartOfDay(), trendEnd.plusDays(1).atStartOfDay());

        DesignerWorkbenchReport report = new DesignerWorkbenchReport();
        report.setRole("Designer");
        report.setEmployeeId(empId);
        report.setEmployeeName(current.getName());
        report.setCompanyId(comId);
        report.setStoreId(current.getStore() == null ? null : current.getStore().getStoreId());
        report.setDataScope("Self");
        report.setGeneratedAt(now);
        report.setMonth(today.format(MONTH));
        report.setTrendStart(trendStart);
        report.setTrendEnd(trendEnd);
        report.setStatus("ready");

        report.setSummary(List.of(
                summary("designerInProgressCustomerCount", customerRow[0], "count"),
                summaryWithComparison("designerMonthQuoteAmount", quoteRow[7], quoteRow[8], "amount"),
                summaryWithComparison("designerMonthSignedAmount", contractRow[3], contractRow[4], "amount")
        ));

        List<DesignerWorkbenchReport.CardMetric> cards = List.of(
                card("designerMeasurePendingCount", propertyRow[0], List.of(
                        breakdown("pendingMeasure", propertyRow[1]),
                        breakdown("measuring", propertyRow[2])
                )),
                card("designerDesignPendingCount", propertyRow[3], List.of(
                        breakdown("noScheme", propertyRow[4]),
                        breakdown("draft", propertyRow[5]),
                        breakdown("ready", propertyRow[6]),
                        breakdown("presented", propertyRow[7])
                )),
                card("designerQuoteActionCount", quoteRow[0], List.of(
                        breakdown("draft", quoteRow[1]),
                        breakdown("rejected", quoteRow[2])
                )),
                card("pendingQuoteDecisionCount", quoteRow[3], List.of(
                        breakdown("within3Days", quoteRow[4]),
                        breakdown("from3To6Days", quoteRow[5]),
                        breakdown("sevenDaysOrMore", quoteRow[6])
                )),
                card("designerContractHandoffCount", contractRow[0], List.of(
                        breakdown("pendingSign", contractRow[1]),
                        breakdown("signed", contractRow[2])
                )),
                card("designerProjectActiveCount", activeProjectCount(projectStatusCounts), List.of(
                        breakdown("preparing", projectStatusCount(projectStatusCounts, "Preparing")),
                        breakdown("ongoing", projectStatusCount(projectStatusCounts, "Ongoing")),
                        breakdown("suspended", projectStatusCount(projectStatusCounts, "Suspended")),
                        breakdown("accepting", projectStatusCount(projectStatusCounts, "Accepting"))
                )),
                card("designerMaterialSelectionPendingCount", materialRow[0], List.of(
                        breakdown("plannedPlans", materialRow[1]),
                        breakdown("pendingConfirm", materialRow[2])
                )),
                card("designerContractChangePendingCount", changeRow[0], List.of(
                        breakdown("pending", changeRow[1]),
                        breakdown("approved", changeRow[2])
                )),
                card("designerAcceptancePendingCount", acceptanceRow[0], List.of(
                        breakdown("pending", acceptanceRow[1]),
                        breakdown("rejected", acceptanceRow[2]),
                        breakdown("accepting", acceptanceRow[3])
                ))
        );
        report.setCards(cards);
        report.setPipeline(pipeline(propertyRow[0], propertyRow[3], quoteRow[0], contractRow[0]));
        report.setCharts(List.of(
                trendChart(trendRows, trendStart, trendEnd),
                funnelChart(customerRow[0], propertyRow[3], quoteRow[0], quoteRow[3], contractRow[0]),
                distributionChart(cards)
        ));
        report.setWarnings(List.of());
        report.setPerformanceRanking(buildPerformanceRanking(
                performanceRankingRows(comId, monthStartDate, monthEndDate), empId));
        return report;
    }

    /** 本月签约额排名：设计是客户归属设计师，签约额口径与本月签约金额完全一致。 */
    private List<Object[]> performanceRankingRows(Long comId,
                                                   LocalDate monthStart,
                                                   LocalDate monthEnd) {
        return rows("""
                SELECT
                  e.emp_id,
                  e.name,
                  COALESCE(SUM(CASE WHEN (ct.business_origin = 'New' OR ct.business_origin IS NULL)
                       AND ct.signed_at >= :monthStart
                       AND ct.signed_at < :monthEnd
                      THEN COALESCE(NULLIF(ct.current_amount, 0), ct.baseline_amount, 0) ELSE 0 END), 0)
                    AS month_signed_amount,
                  COUNT(DISTINCT CASE WHEN (ct.business_origin = 'New' OR ct.business_origin IS NULL)
                       AND ct.signed_at >= :monthStart
                       AND ct.signed_at < :monthEnd
                      THEN ct.contract_id END) AS signed_project_count
                FROM db_tenant_employee e
                LEFT JOIN cust_customer c
                  ON c.com_id = e.com_id
                 AND c.designer_id = e.emp_id
                LEFT JOIN db_project_contract ct
                  ON ct.com_id = c.com_id
                 AND ct.customer_id = c.cust_id
                WHERE e.com_id = :comId
                  AND e.role = 'Designer'
                  AND e.status = 'Active'
                GROUP BY e.emp_id, e.name
                ORDER BY month_signed_amount DESC, signed_project_count DESC, e.emp_id ASC
                """, Map.of(
                "comId", comId,
                "monthStart", monthStart,
                "monthEnd", monthEnd
        ));
    }

    static DesignerWorkbenchReport.PerformanceRanking buildPerformanceRanking(List<Object[]> rows,
                                                                               Long currentEmpId) {
        DesignerWorkbenchReport.PerformanceRanking ranking = new DesignerWorkbenchReport.PerformanceRanking();
        if (rows == null || rows.isEmpty()) {
            ranking.setSampleSufficient(false);
            return ranking;
        }
        List<DesignerWorkbenchReport.RankingItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal previousValue = null;
        int currentRank = 0;
        for (int index = 0; index < rows.size(); index += 1) {
            Object[] row = rows.get(index);
            BigDecimal value = row.length > 2 && row[2] != null
                    ? new BigDecimal(String.valueOf(row[2])).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            if (previousValue == null || value.compareTo(previousValue) != 0) {
                currentRank = index + 1;
                previousValue = value;
            }
            DesignerWorkbenchReport.RankingItem item = new DesignerWorkbenchReport.RankingItem();
            item.setEmployeeId(row.length > 0 && row[0] != null ? Long.valueOf(String.valueOf(row[0])) : null);
            item.setEmployeeName(row.length > 1 && row[1] != null ? String.valueOf(row[1]) : "");
            item.setRank(currentRank);
            item.setValue(value);
            item.setSignedProjectCount(row.length > 3 && row[3] != null ? Integer.parseInt(String.valueOf(row[3])) : 0);
            item.setCurrentEmployee(item.getEmployeeId() != null && item.getEmployeeId().equals(currentEmpId));
            items.add(item);
            total = total.add(value);
        }
        DesignerWorkbenchReport.RankingItem mine = items.stream()
                .filter(item -> Boolean.TRUE.equals(item.getCurrentEmployee()))
                .findFirst()
                .orElse(null);
        BigDecimal myValue = mine == null ? BigDecimal.ZERO : mine.getValue();
        BigDecimal topValue = items.get(0).getValue();
        BigDecimal peerAverage = total.divide(BigDecimal.valueOf(items.size()), 2, RoundingMode.HALF_UP);
        ranking.setRank(mine == null ? 0 : mine.getRank());
        ranking.setDesignerCount(items.size());
        ranking.setSampleSufficient(items.size() >= 3);
        ranking.setMyValue(myValue);
        ranking.setTopValue(topValue);
        ranking.setPeerAverage(peerAverage);
        ranking.setGapToTop(topValue.subtract(myValue).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP));
        ranking.setGapToAverage(myValue.subtract(peerAverage).setScale(2, RoundingMode.HALF_UP));
        ranking.setTopDesigners(items.subList(0, Math.min(5, items.size())));
        return ranking;
    }

    /**
     * 客户 + 房屋 + 设计状态一次聚合。
     *
     * <p>设计状态先按当前设计师范围形成 property 级派生表，避免每个房屋反复执行多个
     * NOT EXISTS 子查询；房屋量房状态仍在同一个结果集中通过 CASE 聚合。</p>
     */
    private Object[] customerPropertyMetrics(Long comId, Long empId) {
        return singleRow("""
                SELECT
                  COUNT(DISTINCT CASE WHEN p.status IN ('PendingMeasure','Measuring')
                      THEN p.property_id END) AS measure_pending_count,
                  COUNT(DISTINCT CASE WHEN p.status = 'PendingMeasure'
                      THEN p.property_id END) AS measure_pending_status_count,
                  COUNT(DISTINCT CASE WHEN p.status = 'Measuring'
                      THEN p.property_id END) AS measure_measuring_count,
                  COUNT(DISTINCT CASE WHEN p.status IN ('Measured','Designing','Quoted')
                      AND COALESCE(ds.has_closed, 0) = 0
                      THEN p.property_id END) AS design_pending_count,
                  COUNT(DISTINCT CASE WHEN p.status IN ('Measured','Designing','Quoted')
                      AND COALESCE(ds.design_count, 0) = 0
                      THEN p.property_id END) AS design_no_scheme_count,
                  COUNT(DISTINCT CASE WHEN p.status IN ('Measured','Designing','Quoted')
                      AND COALESCE(ds.has_draft, 0) = 1
                      AND COALESCE(ds.has_closed, 0) = 0
                      THEN p.property_id END) AS design_draft_count,
                  COUNT(DISTINCT CASE WHEN p.status IN ('Measured','Designing','Quoted')
                      AND COALESCE(ds.has_ready, 0) = 1
                      AND COALESCE(ds.has_closed, 0) = 0
                      THEN p.property_id END) AS design_ready_count,
                  COUNT(DISTINCT CASE WHEN p.status IN ('Measured','Designing','Quoted')
                      AND COALESCE(ds.has_presented, 0) = 1
                      AND COALESCE(ds.has_closed, 0) = 0
                      THEN p.property_id END) AS design_presented_count
                FROM cust_customer c
                STRAIGHT_JOIN db_property p
                  ON p.com_id = c.com_id
                 AND p.customer_id = c.cust_id
                LEFT JOIN (
                  SELECT d.com_id,
                         d.property_id,
                         COUNT(*) AS design_count,
                         MAX(CASE WHEN d.status = 'Draft' THEN 1 ELSE 0 END) AS has_draft,
                         MAX(CASE WHEN d.status = 'Ready' THEN 1 ELSE 0 END) AS has_ready,
                         MAX(CASE WHEN d.status = 'Presented' THEN 1 ELSE 0 END) AS has_presented,
                         MAX(CASE WHEN d.status IN ('Selected','Archived') THEN 1 ELSE 0 END) AS has_closed
                  FROM cust_customer dc
                  STRAIGHT_JOIN db_property dp
                    ON dp.com_id = dc.com_id
                   AND dp.customer_id = dc.cust_id
                  STRAIGHT_JOIN db_design d
                    ON d.com_id = dp.com_id
                   AND d.property_id = dp.property_id
                  WHERE dc.com_id = :comId
                    AND dc.designer_id = :empId
                  GROUP BY d.com_id, d.property_id
                ) ds
                  ON ds.com_id = p.com_id
                 AND ds.property_id = p.property_id
                WHERE c.com_id = :comId
                  AND c.designer_id = :empId
                """, Map.of("comId", comId, "empId", empId));
    }

    private Object[] customerMetrics(Long comId, Long empId) {
        return singleRow("""
                SELECT COUNT(DISTINCT c.cust_id)
                FROM cust_customer c
                WHERE c.com_id = :comId
                  AND c.designer_id = :empId
                  AND c.status IN ('New','Following','PendingMeasure','Quoted')
                """, Map.of("comId", comId, "empId", empId));
    }

    private Object[] quoteMetrics(Long comId,
                                  Long empId,
                                  LocalDateTime now,
                                  LocalDateTime monthStart,
                                  LocalDateTime monthEnd,
                                  LocalDateTime previousMonthStart,
                                  LocalDateTime previousMonthEnd) {
        return singleRow("""
                SELECT
                  COUNT(DISTINCT CASE WHEN q.status IN ('Draft','Rejected') THEN q.quote_id END) AS action_count,
                  COUNT(DISTINCT CASE WHEN q.status = 'Draft' THEN q.quote_id END) AS draft_count,
                  COUNT(DISTINCT CASE WHEN q.status = 'Rejected' THEN q.quote_id END) AS rejected_count,
                  COUNT(DISTINCT CASE WHEN q.status = 'Submitted' THEN q.quote_id END) AS waiting_count,
                  COUNT(DISTINCT CASE WHEN q.status = 'Submitted'
                       AND q.submitted_at IS NOT NULL
                       AND TIMESTAMPDIFF(DAY, q.submitted_at, :now) < 3
                      THEN q.quote_id END) AS waiting_within_3_days,
                  COUNT(DISTINCT CASE WHEN q.status = 'Submitted'
                       AND q.submitted_at IS NOT NULL
                       AND TIMESTAMPDIFF(DAY, q.submitted_at, :now) BETWEEN 3 AND 6
                      THEN q.quote_id END) AS waiting_3_to_6_days,
                  COUNT(DISTINCT CASE WHEN q.status = 'Submitted'
                       AND q.submitted_at IS NOT NULL
                       AND TIMESTAMPDIFF(DAY, q.submitted_at, :now) >= 7
                      THEN q.quote_id END) AS waiting_7_days_or_more,
                  COALESCE(SUM(CASE WHEN q.submitted_at >= :monthStart
                       AND q.submitted_at < :monthEnd
                       AND q.status NOT IN ('Draft','Superseded')
                      THEN q.amount ELSE 0 END), 0) AS month_quote_amount,
                  COALESCE(SUM(CASE WHEN q.submitted_at >= :previousMonthStart
                       AND q.submitted_at < :previousMonthEnd
                       AND q.status NOT IN ('Draft','Superseded')
                      THEN q.amount ELSE 0 END), 0) AS previous_month_quote_amount
                FROM quote q
                WHERE q.com_id = :comId
                  AND q.emp_id = :empId
                """, Map.of(
                "comId", comId,
                "empId", empId,
                "now", now,
                "monthStart", monthStart,
                "monthEnd", monthEnd,
                "previousMonthStart", previousMonthStart,
                "previousMonthEnd", previousMonthEnd
        ));
    }

    private Object[] contractMetrics(Long comId,
                                     Long empId,
                                     LocalDate monthStart,
                                     LocalDate monthEnd,
                                     LocalDate previousMonthStart,
                                     LocalDate previousMonthEnd) {
        return singleRow("""
                SELECT
                  COUNT(DISTINCT CASE WHEN ct.status IN ('PendingSign','Signed') THEN ct.contract_id END) AS handoff_count,
                  COUNT(DISTINCT CASE WHEN ct.status = 'PendingSign' THEN ct.contract_id END) AS pending_sign_count,
                  COUNT(DISTINCT CASE WHEN ct.status = 'Signed' THEN ct.contract_id END) AS signed_count,
                  COALESCE(SUM(CASE WHEN (ct.business_origin = 'New' OR ct.business_origin IS NULL)
                       AND ct.signed_at >= :monthStart
                       AND ct.signed_at < :monthEnd
                      THEN COALESCE(NULLIF(ct.current_amount, 0), ct.baseline_amount, 0) ELSE 0 END), 0)
                    AS month_signed_amount,
                  COALESCE(SUM(CASE WHEN (ct.business_origin = 'New' OR ct.business_origin IS NULL)
                       AND ct.signed_at >= :previousMonthStart
                       AND ct.signed_at < :previousMonthEnd
                      THEN COALESCE(NULLIF(ct.current_amount, 0), ct.baseline_amount, 0) ELSE 0 END), 0)
                    AS previous_month_signed_amount
                FROM cust_customer c
                JOIN db_project_contract ct
                  ON ct.com_id = c.com_id
                 AND ct.customer_id = c.cust_id
                WHERE c.com_id = :comId
                  AND c.designer_id = :empId
                """, Map.of(
                "comId", comId,
                "empId", empId,
                "monthStart", monthStart,
                "monthEnd", monthEnd,
                "previousMonthStart", previousMonthStart,
                "previousMonthEnd", previousMonthEnd
        ));
    }

    /** 项目状态一次分组，由 Java 组装主数量和状态细分，避免同类子查询重复扫描。 */
    private Map<String, Integer> projectStatusMetrics(Long comId, Long empId) {
        List<Object[]> rows = rows("""
                SELECT p.status, COUNT(*)
                FROM db_project p
                WHERE p.com_id = :comId
                  AND p.designer_id = :empId
                GROUP BY p.status
                """, Map.of("comId", comId, "empId", empId));
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            result.put(String.valueOf(row[0]), integer(row[1]));
        }
        return result;
    }

    private Object[] materialSelectionMetrics(Long comId, Long empId) {
        return singleRow("""
                SELECT
                  COUNT(DISTINCT p.project_id) AS project_count,
                  COUNT(m.plan_id) AS plan_count,
                  COUNT(CASE WHEN ms.status = 'PendingConfirm' THEN m.plan_id END) AS pending_confirm_count
                FROM db_project p
                JOIN db_material_plan m
                  ON m.com_id = p.com_id
                 AND m.project_id = p.project_id
                LEFT JOIN db_material_selection ms
                  ON ms.com_id = m.com_id
                 AND ms.selection_id = m.selection_id
                WHERE p.com_id = :comId
                  AND p.designer_id = :empId
                  AND p.status IN ('Ongoing','Accepting')
                  AND m.status = 'Planned'
                """, Map.of("comId", comId, "empId", empId));
    }

    private Object[] contractChangeMetrics(Long comId, Long empId) {
        return singleRow("""
                SELECT
                  COUNT(DISTINCT cc.project_id) AS project_count,
                  COUNT(CASE WHEN cc.status = 'Pending' THEN cc.change_id END) AS pending_count,
                  COUNT(CASE WHEN cc.status = 'Approved' THEN cc.change_id END) AS approved_count
                FROM db_contract_change cc
                JOIN db_project p
                  ON p.com_id = cc.com_id
                 AND p.project_id = cc.project_id
                WHERE cc.com_id = :comId
                  AND p.designer_id = :empId
                  AND cc.status IN ('Pending','Approved')
                """, Map.of("comId", comId, "empId", empId));
    }

    private Object[] acceptanceMetrics(Long comId, Long empId) {
        return singleRow("""
                SELECT
                  COUNT(DISTINCT CASE WHEN a.acceptance_id IS NOT NULL OR ps.stage_id IS NOT NULL
                      THEN p.project_id END) AS project_count,
                  COUNT(DISTINCT CASE WHEN a.status = 'Pending' THEN a.acceptance_id END) AS pending_count,
                  COUNT(DISTINCT CASE WHEN a.status = 'Rejected' THEN a.acceptance_id END) AS rejected_count,
                  COUNT(DISTINCT ps.stage_id) AS accepting_count
                FROM db_project p
                LEFT JOIN db_acceptance a
                  ON a.com_id = p.com_id
                 AND a.project_id = p.project_id
                 AND a.status IN ('Pending','Rejected')
                LEFT JOIN db_project_stage ps
                  ON ps.com_id = p.com_id
                 AND ps.project_id = p.project_id
                 AND ps.status = 'Accepting'
                WHERE p.com_id = :comId
                  AND p.designer_id = :empId
                """, Map.of("comId", comId, "empId", empId));
    }

    private List<Object[]> trendMetrics(Long comId,
                                        Long empId,
                                        LocalDateTime trendStartAt,
                                        LocalDateTime trendEndExclusive) {
        return rows("""
                SELECT 'designerMeasureCompletedDailyCount' AS metric_key,
                       DATE(COALESCE(m.completed_at, m.measure_time)) AS stat_date,
                       COUNT(DISTINCT m.measure_id) AS metric_value
                FROM cust_customer c
                JOIN db_measure m
                  ON m.com_id = c.com_id
                 AND m.customer_id = c.cust_id
                WHERE c.com_id = :comId
                  AND c.designer_id = :empId
                  AND m.status = 'Completed'
                  AND COALESCE(m.completed_at, m.measure_time) >= :trendStartAt
                  AND COALESCE(m.completed_at, m.measure_time) < :trendEndExclusive
                GROUP BY DATE(COALESCE(m.completed_at, m.measure_time))
                UNION ALL
                SELECT 'designerQuoteSubmittedDailyCount' AS metric_key,
                       DATE(q.submitted_at) AS stat_date,
                       COUNT(DISTINCT q.quote_id) AS metric_value
                FROM quote q
                WHERE q.com_id = :comId
                  AND q.emp_id = :empId
                  AND q.submitted_at >= :trendStartAt
                  AND q.submitted_at < :trendEndExclusive
                GROUP BY DATE(q.submitted_at)
                UNION ALL
                SELECT 'designerQuoteAcceptedDailyCount' AS metric_key,
                       DATE(q.accepted_at) AS stat_date,
                       COUNT(DISTINCT q.quote_id) AS metric_value
                FROM quote q
                WHERE q.com_id = :comId
                  AND q.emp_id = :empId
                  AND q.accepted_at >= :trendStartAt
                  AND q.accepted_at < :trendEndExclusive
                GROUP BY DATE(q.accepted_at)
                UNION ALL
                SELECT 'designerContractEffectiveDailyCount' AS metric_key,
                       DATE(ct.effective_at) AS stat_date,
                       COUNT(DISTINCT ct.contract_id) AS metric_value
                FROM cust_customer c
                JOIN db_project_contract ct
                  ON ct.com_id = c.com_id
                 AND ct.customer_id = c.cust_id
                WHERE c.com_id = :comId
                  AND c.designer_id = :empId
                  AND ct.effective_at >= :trendStartAt
                  AND ct.effective_at < :trendEndExclusive
                GROUP BY DATE(ct.effective_at)
                """, Map.of(
                "comId", comId,
                "empId", empId,
                "trendStartAt", trendStartAt,
                "trendEndExclusive", trendEndExclusive
        ));
    }

    private DesignerWorkbenchReport.SummaryMetric summary(String key, Object rawValue, String unit) {
        return new DesignerWorkbenchReport.SummaryMetric(key, decimal(rawValue), unit);
    }

    private DesignerWorkbenchReport.SummaryMetric summaryWithComparison(String key,
                                                                        Object rawValue,
                                                                        Object rawPreviousValue,
                                                                        String unit) {
        DesignerWorkbenchReport.SummaryMetric metric = summary(key, rawValue, unit);
        BigDecimal previous = decimal(rawPreviousValue);
        metric.setPreviousValue(previous);
        if (previous.signum() == 0) {
            metric.setComparisonState("unavailable");
            metric.setComparisonLabel("暂无基期");
            return metric;
        }
        BigDecimal current = decimal(rawValue);
        BigDecimal rate = current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous.abs(), 2, RoundingMode.HALF_UP);
        metric.setChangeRate(rate);
        metric.setComparisonState(rate.signum() > 0 ? "up" : rate.signum() < 0 ? "down" : "flat");
        metric.setComparisonLabel("较上月");
        return metric;
    }

    private List<DesignerWorkbenchReport.PipelineStage> pipeline(Object measureCount,
                                                                Object designCount,
                                                                Object quoteCount,
                                                                Object contractCount) {
        int measure = integer(measureCount);
        int design = integer(designCount);
        int quote = integer(quoteCount);
        int contract = integer(contractCount);
        int current = measure > 0 ? 0 : design > 0 ? 1 : quote > 0 ? 2 : contract > 0 ? 3 : 3;
        return List.of(
                pipelineStage("measure", "量房", measure, 0, current,
                        measure == 0 ? "量房已全部完成" : "待安排 " + measure + " 套", "/designer/customers?surface=measure"),
                pipelineStage("design", "设计", design, 1, current,
                        "待推进 " + design + " 套", "/designer/design"),
                pipelineStage("quote", "报价", quote, 2, current,
                        "待处理 " + quote + " 份", "/designer/quotes"),
                pipelineStage("contract", "合同", contract, 3, current,
                        "待交接 " + contract + " 份", "/designer/contract")
        );
    }

    private DesignerWorkbenchReport.PipelineStage pipelineStage(String key,
                                                                String label,
                                                                int count,
                                                                int index,
                                                                int current,
                                                                String description,
                                                                String route) {
        String state = index < current ? "done" : index == current ? "current" : "pending";
        return new DesignerWorkbenchReport.PipelineStage(key, label, count, null, null, state, description, route);
    }

    private DesignerWorkbenchReport.CardMetric card(String key,
                                                    Object rawCount,
                                                    List<DesignerWorkbenchReport.Breakdown> breakdown) {
        return new DesignerWorkbenchReport.CardMetric(key, integer(rawCount), breakdown);
    }

    private DesignerWorkbenchReport.Breakdown breakdown(String key, Object rawCount) {
        return new DesignerWorkbenchReport.Breakdown(key, integer(rawCount));
    }

    private DesignerWorkbenchReport.Chart trendChart(List<Object[]> rows,
                                                     LocalDate start,
                                                     LocalDate end) {
        Map<String, Map<LocalDate, BigDecimal>> values = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String key = String.valueOf(row[0]);
            LocalDate date = date(row[1]);
            BigDecimal value = decimal(row[2]);
            values.computeIfAbsent(key, ignored -> new LinkedHashMap<>()).put(date, value);
        }
        List<DesignerWorkbenchReport.ChartSeries> series = new ArrayList<>();
        for (String key : TREND_KEYS) {
            List<DesignerWorkbenchReport.TrendPoint> points = new ArrayList<>();
            Map<LocalDate, BigDecimal> byDate = values.getOrDefault(key, Map.of());
            for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                points.add(new DesignerWorkbenchReport.TrendPoint(date, byDate.getOrDefault(date, BigDecimal.ZERO)));
            }
            series.add(new DesignerWorkbenchReport.ChartSeries(key, null, points));
        }
        return new DesignerWorkbenchReport.Chart("designer.businessTrend", "line", series);
    }

    private DesignerWorkbenchReport.Chart funnelChart(Object inProgressCustomers,
                                                      Object designPending,
                                                      Object quoteAction,
                                                      Object waitingConfirmation,
                                                      Object contractHandoff) {
        List<DesignerWorkbenchReport.ChartSeries> series = List.of(
                new DesignerWorkbenchReport.ChartSeries("inProgressCustomers", decimal(inProgressCustomers), List.of()),
                new DesignerWorkbenchReport.ChartSeries("designPending", decimal(designPending), List.of()),
                new DesignerWorkbenchReport.ChartSeries("quoteAction", decimal(quoteAction), List.of()),
                new DesignerWorkbenchReport.ChartSeries("waitingConfirmation", decimal(waitingConfirmation), List.of()),
                new DesignerWorkbenchReport.ChartSeries("contractHandoff", decimal(contractHandoff), List.of())
        );
        return new DesignerWorkbenchReport.Chart("designer.conversionFunnel", "funnel", series);
    }

    private DesignerWorkbenchReport.Chart distributionChart(List<DesignerWorkbenchReport.CardMetric> cards) {
        List<DesignerWorkbenchReport.ChartSeries> series = cards.stream()
                .map(card -> new DesignerWorkbenchReport.ChartSeries(
                        card.getKey(),
                        BigDecimal.valueOf(card.getCount() == null ? 0 : card.getCount()),
                        List.of()))
                .toList();
        return new DesignerWorkbenchReport.Chart("designer.workDistribution", "bar", series);
    }

    private int activeProjectCount(Map<String, Integer> counts) {
        return ACTIVE_PROJECT_STATUSES.stream()
                .mapToInt(status -> projectStatusCount(counts, status))
                .sum();
    }

    private int projectStatusCount(Map<String, Integer> counts, String status) {
        return counts.getOrDefault(status, 0);
    }

    /**
     * 原生查询可能返回多列 Object[]，也可能在只有一列时返回标量 Long/BigDecimal；
     * 统一包装后再由上层按列读取，避免 ClassCastException。
     */
    private Object[] singleRow(String sql, Map<String, Object> parameters) {
        Query query = entityManager.createNativeQuery(sql);
        parameters.forEach(query::setParameter);
        Object result = query.getSingleResult();
        return result instanceof Object[] values ? values : new Object[]{result};
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> rows(String sql, Map<String, Object> parameters) {
        Query query = entityManager.createNativeQuery(sql);
        parameters.forEach(query::setParameter);
        return query.getResultList();
    }

    private int integer(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
    }

    private BigDecimal decimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal decimal) return decimal.setScale(2, RoundingMode.HALF_UP);
        return new BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP);
    }

    private LocalDate date(Object value) {
        if (value instanceof java.sql.Date sqlDate) return sqlDate.toLocalDate();
        if (value instanceof LocalDate localDate) return localDate;
        if (value instanceof LocalDateTime localDateTime) return localDateTime.toLocalDate();
        return LocalDate.parse(String.valueOf(value));
    }
}
