package com.th4.edge.decoboon.finance.service.ledgers.save.support;
import com.th4.edge.decoboon.customer.model.Customer;

import com.th4.edge.decoboon.finance.configure.FundAccount;
import com.th4.edge.decoboon.finance.configure.LedgerDirection;
import com.th4.edge.decoboon.finance.configure.LedgerSource;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.context.LedgerCategoryContext;
import com.th4.edge.decoboon.finance.model.Ledger;
import com.th4.edge.decoboon.finance.model.LedgerCategory;
import com.th4.edge.decoboon.finance.repository.LedgerRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 手工记账实现。 */
@Note("手工记账")
@Properties(allow = {
        @AllowProperties(model = Ledger.class,
                properties = {"ledgerId", "project", "direction", "category", "categoryNode", "source", "amount", "account",
                        "occurredAt", "counterparty", "summary", "remark", "createdBy", "createTime"}),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "contractAmount", "property", "customer", "status" }),
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "community", "addr", "area", "budget", "status"}),
        @AllowProperties(model = LedgerCategory.class,
                properties = {"categoryId", "name", "direction", "code", "builtIn", "sort", "active"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("finance/ledger/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.finance.service.ledgers.save.Save {

    @User
    private Employee employee;
    @AutoInject
    private Long projectId;
    @AutoInject
    @NotNull
    private LedgerDirection direction;
    @AutoInject
    @NotNull
    private Long categoryId;
    @AutoInject
    @NotNull
    private BigDecimal amount;
    @AutoInject
    private FundAccount account;
    @AutoInject
    @NotNull
    private LocalDate occurredAt;
    @AutoInject
    private String counterparty;
    @AutoInject
    @NotEmpty
    private String summary;
    @AutoInject
    private String remark;

    private final LedgerRepository ledgerRepository;
    private final FinanceAccessContext accessContext;
    private final LedgerCategoryContext categoryContext;

    public Save(LedgerRepository ledgerRepository, FinanceAccessContext accessContext,
                LedgerCategoryContext categoryContext) {
        this.ledgerRepository = ledgerRepository;
        this.accessContext = accessContext;
        this.categoryContext = categoryContext;
    }

    @Override
    @Transactional
    public Ledger call(Ledger request) {
        Employee current = accessContext.requireEditor(employee);
        BigDecimal total = accessContext.value(amount);
        if (total.signum() <= 0) {
            throw new DefinedException(new DefaultErrorCode("400", "记账金额必须大于 0"));
        }
        Project project = projectId == null ? null : accessContext.resolveProject(current, projectId);
        request.setDirection(direction);
        // 业务分类只认关联：分类文本由服务端按关联生成快照，客户端不再提交自由文本。
        LedgerCategory categoryNode = categoryContext.select(current, categoryId, direction);
        request.setCategory(categoryNode.getName());
        request.setCategoryNode(categoryNode);
        request.setSource(LedgerSource.Manual);
        request.setAmount(total);
        request.setAccount(account == null ? FundAccount.CompanyBank : account);
        request.setOccurredAt(occurredAt);
        request.setCounterparty(counterparty);
        request.setSummary(summary.trim());
        request.setRemark(remark);
        request.setCreatedBy(current);
        accessContext.applyOwnership(request, current, project);
        return ledgerRepository.save(request);
    }
}
