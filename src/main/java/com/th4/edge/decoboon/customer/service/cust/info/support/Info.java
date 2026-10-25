package com.th4.edge.decoboon.customer.service.cust.info.support;

import com.th4.edge.decoboon.customer.context.CustomerGradeContext;
import com.th4.edge.decoboon.customer.context.CustomerContext;
import com.th4.edge.decoboon.customer.context.CustomerLossContext;
import com.th4.edge.decoboon.customer.context.CustomerTagContext;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerGrade;
import com.th4.edge.decoboon.customer.model.CustomerPortrait;
import com.th4.edge.decoboon.customer.model.CustomerTag;
import com.th4.edge.decoboon.customer.repository.CustomerFollowRepository;
import com.th4.edge.decoboon.customer.repository.CustomerPortraitRepository;
import com.th4.edge.decoboon.property.configure.MeasureStatus;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 客户详情 Provider。
 *
 * <p>按 P0-b 契约装配只读投影：系统标签 + 人工标签、客户画像、客户分级。
 * 分级为派生值，含可解释 breakdown，不接受前端写入。</p>
 */
@Note("客户详情")
@Properties(allow = {
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "gender", "source", "community", "addr",
                        "area", "budget", "status", "statusBeforeDormant", "dormantReason", "dormantReasonText", "dormantAt", "dormantBy", "restoredAt", "restoredBy", "owner", "remark", "wechatAdded", "wechatNo", "wechatAddedAt", "visitCount", "lastVisitAt",
                        "lastFollowAt", "nextFollowAt", "pendingMeasureAt", "measureAt", "measureRequestedAt", "handedOffAt", "designer",
                        "lossId", "lossScope", "lossStage", "lossReason", "lossReasonText", "competitorName", "lostAt", "lostBy",
                        "lossPropertyId", "lossPropertyName", "lossFromStatus", "activeLossCount", "partialLoss",
                        "createTime", "tags", "portrait", "grade"}),
        @AllowProperties(model = Employee.class,
                properties = {"empId", "name"}),
        @AllowProperties(model = CustomerTag.class,
                properties = {"tagId", "key", "name", "category", "scope", "color", "status", "sort"}),
        @AllowProperties(model = CustomerPortrait.class,
                properties = {"portraitId", "decisionRole", "purpose", "houseCondition", "startWindow",
                        "style", "eco", "household", "payment", "concerns", "competitor", "updatedBy"}),
        @AllowProperties(model = CustomerGrade.class,
                properties = {"level", "score", "computedAt", "breakdown"}),
        @AllowProperties(model = CustomerGrade.Item.class,
                properties = {"dimension", "label", "weight", "score", "reason"})
})
@Service("customer/customer/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.customer.service.cust.info.Info {

    @AutoInject
    @NotNull
    private Long customerId;

    /** 可选：只按指定房屋投影量房/设计事实，用于多房屋客户 360 按 activeProperty 重算。 */
    @AutoInject
    private Long propertyId;

    /** 框架注入：当前登录员工。 */
    @User
    private Employee employee;

    private final CustomerPortraitRepository portraitRepository;
    private final CustomerFollowRepository followRepository;
    private final PropertyRepository propertyRepository;
    private final MeasureRepository measureRepository;
    private final CustomerTagContext tagContext;
    private final CustomerGradeContext gradeContext;
    private final CustomerContext customerContext;
    private final CustomerLossContext lossContext;

    public Info(CustomerPortraitRepository portraitRepository,
                CustomerFollowRepository followRepository,
                PropertyRepository propertyRepository,
                MeasureRepository measureRepository,
                CustomerTagContext tagContext,
                CustomerGradeContext gradeContext,
                CustomerContext customerContext,
                CustomerLossContext lossContext) {
        this.portraitRepository = portraitRepository;
        this.followRepository = followRepository;
        this.propertyRepository = propertyRepository;
        this.measureRepository = measureRepository;
        this.tagContext = tagContext;
        this.gradeContext = gradeContext;
        this.customerContext = customerContext;
        this.lossContext = lossContext;
    }

    @Override
    @Transactional(readOnly = true)
    public Customer call(Customer request) {
        Customer customer = customerContext.requireVisible(customerId, employee);
        lossContext.enrich(List.of(customer));
        assemble(customer);
        return customer;
    }

    /** 装配标签、画像与分级只读投影。 */
    private void assemble(Customer customer) {
        Long comId = customer.getCompany() == null ? null : customer.getCompany().getComId();
        if (comId == null || customer.getCustId() == null) {
            return;
        }
        List<Property> properties = propertyRepository
                .findByCompany_ComIdAndCustomer_CustIdOrderByPropertyIdAsc(comId, customer.getCustId());
        if (propertyId != null) {
            // 多房屋客户：状态必须绑定显式选择的当前房屋，跨房屋不得兜底。
            properties = properties.stream()
                    .filter(item -> propertyId.equals(item.getPropertyId()))
                    .toList();
            if (properties.isEmpty()) {
                throw new DefinedException(new DefaultErrorCode("404", "房屋不存在或不属于当前客户"));
            }
        }
        // 只有该（组）房屋存在 Completed 量房记录才算已量房；
        // 客户级 measureAt 仅是预约时间，不能作为量房完成事实。
        List<Long> propertyIds = properties.stream()
                .map(Property::getPropertyId)
                .filter(java.util.Objects::nonNull)
                .toList();
        boolean hasMeasure = properties.stream().anyMatch(item -> item.getLatestMeasureAt() != null)
                || (!propertyIds.isEmpty() && measureRepository
                .existsByCompany_ComIdAndProperty_PropertyIdInAndStatus(comId, propertyIds, MeasureStatus.Completed));
        boolean hasMeasureScheduled = !hasMeasure && !propertyIds.isEmpty() && measureRepository
                .existsByCompany_ComIdAndProperty_PropertyIdInAndStatus(comId, propertyIds, MeasureStatus.Scheduled);

        List<CustomerTag> tags = tagContext.assemble(customer, hasMeasure, hasMeasureScheduled);
        customer.setTags(tags);

        CustomerPortrait portrait = portraitRepository
                .findByCompany_ComIdAndCustomer_CustId(comId, customer.getCustId())
                .orElse(null);
        customer.setPortrait(portrait);

        int followCount = (int) followRepository
                .findByCustomer_CustIdOrderByCreateTime_DateTimeDesc(customer.getCustId(), PageRequest.of(0, 1))
                .getTotalElements();
        LocalDateTime lastFollowAt = customer.getLastFollowAt();
        Set<String> tagKeys = new HashSet<>();
        for (CustomerTag tag : tags) {
            if (tag.getKey() != null) tagKeys.add(tag.getKey());
        }
        customer.setGrade(gradeContext.compute(customer, portrait, hasMeasure, followCount, lastFollowAt, tagKeys));
    }
}
