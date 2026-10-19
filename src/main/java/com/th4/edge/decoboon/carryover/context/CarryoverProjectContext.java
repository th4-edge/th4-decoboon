package com.th4.edge.decoboon.carryover.context;

import com.th4.edge.components.lock.DistributedLock;

import com.th4.edge.decoboon.carryover.configure.CarryoverBatchStatus;
import com.th4.edge.decoboon.carryover.configure.CarryoverConfirmationRole;
import com.th4.edge.decoboon.carryover.configure.CarryoverCostStatus;
import com.th4.edge.decoboon.carryover.configure.CarryoverProjectStatus;
import com.th4.edge.decoboon.carryover.model.CarryoverBatch;
import com.th4.edge.decoboon.carryover.model.CarryoverConfirmation;
import com.th4.edge.decoboon.carryover.model.CarryoverProject;
import com.th4.edge.decoboon.carryover.repository.CarryoverBatchRepository;
import com.th4.edge.decoboon.carryover.repository.CarryoverConfirmationRepository;
import com.th4.edge.decoboon.carryover.repository.CarryoverProjectRepository;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** 承接项目草稿、校验和确认。 */
@Note("承接项目草稿与确认")
@Component
public class CarryoverProjectContext {

    private static final List<CarryoverConfirmationRole> REQUIRED_ROLES = List.of(
            CarryoverConfirmationRole.Boss,
            CarryoverConfirmationRole.Finance,
            CarryoverConfirmationRole.Pm);

    private final CarryoverBatchRepository batchRepository;
    private final CarryoverProjectRepository projectRepository;
    private final CarryoverConfirmationRepository confirmationRepository;
    private final EmployeeRepository employeeRepository;

    private final DistributedLock distributedLock;

    public CarryoverProjectContext(CarryoverBatchRepository batchRepository,
                                   CarryoverProjectRepository projectRepository,
                                   CarryoverConfirmationRepository confirmationRepository,
                                   EmployeeRepository employeeRepository,
                DistributedLock distributedLock) {
        this.batchRepository = batchRepository;
        this.projectRepository = projectRepository;
        this.confirmationRepository = confirmationRepository;
        this.employeeRepository = employeeRepository;
        this.distributedLock = distributedLock;
    }

    public CarryoverBatch ensureActiveBatch(Employee current, LocalDate asOfDate) {
        return batchRepository.findFirstByCompany_ComIdAndStatusOrderByBatchIdDesc(
                        current.getCompany().getComId(), CarryoverBatchStatus.Active)
                .orElseGet(() -> {
                    CarryoverBatch batch = new CarryoverBatch();
                    batch.setCompany(current.getCompany());
                    batch.setCreatedBy(current);
                    batch.setAsOfDate(asOfDate == null ? LocalDate.now() : asOfDate);
                    batch.setStatus(CarryoverBatchStatus.Active);
                    return batchRepository.save(batch);
                });
    }

