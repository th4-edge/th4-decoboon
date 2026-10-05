package com.th4.edge.decoboon.finance.service.ledgers.save.support;
import com.th4.edge.decoboon.customer.model.Customer;

import com.th4.edge.decoboon.finance.configure.FundAccount;
import com.th4.edge.decoboon.finance.configure.LedgerDirection;
import com.th4.edge.decoboon.finance.configure.LedgerManualPurpose;
import com.th4.edge.decoboon.finance.configure.LedgerSource;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.context.LedgerCategoryContext;
import com.th4.edge.decoboon.finance.context.ManualLedgerContext;
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
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 手工记账实现：只登记非计划内其他账务，分类为文本，按账务用途约束边界。 */
@Note("手工记账")
@Properties(allow = {
        @AllowProperties(model = Ledger.class,
                properties = {"ledgerId", "project", "direction", "category", "categoryNode", "purpose", "source", "amount", "account",
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
    /** 手工账务用途；历史客户端不传时按“其他”兼容。 */
    @AutoInject
    private LedgerManualPurpose purpose;
    /** 手工分类文本，如“银行手续费”“临时保洁”“股东借款”。 */
    @AutoInject
    @NotEmpty
    @Length(max = 128)
    private String category;
    /** 历史分类节点；仅兼容旧版 App，新版本不再传。 */
    @AutoInject
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
    private final ManualLedgerContext manualContext;

    public Save(LedgerRepository ledgerRepository, FinanceAccessContext accessContext,
                LedgerCategoryContext categoryContext, ManualLedgerContext manualContext) {
        this.ledgerRepository = ledgerRepository;
        this.accessContext = accessContext;
        this.categoryContext = categoryContext;
        this.manualContext = manualContext;
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
        if (category == null || category.isBlank()) {
            throw new DefinedException(new DefaultErrorCode("400", "请填写分类说明"));
        }
        LedgerManualPurpose effectivePurpose = manualContext.purposeOrDefault(purpose);
        manualContext.validatePurpose(effectivePurpose, project);
        manualContext.validateAllowed(direction, category, summary);
        String categoryText = category.trim();
        LedgerCategory categoryNode = null;
        if (categoryId != null && categoryId != 0L) {
            // 兼容旧版 App：仍接受分类节点关联；新版本只传文本分类。
            categoryNode = categoryContext.select(current, categoryId, direction);
            if (categoryText.isBlank()) categoryText = categoryNode.getName();
        }
        request.setDirection(direction);
        request.setPurpose(effectivePurpose);
        request.setCategory(categoryText);
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
