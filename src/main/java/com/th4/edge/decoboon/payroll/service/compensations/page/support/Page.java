package com.th4.edge.decoboon.payroll.service.compensations.page.support;

import com.th4.edge.decoboon.common.context.StoreFilterContext;
import com.th4.edge.decoboon.common.context.StoreScope;
import com.th4.edge.decoboon.payroll.configure.CompensationStatus;
import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.payroll.context.PayrollCompensationContext;
import com.th4.edge.decoboon.payroll.model.CompensationScheme;
import com.th4.edge.decoboon.payroll.repository.CompensationSchemeRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

@Note("薪酬方案分页")
@Properties(allow = {
        @AllowProperties(model = CompensationScheme.class,
                properties = {"schemeId", "employee", "baseSalary", "scheduledDays", "effectiveFrom", "effectiveTo",
                        "status", "remark", "locked", "lockedReason", "createdBy", "createTime", "updateTime"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "avatarUrl", "role", "status", "store", "team"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name"}),
        @AllowProperties(model = Team.class, properties = {"teamId", "name"})
})
@Service("payroll/compensation/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.payroll.service.compensations.page.Page {
    @User private Employee employee;
    @AutoInject private Integer current;
    @AutoInject private Integer size;
    @AutoInject private String keyword;
    @AutoInject private String status;
    @AutoInject private String effectiveMonth;
    @AutoInject private Long storeId;
    private final CompensationSchemeRepository repository;
    private final PayrollAccessContext accessContext;
    private final PayrollCompensationContext compensationContext;
    @org.springframework.beans.factory.annotation.Autowired
    private StoreFilterContext storeFilterContext;

    public Page(CompensationSchemeRepository repository,
                PayrollAccessContext accessContext,
                PayrollCompensationContext compensationContext) {
        this.repository = repository;
        this.accessContext = accessContext;
        this.compensationContext = compensationContext;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<CompensationScheme> call(CompensationScheme request) {
        Employee currentEmployee = accessContext.requireManager(employee);
        Long storeFilter = storeFilterContext.resolve(currentEmployee, storeId);
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        CompensationStatus statusFilter = statusFilter(status);
        YearMonth monthFilter = monthFilter(effectiveMonth);
        var items = repository.findAll(accessContext.visible(currentEmployee, CompensationScheme.class))
                .stream()
                .filter(item -> StoreScope.inStore(StoreScope.storeIdOf(item.getStore()), storeFilter))
                .filter(item -> statusFilter == null || item.getStatus() == statusFilter)
                .filter(item -> monthFilter == null || overlaps(item, monthFilter))
                .filter(item -> kw.isBlank()
                        || (item.getEmployee() != null && item.getEmployee().getName() != null
                        && item.getEmployee().getName().toLowerCase().contains(kw)))
                .sorted(Comparator
                        .comparing(CompensationScheme::getEffectiveFrom,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(item -> item.getEmployee() == null ? "" : item.getEmployee().getName()))
                .toList();
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 200);
        int from = Math.min(page * pageSize, items.size());
        int to = Math.min(from + pageSize, items.size());
        List<CompensationScheme> pageItems = items.subList(from, to);
        compensationContext.enrichLockState(pageItems, currentEmployee.getCompany().getComId());
        return new PageImpl<>(pageItems, PageRequest.of(page, pageSize), items.size());
    }

    private CompensationStatus statusFilter(String value) {
        if (value == null || value.isBlank() || "all".equalsIgnoreCase(value)) return null;
        try {
            return CompensationStatus.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            throw new DefinedException(new DefaultErrorCode("400", "薪酬方案状态筛选值无效"));
        }
    }

    private YearMonth monthFilter(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return YearMonth.parse(value);
        } catch (RuntimeException ignored) {
            throw new DefinedException(new DefaultErrorCode("400", "工资月份格式应为 YYYY-MM"));
        }
    }

    private boolean overlaps(CompensationScheme scheme, YearMonth month) {
        var start = month.atDay(1);
        var end = month.atEndOfMonth();
        return scheme.getEffectiveFrom() != null
                && !scheme.getEffectiveFrom().isAfter(end)
                && (scheme.getEffectiveTo() == null || !scheme.getEffectiveTo().isBefore(start));
    }
}
