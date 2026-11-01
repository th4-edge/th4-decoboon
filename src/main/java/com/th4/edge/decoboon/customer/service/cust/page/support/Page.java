package com.th4.edge.decoboon.customer.service.cust.page.support;

import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.context.CustomerGradeContext;
import com.th4.edge.decoboon.customer.context.CustomerLossContext;
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
import com.th4.edge.tenant.user.access.DataScopeSpecification;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import com.th4.edge.decoboon.common.context.StoreFilterContext;
import com.th4.edge.decoboon.common.context.StoreScope;

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
                        "lossId", "lossScope", "lossStage", "lossReason", "lossReasonText", "competitorName", "lostAt", "lostBy",
                        "lossPropertyId", "lossPropertyName", "lossFromStatus", "activeLossCount", "partialLoss",
                        "tags", "grade",
                        // 门店归属（DEC-070 单一事实源=客户门店）：BOSS「全部客户」需要按门店解释每一行，
                        // 并标注「未归属门店」；只给摘要（storeId/name），不展开门店其他字段（响应体最小白名单）。
                        "store"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name"}),
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
    private final StoreFilterContext storeFilterContext;
    private final CustomerGradeContext gradeContext;
    private final CustomerLossContext lossContext;

    public Page(TenantContext tenantContext,
                DataScopeContext dataScopeContext,
                CustomerRepository customerRepository,
                CustomerPortraitRepository portraitRepository,
                CustomerFollowRepository followRepository,
                PropertyRepository propertyRepository,
                MeasureRepository measureRepository,
                CustomerTagContext tagContext,
                CustomerGradeContext gradeContext, StoreFilterContext storeFilterContext,
                CustomerLossContext lossContext) {
        this.tenantContext = tenantContext;
        this.dataScopeContext = dataScopeContext;
        this.customerRepository = customerRepository;
        this.portraitRepository = portraitRepository;
        this.followRepository = followRepository;
        this.propertyRepository = propertyRepository;
        this.measureRepository = measureRepository;
        this.tagContext = tagContext;
        this.storeFilterContext = storeFilterContext;
        this.gradeContext = gradeContext;
        this.lossContext = lossContext;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Customer> call(Customer request) {
        Employee currentEmployee = tenantContext.requireEmployee(employee);
        DataScope scope = dataScopeContext.requireScope(currentEmployee);
        Specification<Customer> specification = DataScopeSpecification.visible(Customer.class, scope);
        if (currentEmployee.getRole() == EmployeeRole.Designer) {
            Long designerId = currentEmployee.getEmpId();
            specification = specification.or((root, query, builder) ->
                    builder.equal(root.get("designer").get("empId"), designerId));
        }
        // 门店维度（DEC-067）：共享解析 + 越权校验；不传 / null / 0 等价为全部门店
        Long effectiveStoreId = storeFilterContext.resolve(currentEmployee, storeId);
        if (!StoreScope.isAllStores(effectiveStoreId)) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("store").get("storeId"), effectiveStoreId));
        }
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        if (status != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("status"), status));
        }
        if (!normalizedKeyword.isEmpty()) {
            String pattern = "%" + normalizedKeyword + "%";
            specification = specification.and((root, query, builder) -> builder.or(
                    builder.like(builder.lower(root.get("name")), pattern),
                    builder.like(builder.lower(root.get("telNo")), pattern),
                    builder.like(builder.lower(root.get("community")), pattern),
                    builder.like(builder.lower(root.get("addr")), pattern)));
        }
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 200);
        org.springframework.data.domain.Page<Customer> result = customerRepository.findAll(
                specification, PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "custId")));
        assemble(currentEmployee.getCompany().getComId(), result.getContent());
        return result;
    }

    /** 批量装配标签与分级（只读投影），避免 N+1。 */
    private void assemble(Long comId, List<Customer> content) {
        if (comId == null || content.isEmpty()) return;
        lossContext.enrich(content);
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

}
