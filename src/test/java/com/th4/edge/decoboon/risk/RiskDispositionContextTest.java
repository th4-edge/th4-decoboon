package com.th4.edge.decoboon.risk;

import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.risk.configure.RiskStatus;
import com.th4.edge.decoboon.risk.context.RiskDispositionContext;
import com.th4.edge.decoboon.risk.model.RiskItem;
import com.th4.edge.decoboon.risk.repository.RiskDispositionLogRepository;
import com.th4.edge.decoboon.risk.repository.RiskItemRepository;
import com.th4.edge.decoboon.statistic.configure.BusinessAlertLevel;
import com.th4.edge.decoboon.statistic.configure.BusinessAlertType;
import com.th4.edge.decoboon.statistic.model.BusinessAlert;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 风险处置闭环：用户 2026-09-23 裁决的六条硬规则。
 * ① 同一 alertKey 唯一建单；② 级别升级自动重开且轮次递增；③ 同级复现只累计不重开；
 * ④ 预警条件消失自动 Void；⑤ 关闭必填结果 / 重开必填原因；⑥ 跨企业指派被拒。
 */
class RiskDispositionContextTest {

    private static final long COM_ID = 41L;

    private final RiskItemRepository itemRepository = mock(RiskItemRepository.class);
    private final RiskDispositionLogRepository logRepository = mock(RiskDispositionLogRepository.class);
    private final EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
    private final DataScopeContext dataScopeContext = mock(DataScopeContext.class);

    private final RiskDispositionContext context = new RiskDispositionContext(
            itemRepository, logRepository, employeeRepository, dataScopeContext);

    /** 内存里的「库」 */
    private final List<RiskItem> stored = new ArrayList<>();
    /** 模拟 @SnowflakeId 在持久化时生成主键 */
    private final java.util.concurrent.atomic.AtomicLong idSeq = new java.util.concurrent.atomic.AtomicLong(1000L);

    private Employee boss;

