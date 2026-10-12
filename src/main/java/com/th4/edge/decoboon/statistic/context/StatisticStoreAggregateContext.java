package com.th4.edge.decoboon.statistic.context;

import com.th4.edge.decoboon.common.configure.BusinessOrigin;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计域门店维度的**聚合下推取值**（`DEC-036` / `BR-031` 正模式 / P0B-02 聚合下推）。
 *
 * <p>本上下文只做一件事：把「数字」用一条 SQL 算出，并把**门店条件**放进 SQL 的 WHERE
 * （不传 / `null` / `0` 三者等价为「全部门店」；单店视图用真实门店 ID 收窄）；全部门店时
 * SQL 里**不出现门店条件**（保留 `store` 为空的记录），单店时用显式 `left join` + 等值比较
 * 排除未归属记录——与整改前 Java 过滤语义完全一致。</p>
 *
 * <p>口径逐字段与整改前一致，由 {@code StatisticScopeGoldenCaptureTest} 的 before/after 金标 diff 证明：
 * 门店归属按各报表既有基准（合同=自身门店；项目=`store` 缺失回退客户门店；收款=自身门店；
 * 应付=自身门店；员工=`store`）。业务公式（成本完整性/毛利、MET-001 报价机会口径、岗位比率）
 * 仍由各自口径实现计算，但输入集合已由 SQL 收窄且**循环体内零查询**（`BR-031` 判定要点 2）。</p>
 */
@Component
public class StatisticStoreAggregateContext {

    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");
    /**
     * 客单 5 桶的 SQL 阈值：与整改前 Java「万元 / 10、20、30、50 + HALF_UP 保留 2 位」等价
     * （99950/10000=9.995 → 10.00 进位，故边界是 99950/199950/299950/499950，而不是 100000 等）。
     */
    private static final long BUCKET_1 = 99_950L;
    private static final long BUCKET_2 = 199_950L;
    private static final long BUCKET_3 = 299_950L;
    private static final long BUCKET_4 = 499_950L;

    private final EntityManager entityManager;

    public StatisticStoreAggregateContext(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    // ------------------------------------------------------------------ 驾驶舱

    /** 合同签约额（月）：合同**自身门店**；正常经营来源；金额 = `currentAmount` 非零否则 `baselineAmount`。 */
    public BigDecimal signedContractAmount(Long comId, Long storeId, LocalDate month) {
        LocalDate from = month.withDayOfMonth(1);
        LocalDate to = YearMonth.from(month).atEndOfMonth();
        String jpql = "select coalesce(sum(" + contractAmount("ct") + "), 0) "
                + "from ProjectContract ct left join ct.store cs "
                + "where ct.company.comId = :comId and ct.signedAt >= :from and ct.signedAt <= :to "
                + "and (ct.businessOrigin is null or ct.businessOrigin = :normal)" + ownStoreClause("cs", storeId);
        TypedQuery<BigDecimal> query = entityManager.createQuery(jpql, BigDecimal.class)
                .setParameter("comId", comId).setParameter("from", from).setParameter("to", to)
                .setParameter("normal", BusinessOrigin.New);
        return money(bind(query, storeId).getSingleResult());
    }

    /** 合同签约额（月，组织效能口径）：合同按**项目门店**（`store` 缺失回退客户门店，`DEC-067` BOSS-STORE-09）。 */
    public BigDecimal organizationSignedAmount(Long comId, Long storeId, LocalDate month) {
        LocalDate from = month.withDayOfMonth(1);
        LocalDate to = YearMonth.from(month).atEndOfMonth();
        String jpql = "select coalesce(sum(" + contractAmount("ct") + "), 0) from ProjectContract ct "
                + "left join ct.store cs left join ct.project p left join p.store ps left join p.customer pcu left join pcu.store pcs "
                + "where ct.company.comId = :comId and ct.signedAt >= :from and ct.signedAt <= :to"
                + projectStoreClause(storeId);
        TypedQuery<BigDecimal> query = entityManager.createQuery(jpql, BigDecimal.class)
                .setParameter("comId", comId).setParameter("from", from).setParameter("to", to);
        return money(bind(query, storeId).getSingleResult());
    }

    /** 项目签约数（月，组织效能口径，按项目去重）。 */
    public long organizationNewProjectCount(Long comId, Long storeId, LocalDate month) {
        LocalDate from = month.withDayOfMonth(1);
        LocalDate to = YearMonth.from(month).atEndOfMonth();
        String jpql = "select count(distinct ct.project.projectId) from ProjectContract ct "
                + "left join ct.project p left join p.store ps left join p.customer pcu left join pcu.store pcs "
                + "where ct.company.comId = :comId and ct.signedAt >= :from and ct.signedAt <= :to "
                + "and ct.project is not null" + projectStoreClause(storeId);
        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class)
                .setParameter("comId", comId).setParameter("from", from).setParameter("to", to);
        return bind(query, storeId).getSingleResult();
    }

