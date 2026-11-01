package com.th4.edge.decoboon.statistic.service.workbench.design.page.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.design.configure.DesignStatus;
import com.th4.edge.decoboon.design.context.DesignPriceContext;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.Requirement;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import com.th4.edge.decoboon.design.repository.RequirementRepository;
import com.th4.edge.decoboon.property.configure.MeasureStatus;
import com.th4.edge.decoboon.property.model.Measure;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.statistic.model.DesignWorkbenchItem;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** 设计工作台实现：设计师看本人，店长看本店，老板看全企业。 */
@Note("设计工作台")
@Properties(allow = {
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "status"}),
        @AllowProperties(model = Design.class,
                properties = {"designId", "no", "name", "version", "status", "updateTime"}),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "community", "address", "area"}),
        @AllowProperties(model = com.th4.edge.decoboon.statistic.model.DesignWorkbenchItem.class, properties = {"custId", "propertyId", "customerName", "telNo", "community", "addr", "area", "customerStatus", "customerStatusName", "measureAt", "pendingMeasureAt", "designStatus", "designStatusName", "schemeCount", "hasRequirement", "lastDesignAt", "latestDesignId", "latestDesignName", "latestDesignStatusName", "calculatedAmount", "currentQuoteAmount", "currentQuoteNo", "currentQuoteStatusName", "priceSource", "stayDays"})
})
@Service("statistic/workbench/design/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.statistic.service.workbench.design.page.Page {

    @AutoInject
    private Integer current;
    @AutoInject
    private Integer size;
    @AutoInject
    private String keyword;
    /** all / Pending / Doing / Done */
    @AutoInject
    private String filter;

    private final TenantContext tenantContext;
    private final DesignRepository designRepository;
    private final RequirementRepository requirementRepository;
    private final PropertyRepository propertyRepository;
    private final MeasureRepository measureRepository;
    private final DesignPriceContext priceContext;

    public Page(TenantContext tenantContext,
                DesignRepository designRepository,
                RequirementRepository requirementRepository,
                PropertyRepository propertyRepository,
                MeasureRepository measureRepository,
                DesignPriceContext priceContext) {
        this.tenantContext = tenantContext;
        this.designRepository = designRepository;
        this.requirementRepository = requirementRepository;
        this.propertyRepository = propertyRepository;
        this.measureRepository = measureRepository;
        this.priceContext = priceContext;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<DesignWorkbenchItem> call(@User Employee employee) {
        Employee operator = tenantContext.requireEmployee(employee);
        Long comId = operator.getCompany().getComId();
        Long empId = operator.getEmpId();
        boolean boss = operator.getRole() == EmployeeRole.Boss;
        boolean storeManager = operator.getRole() == EmployeeRole.StoreManager;
        Long storeId = operator.getStore() == null ? null : operator.getStore().getStoreId();
        int pageIndex = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        PageRequest pageable = PageRequest.of(pageIndex, pageSize);
        if (storeManager && storeId == null) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        String normalizedFilter = normalizeFilter(filter);
        var anchors = propertyRepository.findDesignTaskAnchors(
                comId, empId, boss, storeManager, storeId,
                keyword == null ? "" : keyword.trim(), normalizedFilter, pageable);
        if (anchors.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, anchors.getTotalElements());
        }

        List<Long> propertyIds = anchors.getContent().stream()
                .map(PropertyRepository.DesignTaskAnchor::getPropertyId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Property> properties = propertyRepository.findByCompany_ComIdAndPropertyIdIn(comId, propertyIds)
                .stream()
                .collect(Collectors.toMap(Property::getPropertyId, item -> item, (left, right) -> left));
        List<Design> scopedDesigns = designRepository
                .findByCompany_ComIdAndProperty_PropertyIdInOrderByVersionDesc(comId, propertyIds)
                .stream()
                .filter(design -> design.getProperty() != null)
                .toList();
        priceContext.enrich(scopedDesigns);
        Map<Long, List<Design>> designsByProperty = scopedDesigns.stream()
                .collect(Collectors.groupingBy(design -> design.getProperty().getPropertyId()));
        Map<Long, List<Requirement>> requirementsByProperty = requirementRepository
                .findByCompany_ComIdAndProperty_PropertyIdInOrderByRequirementIdDesc(comId, propertyIds)
                .stream()
                .filter(requirement -> requirement.getProperty() != null)
                .collect(Collectors.groupingBy(requirement -> requirement.getProperty().getPropertyId()));
        Map<Long, List<Measure>> measuresByProperty = measureRepository
                .findByCompany_ComIdAndProperty_PropertyIdInOrderByMeasureTimeDesc(comId, propertyIds)
                .stream()
                .filter(measure -> measure.getProperty() != null)
                .collect(Collectors.groupingBy(measure -> measure.getProperty().getPropertyId()));

        LocalDateTime now = LocalDateTime.now();
        List<DesignWorkbenchItem> items = new ArrayList<>();
        for (PropertyRepository.DesignTaskAnchor anchor : anchors.getContent()) {
            Property property = properties.get(anchor.getPropertyId());
            if (property == null || property.getCustomer() == null) {
                continue;
            }
            items.add(buildItem(property,
                    designsByProperty.getOrDefault(property.getPropertyId(), List.of()),
                    measuresByProperty.getOrDefault(property.getPropertyId(), List.of()),
                    requirementsByProperty.getOrDefault(property.getPropertyId(), List.of()),
                    anchor.getMeasureAt(),
                    now));
        }
        return new PageImpl<>(items, pageable, anchors.getTotalElements());
    }

    private DesignWorkbenchItem buildItem(Property property,
                                          List<Design> designs,
                                          List<Measure> measures,
                                          List<Requirement> requirements,
                                          LocalDateTime anchorMeasureAt,
                                          LocalDateTime now) {
        Customer customer = property.getCustomer();
        LocalDateTime measureAt = latestCompletedMeasureAt(measures);
        if (measureAt == null) measureAt = anchorMeasureAt;
        if (measureAt == null) measureAt = property.getLatestMeasureAt();
        LocalDateTime pendingMeasureAt = nextScheduledMeasureAt(measures, now);
        LocalDateTime lastDesignAt = latestDesignAt(designs);

        DesignWorkbenchItem item = new DesignWorkbenchItem();
        item.setCustId(customer.getCustId());
        item.setPropertyId(property.getPropertyId());
        item.setCustomerName(customer.getName());
        item.setTelNo(customer.getTelNo());
        item.setCommunity(firstNonBlank(property.getCommunity(), customer.getCommunity()));
        item.setAddr(firstNonBlank(property.getAddress(), customer.getAddr()));
        item.setArea(property.getArea() == null ? customer.getArea() : property.getArea());
        item.setCustomerStatus(customer.getStatus() == null ? null : customer.getStatus().name());
        item.setCustomerStatusName(customer.getStatus() == null ? null : customer.getStatus().getName());
        item.setMeasureAt(measureAt);
        item.setPendingMeasureAt(pendingMeasureAt);
        item.setSchemeCount(designs.size());
        item.setHasRequirement(!requirements.isEmpty());
        item.setLastDesignAt(lastDesignAt);

        if (!designs.isEmpty()) {
            Design latest = designs.get(0);
            item.setLatestDesignId(latest.getDesignId());
            item.setLatestDesignName(latest.getName());
            item.setLatestDesignStatusName(latest.getStatus() == null ? null : latest.getStatus().getName());
            item.setCalculatedAmount(latest.getCalculatedAmount());
            item.setCurrentQuoteAmount(latest.getCurrentQuoteAmount());
            item.setCurrentQuoteNo(latest.getCurrentQuoteNo());
            item.setCurrentQuoteStatusName(latest.getCurrentQuoteStatusName());
            item.setPriceSource(latest.getCurrentQuoteId() != null
                    ? "Quote"
                    : latest.getItemCount() != null && latest.getItemCount() > 0 ? "Calculated" : "Unavailable");
        } else {
            item.setCalculatedAmount(BigDecimal.ZERO);
            item.setPriceSource("Unavailable");
        }

        if (designs.isEmpty()) {
            item.setDesignStatus("Pending");
            item.setDesignStatusName("待设计");
        } else if (designs.stream().anyMatch(design -> design.getStatus() != DesignStatus.Draft)) {
            item.setDesignStatus("Done");
            item.setDesignStatusName("方案已完成");
        } else {
            item.setDesignStatus("Doing");
            item.setDesignStatusName("设计中");
        }

        LocalDateTime stageStart = stageStart(property, customer, measureAt, pendingMeasureAt, lastDesignAt, now);
        item.setStayDays(Math.max(0, Duration.between(stageStart, now).toDays()));
        return item;
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank() || "all".equalsIgnoreCase(value)) return "all";
        return switch (value) {
            case "Pending", "Doing", "Done" -> value;
            default -> "all";
        };
    }

    private LocalDateTime latestCompletedMeasureAt(List<Measure> measures) {
        return measures.stream()
                .filter(measure -> measure.getStatus() == MeasureStatus.Completed)
                .map(measure -> measure.getCompletedAt() != null ? measure.getCompletedAt() : measure.getMeasureTime())
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }

    private LocalDateTime nextScheduledMeasureAt(List<Measure> measures, LocalDateTime now) {
        List<LocalDateTime> scheduled = measures.stream()
                .filter(measure -> measure.getStatus() == MeasureStatus.Scheduled)
                .map(measure -> measure.getScheduledAt() != null ? measure.getScheduledAt() : measure.getMeasureTime())
                .filter(Objects::nonNull)
                .toList();
        return scheduled.stream()
                .filter(measureTime -> !measureTime.isBefore(now))
                .min(LocalDateTime::compareTo)
                .orElseGet(() -> scheduled.stream().max(LocalDateTime::compareTo).orElse(null));
    }

    private LocalDateTime latestDesignAt(List<Design> designs) {
        return designs.stream()
                .map(design -> design.getUpdateTime() == null ? null : design.getUpdateTime().toLocalDateTime())
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }

    private LocalDateTime stageStart(Property property,
                                     Customer customer,
                                     LocalDateTime measureAt,
                                     LocalDateTime pendingMeasureAt,
                                     LocalDateTime lastDesignAt,
                                     LocalDateTime now) {
        if (measureAt != null) return measureAt;
        if (pendingMeasureAt != null) return pendingMeasureAt;
        if (lastDesignAt != null) return lastDesignAt;
        if (customer.getHandedOffAt() != null) return customer.getHandedOffAt();
        if (property.getCreateTime() != null) return property.getCreateTime().toLocalDateTime();
        return now;
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }
}
