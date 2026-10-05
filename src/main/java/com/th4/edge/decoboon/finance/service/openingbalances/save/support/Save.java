package com.th4.edge.decoboon.finance.service.openingbalances.save.support;

import com.th4.edge.decoboon.finance.configure.FundAccount;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.context.OpeningBalanceContext;
import com.th4.edge.decoboon.finance.model.OpeningBalance;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 期初资金余额保存实现。 */
@Note("保存期初资金余额")
@Properties(allow = @AllowProperties(model = OpeningBalance.class,
        properties = {"openingId", "account", "amount", "asOfDate", "remark", "createdBy"}))
@Service("finance/opening-balance/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.finance.service.openingbalances.save.Save {

    @User
    private Employee employee;
    @AutoInject
    @NotNull
    private FundAccount account;
    @AutoInject
    @NotNull
    private BigDecimal amount;
    @AutoInject
    @NotNull
    private LocalDate asOfDate;
    @AutoInject
    @Length(max = 500)
    private String remark;

    private final OpeningBalanceContext openingBalanceContext;
    private final FinanceAccessContext accessContext;

    public Save(OpeningBalanceContext openingBalanceContext, FinanceAccessContext accessContext) {
        this.openingBalanceContext = openingBalanceContext;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional
    public OpeningBalance call(OpeningBalance request) {
        Employee current = accessContext.requireEditor(employee);
        return openingBalanceContext.save(current, account, amount, asOfDate, remark);
    }
}
