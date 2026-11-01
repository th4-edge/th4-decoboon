package com.th4.edge.decoboon.finance.service.payments.employee.page.support;

import com.th4.edge.decoboon.common.context.StoreFilterContext;
import com.th4.edge.decoboon.common.context.StoreScope;
import com.th4.edge.decoboon.finance.configure.PayableType;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.payroll.model.PayrollItem;
import com.th4.edge.decoboon.payroll.repository.PayrollItemRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 员工待发应付分页：薪酬发放只读取当前账号范围内、仍有未付金额的员工工资/提成应付。 */
@Note("员工待发应付分页")
@Properties(allow = {
        @AllowProperties(model = Payable.class,
                properties = {"payableId", "project", "name", "type", "payeeEmployee", "payeeName", "period",
                        "amount", "paidAmount", "dueDate", "status", "sourceType", "sourceId", "remark"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("finance/payment/employee/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.finance.service.payments.employee.page.Page {
    @User private Employee employee;
    @AutoInject private Long empId;
    @AutoInject private Long payableId;
    @AutoInject private Integer current;
    @AutoInject private Integer size;
    @AutoInject private Long storeId;
    /** 工资单详情内的发放只允许选择该工资单产生的 PayrollItem 应付。 */
    @AutoInject private Long sheetId;
    /** EmployeeSalary / EmployeeCommission；不传时两者均可。 */
    @AutoInject private String type;
    @AutoInject private String keyword;

    private final PayableRepository repository;
    private final FinanceAccessContext accessContext;
    private final PayrollAccessContext payrollAccessContext;
    private final StoreFilterContext storeFilterContext;
    private final PayrollItemRepository payrollItemRepository;

    public Page(PayableRepository repository, FinanceAccessContext accessContext,
                PayrollAccessContext payrollAccessContext, StoreFilterContext storeFilterContext,
                PayrollItemRepository payrollItemRepository) {
        this.repository = repository;
        this.accessContext = accessContext;
        this.payrollAccessContext = payrollAccessContext;
        this.storeFilterContext = storeFilterContext;
        this.payrollItemRepository = payrollItemRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Payable> call(Payable request) {
        // DEC-097：BOSS/财务和本店店长可读取待发池；门店范围由 StoreFilterContext/DataScope 强制。
        Employee currentEmployee = payrollAccessContext.requirePaymentActor(employee);
        Long storeFilter = storeFilterContext.resolve(currentEmployee, storeId);
        Long comId = currentEmployee.getCompany().getComId();
        List<Long> sheetItemIds = sheetId == null ? List.of() : payrollItemRepository
                .findByCompany_ComIdAndSheet_SheetIdOrderByEmployee_NameAsc(comId, sheetId)
                .stream()
                .map(PayrollItem::getItemId)
                .toList();
        List<PayableType> typeFilter = typeFilter(type);
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();

        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 50);
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(
                Sort.Order.asc("dueDate"),
                Sort.Order.desc("payableId")));
        Specification<Payable> specification = accessContext.visible(currentEmployee, Payable.class)
                .and((root, query, cb) -> {
                    List<Predicate> predicates = new ArrayList<>();
                    predicates.add(root.get("type").in(typeFilter));
                    predicates.add(cb.greaterThan(
                            root.<BigDecimal>get("amount"),
                            cb.coalesce(root.<BigDecimal>get("paidAmount"), BigDecimal.ZERO)));
                    if (!StoreScope.isAllStores(storeFilter)) {
                        predicates.add(cb.equal(root.get("store").get("storeId"), storeFilter));
                    }
                    if (sheetId != null) {
                        if (sheetItemIds.isEmpty()) {
                            predicates.add(cb.disjunction());
                        } else {
                            predicates.add(cb.equal(root.get("sourceType"), "PayrollItem"));
                            predicates.add(root.get("sourceId").in(sheetItemIds));
                        }
                    }
                    if (empId != null) {
                        predicates.add(cb.equal(root.get("payeeEmployee").get("empId"), empId));
                    }
                    if (payableId != null) {
                        predicates.add(cb.equal(root.get("payableId"), payableId));
                    }
                    if (!kw.isBlank()) {
                        Join<Payable, Project> project = root.join("project", JoinType.LEFT);
                        Join<Payable, Employee> payee = root.join("payeeEmployee", JoinType.LEFT);
                        String pattern = "%" + kw + "%";
                        predicates.add(cb.or(
                                cb.like(cb.lower(cb.coalesce(root.get("name"), "")), pattern),
                                cb.like(cb.lower(cb.coalesce(root.get("payeeName"), "")), pattern),
                                cb.like(cb.lower(cb.coalesce(root.get("period"), "")), pattern),
                                cb.like(cb.lower(cb.coalesce(payee.get("name"), "")), pattern),
                                cb.like(cb.lower(cb.coalesce(project.get("name"), "")), pattern)));
                    }
                    return cb.and(predicates.toArray(Predicate[]::new));
                });

        org.springframework.data.domain.Page<Payable> skeleton = repository.findAll(specification, pageable);
        if (skeleton.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, skeleton.getTotalElements());
        }
        List<Long> ids = skeleton.getContent().stream().map(Payable::getPayableId).toList();
        Map<Long, Payable> byId = new LinkedHashMap<>();
        for (Payable item : repository.findDetailsByIdIn(ids)) {
            byId.put(item.getPayableId(), item);
        }
        List<Payable> ordered = ids.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
        return new PageImpl<>(ordered, pageable, skeleton.getTotalElements());
    }

    private List<PayableType> typeFilter(String value) {
        if (value == null || value.isBlank()) {
            return List.of(PayableType.EmployeeSalary, PayableType.EmployeeCommission);
        }
        for (PayableType candidate : List.of(PayableType.EmployeeSalary, PayableType.EmployeeCommission)) {
            if (candidate.name().equalsIgnoreCase(value.trim())) {
                return List.of(candidate);
            }
        }
        return List.of();
    }
}
