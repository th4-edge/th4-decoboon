package com.th4.edge.decoboon.statistic.context;

import com.th4.edge.decoboon.common.configure.BusinessOrigin;
import com.th4.edge.decoboon.common.context.StoreScope;
import com.th4.edge.decoboon.finance.context.ManualLedgerContext;
import com.th4.edge.decoboon.project.context.ProjectCostContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.statistic.model.StoreComparisonReport;
import com.th4.edge.decoboon.statistic.model.StoreComparisonRow;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.StoreStatus;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.StoreRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 门店对比矩阵（`P0D-05` / `DESIGN-BIZ-20261013-003`）。
 *
 * <p><b>同源</b>：签约额与在建项目数门店口径、`receiptAmount` 与 `projectStoreClause` 均与
 * `statistic/boss/dashboard` 同源（同一 `StatisticStoreAggregateContext` 取数语义）；
 * 毛利直接复用 {@link BossDashboardContext#profitOf}（唯一实现），不存在第二套公式。</p>
 *
 * <p><b>SQL 下推</b>（`BR-031`/`DEC-036`）：门店 × 指标聚合为**一条按门店 `group by` 的 SQL**
 * （门店数不增加 SQL 条数）；毛利所需的批量取数（本月合同 1 条 + 采购应付 1 条 + 成本汇总 + 手工账）
 * 均为**一次性批量**，在内存按门店分组，循环体内零查询。</p>
 *
 * <p><b>口径声明</b>：财务聚合（应收/应付）按**单据自身门店、全期间**统计（未归属门店行 = `store_id` 为空），
 * 与单店「财务工作台」的可见范围聚合口径一致（均排除历史归档项目）；排名仅在**启用门店**之间进行。</p>
 */
@Component
public class StoreComparisonContext {

    private static final DateTimeFormatter PERIOD = DateTimeFormatter.ofPattern("yyyy-MM");
    /**
     * 门店 × 指标聚合（单条 SQL）：签约额/回款额/应收/应付/在建项目/客户数/在职人数。
     * 金额口径与 `StatisticStoreAggregateContext` 一致：合同金额 `current_amount` 为 0 时取 `baseline_amount`，
     * 来源为正常经营（`business_origin` 为空或 `New`），签约时间落在本月。
     */
    private static final String STORE_METRICS_SQL = """
            select s.store_id,
              (select coalesce(sum(case when (c.current_amount is null or c.current_amount = 0)
                          then coalesce(c.baseline_amount, 0) else c.current_amount end), 0)
                 from db_project_contract c
                where c.com_id = :comId and c.store_id = s.store_id
                  and (c.business_origin is null or c.business_origin = 'New')
                  and c.signed_at >= :from and c.signed_at <= :to) as contract_amount,
              (select coalesce(sum(r.amount), 0) from db_finance_receipt r
                where r.com_id = :comId and r.store_id = s.store_id
                  and r.received_at >= :from and r.received_at <= :to) as receipt_amount,
              (select coalesce(sum(rc.amount), 0) from db_finance_receivable rc
                where rc.com_id = :comId and rc.store_id = s.store_id) as receivable_amount,
              (select coalesce(sum(rc.amount - coalesce(rc.received_amount, 0)), 0) from db_finance_receivable rc
                where rc.com_id = :comId and rc.store_id = s.store_id) as outstanding_amount,
              (select coalesce(sum(p.amount), 0) from db_finance_payable p
                where p.com_id = :comId and p.store_id = s.store_id) as payable_amount,
              (select coalesce(sum(p.amount - coalesce(p.paid_amount, 0)), 0) from db_finance_payable p
                where p.com_id = :comId and p.store_id = s.store_id) as unpaid_amount,
              (select count(*) from db_project pr
                where pr.com_id = :comId and pr.store_id = s.store_id
                  and pr.status not in ('Finished', 'Settled')) as active_projects,
              (select count(*) from cust_customer cu
                where cu.com_id = :comId and cu.store_id = s.store_id) as customers,
              (select count(*) from db_tenant_employee e
                where e.com_id = :comId and e.store_id = s.store_id and e.status = 'Active') as headcount
            from db_tenant_store s
            where s.company_com_id = :comId
            order by s.sort, s.store_id
            """;

    /** 未归属门店行（`store_id` 为空）的同口径聚合；合同/项目按「项目门店缺失回退客户门店」不适用，按空门店取值。 */
    private static final String UNASSIGNED_METRICS_SQL = """
            select
              null as store_id,
              (select coalesce(sum(case when (c.current_amount is null or c.current_amount = 0)
                          then coalesce(c.baseline_amount, 0) else c.current_amount end), 0)
                 from db_project_contract c
                where c.com_id = :comId and c.store_id is null
                  and (c.business_origin is null or c.business_origin = 'New')
                  and c.signed_at >= :from and c.signed_at <= :to) as contract_amount,
              (select coalesce(sum(r.amount), 0) from db_finance_receipt r
                where r.com_id = :comId and r.store_id is null
                  and r.received_at >= :from and r.received_at <= :to) as receipt_amount,
              (select coalesce(sum(rc.amount), 0) from db_finance_receivable rc
                where rc.com_id = :comId and rc.store_id is null) as receivable_amount,
              (select coalesce(sum(rc.amount - coalesce(rc.received_amount, 0)), 0) from db_finance_receivable rc
                where rc.com_id = :comId and rc.store_id is null) as outstanding_amount,
              (select coalesce(sum(p.amount), 0) from db_finance_payable p
                where p.com_id = :comId and p.store_id is null) as payable_amount,
              (select coalesce(sum(p.amount - coalesce(p.paid_amount, 0)), 0) from db_finance_payable p
                where p.com_id = :comId and p.store_id is null) as unpaid_amount,
              (select count(*) from db_project pr
                where pr.com_id = :comId and pr.store_id is null and pr.status not in ('Finished', 'Settled')) as active_projects,
              (select count(*) from cust_customer cu
                where cu.com_id = :comId and cu.store_id is null) as customers,
              (select count(*) from db_tenant_employee e
                where e.com_id = :comId and e.store_id is null and e.status = 'Active') as headcount
            """;

    private final TenantContext tenantContext;
    private final EntityManager entityManager;
    private final StoreRepository storeRepository;
    private final ProjectContractRepository contractRepository;
    private final StatisticStoreAggregateContext aggregateContext;
    private final ProjectCostContext projectCostContext;
    private final ManualLedgerContext manualLedgerContext;

    public StoreComparisonContext(TenantContext tenantContext,
                                 EntityManager entityManager,
                                 StoreRepository storeRepository,
                                 ProjectContractRepository contractRepository,
                                 StatisticStoreAggregateContext aggregateContext,
                                 ProjectCostContext projectCostContext,
                                 ManualLedgerContext manualLedgerContext) {
        this.tenantContext = tenantContext;
        this.entityManager = entityManager;
        this.storeRepository = storeRepository;
        this.contractRepository = contractRepository;
        this.aggregateContext = aggregateContext;
        this.projectCostContext = projectCostContext;
        this.manualLedgerContext = manualLedgerContext;
    }

    /** 门店对比矩阵（BOSS / 总部财务可用；门店范围角色拒绝）。 */
    @Transactional(readOnly = true)
    public StoreComparisonReport report(Employee principal, LocalDate month) {
        Employee current = tenantContext.requireEmployee(principal);
        requireCompanyWide(current);
        Long comId = current.getCompany().getComId();
        LocalDate period = month == null ? LocalDate.now().withDayOfMonth(1) : month.withDayOfMonth(1);
        LocalDate from = period.withDayOfMonth(1);
        LocalDate to = YearMonth.from(period).atEndOfMonth();

        List<Store> stores = storeRepository.findByCompany_ComIdOrderBySortAscStoreIdAsc(comId);
        Map<Long, Store> storeById = new LinkedHashMap<>();
        stores.forEach(store -> storeById.put(store.getStoreId(), store));

        List<StoreComparisonRow> rows = new ArrayList<>();
        for (Object[] row : metrics(STORE_METRICS_SQL, comId, from, to)) {
            Store store = storeById.get(numberLong(row[0]));
            StoreComparisonRow item = new StoreComparisonRow();
            item.setStoreId(store == null ? numberLong(row[0]) : store.getStoreId());
            item.setStoreName(store == null ? "未知门店" : store.getName());
            item.setStoreStatus((store == null || store.getStatus() == null ? StoreStatus.Active : store.getStatus()).name());
            item.setRanked(item.getStoreStatus().equals(StoreStatus.Active.name()));
            fill(item, row);
            rows.add(item);
        }

        StoreComparisonRow unassigned = new StoreComparisonRow();
        unassigned.setStoreName("未归属门店");
        unassigned.setUnassigned(true);
        unassigned.setRanked(false);
        unassigned.setStoreStatus(StoreStatus.Active.name());
        fill(unassigned, metrics(UNASSIGNED_METRICS_SQL, comId, from, to).get(0));
        rows.add(unassigned);

        fillProfit(comId, from, to, rows, storeById);
        rank(rows);
        rows.forEach(this::perCapita);

        StoreComparisonRow total = total(rows);
        StoreComparisonReport report = new StoreComparisonReport();
        report.setMonth(period);
        report.setPeriod(PERIOD.format(period));
        report.setRows(rows);
        report.setTotal(total);
        report.setRankingNote("排名仅包含启用门店，按签约额（%s，门店自身门店口径）降序；人效分母 = 该店在职员工数"
                .formatted(report.getPeriod()));
        return report;
    }

    /** 矩阵页角色门禁：BOSS 或**企业范围**财务；门店范围角色只有一家店，无对比意义。 */
    private void requireCompanyWide(Employee current) {
        boolean companyWide = current.getRole() == EmployeeRole.Boss
                || current.getRole() == EmployeeRole.Finance;
        if (!companyWide) {
            throw new com.th4.edge.framework.exception.DefinedException(
                    new com.th4.edge.framework.exception.DefaultErrorCode("403",
                            "门店对比仅企业负责人与总部财务可用（门店范围账号只有一家门店，无需对比）"));
        }
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> metrics(String sql, Long comId, LocalDate from, LocalDate to) {
        return entityManager.createNativeQuery(sql)
                .setParameter("comId", comId).setParameter("from", from).setParameter("to", to)
                .getResultList();
    }

    private void fill(StoreComparisonRow item, Object[] row) {
        item.setContractAmount(decimal(row[1]));
        item.setReceiptAmount(decimal(row[2]));
        item.setReceivableAmount(decimal(row[3]));
        item.setOutstandingAmount(decimal(row[4]));
        item.setPayableAmount(decimal(row[5]));
        item.setUnpaidAmount(decimal(row[6]));
        item.setActiveProjectCount(number(row[7]));
        item.setCustomerCount(number(row[8]));
        item.setHeadcount(number(row[9]));
    }

    /** 毛利：与经营总览同一实现（`BossDashboardContext.profitOf`），按门店分组在内存计算（循环体内零查询）。 */
    private void fillProfit(Long comId, LocalDate from, LocalDate to, List<StoreComparisonRow> rows,
                            Map<Long, Store> storeById) {
        List<ProjectContract> contracts = contractRepository.findNormalSignedBetweenByStoreScope(
                comId, from, to, BusinessOrigin.New, true, StoreScope.ALL_STORES);
        Map<Long, Project> projects = new LinkedHashMap<>();
        for (ProjectContract contract : contracts) {
            if (contract.getSignedAt() == null || !YearMonth.from(contract.getSignedAt()).equals(YearMonth.from(from))) {
                continue;
            }
            Project project = contract.getProject();
            if (project != null && project.getProjectId() != null) {
                projects.putIfAbsent(project.getProjectId(), project);
            }
        }
        List<Long> projectIds = List.copyOf(projects.keySet());
        Map<Long, ProjectCostContext.CostSummary> costSummaries = projectCostContext.summarizeAll(new ArrayList<>(projects.values()));
        Map<Long, ManualLedgerContext.ManualAmounts> manualByProject = manualLedgerContext.amountsAll(new ArrayList<>(projects.values()));

        Map<Long, List<ProjectContract>> byStore = new LinkedHashMap<>();
        List<ProjectContract> unassignedContracts = new ArrayList<>();
        for (ProjectContract contract : contracts) {
            Long storeId = contract.getStore() == null ? null : contract.getStore().getStoreId();
            if (storeId == null) {
                unassignedContracts.add(contract);
            } else {
                byStore.computeIfAbsent(storeId, key -> new ArrayList<>()).add(contract);
            }
        }
        for (StoreComparisonRow row : rows) {
            List<ProjectContract> scope = row.isUnassigned()
                    ? unassignedContracts
                    : byStore.getOrDefault(row.getStoreId(), List.of());
            BossDashboardContext.ProfitResult profit =
                    BossDashboardContext.profitOf(scope, from, costSummaries, manualByProject);
            row.setGrossProfit(profit.complete() ? profit.value() : null);
            row.setCostComplete(profit.complete());
            row.setCostWarning(profit.warning());
            row.setGrossMargin(profit.complete() && profit.value() != null && row.getContractAmount().signum() > 0
                    ? profit.value().multiply(BigDecimal.valueOf(100))
                            .divide(row.getContractAmount(), 1, RoundingMode.HALF_UP)
                    : null);
        }
    }

    /** 排名：仅启用门店，按签约额降序；并列按门店索引升序。 */
    private void rank(List<StoreComparisonRow> rows) {
        List<StoreComparisonRow> ranked = rows.stream().filter(StoreComparisonRow::isRanked).sorted(
                Comparator.comparing(StoreComparisonRow::getContractAmount, Comparator.reverseOrder())
                        .thenComparing(StoreComparisonRow::getStoreId)).toList();
        int index = 1;
        for (StoreComparisonRow row : ranked) {
            row.setRank(index++);
        }
    }

    private void perCapita(StoreComparisonRow row) {
        if (row.getHeadcount() == null || row.getHeadcount() <= 0) {
            row.setPerCapitaAmount(BigDecimal.ZERO);
            row.setRanked(row.isRanked());
            return;
        }
        row.setPerCapitaAmount(row.getContractAmount()
                .divide(BigDecimal.valueOf(row.getHeadcount()), 2, RoundingMode.HALF_UP));
    }

    /** 合计 = Σ门店 + 未归属；成本不完整时合计毛利为 null（不得展示伪利润）。 */
    private StoreComparisonRow total(List<StoreComparisonRow> rows) {
        StoreComparisonRow total = new StoreComparisonRow();
        total.setStoreName("全部门店合计（含未归属门店）");
        total.setStoreStatus("AllStores");
        for (StoreComparisonRow row : rows) {
            total.setContractAmount(total.getContractAmount().add(row.getContractAmount()));
            total.setReceiptAmount(total.getReceiptAmount().add(row.getReceiptAmount()));
            total.setReceivableAmount(total.getReceivableAmount().add(row.getReceivableAmount()));
            total.setOutstandingAmount(total.getOutstandingAmount().add(row.getOutstandingAmount()));
            total.setPayableAmount(total.getPayableAmount().add(row.getPayableAmount()));
            total.setUnpaidAmount(total.getUnpaidAmount().add(row.getUnpaidAmount()));
            total.setActiveProjectCount(total.getActiveProjectCount() + row.getActiveProjectCount());
            total.setCustomerCount(total.getCustomerCount() + row.getCustomerCount());
            total.setHeadcount(total.getHeadcount() + row.getHeadcount());
            total.setCostComplete(total.isCostComplete() && row.isCostComplete());
        }
        BigDecimal profit = BigDecimal.ZERO;
        for (StoreComparisonRow row : rows) {
            if (row.getGrossProfit() != null) {
                profit = profit.add(row.getGrossProfit());
            }
        }
        total.setGrossProfit(total.isCostComplete() ? profit : null);
        total.setGrossMargin(total.isCostComplete() && total.getContractAmount().signum() > 0
                ? profit.multiply(BigDecimal.valueOf(100)).divide(total.getContractAmount(), 1, RoundingMode.HALF_UP)
                : null);
        total.setCostWarning(total.isCostComplete() ? null : "存在成本未完整的项目，合计毛利暂不展示");
        perCapita(total);
        return total;
    }

    private BigDecimal decimal(Object value) {
        return value == null ? BigDecimal.ZERO
                : new BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP);
    }

    private int number(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
    }

    private Long numberLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }
}
