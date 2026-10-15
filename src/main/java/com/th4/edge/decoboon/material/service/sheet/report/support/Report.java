package com.th4.edge.decoboon.material.service.sheet.report.support;

import com.th4.edge.decoboon.material.context.MaterialSheetContext;
import com.th4.edge.decoboon.material.model.MaterialSheetReportRow;
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

import java.util.List;

/** 主材下单作业单品类汇总（聚合口径，未定价单据数即"成本待结算"规模）。 */
@Note("主材下单作业单汇总")
@Properties(allow = @AllowProperties(model = MaterialSheetReportRow.class,
        properties = {"sheetType", "sheetCount", "unpricedSheetCount", "orderedSheetCount"}))
@Service("material/sheet/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.material.service.sheet.report.Report {

    @User private Employee employee;
    @AutoInject private Long projectId;

    private final MaterialSheetContext context;

    public Report(MaterialSheetContext context) {
        this.context = context;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaterialSheetReportRow> call(Employee request) {
        return context.report(employee, projectId);
    }
}
