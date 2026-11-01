package com.th4.edge.decoboon.payroll.service.commissions.page.support;

import com.th4.edge.decoboon.common.context.StoreFilterContext;
import com.th4.edge.decoboon.common.context.StoreScope;
import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.payroll.model.CommissionItem;
import com.th4.edge.decoboon.payroll.repository.CommissionItemRepository;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Note("提成明细分页")
@Properties(allow = {
        @AllowProperties(model = CommissionItem.class,
                properties = {"commissionId", "employee", "project", "contractAmount", "manualRate", "commissionAmount",
                        "accruedAmount", "releasedAmount", "paidAmount", "status", "remark", "createTime"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = com.th4.edge.decoboon.project.model.Project.class,
                properties = {"projectId", "no", "name", "status"})
})
@Service("payroll/commission/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.payroll.service.commissions.page.Page {

    @User
    private Employee employee;
    @AutoInject
    private Integer current;
    @AutoInject
    private Integer size;
    /** 门店维度（`DEC-065` / `P1-a2`）：不传 / 0 = 全部门店；门店范围角色恒锁本店，传他店 403。 */
    @AutoInject
    private Long storeId;
    /** 本人视角：管理角色也可显式限定为登录员工本人，客户端不能借此指定他人。 */
    @AutoInject
    private Boolean selfOnly;
    @org.springframework.beans.factory.annotation.Autowired
    private StoreFilterContext storeFilterContext;

    private final CommissionItemRepository repository;
    private final PayrollAccessContext accessContext;

    public Page(CommissionItemRepository repository, PayrollAccessContext accessContext) {
        this.repository = repository;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<CommissionItem> call(CommissionItem request) {
        Employee currentEmployee = accessContext.requireCommissionViewer(employee);
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 200);
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(
                Sort.Order.desc("createTime.dateTime"),
                Sort.Order.desc("commissionId")));
        Specification<CommissionItem> specification = accessContext.visible(currentEmployee, CommissionItem.class);
        if (Boolean.TRUE.equals(selfOnly) || !accessContext.isPayrollManager(currentEmployee)) {
            Long currentEmpId = currentEmployee.getEmpId();
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("employee").get("empId"), currentEmpId));
        } else {
            Long storeFilter = storeFilterContext.resolve(currentEmployee, storeId);
            specification = specification.and((root, query, cb) -> StoreScope.isAllStores(storeFilter)
                    ? cb.conjunction()
                    : cb.equal(root.get("store").get("storeId"), storeFilter));
        }

        org.springframework.data.domain.Page<CommissionItem> skeleton = repository.findAll(specification, pageable);
        if (skeleton.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, skeleton.getTotalElements());
        }
        List<Long> ids = skeleton.getContent().stream().map(CommissionItem::getCommissionId).toList();
        Map<Long, CommissionItem> byId = new LinkedHashMap<>();
        for (CommissionItem item : repository.findByCommissionIdIn(ids)) {
            byId.put(item.getCommissionId(), item);
        }
        List<CommissionItem> ordered = ids.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
        return new PageImpl<>(ordered, pageable, skeleton.getTotalElements());
    }
}
