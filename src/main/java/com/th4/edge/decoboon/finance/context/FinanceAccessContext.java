package com.th4.edge.decoboon.finance.context;

import com.th4.edge.decoboon.common.context.StoreScope;
import com.th4.edge.decoboon.finance.configure.FundAccount;
import com.th4.edge.decoboon.finance.configure.LedgerDirection;
import com.th4.edge.decoboon.finance.configure.LedgerSource;
import com.th4.edge.decoboon.finance.configure.PayableStatus;
import com.th4.edge.decoboon.finance.configure.ReceivableStatus;
import com.th4.edge.decoboon.finance.model.Ledger;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.model.ReceiptRefund;
import com.th4.edge.decoboon.finance.repository.LedgerRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.configure.DataScopeType;
import com.th4.edge.tenant.user.access.DataScopeSpecification;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.repository.StoreRepository;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 财务权限、归属快照、余额状态和流水归集上下文。 */
@Component
public class FinanceAccessContext {

    /** 门店归属唯一写入解析器（`DEC-070`）。 */
    @org.springframework.beans.factory.annotation.Autowired
    private com.th4.edge.decoboon.common.context.StoreAttributionContext storeAttributionContext;


    private final TenantContext tenantContext;
    private final DataScopeContext dataScopeContext;
    private final ProjectRepository projectRepository;
    private final LedgerRepository ledgerRepository;
    private final StoreRepository storeRepository;

