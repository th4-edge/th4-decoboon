package com.th4.edge.decoboon.statistic.service.boss.contractchange.report.support;

import com.th4.edge.decoboon.statistic.context.BossContractChangeContext;
import com.th4.edge.decoboon.statistic.model.BossContractChangeGroup;
import com.th4.edge.decoboon.statistic.model.BossContractChangeItem;
import com.th4.edge.decoboon.statistic.model.BossContractChangePeriod;
import com.th4.edge.decoboon.statistic.model.BossContractChangeQuery;
import com.th4.edge.decoboon.statistic.model.BossContractChangeReport;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 企业负责人增减项经营工作台实现。 */
@Note("企业负责人增减项经营工作台")
@Properties(allow = {
        @AllowProperties(model = BossContractChangeReport.class,
                properties = {"asOf", "itemCount", "pendingCount", "pendingAmount", "approvedCount",
                        "approvedAmount", "confirmedCount", "confirmedNetAmount", "thisMonthConfirmedCount",
                        "thisMonthConfirmedAmount", "rejectedCount", "cancelledCount", "confirmedCostDelta",
                        "projectedContractAmount", "projectedGrossProfit", "projectedGrossMargin", "costVisible",
                        "overdueApprovalCount", "overdueConfirmCount", "materialPendingCount", "materialOrderedCount",
                        "costIncompleteCount", "receivableGenerated", "receivableOverdueCount",
                        "receivableChangeTotal", "receivableReceivedTotal", "receivableOutstandingTotal",
                        "receivableOverdueTotal", "periods", "groups", "items"}),
        @AllowProperties(model = BossContractChangePeriod.class,
                properties = {"key", "label", "createdCount", "confirmedCount", "confirmedAmount"}),
        @AllowProperties(model = BossContractChangeGroup.class,
                properties = {"projectId", "projectNo", "projectName", "projectStatus", "customerName",
                        "community", "building", "roomNo", "address", "contractId", "contractNo",
                        "contractCurrentAmount", "inFlightAmount", "projectedContractAmount", "totalCount",
                        "pendingCount", "approvedCount", "confirmedCount", "pendingAmount", "confirmedNetAmount",
                        "costDelta", "projectedGrossProfit", "projectedGrossMargin", "costComplete",
                        "materialPendingCount", "materialOrderedCount", "riskCount", "riskLevel", "riskText",
                        "receivableGenerated", "receivableTotal", "receivableReceivedTotal",
                        "receivableOutstandingTotal", "receivableOverdueTotal", "receivableOverdueCount", "items"}),
        @AllowProperties(model = BossContractChangeItem.class,
                properties = {"changeId", "changeNo", "status", "statusName", "type", "typeName", "projectId",
                        "projectNo", "projectName", "projectStatus", "customerName", "community", "building",
                        "roomNo", "address", "contractId", "contractNo", "contractCurrentAmount",
                        "projectedContractAmount", "amount", "costDelta", "projectedGrossProfit",
                        "projectedGrossMargin", "costComplete", "itemCount", "constructionItemCount",
                        "materialItemCount", "materialPendingCount", "materialOrderedCount", "requestedById",
                        "requestedByName", "approvedById", "approvedByName", "createTime", "approvedAt",
                        "customerConfirmedAt", "waitingHours", "riskLevel", "riskText", "nextAction",
                        "lineSummary", "evidenceImageCount", "hasSignature"})
})
@Service("statistic/boss/contract-change/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.statistic.service.boss.contractchange.report.Report {

    @User
    private Employee employee;

    private final BossContractChangeContext context;

    public Report(BossContractChangeContext context) {
        this.context = context;
    }

    @Override
    @Transactional(readOnly = true)
    public BossContractChangeReport call(BossContractChangeQuery request) {
        return context.report(employee, request);
    }
}
