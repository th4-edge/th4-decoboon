package com.th4.edge.decoboon.customer.context;

import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.configure.CustomerDormantReason;
import com.th4.edge.decoboon.customer.configure.CustomerStatusChangeType;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerStatusLog;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.customer.repository.CustomerStatusLogRepository;
import com.th4.edge.decoboon.project.configure.ContractStatus;
import com.th4.edge.decoboon.property.configure.MeasureStatus;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

/** 客户休眠状态规则与状态日志的唯一领域实现。 */
@Component
public class CustomerDormantContext {

    private static final Set<CustStatus> ALLOWED_SOURCE_STATUSES = EnumSet.of(
            CustStatus.New, CustStatus.Following, CustStatus.PendingMeasure, CustStatus.Quoted);
    private static final Set<CustStatus> ALLOWED_RESTORE_STATUSES = EnumSet.of(
            CustStatus.New, CustStatus.Following, CustStatus.PendingMeasure, CustStatus.Quoted);
    private static final Set<ContractStatus> TERMINAL_CONTRACT_STATUSES = EnumSet.of(
            ContractStatus.Closed, ContractStatus.Terminated);

    private final CustomerRepository customerRepository;
    private final CustomerStatusLogRepository statusLogRepository;
    private final ProjectContractRepository contractRepository;
    private final MeasureRepository measureRepository;

    public CustomerDormantContext(CustomerRepository customerRepository,
                                  CustomerStatusLogRepository statusLogRepository,
                                  ProjectContractRepository contractRepository,
                                  MeasureRepository measureRepository) {
        this.customerRepository = customerRepository;
        this.statusLogRepository = statusLogRepository;
        this.contractRepository = contractRepository;
        this.measureRepository = measureRepository;
    }

    public void requireWritableRole(Employee current) {
        if (current == null || current.getRole() == null
                || (current.getRole() != EmployeeRole.Boss
                && current.getRole() != EmployeeRole.StoreManager
                && current.getRole() != EmployeeRole.Marketer
                && current.getRole() != EmployeeRole.Designer)) {
            throw new DefinedException(new DefaultErrorCode("403", "仅业务员、店长、企业负责人或设计师可以设置休眠客户"));
        }
    }

    public Customer setDormant(Customer customer,
                               Employee current,
                               CustomerDormantReason reason,
                               String reasonText,
                               CustStatus expectedStatus,
                               String requestId) {
        requireWritableRole(current);
        Long comId = current.getCompany().getComId();
        CustomerStatusLog replayed = replay(comId, customer.getCustId(), requestId);
        if (replayed != null) return customer;
        if (customer.getStatus() == CustStatus.Dormant) return customer;
        if (expectedStatus != null && expectedStatus != customer.getStatus()) {
            throw new DefinedException(new DefaultErrorCode("409", "客户状态已变化，请刷新后重试"));
        }
        if (reason == null) {
            throw new DefinedException(new DefaultErrorCode("400", "请选择休眠原因"));
        }
        String detail = trim(reasonText);
        if (reason == CustomerDormantReason.Other && detail == null) {
            throw new DefinedException(new DefaultErrorCode("400", "选择“其他”时必须填写休眠原因说明"));
        }
        if (detail != null && detail.length() > 200) {
            throw new DefinedException(new DefaultErrorCode("400", "休眠原因说明不能超过200字"));
        }
        if (!ALLOWED_SOURCE_STATUSES.contains(customer.getStatus())) {
            throw new DefinedException(new DefaultErrorCode("409", "当前客户状态不允许设置休眠"));
        }
        LocalDateTime now = LocalDateTime.now();
        if (contractRepository.existsByCompany_ComIdAndCustomer_CustIdAndStatusNotIn(
                comId, customer.getCustId(), TERMINAL_CONTRACT_STATUSES)) {
            throw new DefinedException(new DefaultErrorCode("409", "客户存在未结束正式合同，不能设置休眠"));
        }
        if (measureRepository.existsByCompany_ComIdAndCustomer_CustIdAndStatusAndScheduledAtAfter(
                comId, customer.getCustId(), MeasureStatus.Scheduled, now)) {
            throw new DefinedException(new DefaultErrorCode("409", "客户存在未来量房预约，请先处理预约后休眠"));
        }

        CustStatus from = customer.getStatus();
        customer.setStatusBeforeDormant(from);
        customer.setDormantReason(reason);
        customer.setDormantReasonText(detail);
        customer.setDormantAt(now);
        customer.setDormantBy(current);
        customer.setRestoredAt(null);
        customer.setRestoredBy(null);
        customer.setNextFollowAt(null);
        customer.setStatus(CustStatus.Dormant);
        Customer saved = customerRepository.save(customer);
        saveLog(saved, current, from, CustStatus.Dormant, CustomerStatusChangeType.Dormant,
                reason, detail, requestId, now);
        return saved;
    }

