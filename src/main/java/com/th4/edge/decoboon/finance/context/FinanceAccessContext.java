package com.th4.edge.decoboon.finance.context;

import com.th4.edge.decoboon.finance.configure.FundAccount;
import com.th4.edge.decoboon.finance.configure.LedgerDirection;
import com.th4.edge.decoboon.finance.configure.LedgerSource;
import com.th4.edge.decoboon.finance.configure.PayableStatus;
import com.th4.edge.decoboon.finance.configure.ReceivableStatus;
import com.th4.edge.decoboon.finance.model.Ledger;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.repository.LedgerRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.DataScopeSpecification;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 财务权限、归属快照、余额状态和流水归集上下文。 */
@Component
public class FinanceAccessContext {

    private final TenantContext tenantContext;
    private final DataScopeContext dataScopeContext;
    private final ProjectRepository projectRepository;
    private final LedgerRepository ledgerRepository;

    public FinanceAccessContext(TenantContext tenantContext,
                                DataScopeContext dataScopeContext,
                                ProjectRepository projectRepository,
                                LedgerRepository ledgerRepository) {
        this.tenantContext = tenantContext;
        this.dataScopeContext = dataScopeContext;
        this.projectRepository = projectRepository;
        this.ledgerRepository = ledgerRepository;
    }

    public Employee requireReader(Employee principal) {
        return tenantContext.requireRole(principal,
                EmployeeRole.Boss, EmployeeRole.Finance, EmployeeRole.StoreManager);
    }

    public Employee requireEditor(Employee principal) {
        return tenantContext.requireRole(principal, EmployeeRole.Boss, EmployeeRole.Finance);
    }

    /** 手工作废、更正和冲正重记仅允许企业负责人执行。 */
    public Employee requireBoss(Employee principal) {
        return tenantContext.requireRole(principal, EmployeeRole.Boss);
    }

    public Project resolveProject(Employee current, Long projectId) {
        Project project = projectRepository.findByProjectIdAndCompany_ComId(projectId, current.getCompany().getComId())
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "项目不存在或无权访问")));
        if (current.getRole() == EmployeeRole.StoreManager) {
            Long storeId = current.getStore() == null ? null : current.getStore().getStoreId();
            Long projectStoreId = project.getCustomer() == null || project.getCustomer().getStore() == null
                    ? null : project.getCustomer().getStore().getStoreId();
            if (storeId == null || !storeId.equals(projectStoreId)) {
                throw new DefinedException(new DefaultErrorCode("404", "项目不存在或无权访问"));
            }
        }
        return project;
    }

    public <T> Specification<T> visible(Employee principal, Class<T> modelClass) {
        Employee current = tenantContext.requireEmployee(principal);
        if (current.getRole() == EmployeeRole.Boss || current.getRole() == EmployeeRole.Finance) {
            Long comId = current.getCompany().getComId();
            return (root, query, builder) -> builder.equal(root.get("company").get("comId"), comId);
        }
        DataScope scope = dataScopeContext.resolve(current);
        return DataScopeSpecification.visible(modelClass, scope);
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

    public void applyOwnership(com.th4.edge.decoboon.finance.model.Payment payment,
                               Employee current, Project project) {
        applyBase(payment, current, project);
    }

    public void applyOwnership(com.th4.edge.decoboon.finance.model.Settlement settlement,
                               Employee current, Project project) {
        applyBase(settlement, current, project);
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
        Ledger ledger = new Ledger();
        ledger.setProject(project);
        applyOwnership(ledger, current, project);
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

    private void applyBase(Object target, Employee current, Project project) {
        Long storeId = null;
        Long teamId = null;
        Employee owner = current;
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
            receivable.setStore(project == null ? current.getStore() : project.getCustomer().getStore());
            receivable.setTeam(project == null ? current.getTeam()
                    : project.getCustomer().getTeam());
            receivable.setOwner(owner);
        } else if (target instanceof Payable payable) {
            payable.setCompany(current.getCompany());
            payable.setStore(project == null ? current.getStore() : project.getCustomer().getStore());
            payable.setTeam(project == null ? current.getTeam()
                    : project.getCustomer().getTeam());
            payable.setOwner(owner);
        } else if (target instanceof com.th4.edge.decoboon.finance.model.Receipt receipt) {
            receipt.setCompany(current.getCompany());
            receipt.setStore(project.getCustomer().getStore());
            receipt.setTeam(project.getCustomer().getTeam());
            receipt.setOwner(owner);
        } else if (target instanceof com.th4.edge.decoboon.finance.model.Payment payment) {
            payment.setCompany(current.getCompany());
            payment.setStore(project == null ? current.getStore() : project.getCustomer().getStore());
            payment.setTeam(project == null ? current.getTeam()
                    : project.getCustomer().getTeam());
            payment.setOwner(owner);
        } else if (target instanceof com.th4.edge.decoboon.finance.model.Settlement settlement) {
            settlement.setCompany(current.getCompany());
            settlement.setStore(project.getCustomer().getStore());
            settlement.setTeam(project.getCustomer().getTeam());
            settlement.setOwner(owner);
        } else if (target instanceof Ledger ledger) {
            ledger.setCompany(current.getCompany());
            ledger.setStore(project == null ? current.getStore() : project.getCustomer().getStore());
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