    public FinanceAccessContext(TenantContext tenantContext,
                                DataScopeContext dataScopeContext,
                                ProjectRepository projectRepository,
                                LedgerRepository ledgerRepository) {
        this(tenantContext, dataScopeContext, projectRepository, ledgerRepository, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public FinanceAccessContext(TenantContext tenantContext,
                                DataScopeContext dataScopeContext,
                                ProjectRepository projectRepository,
                                LedgerRepository ledgerRepository,
                                StoreRepository storeRepository) {
        this.tenantContext = tenantContext;
        this.dataScopeContext = dataScopeContext;
        this.projectRepository = projectRepository;
        this.ledgerRepository = ledgerRepository;
        this.storeRepository = storeRepository;
    }

    public Employee requireReader(Employee principal) {
        return tenantContext.requireRole(principal,
                EmployeeRole.Boss, EmployeeRole.Finance, EmployeeRole.StoreManager);
    }

    /** 财务/店长可记账（DEC-065 FIN-STORE-06：店长写权限边界与门店财务一致）。 */
    public Employee requireEditor(Employee principal) {
        return tenantContext.requireRole(principal,
                EmployeeRole.Boss, EmployeeRole.Finance, EmployeeRole.StoreManager);
    }

    /** 手工作废、更正和冲正重记仅允许企业负责人执行（历史入口保留）。 */
    public Employee requireBoss(Employee principal) {
        return tenantContext.requireRole(principal, EmployeeRole.Boss);
    }

    /**
     * 财务高危动作（DEC-075/DEC-076）：BOSS 全企业，店长本店；门店财务/总部财务不自动获得。
     */
    public Employee requireHighRiskEditor(Employee principal) {
        return tenantContext.requireRole(principal, EmployeeRole.Boss, EmployeeRole.StoreManager);
    }

    /** 店长执行财务高危动作时，目标收款必须属于本人门店。 */
    public void requireReceiptScope(Employee principal,
                                    com.th4.edge.decoboon.finance.model.Receipt receipt) {
        if (principal.getRole() != EmployeeRole.StoreManager) {
            return;
        }
        Long currentStoreId = scope(principal).type() == DataScopeType.Store ? scope(principal).storeId() : null;
        Long receiptStoreId = receipt == null || receipt.getStore() == null ? null : receipt.getStore().getStoreId();
        if (currentStoreId == null || !currentStoreId.equals(receiptStoreId)) {
            throw new DefinedException(new DefaultErrorCode("404", "收款记录不存在或无权访问"));
        }
    }

    /** 店长执行财务高危动作时，目标流水必须属于本人门店。 */
    public void requireLedgerScope(Employee principal, Ledger ledger) {
        if (principal.getRole() != EmployeeRole.StoreManager) {
            return;
        }
        Long currentStoreId = scope(principal).type() == DataScopeType.Store ? scope(principal).storeId() : null;
        Long ledgerStoreId = ledger == null || ledger.getStore() == null ? null : ledger.getStore().getStoreId();
        if (currentStoreId == null || !currentStoreId.equals(ledgerStoreId)) {
            throw new DefinedException(new DefaultErrorCode("404", "手工流水不存在或无权访问"));
        }
    }

    @Transactional(readOnly = true)
    public Project resolveProject(Employee current, Long projectId) {
        Project project = projectRepository.findScopeDetailByProjectIdAndCompany_ComId(
                        projectId, current.getCompany().getComId())
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "项目不存在或无权访问")));
        // DEC-065：门店范围主体（门店财务、店长）只能访问本店项目；总部财务/BOSS 为企业范围。
        if (scope(current).type() == DataScopeType.Store) {
            Long storeId = scope(current).storeId();
            Long projectStoreId = project.getCustomer() == null || project.getCustomer().getStore() == null
                    ? null : project.getCustomer().getStore().getStoreId();
            if (storeId == null || !storeId.equals(projectStoreId)) {
                throw new DefinedException(new DefaultErrorCode("404", "项目不存在或无权访问"));
            }
        }
        return project;
    }

    /**
     * 财务域可见范围（DEC-065）。
     *
     * <p>BOSS 与企业范围主体按 {@code comId}；门店财务与店长按 {@code comId AND storeId}。
     * BOSS 之外的判定统一交给数据范围解析，避免「角色硬编码 + 数据范围」两套口径。</p>
     */
    public <T> Specification<T> visible(Employee principal, Class<T> modelClass) {
        Employee current = tenantContext.requireEmployee(principal);
        if (current.getRole() == EmployeeRole.Boss) {
            Long comId = current.getCompany().getComId();
            return (root, query, builder) -> builder.equal(root.get("company").get("comId"), comId);
        }
        return DataScopeSpecification.visible(modelClass, dataScopeContext.resolve(current));
    }

    /** 当前登录人的数据范围（DEC-065：门店财务/店长为门店，总部财务/BOSS 为企业）。 */
    public DataScope scope(Employee principal) {
        return dataScopeContext.resolve(tenantContext.requireEmployee(principal));
    }

    /**
     * 解析「门店维度」过滤值（DEC-065 FIN-STORE-05 / DEC-067 BOSS-STORE-10）。
     *
     * <p>实现统一收敛到 {@link StoreScope#resolve}（全产品唯一门店哨兵与判定）：
     * 企业范围（BOSS、总部财务）可选「全部门店」或本企业任一门店；
     * 门店范围（门店财务、店长）恒为本店；非法门店一律 403「门店不存在或无权访问」，不静默忽略。</p>
     *
     * @return 生效门店 ID；{@link StoreScope#ALL_STORES} 表示不限门店
     */
    public Long resolveStoreFilter(Employee principal, Long requestedStoreId) {
        Employee current = tenantContext.requireEmployee(principal);
        return StoreScope.resolve(dataScopeContext.resolve(current), requestedStoreId, storeRepository);
    }

    public void applyOwnership(Receivable receivable, Employee current, Project project) {
        applyBase(receivable, current, project);
        receivable.setCustomer(project.getCustomer());
    }

    public void applyOwnership(Ledger ledger, Employee current, Project project) {
        // 项目账本按 project_id 归集；这里必须固化项目关联，不能只写门店/团队/负责人快照。
        ledger.setProject(project);
        applyBase(ledger, current, project);
    }

    public void applyOwnership(Payable payable, Employee current, Project project) {
        applyBase(payable, current, project);
    }

    public void applyOwnership(com.th4.edge.decoboon.finance.model.Receipt receipt,
                               Employee current, Project project) {
        applyBase(receipt, current, project);
        receipt.setCustomer(project.getCustomer());
    }

    public void applyOwnership(ReceiptRefund refund, Employee current, Project project) {
        applyBase(refund, current, project);
        refund.setProject(project);
        refund.setCustomer(project.getCustomer());
    }

    public void applyOwnership(com.th4.edge.decoboon.finance.model.Payment payment,
                               Employee current, Project project) {
        applyBase(payment, current, project);
    }

    public void applyOwnership(com.th4.edge.decoboon.finance.model.Settlement settlement,
                               Employee current, Project project) {
        applyBase(settlement, current, project);
    }

    /** 付款/退款等下游单据继承来源单据门店，防止企业范围操作人把来源门店改写成自己的默认门店。 */
    public void applySourceOwnership(com.th4.edge.decoboon.finance.model.Payment payment, Payable payable) {
        if (payment == null || payable == null) return;
        payment.setStore(payable.getStore());
        payment.setTeam(payable.getTeam());
        payment.setOwner(payable.getOwner());
    }

    public void applySourceOwnership(ReceiptRefund refund,
                                     com.th4.edge.decoboon.finance.model.Receipt receipt) {
        if (refund == null || receipt == null) return;
        refund.setStore(receipt.getStore());
        refund.setTeam(receipt.getTeam());
        refund.setOwner(receipt.getOwner());
    }

    public void refreshReceivableStatus(Receivable receivable) {
        if (receivable.getStatus() == ReceivableStatus.Closed) {
            return;
        }
        BigDecimal amount = value(receivable.getAmount());
        BigDecimal received = value(receivable.getReceivedAmount());
        if (received.compareTo(amount) >= 0) {
            receivable.setStatus(ReceivableStatus.Received);
        } else if (received.signum() > 0) {
            receivable.setStatus(ReceivableStatus.Partial);
        } else if (receivable.getDueDate() != null && receivable.getDueDate().isBefore(LocalDate.now())) {
            receivable.setStatus(ReceivableStatus.Overdue);
        } else {
            receivable.setStatus(ReceivableStatus.Pending);
        }
    }

    public void refreshPayableStatus(Payable payable) {
        if (payable.getStatus() == PayableStatus.Closed) {
            return;
        }
        BigDecimal amount = value(payable.getAmount());
        BigDecimal paid = value(payable.getPaidAmount());
        if (paid.compareTo(amount) >= 0) {
            payable.setStatus(PayableStatus.Paid);
        } else if (paid.signum() > 0) {
            payable.setStatus(PayableStatus.Partial);
        } else if (payable.getDueDate() != null && payable.getDueDate().isBefore(LocalDate.now())) {
            payable.setStatus(PayableStatus.Overdue);
        } else {
            payable.setStatus(PayableStatus.Pending);
        }
    }

    public Ledger recordLedger(Employee current, Project project, LedgerDirection direction,
                               String category, LedgerSource source, Long sourceId,
                               BigDecimal amount, FundAccount account, LocalDate occurredAt,
                               String counterparty, String summary, String remark) {
        return recordLedger(current, project, direction, category, source, sourceId, amount, account,
                occurredAt, counterparty, summary, remark, false, null, null, null);
    }

    /**
     * 下游业务流水继承来源单据门店，企业范围操作人不得把来源门店改写成自己的默认门店。
     * 来源门店为空时必须保留空归属，不能回落到操作人或项目门店。
     */
    public Ledger recordLedger(Employee current, Project project, LedgerDirection direction,
                               String category, LedgerSource source, Long sourceId,
                               BigDecimal amount, FundAccount account, LocalDate occurredAt,
                               String counterparty, String summary, String remark,
                               Store sourceStore, Team sourceTeam, Employee sourceOwner) {
        return recordLedger(current, project, direction, category, source, sourceId, amount, account,
                occurredAt, counterparty, summary, remark, true, sourceStore, sourceTeam, sourceOwner);
    }

    private Ledger recordLedger(Employee current, Project project, LedgerDirection direction,
                                String category, LedgerSource source, Long sourceId,
                                BigDecimal amount, FundAccount account, LocalDate occurredAt,
                                String counterparty, String summary, String remark,
                                boolean sourceOwned, Store sourceStore, Team sourceTeam, Employee sourceOwner) {
        Ledger ledger = new Ledger();
        ledger.setProject(project);
        applyOwnership(ledger, current, project);
        if (sourceOwned) {
            ledger.setStore(sourceStore);
            ledger.setTeam(sourceTeam);
            if (sourceOwner != null) ledger.setOwner(sourceOwner);
        }
        ledger.setDirection(direction);
        ledger.setCategory(category);
        ledger.setSource(source);
        ledger.setSourceId(sourceId);
        ledger.setAmount(amount);
        ledger.setAccount(account == null ? FundAccount.CompanyBank : account);
        ledger.setOccurredAt(occurredAt == null ? LocalDate.now() : occurredAt);
        ledger.setCounterparty(counterparty);
        ledger.setSummary(summary);
        ledger.setRemark(remark);
        ledger.setCreatedBy(current);
        return ledgerRepository.save(ledger);
    }

    public BigDecimal value(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    public BigDecimal outstanding(BigDecimal total, BigDecimal settled) {
        BigDecimal result = value(total).subtract(value(settled));
        return result.signum() < 0 ? BigDecimal.ZERO : result;
    }

    /**
     * 收付/单据归属统一写入（`DEC-070`）：门店一律经唯一解析器取值。
     *
     * <ul>
     *   <li>项目单：**客户门店快照**（应收/应付/收款/付款/结算/退款/台账同一来源，双/三路径收敛）；</li>
     *   <li>无项目手工流水：**操作人门店 → 当前门店维度的门店 → 阻断**（操作人无门店时）；</li>
     * </ul>
     */
    /**
     * 应付门店归属（采购下单 / 到货补建 / 手工三条路径**同源**，`DEC-070` §2）：
     * 一律经唯一解析器取「客户门店快照」（客户无门店时按负责人 → 单门店默认 → 历史回退 → 告警）。
     */
    public void applyPayableStore(Payable payable,
                                  com.th4.edge.decoboon.customer.model.Customer customer,
                                  Employee fallbackOwner,
                                  com.th4.edge.tenant.user.model.Store legacyFallbackStore) {
        payable.setStore(storeAttributionContext.forCustomerSnapshot(customer, fallbackOwner, legacyFallbackStore).store());
    }

    private void applyBase(Object target, Employee current, Project project) {
        Long storeId = null;
        Long teamId = null;
        Employee owner = current;
        com.th4.edge.tenant.user.model.Store attributedStore = project == null
                ? storeAttributionContext.forOperator(current, null).store()
                : storeAttributionContext.forCustomerSnapshot(project.getCustomer(), current, null).store();
        if (project != null && project.getCustomer() != null) {
            if (project.getCustomer().getStore() != null) {
                storeId = project.getCustomer().getStore().getStoreId();
            }
            if (project.getCustomer().getTeam() != null) {
                teamId = project.getCustomer().getTeam().getTeamId();
            }
            if (project.getCustomer().getOwner() != null) {
                owner = project.getCustomer().getOwner();
            }
        }
        if (target instanceof Receivable receivable) {
            receivable.setCompany(current.getCompany());
            receivable.setStore(attributedStore);
            receivable.setTeam(project == null ? current.getTeam()
                    : project.getCustomer().getTeam());
            receivable.setOwner(owner);
        } else if (target instanceof Payable payable) {
            payable.setCompany(current.getCompany());
            payable.setStore(attributedStore);
            payable.setTeam(project == null ? current.getTeam()
                    : project.getCustomer().getTeam());
            payable.setOwner(owner);
        } else if (target instanceof com.th4.edge.decoboon.finance.model.Receipt receipt) {
            receipt.setCompany(current.getCompany());
            receipt.setStore(attributedStore);
            receipt.setTeam(project.getCustomer().getTeam());
            receipt.setOwner(owner);
        } else if (target instanceof ReceiptRefund refund) {
            refund.setCompany(current.getCompany());
            refund.setStore(attributedStore);
            refund.setTeam(project.getCustomer().getTeam());
            refund.setOwner(owner);
        } else if (target instanceof com.th4.edge.decoboon.finance.model.Payment payment) {
            payment.setCompany(current.getCompany());
            payment.setStore(attributedStore);
            payment.setTeam(project == null ? current.getTeam()
                    : project.getCustomer().getTeam());
            payment.setOwner(owner);
        } else if (target instanceof com.th4.edge.decoboon.finance.model.Settlement settlement) {
            settlement.setCompany(current.getCompany());
            settlement.setStore(attributedStore);
            settlement.setTeam(project.getCustomer().getTeam());
            settlement.setOwner(owner);
        } else if (target instanceof Ledger ledger) {
            ledger.setCompany(current.getCompany());
            ledger.setStore(attributedStore);
            ledger.setTeam(project == null ? current.getTeam() : project.getCustomer().getTeam());
            ledger.setOwner(owner);
        }
        if (project != null && project.getCustomer() == null) {
            throw new DefinedException(new DefaultErrorCode("400", "项目客户信息不完整"));
        }
        if (project == null && (storeId != null || teamId != null)) {
            // 非项目账务以当前操作人的组织归属为准，避免前端伪造组织字段。
            return;
        }
    }
}
