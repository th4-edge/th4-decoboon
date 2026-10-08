package com.th4.edge.decoboon.customer.service.cust.page.support;

import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.context.CustomerGradeContext;
import com.th4.edge.decoboon.customer.context.CustomerTagContext;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.property.configure.MeasureStatus;
import com.th4.edge.decoboon.property.model.Measure;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
import com.th4.edge.decoboon.customer.model.CustomerGrade;
import com.th4.edge.decoboon.customer.model.CustomerPortrait;
import com.th4.edge.decoboon.customer.model.CustomerTag;
import com.th4.edge.decoboon.customer.repository.CustomerFollowRepository;
import com.th4.edge.decoboon.customer.repository.CustomerPortraitRepository;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.DataScopeType;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 客户分页（显式实现，支持门店维度）。
 *
 * <p>数据范围与 {@code DataScopeRequestFilter} 口径保持一致：企业负责人=全公司、
 * 店长=本门店、团队负责人=本团队、普通员工=本人负责客户；
 * 在此基础上支持 {@code storeId} 再按门店收窄（非企业负责人忽略前端传值，固定本门店）。</p>
 */
@Note("客户分页")
@Properties(allow = {
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "gender", "source", "community", "addr", "area",
                        "budget", "status", "statusBeforeDormant", "dormantReason", "dormantReasonText", "dormantAt", "dormantBy", "restoredAt", "restoredBy", "owner", "wechatAdded", "wechatNo", "wechatAddedAt", "visitCount", "lastVisitAt", "lastFollowAt",
                        "nextFollowAt", "pendingMeasureAt", "measureAt", "measureRequestedAt", "handedOffAt", "designer", "createTime",
                        "tags", "grade"}),
        @AllowProperties(model = Employee.class,
                properties = {"empId", "name"}),
        @AllowProperties(model = CustomerTag.class,
                properties = {"tagId", "key", "name", "category", "scope", "color", "status", "sort"}),
        @AllowProperties(model = CustomerGrade.class,
                properties = {"level", "score", "computedAt"})
})
@Service("customer/customer/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.customer.service.cust.page.Page {

    @User
    private Employee employee;

    @AutoInject
    private Integer current;

    @AutoInject
    private Integer size;

    /** 门店维度筛选；为空表示不按门店收窄。 */
    @AutoInject
    private Long storeId;

    /** 客户姓名、手机号、小区或地址关键词。 */
    @AutoInject
    private String keyword;

    /** 客户生命周期阶段。 */
    @AutoInject
    private CustStatus status;

    private final TenantContext tenantContext;
    private final DataScopeContext dataScopeContext;
    private final CustomerRepository customerRepository;
    private final CustomerPortraitRepository portraitRepository;
    private final CustomerFollowRepository followRepository;
    private final PropertyRepository propertyRepository;
    private final MeasureRepository measureRepository;
    private final CustomerTagContext tagContext;
    private final CustomerGradeContext gradeContext;

    public Page(TenantContext tenantContext,
                DataScopeContext dataScopeContext,
                CustomerRepository customerRepository,
                CustomerPortraitRepository portraitRepository,
                CustomerFollowRepository followRepository,
                PropertyRepository propertyRepository,
                MeasureRepository measureRepository,
                CustomerTagContext tagContext,
                CustomerGradeContext gradeContext) {
        this.tenantContext = tenantContext;
        this.dataScopeContext = dataScopeContext;
        this.customerRepository = customerRepository;
        this.portraitRepository = portraitRepository;
        this.followRepository = followRepository;
        this.propertyRepository = propertyRepository;
        this.measureRepository = measureRepository;
        this.tagContext = tagContext;
        this.gradeContext = gradeContext;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Customer> call(Customer request) {
        Employee currentEmployee = tenantContext.requireEmployee(employee);
        DataScope scope = dataScopeContext.requireScope(currentEmployee);
        List<Customer> all = scoped(scope);
        if (currentEmployee.getRole() == EmployeeRole.Designer) {
            var assigned = customerRepository.findByCompany_ComIdAndDesigner_EmpId(
                    currentEmployee.getCompany().getComId(), currentEmployee.getEmpId());
            java.util.LinkedHashMap<Long, Customer> merged = new java.util.LinkedHashMap<>();
            all.forEach(item -> merged.put(item.getCustId(), item));
            assigned.forEach(item -> merged.put(item.getCustId(), item));
            all = new java.util.ArrayList<>(merged.values());
        }
        Long effectiveStoreId = resolveStoreId(scope, storeId);
        if (effectiveStoreId != null) {
            all = all.stream()
                    .filter(item -> item.getStore() != null && effectiveStoreId.equals(item.getStore().getStoreId()))
                    .toList();
        }
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        if (status != null || !normalizedKeyword.isEmpty()) {
            all = all.stream()
                    .filter(item -> status == null || item.getStatus() == status)
                    .filter(item -> normalizedKeyword.isEmpty() || matchesKeyword(item, normalizedKeyword))
                    .toList();
        }
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 200);
        int from = Math.min(page * pageSize, all.size());
        int to = Math.min(from + pageSize, all.size());
        List<Customer> content = new ArrayList<>(all.subList(from, to));
        assemble(currentEmployee.getCompany().getComId(), content);
        return new PageImpl<>(content, PageRequest.of(page, pageSize), all.size());
    }

    /** 批量装配标签与分级（只读投影），避免 N+1。 */
    private void assemble(Long comId, List<Customer> content) {
        if (comId == null || content.isEmpty()) return;
        List<Long> customerIds = content.stream()
                .map(Customer::getCustId)
                .filter(java.util.Objects::nonNull)
                .toList();
        if (customerIds.isEmpty()) return;

        // 已量房只认 Completed 量房事实（房屋 latestMeasureAt 或量房记录），
        // 客户级 measureAt（预约时间）不得作为完成依据。
        Set<Long> measuredCustomerIds = new HashSet<>();
        for (Property property : propertyRepository.findByCompany_ComIdAndCustomer_CustIdIn(comId, customerIds)) {
            if (property.getLatestMeasureAt() != null && property.getCustomer() != null) {
                measuredCustomerIds.add(property.getCustomer().getCustId());
            }
        }
        for (Measure measure : measureRepository
                .findByCompany_ComIdAndCustomer_CustIdInAndStatus(comId, customerIds, MeasureStatus.Completed)) {
            if (measure.getCustomer() != null) {
                measuredCustomerIds.add(measure.getCustomer().getCustId());
            }
        }
        Set<Long> scheduledCustomerIds = new HashSet<>();
        for (Measure measure : measureRepository
                .findByCompany_ComIdAndCustomer_CustIdInAndStatus(comId, customerIds, MeasureStatus.Scheduled)) {
            if (measure.getCustomer() != null) {
                scheduledCustomerIds.add(measure.getCustomer().getCustId());
            }
        }
        Map<Long, List<CustomerTag>> tagMap = tagContext.assembleBatch(comId, content, measuredCustomerIds, scheduledCustomerIds);

        Map<Long, CustomerPortrait> portraitMap = new HashMap<>();
        for (CustomerPortrait portrait : portraitRepository.findByCompany_ComIdAndCustomer_CustIdIn(comId, customerIds)) {
            if (portrait.getCustomer() != null) {
                portraitMap.put(portrait.getCustomer().getCustId(), portrait);
            }
        }
        Map<Long, Integer> followCountMap = new HashMap<>();
        for (CustomerFollowRepository.CustomerFollowCount row : followRepository.countGroupByCustomer(comId, customerIds)) {
            if (row.getCustomerId() != null) {
                followCountMap.put(row.getCustomerId(), row.getTotal() == null ? 0 : row.getTotal().intValue());
            }
        }

        for (Customer customer : content) {
            List<CustomerTag> tags = tagMap.getOrDefault(customer.getCustId(), List.of());
            customer.setTags(tags);
            boolean hasMeasure = measuredCustomerIds.contains(customer.getCustId());
            CustomerPortrait portrait = portraitMap.get(customer.getCustId());
            Set<String> tagKeys = new HashSet<>();
            for (CustomerTag tag : tags) {
                if (tag.getKey() != null) tagKeys.add(tag.getKey());
            }
            customer.setGrade(gradeContext.compute(customer, portrait, hasMeasure,
                    followCountMap.getOrDefault(customer.getCustId(), 0), customer.getLastFollowAt(), tagKeys));
        }
    }

    private boolean matchesKeyword(Customer customer, String keyword) {
        return contains(customer.getName(), keyword)
                || contains(customer.getTelNo(), keyword)
                || contains(customer.getCommunity(), keyword)
                || contains(customer.getAddr(), keyword);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    /** 与数据权限过滤器一致的角色数据范围。 */
    private List<Customer> scoped(DataScope scope) {
        Long comId = scope.comId();
        return switch (scope.type()) {
            case Company -> customerRepository.findByCompany_ComId(comId);
            case Store -> customerRepository.findByCompany_ComIdAndStore_StoreIdOrderByCustIdDesc(comId, scope.storeId());
            case Team -> customerRepository.findByCompany_ComIdAndTeam_TeamIdOrderByCustIdDesc(comId, scope.teamId());
            case Self -> customerRepository.findByCompany_ComIdAndOwner_EmpId(comId, scope.empId());
        };
    }

    /** 门店维度：企业负责人可跨门店，其他角色固定本门店，避免越权。 */
    private Long resolveStoreId(DataScope scope, Long requested) {
        if (requested == null) {
            return null;
        }
        if (scope.type() != DataScopeType.Company) {
            return scope.storeId();
        }
        return requested;
    }
}
