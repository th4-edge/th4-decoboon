package com.th4.edge.decoboon.customer.context;

import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.configure.CustomerLossReason;
import com.th4.edge.decoboon.customer.configure.CustomerLossScope;
import com.th4.edge.decoboon.customer.configure.CustomerLossStage;
import com.th4.edge.decoboon.customer.configure.CustomerStatusChangeType;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerLoss;
import com.th4.edge.decoboon.customer.model.CustomerStatusLog;
import com.th4.edge.decoboon.customer.repository.CustomerLossRepository;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.customer.repository.CustomerStatusLogRepository;
import com.th4.edge.decoboon.project.configure.ContractStatus;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.property.configure.PropertyStatus;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 客户签约前流失与恢复的唯一领域实现。 */
@Component
public class CustomerLossContext {

    private static final Set<CustStatus> ALLOWED_LOSS_STATUSES = EnumSet.of(
            CustStatus.New, CustStatus.Following, CustStatus.PendingMeasure, CustStatus.Quoted, CustStatus.Dormant);
    private static final Set<CustStatus> ALLOWED_RESTORE_STATUSES = EnumSet.of(
            CustStatus.New, CustStatus.Following, CustStatus.PendingMeasure, CustStatus.Quoted);
    private static final Set<PropertyStatus> TERMINAL_PROPERTY_STATUSES = EnumSet.of(
            PropertyStatus.Contracted, PropertyStatus.Serving, PropertyStatus.Completed);
    private static final Set<ContractStatus> TERMINAL_CONTRACT_STATUSES = EnumSet.of(
            ContractStatus.Completed, ContractStatus.Settled, ContractStatus.Closed, ContractStatus.Terminated);

    private final CustomerRepository customerRepository;
    private final CustomerLossRepository lossRepository;
    private final CustomerStatusLogRepository statusLogRepository;
    private final PropertyRepository propertyRepository;
    private final ProjectContractRepository contractRepository;

    public CustomerLossContext(CustomerRepository customerRepository,
                               CustomerLossRepository lossRepository,
                               CustomerStatusLogRepository statusLogRepository,
                               PropertyRepository propertyRepository,
                               ProjectContractRepository contractRepository) {
        this.customerRepository = customerRepository;
        this.lossRepository = lossRepository;
        this.statusLogRepository = statusLogRepository;
        this.propertyRepository = propertyRepository;
        this.contractRepository = contractRepository;
    }

    public void requireWritableRole(Employee current) {
        if (current == null || current.getRole() == null
                || (current.getRole() != EmployeeRole.Boss
                && current.getRole() != EmployeeRole.StoreManager
                && current.getRole() != EmployeeRole.Marketer
                && current.getRole() != EmployeeRole.Designer
                && current.getRole() != EmployeeRole.Finance)) {
            throw new DefinedException(new DefaultErrorCode("403",
                    "仅业务员、店长、企业负责人、设计师或财务可以操作客户流失"));
        }
    }