    public CarryoverProject save(Employee current, CarryoverProject request) {
        if (request == null) {
            throw new DefinedException(new DefaultErrorCode("400", "承接项目不能为空"));
        }
        CarryoverProject target;
        boolean update = request.getItemId() != null;
        if (update) {
            target = projectRepository.findByItemIdAndCompany_ComId(
                            request.getItemId(), current.getCompany().getComId())
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "承接项目不存在或无权访问")));
            if (!target.getStatus().editable()) {
                throw new DefinedException(new DefaultErrorCode("400", "当前承接项目已不能直接编辑"));
            }
            target.setVersion((target.getVersion() == null ? 1 : target.getVersion()) + 1);
        } else {
            target = new CarryoverProject();
            target.setCompany(current.getCompany());
            CarryoverBatch batch = request.getBatch();
            if (batch == null || batch.getBatchId() == null) {
                batch = ensureActiveBatch(current, null);
            } else {
                batch = batchRepository.findByBatchIdAndCompany_ComId(batch.getBatchId(), current.getCompany().getComId())
                        .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "承接任务不存在或无权访问")));
            }
            target.setBatch(batch);
            target.setStatus(CarryoverProjectStatus.Draft);
            target.setVersion(1);
        }
        copyEditableFields(target, request, current);
        target.setStatus(validate(target).isEmpty() ? CarryoverProjectStatus.Ready : CarryoverProjectStatus.Draft);
        return projectRepository.save(target);
    }

    public List<String> validate(CarryoverProject item) {
        List<String> blockers = new ArrayList<>();
        if (item == null) {
            blockers.add("承接项目不存在");
            return blockers;
        }
        required(blockers, item.getCustomerName(), "客户姓名");
        required(blockers, item.getCustomerPhone(), "客户手机号");
        required(blockers, item.getAddress(), "房屋地址");
        if (item.getOwner() == null) blockers.add("经营负责人");
        if (item.getManager() == null) blockers.add("项目经理");
        required(blockers, item.getContractNo(), "合同编号");
        if (item.getCurrentAmount() == null || item.getCurrentAmount().signum() <= 0) blockers.add("当前合同金额");
        if (item.getSignedAt() == null) blockers.add("签约日期");
        if (item.getProjectStatus() == null) blockers.add("项目状态");
        required(blockers, item.getCurrentStageName(), "当前施工阶段");
        if (item.getProgress() == null || item.getProgress() < 0 || item.getProgress() > 100) blockers.add("当前进度");
        required(blockers, item.getRemainingWork(), "剩余工作摘要");
        if (!Boolean.TRUE.equals(item.getReceivableConfirmed())
                || item.getReceivableOutstanding() == null
                || item.getReceivableOutstanding().signum() < 0) {
            blockers.add("期初应收确认");
        } else if (item.getCurrentAmount() != null && item.getCurrentAmount().signum() > 0
                && item.getReceivableOutstanding().compareTo(item.getCurrentAmount()) > 0) {
            blockers.add("期初应收不能超过当前合同金额");
        }
        if (!Boolean.TRUE.equals(item.getPayableConfirmed())
                || item.getPayableOutstanding() == null
                || item.getPayableOutstanding().signum() < 0) {
            blockers.add("期初应付确认");
        }
        CarryoverCostStatus costStatus = item.getCostStatus() == null ? CarryoverCostStatus.Unknown : item.getCostStatus();
        if (costStatus != CarryoverCostStatus.Unknown && item.getCostTotal() == null) {
            blockers.add("接管前成本总额");
        }
        return blockers;
    }

    public CarryoverConfirmation confirm(Employee principal, Long itemId, CarryoverConfirmationRole role,
                                         String summary) {
        if (role == null) {
            throw new DefinedException(new DefaultErrorCode("400", "确认角色不能为空"));
        }
        String lockKey = "carryover:" + principal.getCompany().getComId() + ":" + itemId;
        if (!distributedLock.tryLock(lockKey, 1000, 30000)) {
            throw new DefinedException(new DefaultErrorCode("409", "承接项目正在处理中，请稍后重试"));
        }
        try {
        CarryoverProject item = projectRepository.findByItemId(itemId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "承接项目不存在或无权访问")));
        if (item.getCompany() == null || !item.getCompany().getComId().equals(principal.getCompany().getComId())) {
            throw new DefinedException(new DefaultErrorCode("404", "承接项目不存在或无权访问"));
        }
        if (principal.getRole() == EmployeeRole.StoreManager) {
            Long currentStoreId = principal.getStore() == null ? null : principal.getStore().getStoreId();
            Long itemStoreId = item.getStore() == null ? null : item.getStore().getStoreId();
            if (currentStoreId == null || !currentStoreId.equals(itemStoreId)) {
                throw new DefinedException(new DefaultErrorCode("404", "承接项目不存在或无权访问"));
            }
        }
        boolean managerProxy = principal.getRole() == EmployeeRole.StoreManager;
        if (principal.getRole() != EmployeeRole.Boss && principal.getRole() != EmployeeRole.Finance
                && !managerProxy && !assignedTo(item, principal, role)) {
            throw new DefinedException(new DefaultErrorCode("403", "当前员工不是该承接项目的责任成员"));
        }
        boolean proxy = (principal.getRole() == EmployeeRole.Boss && role != CarryoverConfirmationRole.Boss)
                || managerProxy;
        if (!proxy && !roleMatches(principal.getRole(), role)) {
            throw new DefinedException(new DefaultErrorCode("403", "当前角色不能执行该确认"));
        }
        if (!validate(item).isEmpty()) {
            throw new DefinedException(new DefaultErrorCode("400", "承接项目仍有待补齐事项，不能确认"));
        }
        CarryoverConfirmation confirmation = confirmationRepository
                .findByItem_ItemIdAndRequiredRoleAndConfirmedVersion(itemId, role, item.getVersion())
                .orElseGet(CarryoverConfirmation::new);
        confirmation.setCompany(principal.getCompany());
        confirmation.setItem(item);
        confirmation.setRequiredRole(role);
        confirmation.setConfirmedBy(principal);
        confirmation.setProxy(proxy);
        confirmation.setConfirmedVersion(item.getVersion());
        confirmation.setConfirmedAt(LocalDateTime.now());
        confirmation.setSummary(summary);
        CarryoverConfirmation saved = confirmationRepository.save(confirmation);
        boolean allConfirmed = REQUIRED_ROLES.stream().allMatch(required ->
                confirmationRepository.findByItem_ItemIdAndRequiredRoleAndConfirmedVersion(
                        itemId, required, item.getVersion()).isPresent());
        if (allConfirmed) {
            item.setStatus(CarryoverProjectStatus.Confirmed);
            projectRepository.save(item);
        }
        return saved;
        } finally {
            distributedLock.unlock(lockKey);
        }
    }

    public CarryoverProject withdraw(Employee principal, Long itemId, String reason) {
        String lockKey = "carryover:" + principal.getCompany().getComId() + ":" + itemId;
        if (!distributedLock.tryLock(lockKey, 1000, 30000)) {
            throw new DefinedException(new DefaultErrorCode("409", "承接项目正在处理中，请稍后重试"));
        }
        try {
        CarryoverProject item = projectRepository.findByItemId(itemId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "承接项目不存在或无权访问")));
        if (item.getCompany() == null || !item.getCompany().getComId().equals(principal.getCompany().getComId())) {
            throw new DefinedException(new DefaultErrorCode("404", "承接项目不存在或无权访问"));
        }
        if (item.getLockedAt() != null) {
            throw new DefinedException(new DefaultErrorCode("400", "项目已发生首笔业务，不能撤回，只能调整或冲正"));
        }
        if (item.getStatus() == CarryoverProjectStatus.Applied) {
            throw new DefinedException(new DefaultErrorCode("400", "已承接项目未锁定前请使用专用撤回流程"));
        }
        item.setStatus(CarryoverProjectStatus.Cancelled);
        item.setCancelledAt(LocalDateTime.now());
        item.setCancelReason(reason);
        return projectRepository.save(item);
        } finally {
            distributedLock.unlock(lockKey);
        }
    }

    private void copyEditableFields(CarryoverProject target, CarryoverProject source, Employee current) {
        target.setCustomerName(trim(source.getCustomerName()));
        target.setCustomerPhone(trim(source.getCustomerPhone()));
        target.setCommunity(trim(source.getCommunity()));
        target.setAddress(trim(source.getAddress()));
        target.setArea(source.getArea());
        Employee owner = resolveEmployee(current, source.getOwner());
        Employee manager = resolveEmployee(current, source.getManager());
        Employee designer = resolveEmployee(current, source.getDesigner());
        target.setOwner(owner == null ? current : owner);
        target.setManager(manager);
        target.setDesigner(designer);
        target.setStore(target.getOwner() == null ? null : target.getOwner().getStore());
        target.setTeam(target.getOwner() == null ? null : target.getOwner().getTeam());
        target.setContractNo(trim(source.getContractNo()));
        target.setSignedAmount(source.getSignedAmount());
        target.setCurrentAmount(source.getCurrentAmount());
        target.setSignedAt(source.getSignedAt());
        target.setStartDate(source.getStartDate());
        target.setPlanEndDate(source.getPlanEndDate());
        target.setProjectName(trim(source.getProjectName()));
        target.setProjectStatus(source.getProjectStatus());
        target.setCurrentStageName(trim(source.getCurrentStageName()));
        target.setProgress(source.getProgress() == null ? 0 : Math.max(0, Math.min(100, source.getProgress())));
        target.setRemainingWork(trim(source.getRemainingWork()));
        target.setCurrentIssue(trim(source.getCurrentIssue()));
        target.setReceivableOutstanding(source.getReceivableOutstanding());
        target.setReceivableDueDate(source.getReceivableDueDate());
        target.setReceivableSourceNo(trim(source.getReceivableSourceNo()));
        target.setReceivableConfirmed(Boolean.TRUE.equals(source.getReceivableConfirmed()));
        target.setPayableOutstanding(source.getPayableOutstanding());
        target.setPayableDueDate(source.getPayableDueDate());
        target.setPayablePayeeName(trim(source.getPayablePayeeName()));
        target.setPayableSourceNo(trim(source.getPayableSourceNo()));
        target.setPayableConfirmed(Boolean.TRUE.equals(source.getPayableConfirmed()));
        target.setCostTotal(source.getCostTotal());
        target.setCostStatus(source.getCostStatus() == null ? CarryoverCostStatus.Unknown : source.getCostStatus());
        target.setCostMissingNote(trim(source.getCostMissingNote()));
        target.setLegacyId(trim(source.getLegacyId()));
        target.setLegacyProjectNo(trim(source.getLegacyProjectNo()));
        target.setLegacyAccountNote(trim(source.getLegacyAccountNote()));
    }

    private Employee resolveEmployee(Employee current, Employee source) {
        if (source == null || source.getEmpId() == null) return null;
        if (Objects.equals(source.getEmpId(), current.getEmpId())) return current;
        Employee employee = employeeRepository.findByEmpIdAndCompany_ComId(
                source.getEmpId(), current.getCompany().getComId());
        if (employee == null) {
            throw new DefinedException(new DefaultErrorCode("400", "员工不存在或不属于当前企业"));
        }
        return employee;
    }

    private boolean assignedTo(CarryoverProject item, Employee employee, CarryoverConfirmationRole role) {
        Long empId = employee.getEmpId();
        if (empId == null) return false;
        return switch (role) {
            case Pm -> item.getManager() != null && empId.equals(item.getManager().getEmpId());
            case Designer -> item.getDesigner() != null && empId.equals(item.getDesigner().getEmpId());
            case Boss, Finance -> item.getOwner() != null && empId.equals(item.getOwner().getEmpId());
        };
    }

    private boolean roleMatches(EmployeeRole actual, CarryoverConfirmationRole required) {
        return switch (required) {
            case Boss -> actual == EmployeeRole.Boss;
            case Finance -> actual == EmployeeRole.Finance;
            case Pm -> actual == EmployeeRole.Pm;
            case Designer -> actual == EmployeeRole.Designer;
        };
    }

    private void required(List<String> blockers, String value, String label) {
        if (value == null || value.isBlank()) blockers.add(label);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