    @BeforeEach
    void setUp() {
        Company company = new Company();
        company.setComId(COM_ID);
        boss = employee(1L, "周总", EmployeeRole.Boss, company);
        when(dataScopeContext.requireEmployee(any())).thenReturn(boss);
        when(itemRepository.findByCompany_ComIdAndStatusIn(any(), anyCollection()))
                .thenAnswer(invocation -> new ArrayList<>(stored));
        when(itemRepository.save(any(RiskItem.class))).thenAnswer(invocation -> {
            RiskItem item = invocation.getArgument(0);
            persist(item);
            return item;
        });
        when(itemRepository.saveAll(any())).thenAnswer(invocation -> {
            List<RiskItem> items = invocation.getArgument(0);
            items.forEach(this::persist);
            return items;
        });
        when(itemRepository.findById(any())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            if (id == null) {
                return Optional.empty();
            }
            return stored.stream().filter(item -> id.equals(item.getRiskId())).findFirst();
        });
        when(employeeRepository.findById(any())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            return id.equals(900L) ? Optional.of(employee(900L, "外企员工", EmployeeRole.Pm, company(99L)))
                    : Optional.of(employee(id, "李工", EmployeeRole.Pm, boss.getCompany()));
        });
    }

    @Test
    void shouldCreateItemOnFirstAlertAndKeepAlertKeyStable() {
        RiskItem item = context.sync(boss, List.of(alert(10231L, BusinessAlertLevel.High))).values().iterator().next();

        assertThat(item.getStatus()).isEqualTo(RiskStatus.Pending);
        assertThat(item.getAlertKey()).isEqualTo("OverdueReceivable:Receivable:10231");
        assertThat(item.getRoundNo()).isEqualTo(1);
        // 高风险 SLA = 24h
        assertThat(item.getDueAt()).isAfter(item.getDiscoveredAt().plusHours(23));
        assertThat(item.getDueAt()).isBefore(item.getDiscoveredAt().plusHours(25));
        assertThat(item.getCompany().getComId()).isEqualTo(COM_ID);
    }

    @Test
    void levelUpgradeShouldAutoReopenWithNewRoundAndKeepHistory() {
        context.sync(boss, List.of(alert(10231L, BusinessAlertLevel.Medium)));
        RiskItem item = stored.get(0);
        context.close(boss, item.getRiskId(), "已确认客户还款计划", null);
        assertThat(item.getStatus()).isEqualTo(RiskStatus.Closed);
        assertThat(item.getCloseResult()).isEqualTo("已确认客户还款计划");

        // 同级复现：不重开，只累计
        context.sync(boss, List.of(alert(10231L, BusinessAlertLevel.Medium)));
        assertThat(item.getStatus()).isEqualTo(RiskStatus.Closed);
        assertThat(item.getRecurrenceCount()).isEqualTo(1);
        assertThat(item.getRoundNo()).isEqualTo(1);

        // 级别升级：自动重开，轮次 +1，历史关闭结果保留
        context.sync(boss, List.of(alert(10231L, BusinessAlertLevel.High)));
        assertThat(item.getStatus()).isEqualTo(RiskStatus.Handling);
        assertThat(item.getRoundNo()).isEqualTo(2);
        assertThat(item.getReopenReason()).contains("升级");
        assertThat(item.getCloseResult()).isEqualTo("已确认客户还款计划");
    }

    @Test
    void vanishedConditionShouldVoidAndNeverCountAsClosed() {
        RiskItem item = context.sync(boss, List.of(alert(10231L, BusinessAlertLevel.High))).values().iterator().next();
        context.claim(boss, item.getRiskId(), null);
        assertThat(item.getStatus()).isEqualTo(RiskStatus.Handling);

        // 预警条件消失（应收已收）
        context.sync(boss, List.of());
        assertThat(item.getStatus()).isEqualTo(RiskStatus.Void);
        assertThat(item.getClosedAt()).isNull();
    }

    @Test
    void closeAndReopenMustCarryRequiredInput() {
        RiskItem item = context.sync(boss, List.of(alert(10231L, BusinessAlertLevel.Low))).values().iterator().next();

        assertThatThrownBy(() -> context.close(boss, item.getRiskId(), "  ", null))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("处置结果");

        context.close(boss, item.getRiskId(), "已完成催收并留痕", null);
        assertThatThrownBy(() -> context.reopen(boss, item.getRiskId(), null))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("原因");

        context.reopen(boss, item.getRiskId(), "客户再次逾期");
        assertThat(item.getStatus()).isEqualTo(RiskStatus.Handling);
        assertThat(item.getRoundNo()).isEqualTo(2);
    }

    @Test
    void assignMustRejectCrossCompanyEmployee() {
        RiskItem item = context.sync(boss, List.of(alert(10231L, BusinessAlertLevel.Medium))).values().iterator().next();

        assertThatThrownBy(() -> context.assign(boss, item.getRiskId(), 900L, "越权指派"))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("本企业");

        context.assign(boss, item.getRiskId(), 77L, "指派李工");
        assertThat(item.getOwner().getEmpId()).isEqualTo(77L);
        assertThat(item.getStatus()).isEqualTo(RiskStatus.Handling);
    }

    @Test
    void batchAssignMustRejectOverLimit() {
        List<Long> ids = new ArrayList<>();
        for (long i = 1; i <= 51; i++) {
            ids.add(i);
        }
        assertThatThrownBy(() -> context.batchAssign(boss, ids, 77L, null))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("最多");
    }

    // ------------------------------------------------------------------ 构造

    private BusinessAlert alert(Long businessId, BusinessAlertLevel level) {
        BusinessAlert alert = new BusinessAlert();
        alert.setType(BusinessAlertType.OverdueReceivable);
        alert.setLevel(level);
        alert.setBusinessType("Receivable");
        alert.setBusinessId(businessId);
        alert.setTitle("逾期应收");
        alert.setMessage("客户逾期未付");
        Project project = new Project();
        project.setProjectId(88L);
        project.setName("保利时代 2-0903");
        project.setStore(store());
        alert.setProject(project);
        alert.setOwner(employee(77L, "张会计", EmployeeRole.Finance, bossCompany()));
        return alert;
    }

    private Employee employee(Long id, String name, EmployeeRole role, Company company) {
        Employee employee = new Employee();
        employee.setEmpId(id);
        employee.setName(name);
        employee.setRole(role);
        employee.setCompany(company);
        employee.setStore(store());
        return employee;
    }

    private Company bossCompany() {
        return boss == null ? company(COM_ID) : boss.getCompany();
    }

    private Company company(Long id) {
        Company company = new Company();
        company.setComId(id);
        return company;
    }

    private Store store() {
        Store store = new Store();
        store.setStoreId(1L);
        store.setName("总店");
        return store;
    }

    private void persist(RiskItem item) {
        if (item.getRiskId() == null) {
            item.setRiskId(idSeq.incrementAndGet());
        }
        if (!stored.contains(item)) {
            stored.add(item);
        }
    }
}