    public CustomerLoss markLost(Customer customer,
                                 Employee current,
                                 CustomerLossScope scope,
                                 CustomerLossStage stage,
                                 CustomerLossReason reason,
                                 String reasonText,
                                 String competitorName,
                                 Long propertyId,
                                 CustStatus expectedStatus,
                                 String requestId) {
        requireWritableRole(current);
        Long comId = current.getCompany().getComId();
        CustomerLoss replayed = replay(comId, customer.getCustId(), requestId);
        if (replayed != null) return replayed;

        if (expectedStatus != null && expectedStatus != customer.getStatus()) {
            throw new DefinedException(new DefaultErrorCode("409", "客户状态已变化，请刷新后重试"));
        }
        if (!ALLOWED_LOSS_STATUSES.contains(customer.getStatus())) {
            throw new DefinedException(new DefaultErrorCode("409", "当前客户状态不允许标记流失；已签约后请走合同终止或关闭"));
        }
        if (stage == null) {
            throw new DefinedException(new DefaultErrorCode("400", "请选择流失阶段"));
        }
        if (reason == null) {
            throw new DefinedException(new DefaultErrorCode("400", "请选择流失原因"));
        }
        String detail = trim(reasonText);
        if (reason == CustomerLossReason.Other && detail == null) {
            throw new DefinedException(new DefaultErrorCode("400", "选择“其他”时必须填写流失原因说明"));
        }
        if (detail != null && detail.length() > 500) {
            throw new DefinedException(new DefaultErrorCode("400", "流失原因说明不能超过500字"));
        }
        String competitor = trim(competitorName);
        if (competitor != null && competitor.length() > 100) {
            throw new DefinedException(new DefaultErrorCode("400", "竞争对手名称不能超过100字"));
        }
        if (scope == null) {
            throw new DefinedException(new DefaultErrorCode("400", "请选择流失范围"));
        }
        CustomerLossScope effectiveScope = scope;
        Property property = null;
        if (effectiveScope == CustomerLossScope.Property) {
            if (propertyId == null) {
                throw new DefinedException(new DefaultErrorCode("400", "本次房屋流失必须指定房屋"));
            }
            property = propertyRepository.findByPropertyIdAndCompany_ComId(propertyId, comId)
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "房屋不存在或无权访问")));
            if (property.getCustomer() == null || !customer.getCustId().equals(property.getCustomer().getCustId())) {
                throw new DefinedException(new DefaultErrorCode("404", "房屋不存在或不属于当前客户"));
            }
            CustomerLoss existing = lossRepository
                    .findFirstByCompany_ComIdAndProperty_PropertyIdAndRestoredAtIsNullOrderByLossIdDesc(
                            comId, propertyId)
                    .orElse(null);
            if (existing != null) return existing;
        } else if (customer.getStatus() == CustStatus.Lost) {
            CustomerLoss existing = lossRepository
                    .findFirstByCompany_ComIdAndCustomer_CustIdAndScopeAndRestoredAtIsNullOrderByLossIdDesc(
                            comId, customer.getCustId(), CustomerLossScope.Customer)
                    .orElse(null);
            if (existing != null) return existing;
        }

        if (effectiveScope == CustomerLossScope.Customer
                && contractRepository.existsByCompany_ComIdAndCustomer_CustIdAndStatusNotIn(
                comId, customer.getCustId(), TERMINAL_CONTRACT_STATUSES)) {
            throw new DefinedException(new DefaultErrorCode("409", "客户存在未关闭合同，不能标记为客户整体流失"));
        }
        if (effectiveScope == CustomerLossScope.Property
                && contractRepository.existsByCompany_ComIdAndProperty_PropertyIdAndStatusNotIn(
                comId, propertyId, TERMINAL_CONTRACT_STATUSES)) {
            throw new DefinedException(new DefaultErrorCode("409", "该房屋存在未关闭合同，不能标记流失"));
        }

        LocalDateTime now = LocalDateTime.now();
        CustomerLoss loss = new CustomerLoss();
        loss.setCompany(current.getCompany());
        loss.setCustomer(customer);
        loss.setProperty(property);
        loss.setScope(effectiveScope);
        loss.setStage(stage);
        loss.setFromStatus(customer.getStatus());
        loss.setReasonCode(reason);
        loss.setReasonText(detail);
        loss.setCompetitorName(competitor);
        loss.setLostAt(now);
        loss.setLostBy(current);
        loss.setOperatorRole(current.getRole());
        loss.setRequestId(trim(requestId));
        CustomerLoss saved = lossRepository.save(loss);

        boolean customerLevelLost = effectiveScope == CustomerLossScope.Customer;
        if (effectiveScope == CustomerLossScope.Property) {
            List<Property> properties = propertyRepository
                    .findByCompany_ComIdAndCustomer_CustIdOrderByPropertyIdAsc(comId, customer.getCustId());
            Set<Long> lostPropertyIds = new java.util.HashSet<>(lossRepository.findActivePropertyIds(comId, customer.getCustId()));
            lostPropertyIds.add(propertyId);
            customerLevelLost = properties.stream()
                    .filter(item -> !TERMINAL_PROPERTY_STATUSES.contains(item.getStatus()))
                    .noneMatch(item -> !lostPropertyIds.contains(item.getPropertyId()));
        }
        if (customerLevelLost) {
            CustStatus from = customer.getStatus();
            customer.setStatus(CustStatus.Lost);
            customer.setNextFollowAt(null);
            customerRepository.save(customer);
            statusLogRepository.save(buildLog(customer, current, from, CustStatus.Lost,
                    CustomerStatusChangeType.Lost, detail, requestId, now));
        }
        return saved;
    }

    public CustomerLoss restore(CustomerLoss loss,
                                Customer customer,
                                Employee current,
                                CustStatus restoreStatus,
                                LocalDateTime nextFollowAt,
                                String restoreNote,
                                String requestId) {
        requireWritableRole(current);
        if (loss.getRestoredAt() != null) return loss;
        if (customer.getStatus() == CustStatus.Lost) {
            CustStatus target = restoreStatus;
            if (target == null && ALLOWED_RESTORE_STATUSES.contains(loss.getFromStatus())) {
                target = loss.getFromStatus();
            }
            if (target == null && loss.getFromStatus() == CustStatus.Dormant
                    && ALLOWED_RESTORE_STATUSES.contains(customer.getStatusBeforeDormant())) {
                target = customer.getStatusBeforeDormant();
            }
            if (target == null || !ALLOWED_RESTORE_STATUSES.contains(target)) {
                throw new DefinedException(new DefaultErrorCode("409", "请选择恢复后的客户阶段"));
            }
            if (contractRepository.existsByCompany_ComIdAndCustomer_CustIdAndStatusNotIn(
                    current.getCompany().getComId(), customer.getCustId(), TERMINAL_CONTRACT_STATUSES)) {
                throw new DefinedException(new DefaultErrorCode("409", "客户存在未关闭合同，不能恢复为签约前状态"));
            }
            long remaining = lossRepository
                    .countByCompany_ComIdAndCustomer_CustIdAndRestoredAtIsNull(current.getCompany().getComId(), customer.getCustId()) - 1;
            LocalDateTime now = LocalDateTime.now();
            loss.setRestoredAt(now);
            loss.setRestoredBy(current);
            loss.setRestoreStatus(target);
            loss.setRestoreNote(trim(restoreNote));
            loss.setRestoreRequestId(trim(requestId));
            CustomerLoss saved = lossRepository.save(loss);
            if (remaining <= 0) {
                customer.setStatus(target);
                customer.setNextFollowAt(nextFollowAt);
                customer.setStatusBeforeDormant(null);
                customer.setDormantReason(null);
                customer.setDormantReasonText(null);
                customer.setDormantAt(null);
                customer.setDormantBy(null);
                customer.setRestoredAt(now);
                customer.setRestoredBy(current);
                customerRepository.save(customer);
                statusLogRepository.save(buildLog(customer, current, CustStatus.Lost, target,
                        CustomerStatusChangeType.RestoreLost, trim(restoreNote), requestId, now));
            }
            return saved;
        }

        LocalDateTime now = LocalDateTime.now();
        loss.setRestoredAt(now);
        loss.setRestoredBy(current);
        loss.setRestoreStatus(customer.getStatus());
        loss.setRestoreNote(trim(restoreNote));
        loss.setRestoreRequestId(trim(requestId));
        return lossRepository.save(loss);
    }

    /** 为当前页客户批量装配流失摘要，固定一条查询且不产生 N+1。 */
    public void enrich(Collection<Customer> customers) {
        if (customers == null || customers.isEmpty()) return;
        List<Long> customerIds = customers.stream()
                .map(Customer::getCustId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (customerIds.isEmpty()) return;
        Long comId = customers.iterator().next().getCompany() == null
                ? null : customers.iterator().next().getCompany().getComId();
        if (comId == null) return;
        List<CustomerLoss> losses = lossRepository.findActiveByCustomerIds(comId, customerIds);
        Map<Long, CustomerLoss> latest = new HashMap<>();
        Map<Long, Integer> counts = new HashMap<>();
        for (CustomerLoss loss : losses) {
            if (loss.getCustomer() == null || loss.getCustomer().getCustId() == null) continue;
            Long customerId = loss.getCustomer().getCustId();
            latest.putIfAbsent(customerId, loss);
            counts.merge(customerId, 1, Integer::sum);
        }
        for (Customer customer : customers) {
            CustomerLoss loss = latest.get(customer.getCustId());
            if (loss == null) continue;
            customer.setLossId(loss.getLossId());
            customer.setLossScope(loss.getScope());
            customer.setLossStage(loss.getStage());
            customer.setLossReason(loss.getReasonCode());
            customer.setLossReasonText(loss.getReasonText());
            customer.setCompetitorName(loss.getCompetitorName());
            customer.setLostAt(loss.getLostAt());
            customer.setLostBy(loss.getLostBy());
            customer.setLossFromStatus(loss.getFromStatus());
            if (loss.getProperty() != null) {
                customer.setLossPropertyId(loss.getProperty().getPropertyId());
                customer.setLossPropertyName(propertyName(loss.getProperty()));
            }
            int count = counts.getOrDefault(customer.getCustId(), 1);
            customer.setActiveLossCount(count);
            customer.setPartialLoss(customer.getStatus() != CustStatus.Lost && count > 0);
        }
    }

    private CustomerLoss replay(Long comId, Long customerId, String requestId) {
        if (requestId == null || requestId.isBlank()) return null;
        CustomerLoss loss = lossRepository.findByCompany_ComIdAndRequestId(comId, requestId.trim())
                .orElse(null);
        if (loss == null) return null;
        if (loss.getCustomer() == null || !customerId.equals(loss.getCustomer().getCustId())) {
            throw new DefinedException(new DefaultErrorCode("409", "请求号已被其他客户使用"));
        }
        return loss;
    }

    private CustomerStatusLog buildLog(Customer customer,
                                       Employee operator,
                                       CustStatus from,
                                       CustStatus to,
                                       CustomerStatusChangeType changeType,
                                       String reasonText,
                                       String requestId,
                                       LocalDateTime changedAt) {
        CustomerStatusLog log = new CustomerStatusLog();
        log.setCompany(operator.getCompany());
        log.setCustomer(customer);
        log.setFromStatus(from);
        log.setToStatus(to);
        log.setChangeType(changeType);
        log.setReasonText(trim(reasonText));
        log.setOperator(operator);
        log.setOperatorRole(operator.getRole());
        log.setRequestId(trim(requestId));
        log.setChangedAt(changedAt);
        return log;
    }

    private String propertyName(Property property) {
        StringBuilder value = new StringBuilder();
        append(value, property.getCommunity());
        append(value, property.getBuilding());
        append(value, property.getRoomNo());
        return value.isEmpty() ? property.getAddress() : value.toString();
    }

    private void append(StringBuilder value, String part) {
        if (part == null || part.isBlank()) return;
        if (!value.isEmpty()) value.append(' ');
        value.append(part.trim());
    }

    private String trim(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
