package com.th4.edge.decoboon.finance.service.projectledger.report.support;

import com.th4.edge.decoboon.finance.context.FinanceProjectLedgerContext;
import com.th4.edge.decoboon.finance.model.FinanceProjectLedgerReport;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 项目账本筛选汇总实现。 */
@Note("项目账本汇总")
@Properties(allow = @AllowProperties(model = FinanceProjectLedgerReport.class,
        properties = {"projectCount", "activeProjectCount", "contractAmount", "receivedAmount", "receivableAmount",
                "collectionRate", "costComplete", "costIncompleteProjectCount", "asOf"}))
@Service("finance/project-ledger/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.finance.service.projectledger.report.Report {

    @User
    private Employee employee;
    @AutoInject
    private String keyword;
    @AutoInject
    private String status;
    @AutoInject
    private String scope;
    @AutoInject
    private String filter;
    @AutoInject
    private Long storeId;

    private final FinanceProjectLedgerContext ledgerContext;

    public Report(FinanceProjectLedgerContext ledgerContext) {
        this.ledgerContext = ledgerContext;
    }

    @Override
    @Transactional(readOnly = true)
    public FinanceProjectLedgerReport call(Project request) {
        return ledgerContext.summary(employee, storeId, keyword, status, scope, filter);
    }
}
