package com.th4.edge.decoboon.payroll.service.sheets.page.support;

import com.th4.edge.decoboon.common.context.StoreFilterContext;
import com.th4.edge.decoboon.common.context.StoreScope;

import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.payroll.model.PayrollSheet;
import com.th4.edge.decoboon.payroll.repository.PayrollSheetRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Comparator;

/** 工资单分页。 */
@Note("工资单分页")
@Properties(allow = @AllowProperties(model = PayrollSheet.class,
        properties = {"sheetId", "period", "status", "attendanceComplete", "commissionComplete", "totalGrossAmount",
                "totalDeductionAmount", "totalNetAmount", "confirmedAt", "createTime"}))
@Service("payroll/sheet/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.payroll.service.sheets.page.Page {

    @User
    private Employee employee;
    @AutoInject
    private Integer current;
    @AutoInject
    private Integer size;

    /** 门店维度（`DEC-065` / `P1-a2`）：不传 / 0 = 全部门店；门店范围角色恒锁本店，传他店 403。 */
    @AutoInject
    private Long storeId;
    @org.springframework.beans.factory.annotation.Autowired
    private com.th4.edge.decoboon.common.context.StoreFilterContext storeFilterContext;

    private final PayrollSheetRepository repository;
    private final PayrollAccessContext accessContext;

    public Page(PayrollSheetRepository repository, PayrollAccessContext accessContext) {
        this.repository = repository;
        this.accessContext = accessContext;
    }

    @Override
    public org.springframework.data.domain.Page<PayrollSheet> call(PayrollSheet request) {
        Employee currentEmployee = accessContext.requireManager(employee);
        Long storeFilter = storeFilterContext.resolve(currentEmployee, storeId);
        var items = repository.findAll(accessContext.visible(currentEmployee, PayrollSheet.class))
                .stream()
                .filter(item -> StoreScope.inStore(StoreScope.storeIdOf(item.getStore()), storeFilter))
                .sorted(Comparator.comparing(PayrollSheet::getPeriod,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 200);
        int from = Math.min(page * pageSize, items.size());
        int to = Math.min(from + pageSize, items.size());
        return new PageImpl<>(items.subList(from, to), PageRequest.of(page, pageSize), items.size());
    }
}