    /** 收款额（月）：收款**自身门店**。 */
    public BigDecimal receiptAmount(Long comId, Long storeId, LocalDate month) {
        LocalDate from = month.withDayOfMonth(1);
        LocalDate to = YearMonth.from(month).atEndOfMonth();
        String jpql = "select coalesce(sum(r.amount), 0) from Receipt r left join r.store rs "
                + "where r.company.comId = :comId and r.receivedAt >= :from and r.receivedAt <= :to"
                + ownStoreClause("rs", storeId);
        TypedQuery<BigDecimal> query = entityManager.createQuery(jpql, BigDecimal.class)
                .setParameter("comId", comId).setParameter("from", from).setParameter("to", to);
        return money(bind(query, storeId).getSingleResult());
    }

    /**
     * 在建项目数（月末）：项目门店（`store` 缺失回退客户门店）；排除 `Finished/Settled`；
     * 起始日 = 该项目**最小合同号且 `signedAt` 非空**的签约日（合同同按自身门店过滤），缺省回退项目建档日；
     * 起始日不晚于月末，且（未完工 或 实际完工日晚于月末）。
     */
    public long activeProjectCount(Long comId, Long storeId, LocalDate month) {
        LocalDate monthEnd = YearMonth.from(month).atEndOfMonth();
        String innerStore = allStores(storeId) ? "" : " and ct2.store.storeId = :storeId";
        String jpql = "select count(distinct p) from Project p "
                + "left join p.store ps left join p.customer pcu left join pcu.store pcs "
                + "left join ProjectContract c on c.project = p and c.signedAt is not null "
                + "and c.contractId = (select min(ct2.contractId) from ProjectContract ct2 "
                + "where ct2.project = p and ct2.signedAt is not null" + innerStore + ") "
                + "where p.company.comId = :comId and p.status not in (:finished, :settled) "
                + "and ((c.signedAt is not null and c.signedAt <= :monthEnd) "
                + "or (c.signedAt is null and p.createTime.date is not null and p.createTime.date <= :monthEndInt)) "
                + "and (p.actualEndDate is null or p.actualEndDate > :monthEnd)"
                + projectStoreClause(storeId);
        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class)
                .setParameter("comId", comId)
                .setParameter("finished", ProjectStatus.Finished)
                .setParameter("settled", ProjectStatus.Settled)
                .setParameter("monthEnd", monthEnd)
                .setParameter("monthEndInt", Integer.valueOf(YYYYMMDD.format(monthEnd)));
        return bind(query, storeId).getSingleResult();
    }

    /** 应付按项目归集（成本公式输入）：应付**自身门店**，并限定在给定项目集合内（项目为空的行不计）。 */
    public Map<Long, BigDecimal> payableAmountByProject(Long comId, Long storeId, java.util.Collection<Long> projectIds) {
        List<Long> ids = projectIds == null ? List.of() : projectIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        String jpql = "select py.project.projectId, coalesce(sum(py.amount), 0) from Payable py "
                + "left join py.store pys where py.company.comId = :comId and py.project.projectId in :projectIds"
                + ownStoreClause("pys", storeId) + " group by py.project.projectId";
        TypedQuery<Object[]> query = entityManager.createQuery(jpql, Object[].class)
                .setParameter("comId", comId)
                .setParameter("projectIds", ids);
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        for (Object[] row : bind(query, storeId).getResultList()) {
            result.put((Long) row[0], money(row[1]));
        }
        return result;
    }

    /** 未归属门店记录数（只在「全部门店」口径下有意义）。 */
    public long unassignedCustomers(Long comId) {
        return entityManager.createQuery(
                        "select count(c) from Customer c where c.company.comId = :comId and c.store is null", Long.class)
                .setParameter("comId", comId).getSingleResult();
    }

    public long unassignedEmployees(Long comId) {
        return entityManager.createQuery(
                        "select count(e) from Employee e where e.company.comId = :comId and e.store is null", Long.class)
                .setParameter("comId", comId).getSingleResult();
    }

    // ------------------------------------------------------------------ 业务分析

    /** 客户漏斗（月，客户**自身门店**）：线索 / 到店 / 量房。 */
    public CustomerFunnel customerFunnel(Long comId, Long storeId, LocalDate month) {
        int[] range = monthRange(month);
        String jpql = "select count(c), "
                + "coalesce(sum(case when c.lastVisitAt is not null then 1 else 0 end), 0), "
                + "coalesce(sum(case when c.measureAt is not null then 1 else 0 end), 0) from Customer c "
                + "left join c.store cs "
                + "where c.company.comId = :comId and c.createTime.date >= :from and c.createTime.date <= :to"
                + ownStoreClause("cs", storeId);
        TypedQuery<Object[]> query = entityManager.createQuery(jpql, Object[].class)
                .setParameter("comId", comId).setParameter("from", range[0]).setParameter("to", range[1]);
        Object[] row = bind(query, storeId).getSingleResult();
        return new CustomerFunnel(number(row[0]), number(row[1]), number(row[2]));
    }

    /**
     * 本月报价单数 / 已提交报价量 / 待决策报价量（正常经营来源；报价**自身门店**）。
     *
     * <p>口径与整改前逐字对齐：漏斗「报价」= 本月报价单数（**含草稿与替代**，即 `monthQuotes.size()`）；
     * 转化指标的「已提交」= 排除草稿与替代；「待决策」= 状态为 `Submitted`。</p>
     */
    public QuoteCounts monthQuoteCounts(Long comId, Long storeId, LocalDate month) {
        LocalDate from = month.withDayOfMonth(1);
        LocalDate to = YearMonth.from(month).atEndOfMonth();
        String jpql = "select count(q), "
                + "coalesce(sum(case when q.status not in (:draft, :superseded) then 1 else 0 end), 0), "
                + "coalesce(sum(case when q.status = :submitted then 1 else 0 end), 0) from Quote q "
                + "left join q.store qs "
                + "where q.company.comId = :comId and q.submittedAt >= :from and q.submittedAt < :toExclusive "
                + "and (q.businessOrigin is null or q.businessOrigin = :normal)" + ownStoreClause("qs", storeId);
        TypedQuery<Object[]> query = entityManager.createQuery(jpql, Object[].class)
                .setParameter("comId", comId)
                .setParameter("from", from.atStartOfDay())
                .setParameter("toExclusive", to.plusDays(1).atStartOfDay())
                .setParameter("normal", BusinessOrigin.New)
                .setParameter("draft", QuoteStatus.Draft)
                .setParameter("superseded", QuoteStatus.Superseded)
                .setParameter("submitted", QuoteStatus.Submitted);
        Object[] row = bind(query, storeId).getSingleResult();
        return new QuoteCounts(number(row[0]), number(row[1]), number(row[2]));
    }

    /** 本月签约单数（正常经营来源；合同**自身门店**）。 */
    public long monthSignedContractCount(Long comId, Long storeId, LocalDate month) {
        LocalDate from = month.withDayOfMonth(1);
        LocalDate to = YearMonth.from(month).atEndOfMonth();
        String jpql = "select count(ct) from ProjectContract ct left join ct.store cs "
                + "where ct.company.comId = :comId and ct.signedAt >= :from and ct.signedAt <= :to "
                + "and (ct.businessOrigin is null or ct.businessOrigin = :normal)" + ownStoreClause("cs", storeId);
        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class)
                .setParameter("comId", comId).setParameter("from", from).setParameter("to", to)
                .setParameter("normal", BusinessOrigin.New);
        return bind(query, storeId).getSingleResult();
    }

    /** 渠道线索/到店（月，按客户来源分组）。 */
    public List<Object[]> customerChannelCounts(Long comId, Long storeId, LocalDate month) {
        int[] range = monthRange(month);
        String jpql = "select c.source, count(c), "
                + "coalesce(sum(case when c.lastVisitAt is not null then 1 else 0 end), 0) from Customer c "
                + "left join c.store cs "
                + "where c.company.comId = :comId and c.createTime.date >= :from and c.createTime.date <= :to"
                + ownStoreClause("cs", storeId) + " group by c.source";
        TypedQuery<Object[]> query = entityManager.createQuery(jpql, Object[].class)
                .setParameter("comId", comId).setParameter("from", range[0]).setParameter("to", range[1]);
        return bind(query, storeId).getResultList();
    }

    /** 渠道签约（月，合同→项目→客户来源分组）：签约单数与签约额。 */
    public List<Object[]> contractChannelMetrics(Long comId, Long storeId, LocalDate month) {
        LocalDate from = month.withDayOfMonth(1);
        LocalDate to = YearMonth.from(month).atEndOfMonth();
        String jpql = "select pcu.source, count(ct), coalesce(sum(" + contractAmount("ct") + "), 0) "
                + "from ProjectContract ct left join ct.store cs left join ct.project p left join p.customer pcu "
                + "where ct.company.comId = :comId and ct.signedAt >= :from and ct.signedAt <= :to "
                + "and (ct.businessOrigin is null or ct.businessOrigin = :normal)" + ownStoreClause("cs", storeId)
                + " group by pcu.source";
        TypedQuery<Object[]> query = entityManager.createQuery(jpql, Object[].class)
                .setParameter("comId", comId).setParameter("from", from).setParameter("to", to)
                .setParameter("normal", BusinessOrigin.New);
        return bind(query, storeId).getResultList();
    }

    /**
     * 区域 Top-N（月，合同→项目→客户小区，缺失回退房屋小区，再缺失「未标注区域」）：
     * 按签约额倒序，`LIMIT` 在 SQL 完成。
     */
    public List<Object[]> regionMetrics(Long comId, Long storeId, LocalDate month, int topN) {
        LocalDate from = month.withDayOfMonth(1);
        LocalDate to = YearMonth.from(month).atEndOfMonth();
        String region = "coalesce(nullif(trim(pcu.community), ''), nullif(trim(pj.community), ''), '未标注区域')";
        String jpql = "select " + region + ", count(ct), coalesce(sum(" + contractAmount("ct") + "), 0) "
                + "from ProjectContract ct left join ct.store cs left join ct.project p "
                + "left join p.customer pcu left join p.property pj "
                + "where ct.company.comId = :comId and ct.signedAt >= :from and ct.signedAt <= :to "
                + "and (ct.businessOrigin is null or ct.businessOrigin = :normal)" + ownStoreClause("cs", storeId)
                + " group by " + region + " order by sum(" + contractAmount("ct") + ") desc";
        TypedQuery<Object[]> query = entityManager.createQuery(jpql, Object[].class)
                .setParameter("comId", comId).setParameter("from", from).setParameter("to", to)
                .setParameter("normal", BusinessOrigin.New)
                .setMaxResults(Math.max(1, topN));
        return bind(query, storeId).getResultList();
    }

    /** 客单分布（5 桶，月）：分桶计数在 SQL 侧完成。 */
    public long[] orderSizeBuckets(Long comId, Long storeId, LocalDate month) {
        LocalDate from = month.withDayOfMonth(1);
        LocalDate to = YearMonth.from(month).atEndOfMonth();
        String amount = "(" + contractAmount("ct") + ")";
        String jpql = "select "
                + bucketCount(amount, null, BUCKET_1) + ", "
                + bucketCount(amount, BUCKET_1, BUCKET_2) + ", "
                + bucketCount(amount, BUCKET_2, BUCKET_3) + ", "
                + bucketCount(amount, BUCKET_3, BUCKET_4) + ", "
                + bucketCount(amount, BUCKET_4, null) + " from ProjectContract ct left join ct.store cs "
                + "where ct.company.comId = :comId and ct.signedAt >= :from and ct.signedAt <= :to "
                + "and (ct.businessOrigin is null or ct.businessOrigin = :normal)" + ownStoreClause("cs", storeId);
        TypedQuery<Object[]> query = entityManager.createQuery(jpql, Object[].class)
                .setParameter("comId", comId).setParameter("from", from).setParameter("to", to)
                .setParameter("normal", BusinessOrigin.New);
        Object[] row = bind(query, storeId).getSingleResult();
        return new long[]{number(row[0]), number(row[1]), number(row[2]), number(row[3]), number(row[4])};
    }

    /**
     * 本月合同金额升序单列投影：用于中位数（分布统计，SQL 无法用 SUM/COUNT 表达）。
     * 只取金额一列、按 SQL `ORDER BY` 排序，行数 = 当月合同数（不是全量实体）。
     */
    public List<BigDecimal> contractAmountsAsc(Long comId, Long storeId, LocalDate month) {
        LocalDate from = month.withDayOfMonth(1);
        LocalDate to = YearMonth.from(month).atEndOfMonth();
        String amount = contractAmount("ct");
        String jpql = "select " + amount + " from ProjectContract ct left join ct.store cs "
                + "where ct.company.comId = :comId and ct.signedAt >= :from and ct.signedAt <= :to "
                + "and (ct.businessOrigin is null or ct.businessOrigin = :normal)" + ownStoreClause("cs", storeId)
                + " order by " + amount + " asc";
        TypedQuery<BigDecimal> query = entityManager.createQuery(jpql, BigDecimal.class)
                .setParameter("comId", comId).setParameter("from", from).setParameter("to", to)
                .setParameter("normal", BusinessOrigin.New);
        return bind(query, storeId).getResultList();
    }

    // ------------------------------------------------------------------ 组织效能

    /** 组织人力计数（月，员工**自身门店**）：在职人数 / 本月入职人数 / 各角色在职人数。 */
    public OrganizationCounts organizationCounts(Long comId, Long storeId, LocalDate month) {
        LocalDate from = month.withDayOfMonth(1);
        LocalDate to = YearMonth.from(month).atEndOfMonth();
        String jpql = "select count(e), "
                + "coalesce(sum(case when e.joinedAt >= :from and e.joinedAt < :toExclusive then 1 else 0 end), 0) "
                + "from Employee e left join e.store es "
                + "where e.company.comId = :comId and e.status = :active" + ownStoreClause("es", storeId);
        TypedQuery<Object[]> query = entityManager.createQuery(jpql, Object[].class)
                .setParameter("comId", comId)
                .setParameter("active", EmployeeStatus.Active)
                .setParameter("from", from.atStartOfDay())
                .setParameter("toExclusive", to.plusDays(1).atStartOfDay());
        Object[] row = bind(query, storeId).getSingleResult();

        String roleJpql = "select e.role, count(e) from Employee e left join e.store es "
                + "where e.company.comId = :comId and e.status = :active" + ownStoreClause("es", storeId)
                + " group by e.role";
        TypedQuery<Object[]> roleQuery = entityManager.createQuery(roleJpql, Object[].class)
                .setParameter("comId", comId).setParameter("active", EmployeeStatus.Active);
        Map<Object, Long> roleCounts = new LinkedHashMap<>();
        for (Object[] roleRow : bind(roleQuery, storeId).getResultList()) {
            roleCounts.put(roleRow[0], ((Number) roleRow[1]).longValue());
        }
        return new OrganizationCounts(number(row[0]), number(row[1]), roleCounts);
    }

    // ------------------------------------------------------------------ 工具

    /** 合同金额口径：`currentAmount` 为 0 或空时取 `baselineAmount`（与整改前 `contractRevenue` 等价）。 */
    private String contractAmount(String alias) {
        return "case when (" + alias + ".currentAmount is null or " + alias + ".currentAmount = 0) "
                + "then coalesce(" + alias + ".baselineAmount, 0) else " + alias + ".currentAmount end";
    }

    private String bucketCount(String amount, Long lowerInclusive, Long upperExclusive) {
        StringBuilder condition = new StringBuilder();
        if (lowerInclusive != null) {
            condition.append(amount).append(" >= ").append(lowerInclusive);
        }
        if (upperExclusive != null) {
            if (condition.length() > 0) condition.append(" and ");
            condition.append(amount).append(" < ").append(upperExclusive);
        }
        return "coalesce(sum(case when " + condition + " then 1 else 0 end), 0)";
    }

    /** 单店门店谓词：单店视图排除 `store` 为空的未归属记录（与 Java `equals(null)` 恒 false 等价）。 */
    private String ownStoreClause(String storeAlias, Long storeId) {
        return allStores(storeId) ? "" : " and (" + storeAlias + ".storeId = :storeId)";
    }

    /** 项目归属门店谓词：`project.store` 缺失回退 `project.customer.store`。 */
    private String projectStoreClause(Long storeId) {
        return allStores(storeId) ? "" : " and (coalesce(ps.storeId, pcs.storeId) = :storeId)";
    }

    private boolean allStores(Long storeId) {
        return storeId == null || storeId == 0L;
    }

    /** 全部门店时不绑定门店参数（SQL 里没有这个占位符）。 */
    private <T> TypedQuery<T> bind(TypedQuery<T> query, Long storeId) {
        if (!allStores(storeId)) {
            query.setParameter("storeId", storeId);
        }
        return query;
    }

    private int[] monthRange(LocalDate month) {
        LocalDate from = month.withDayOfMonth(1);
        LocalDate to = YearMonth.from(month).atEndOfMonth();
        return new int[]{Integer.parseInt(YYYYMMDD.format(from)), Integer.parseInt(YYYYMMDD.format(to))};
    }

    private BigDecimal money(Object value) {
        return value == null ? BigDecimal.ZERO : ((BigDecimal) value).setScale(2, RoundingMode.HALF_UP);
    }

    private int number(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
    }

    /** 客户漏斗标量。 */
    public record CustomerFunnel(int leads, int visits, int measures) { }

    /** 本月报价标量：总数 / 已提交 / 待决策。 */
    public record QuoteCounts(int total, int submitted, int pendingDecision) { }

    /** 组织人力标量。 */
    public record OrganizationCounts(int headcount, int newHires, Map<Object, Long> roleCounts) { }
}