    public Customer restore(Customer customer,
                            Employee current,
                            CustStatus restoreStatus,
                            LocalDateTime nextFollowAt,
                            String requestId) {
        requireWritableRole(current);
        Long comId = current.getCompany().getComId();
        CustomerStatusLog replayed = replay(comId, customer.getCustId(), requestId);
        if (replayed != null) return customer;
        if (customer.getStatus() != CustStatus.Dormant) return customer;

        CustStatus target = restoreStatus == null ? customer.getStatusBeforeDormant() : restoreStatus;
        if (target == null || !ALLOWED_RESTORE_STATUSES.contains(target)) {
            throw new DefinedException(new DefaultErrorCode("409", "无法确定恢复后的客户状态，请刷新后重试"));
        }
        if (contractRepository.existsByCompany_ComIdAndCustomer_CustIdAndStatusNotIn(
                comId, customer.getCustId(), TERMINAL_CONTRACT_STATUSES)) {
            throw new DefinedException(new DefaultErrorCode("409", "客户存在未结束正式合同，不能恢复为签约前状态"));
        }

        LocalDateTime now = LocalDateTime.now();
        CustomerStatusLog log = buildLog(customer, current, CustStatus.Dormant, target,
                CustomerStatusChangeType.Restore, null, "手动恢复跟进", requestId, now);
        customer.setStatus(target);
        customer.setNextFollowAt(nextFollowAt);
        customer.setStatusBeforeDormant(null);
        customer.setDormantReason(null);
        customer.setDormantReasonText(null);
        customer.setDormantAt(null);
        customer.setDormantBy(null);
        customer.setRestoredAt(now);
        customer.setRestoredBy(current);
        Customer saved = customerRepository.save(customer);
        statusLogRepository.save(log);
        return saved;
    }

    public Customer wakeByFollow(Customer customer, Employee current) {
        if (customer.getStatus() != CustStatus.Dormant) return customer;
        CustStatus target = customer.getStatusBeforeDormant();
        if (target == null || !ALLOWED_RESTORE_STATUSES.contains(target)) {
            throw new DefinedException(new DefaultErrorCode("409", "无法确定恢复后的客户状态，请先手动恢复"));
        }
        if (contractRepository.existsByCompany_ComIdAndCustomer_CustIdAndStatusNotIn(
                current.getCompany().getComId(), customer.getCustId(), TERMINAL_CONTRACT_STATUSES)) {
            throw new DefinedException(new DefaultErrorCode("409", "客户存在未结束正式合同，不能通过跟进恢复为签约前状态"));
        }
        LocalDateTime now = LocalDateTime.now();
        CustomerStatusLog log = buildLog(customer, current, CustStatus.Dormant, target,
                CustomerStatusChangeType.FollowWake, null, "新增客户跟进，自动恢复为 " + target.getName(), null, now);
        customer.setStatus(target);
        customer.setStatusBeforeDormant(null);
        customer.setDormantReason(null);
        customer.setDormantReasonText(null);
        customer.setDormantAt(null);
        customer.setDormantBy(null);
        customer.setRestoredAt(now);
        customer.setRestoredBy(current);
        Customer saved = customerRepository.save(customer);
        statusLogRepository.save(log);
        return saved;
    }

    private CustomerStatusLog replay(Long comId, Long customerId, String requestId) {
        if (requestId == null || requestId.isBlank()) return null;
        return statusLogRepository.findByCompany_ComIdAndRequestId(comId, requestId.trim())
                .map(log -> {
                    if (log.getCustomer() != null && customerId.equals(log.getCustomer().getCustId())) {
                        return log;
                    }
                    throw new DefinedException(new DefaultErrorCode("409", "请求号已被其他客户使用"));
                })
                .orElse(null);
    }

    private void saveLog(Customer customer,
                         Employee operator,
                         CustStatus from,
                         CustStatus to,
                         CustomerStatusChangeType changeType,
                         CustomerDormantReason reason,
                         String reasonText,
                         String requestId,
                         LocalDateTime changedAt) {
        statusLogRepository.save(buildLog(customer, operator, from, to, changeType,
                reason, reasonText, requestId, changedAt));
    }

    private CustomerStatusLog buildLog(Customer customer,
                                       Employee operator,
                                       CustStatus from,
                                       CustStatus to,
                                       CustomerStatusChangeType changeType,
                                       CustomerDormantReason reason,
                                       String reasonText,
                                       String requestId,
                                       LocalDateTime changedAt) {
        CustomerStatusLog log = new CustomerStatusLog();
        log.setCompany(operator.getCompany());
        log.setCustomer(customer);
        log.setFromStatus(from);
        log.setToStatus(to);
        log.setChangeType(changeType);
        log.setReasonCode(reason);
        log.setReasonText(reasonText);
        log.setOperator(operator);
        log.setOperatorRole(operator.getRole());
        log.setRequestId(trim(requestId));
        log.setChangedAt(changedAt);
        return log;
    }

    private String trim(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
