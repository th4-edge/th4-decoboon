package com.th4.edge.decoboon.statistic.service.workbench.design.page.support;

import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.design.configure.DesignStatus;
import com.th4.edge.decoboon.design.context.DesignPriceContext;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.Requirement;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import com.th4.edge.decoboon.design.repository.RequirementRepository;
import com.th4.edge.decoboon.property.configure.MeasureStatus;
import com.th4.edge.decoboon.property.configure.PropertyStatus;
import com.th4.edge.decoboon.property.model.Measure;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.decoboon.statistic.model.DesignWorkbenchItem;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 设计工作台实现：设计师看本人，店长看本店，老板看全企业。 */
@Note("设计工作台")
@Properties(allow = {
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "status"}),
        @AllowProperties(model = Design.class,
                properties = {"designId", "no", "name", "version", "status", "updateTime"}),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "community", "address", "area"})
})
@Service("statistic/workbench/design/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.statistic.service.workbench.design.page.Page {

    private static final Set<CustStatus> DESIGN_STAGE_CUSTOMER_STATUSES = EnumSet.of(
            CustStatus.New, CustStatus.Following, CustStatus.PendingMeasure, CustStatus.Quoted);
    private static final Set<PropertyStatus> CLOSED_PROPERTY_STATUSES = EnumSet.of(
            PropertyStatus.Contracted, PropertyStatus.Serving, PropertyStatus.Completed);

    private final TenantContext tenantContext;
    private final CustomerRepository customerRepository;
    private final DesignRepository designRepository;
    private final RequirementRepository requirementRepository;
    private final PropertyRepository propertyRepository;
    private final MeasureRepository measureRepository;
    private final QuoteRepository quoteRepository;
    private final DesignPriceContext priceContext;

    public Page(TenantContext tenantContext,
                CustomerRepository customerRepository,
                DesignRepository designRepository,
                RequirementRepository requirementRepository,
                PropertyRepository propertyRepository,
                MeasureRepository measureRepository,
                QuoteRepository quoteRepository,
                DesignPriceContext priceContext) {
        this.tenantContext = tenantContext;
        this.customerRepository = customerRepository;
        this.designRepository = designRepository;
        this.requirementRepository = requirementRepository;
        this.propertyRepository = propertyRepository;
        this.measureRepository = measureRepository;
        this.quoteRepository = quoteRepository;
        this.priceContext = priceContext;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DesignWorkbenchItem> call(@User Employee employee) {
        Employee current = tenantContext.requireEmployee(employee);
        Long comId = current.getCompany().getComId();
        Long empId = current.getEmpId();
        boolean boss = current.getRole() == EmployeeRole.Boss;
        boolean storeManager = current.getRole() == EmployeeRole.StoreManager;
        Long storeId = current.getStore() == null ? null : current.getStore().getStoreId();
        if (storeManager && storeId == null) {
            return List.of();
        }

        List<Customer> assignedCustomers = boss
                ? customerRepository.findByCompany_ComId(comId)
                : storeManager
                ? customerRepository.findByCompany_ComIdAndStore_StoreIdOrderByCustIdDesc(comId, storeId)
                : customerRepository.findByCompany_ComIdAndDesigner_EmpId(comId, empId);
        List<Property> assignedProperties = boss
                ? List.of()
                : storeManager
                ? propertyRepository.findByCompany_ComIdAndStore_StoreIdOrderByPropertyIdDesc(comId, storeId)
                : propertyRepository.findByCompany_ComIdAndDesigner_EmpIdOrderByPropertyIdDesc(comId, empId);
        if (assignedCustomers.isEmpty() && assignedProperties.isEmpty()) {
            return List.of();
        }

        Map<Long, Customer> customerById = assignedCustomers.stream()
                .collect(Collectors.toMap(Customer::getCustId, Function.identity(), (left, right) -> left));
        Map<Long, Property> propertyById = assignedProperties.stream()
                .collect(Collectors.toMap(Property::getPropertyId, Function.identity(), (left, right) -> left));

        if (!assignedCustomers.isEmpty()) {
            List<Long> assignedCustomerIds = assignedCustomers.stream().map(Customer::getCustId).toList();
            propertyRepository.findByCompany_ComIdAndCustomer_CustIdIn(comId, assignedCustomerIds)
                    .forEach(property -> propertyById.putIfAbsent(property.getPropertyId(), property));
        }

        List<Property> properties = propertyById.values().stream()
                .filter(property -> property.getPropertyId() != null)
                .filter(property -> property.getCompany() != null && comId.equals(property.getCompany().getComId()))
                .filter(property -> !CLOSED_PROPERTY_STATUSES.contains(property.getStatus()))
                .filter(property -> property.getCustomer() != null)
                .filter(property -> boss || (storeManager
                        ? inStore(property, storeId)
                        : assignedTo(property, current, customerById)))
                .filter(property -> DESIGN_STAGE_CUSTOMER_STATUSES.contains(property.getCustomer().getStatus()))
                .sorted(Comparator.comparing(Property::getPropertyId))
                .toList();
        if (properties.isEmpty()) {
            return List.of();
        }

        List<Long> candidatePropertyIds = properties.stream().map(Property::getPropertyId).toList();
        Set<Long> confirmedPropertyIds = quoteRepository
                .findByCompany_ComIdAndProperty_PropertyIdInAndStatusIn(
                        comId, candidatePropertyIds, List.of(QuoteStatus.Accepted, QuoteStatus.Converted))
                .stream()
                .filter(quote -> quote.getProperty() != null)
                .map(quote -> quote.getProperty().getPropertyId())
                .collect(Collectors.toSet());
        properties = properties.stream()
                .filter(property -> !confirmedPropertyIds.contains(property.getPropertyId()))
                .toList();
        if (properties.isEmpty()) {
            return List.of();
        }

        List<Long> propertyIds = properties.stream().map(Property::getPropertyId).toList();
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

        List<DesignWorkbenchItem> items = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (Property property : properties) {
            Customer customer = property.getCustomer();
            List<Design> designs = designsByProperty.getOrDefault(property.getPropertyId(), List.of());
            List<Measure> measures = measuresByProperty.getOrDefault(property.getPropertyId(), List.of());
            LocalDateTime measureAt = latestCompletedMeasureAt(measures);
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
            item.setCustomerStatus(customer.getStatus().name());
            item.setCustomerStatusName(customer.getStatus().getName());
            item.setMeasureAt(measureAt);
            item.setPendingMeasureAt(pendingMeasureAt);
            item.setSchemeCount(designs.size());
            item.setHasRequirement(!requirementsByProperty.getOrDefault(property.getPropertyId(), List.of()).isEmpty());
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
            items.add(item);
        }

        items.sort(Comparator.comparing(DesignWorkbenchItem::getStayDays, Comparator.reverseOrder())
                .thenComparing(DesignWorkbenchItem::getPropertyId, Comparator.reverseOrder()));
        return items;
    }

    private boolean inStore(Property property, Long storeId) {
        return property.getStore() != null && storeId.equals(property.getStore().getStoreId());
    }

    private boolean assignedTo(Property property, Employee current, Map<Long, Customer> assignedCustomers) {
        Customer customer = property.getCustomer();
        if (sameEmployee(property.getDesigner(), current)) {
            return true;
        }
        if (customer == null) {
            return false;
        }
        return sameEmployee(customer.getDesigner(), current)
                || assignedCustomers.containsKey(customer.getCustId());
    }

    private boolean sameEmployee(Employee employee, Employee current) {
        return employee != null && Objects.equals(employee.getEmpId(), current.getEmpId());
    }

    private LocalDateTime latestCompletedMeasureAt(List<Measure> measures) {
        return measures.stream()
                .filter(measure -> measure.getStatus() == MeasureStatus.Completed)
                .map(Measure::getMeasureTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }

    private LocalDateTime nextScheduledMeasureAt(List<Measure> measures, LocalDateTime now) {
        List<LocalDateTime> scheduled = measures.stream()
                .filter(measure -> measure.getStatus() == MeasureStatus.Scheduled)
                .map(Measure::getMeasureTime)
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
        if (measureAt != null) {
            return measureAt;
        }
        if (pendingMeasureAt != null) {
            return pendingMeasureAt;
        }
        if (lastDesignAt != null) {
            return lastDesignAt;
        }
        if (customer.getHandedOffAt() != null) {
            return customer.getHandedOffAt();
        }
        if (property.getCreateTime() != null) {
            return property.getCreateTime().toLocalDateTime();
        }
        return now;
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }
}
