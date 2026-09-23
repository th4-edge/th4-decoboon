package com.th4.edge.decoboon.risk.context;

import com.th4.edge.decoboon.risk.configure.RiskAction;
import com.th4.edge.decoboon.risk.configure.RiskStatus;
import com.th4.edge.decoboon.risk.model.RiskDispositionLog;
import com.th4.edge.decoboon.risk.model.RiskItem;
import com.th4.edge.decoboon.risk.model.RiskLogView;
import com.th4.edge.decoboon.risk.repository.RiskDispositionLogRepository;
import com.th4.edge.decoboon.risk.repository.RiskItemRepository;
import com.th4.edge.decoboon.statistic.model.BusinessAlert;
import com.th4.edge.decoboon.statistic.configure.BusinessAlertLevel;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 风险处置闭环领域上下文。
 *
 * <p>依据用户 2026-09-23 裁决：</p>
 * <ul>
 *   <li>SLA：高 4h/24h、中 24h/48h、低 48h/7d（响应时限仅用于看板提示，落库的是处置截止时间 dueAt）；</li>
 *   <li>关闭不限角色，但<strong>必须填写处置结果</strong>；</li>
 *   <li>预警条件自行消失 → 自动 {@link RiskStatus#Void}，不计入闭环率；</li>
 *   <li>同级复现<strong>不自动重开</strong>，仅累计复现次数；仅有级别升级时自动重开并保留历史轮次；</li>
 *   <li>处置单不产生或修改任何账务事实。</li>
 * </ul>
 *
 * <p>预警是实时计算出来的、不落库；本上下文只维护「人对预警的处置事实」，
 * 用稳定键 {@code alertKey} 关联，<strong>不复制预警内容</strong>。</p>
 */
@Component
public class RiskDispositionContext {

    private static final int MAX_BATCH = 50;

    private final RiskItemRepository riskItemRepository;
    private final RiskDispositionLogRepository logRepository;
    private final EmployeeRepository employeeRepository;
    private final DataScopeContext dataScopeContext;

    public RiskDispositionContext(RiskItemRepository riskItemRepository,
                                  RiskDispositionLogRepository logRepository,
                                  EmployeeRepository employeeRepository,
                                  DataScopeContext dataScopeContext) {
        this.riskItemRepository = riskItemRepository;
        this.logRepository = logRepository;
        this.employeeRepository = employeeRepository;
        this.dataScopeContext = dataScopeContext;
    }

    // ------------------------------------------------------------------ 同步（读路径）

    /**
     * 把实时计算的预警与处置单对齐：新预警建单、级别升级自动重开、同级复现累计、条件消失自动消解。
     *
     * @return alertKey → 处置单
     */
    public Map<String, RiskItem> sync(Employee principal, List<BusinessAlert> alerts) {
        Employee current = dataScopeContext.requireEmployee(principal);
        Long comId = current.getCompany().getComId();
        LocalDateTime now = LocalDateTime.now();

        Map<String, RiskItem> existing = new LinkedHashMap<>();
        riskItemRepository.findByCompany_ComIdAndStatusIn(comId,
                        List.of(RiskStatus.Pending, RiskStatus.Handling, RiskStatus.Closed, RiskStatus.Void))
                .forEach(item -> existing.put(item.getAlertKey(), item));

        Map<String, RiskItem> result = new LinkedHashMap<>();
        List<RiskItem> dirty = new ArrayList<>();
        for (BusinessAlert alert : alerts) {
            String key = alertKey(alert);
            RiskItem item = existing.get(key);
            if (item == null) {
                item = create(current, alert, key, now);
            } else {
                applyReappearance(item, alert, now);
            }
            result.put(key, item);
            dirty.add(item);
        }

        // 条件消失：仍处于未关闭状态的处置单自动消解（不计入闭环率）
        existing.values().stream()
                .filter(item -> item.getStatus() != null && item.getStatus().open())
                .filter(item -> !result.containsKey(item.getAlertKey()))
                .forEach(item -> {
                    RiskStatus from = item.getStatus();
                    item.setStatus(RiskStatus.Void);
                    dirty.add(item);
                    append(item, current, RiskAction.Void, from, RiskStatus.Void, "预警条件已消失，自动消解", null, now);
                });

        if (!dirty.isEmpty()) {
            riskItemRepository.saveAll(dirty);
        }
        return result;
    }

    private RiskItem create(Employee current, BusinessAlert alert, String key, LocalDateTime now) {
        RiskItem item = new RiskItem();
        item.setCompany(current.getCompany());
        item.setStore(alert.getProject() == null ? null : alert.getProject().getStore());
        item.setTeam(alert.getProject() == null ? null : alert.getProject().getTeam());
        item.setOwner(alert.getOwner());
        item.setAlertKey(key);
        item.setAlertType(alert.getType());
        item.setBusinessType(alert.getBusinessType());
        item.setBusinessId(alert.getBusinessId());
        item.setProject(alert.getProject());
        item.setLevelAtCreate(alert.getLevel());
        item.setStatus(RiskStatus.Pending);
        item.setDiscoveredAt(now);
        item.setDueAt(dueAt(alert.getLevel(), now));
        item.setRoundNo(1);
        item.setRecurrenceCount(0);
        return item;
    }

    /** 已存在处置单：级别升级 → 自动重开并保留历史；同级复现 → 仅累计，不自动重开。 */
    private void applyReappearance(RiskItem item, BusinessAlert alert, LocalDateTime now) {
        BusinessAlertLevel current = alert.getLevel() == null ? BusinessAlertLevel.Low : alert.getLevel();
        BusinessAlertLevel created = item.getLevelAtCreate() == null ? BusinessAlertLevel.Low : item.getLevelAtCreate();
        if (!item.getStatus().open()) {
            if (current.ordinal() > created.ordinal()) {
                RiskStatus from = item.getStatus();
                item.setStatus(RiskStatus.Handling);
                item.setRoundNo(item.getRoundNo() + 1);
                item.setReopenReason("级别由 " + created.getName() + " 升级为 " + current.getName());
                item.setClosedAt(null);
                item.setClosedBy(null);
                item.setDueAt(dueAt(current, now));
                append(item, item.getOwner(), RiskAction.Reopen, from, RiskStatus.Handling,
                        item.getReopenReason(), null, now);
            } else {
                item.setRecurrenceCount(item.getRecurrenceCount() + 1);
            }
        }
        // 级别变化时重算处置截止时间（未关闭的单）
        if (item.getStatus().open() && current != created) {
            item.setDueAt(dueAt(current, item.getDiscoveredAt() == null ? now : item.getDiscoveredAt()));
        }
        item.setLevelAtCreate(current);
    }

    // ------------------------------------------------------------------ 动作

    public RiskItem assign(Employee principal, Long riskId, Long assigneeId, String note) {
        Employee current = dataScopeContext.requireEmployee(principal);
        RiskItem item = load(current, riskId);
        requireDisposer(current, item);
        Employee assignee = assignee(current, assigneeId);
        RiskStatus from = item.getStatus();
        item.setOwner(assignee);
        item.setStore(assignee.getStore());
        item.setTeam(assignee.getTeam());
        item.setAssignedAt(LocalDateTime.now());
        if (from == RiskStatus.Pending) {
            item.setStatus(RiskStatus.Handling);
        }
        riskItemRepository.save(item);
        append(item, current, RiskAction.Assign, from, item.getStatus(),
                note == null || note.isBlank() ? "指派给 " + assignee.getName() : note, null, LocalDateTime.now());
        return item;
    }

    public RiskItem claim(Employee principal, Long riskId, String note) {
        Employee current = dataScopeContext.requireEmployee(principal);
        RiskItem item = load(current, riskId);
        RiskStatus from = item.getStatus();
        item.setOwner(current);
        item.setStore(current.getStore());
        item.setTeam(current.getTeam());
        item.setAssignedAt(LocalDateTime.now());
        if (from == RiskStatus.Pending) {
            item.setStatus(RiskStatus.Handling);
        }
        riskItemRepository.save(item);
        append(item, current, RiskAction.Claim, from, item.getStatus(),
                note == null || note.isBlank() ? "本人认领" : note, null, LocalDateTime.now());
        return item;
    }

    public int batchAssign(Employee principal, List<Long> riskIds, Long assigneeId, String note) {
        // riskIds 允许为空时由下方校验拦截（改为 context 内校验，避免 @NotNull 先于注入执行）
        if (riskIds == null || riskIds.isEmpty()) {
            throw new DefinedException(new DefaultErrorCode("RISK_ASSIGN_EMPTY", "请选择要指派的风险"));
        }
        if (riskIds.size() > MAX_BATCH) {
            throw new DefinedException(new DefaultErrorCode("RISK_BATCH_LIMIT", "单次批量指派最多 " + MAX_BATCH + " 条"));
        }
        int count = 0;
        for (Long riskId : riskIds) {
            assign(principal, riskId, assigneeId, note);
            count++;
        }
        return count;
    }

    public RiskItem record(Employee principal, Long riskId, String note, List<String> evidenceUrls) {
        Employee current = dataScopeContext.requireEmployee(principal);
        RiskItem item = load(current, riskId);
        requireDisposer(current, item);
        requireOpen(item);
        if (note == null || note.isBlank()) {
            throw new DefinedException(new DefaultErrorCode("RISK_NOTE_REQUIRED", "请填写处置说明"));
        }
        RiskStatus from = item.getStatus();
        if (from == RiskStatus.Pending) {
            item.setStatus(RiskStatus.Handling);
        }
        riskItemRepository.save(item);
        append(item, current, RiskAction.Comment, from, item.getStatus(), note, evidenceUrls, LocalDateTime.now());
        return item;
    }

    public RiskItem urge(Employee principal, Long riskId, String note) {
        Employee current = dataScopeContext.requireEmployee(principal);
        RiskItem item = load(current, riskId);
        requireDisposer(current, item);
        requireOpen(item);
        append(item, current, RiskAction.Urge, item.getStatus(), item.getStatus(),
                note == null || note.isBlank() ? "催办受理人尽快处置" : note, null, LocalDateTime.now());
        return item;
    }

    /** 关闭：必须填写处置结果（用户裁决）。 */
    public RiskItem close(Employee principal, Long riskId, String result, List<String> evidenceUrls) {
        Employee current = dataScopeContext.requireEmployee(principal);
        RiskItem item = load(current, riskId);
        requireDisposer(current, item);
        requireOpen(item);
        if (result == null || result.isBlank()) {
            throw new DefinedException(new DefaultErrorCode("RISK_CLOSE_RESULT_REQUIRED", "关闭风险必须填写处置结果"));
        }
        RiskStatus from = item.getStatus();
        item.setStatus(RiskStatus.Closed);
        item.setClosedAt(LocalDateTime.now());
        item.setClosedBy(current);
        item.setCloseResult(result);
        riskItemRepository.save(item);
        append(item, current, RiskAction.Close, from, RiskStatus.Closed, result, evidenceUrls, LocalDateTime.now());
        return item;
    }

    /** 人工重开：必须填写重开原因，轮次递增，历史保留。 */
    public RiskItem reopen(Employee principal, Long riskId, String reason) {
        Employee current = dataScopeContext.requireEmployee(principal);
        RiskItem item = load(current, riskId);
        requireDisposer(current, item);
        if (item.getStatus() == RiskStatus.Void) {
            throw new DefinedException(new DefaultErrorCode("RISK_VOID_CANNOT_REOPEN", "已消解的风险不可重开"));
        }
        if (item.getStatus().open()) {
            throw new DefinedException(new DefaultErrorCode("RISK_STILL_OPEN", "该风险仍在处理中，无需重开"));
        }
        if (reason == null || reason.isBlank()) {
            throw new DefinedException(new DefaultErrorCode("RISK_REOPEN_REASON_REQUIRED", "重开必须填写原因"));
        }
        RiskStatus from = item.getStatus();
        item.setStatus(RiskStatus.Handling);
        item.setRoundNo(item.getRoundNo() + 1);
        item.setReopenReason(reason);
        item.setClosedAt(null);
        item.setClosedBy(null);
        item.setDueAt(dueAt(item.getLevelAtCreate(), LocalDateTime.now()));
        riskItemRepository.save(item);
        append(item, current, RiskAction.Reopen, from, RiskStatus.Handling, reason, null, LocalDateTime.now());
        return item;
    }

    public List<RiskLogView> logs(Employee principal, Long riskId) {
        Employee current = dataScopeContext.requireEmployee(principal);
        RiskItem item = load(current, riskId);
        return logRepository.findByRisk_RiskIdOrderByLogIdAsc(item.getRiskId()).stream()
                .map(this::toView)
                .toList();
    }

    // ------------------------------------------------------------------ 权限与校验

    /** 处置权：企业负责人/财务可全企业；店长限本店；其余角色仅限本人负责的风险。 */
    private void requireDisposer(Employee current, RiskItem item) {
        if (current.getRole() == EmployeeRole.Boss || current.getRole() == EmployeeRole.Finance) {
            return;
        }
        if (current.getRole() == EmployeeRole.StoreManager) {
            Long storeId = current.getStore() == null ? null : current.getStore().getStoreId();
            Long itemStoreId = item.getStore() == null ? null : item.getStore().getStoreId();
            if (storeId != null && storeId.equals(itemStoreId)) {
                return;
            }
            throw new DefinedException(new DefaultErrorCode("RISK_FORBIDDEN_STORE", "仅可处置本门店的风险"));
        }
        if (item.getOwner() != null && Objects.equals(item.getOwner().getEmpId(), current.getEmpId())) {
            return;
        }
        throw new DefinedException(new DefaultErrorCode("RISK_FORBIDDEN_OWNER", "仅可处置本人负责的风险"));
    }

    private RiskItem load(Employee current, Long riskId) {
        if (riskId == null) {
            throw new DefinedException(new DefaultErrorCode("RISK_ID_REQUIRED", "缺少风险处置单标识"));
        }
        RiskItem item = riskItemRepository.findById(riskId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("RISK_NOT_FOUND", "风险处置单不存在")));
        if (item.getCompany() == null || !Objects.equals(item.getCompany().getComId(), current.getCompany().getComId())) {
            throw new DefinedException(new DefaultErrorCode("RISK_FORBIDDEN", "无权访问该风险处置单"));
        }
        return item;
    }

    private Employee assignee(Employee current, Long assigneeId) {
        if (assigneeId == null) {
            throw new DefinedException(new DefaultErrorCode("RISK_ASSIGNEE_REQUIRED", "请选择受理人"));
        }
        Employee assignee = employeeRepository.findById(assigneeId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("RISK_ASSIGNEE_NOT_FOUND", "受理人不存在")));
        if (assignee.getCompany() == null
                || !Objects.equals(assignee.getCompany().getComId(), current.getCompany().getComId())) {
            throw new DefinedException(new DefaultErrorCode("RISK_ASSIGNEE_CROSS_COMPANY", "只能指派给本企业员工"));
        }
        return assignee;
    }

    private void requireOpen(RiskItem item) {
        if (!item.getStatus().open()) {
            throw new DefinedException(new DefaultErrorCode("RISK_ALREADY_FINISHED", "该风险已结束，无法继续处置"));
        }
    }

    // ------------------------------------------------------------------ 工具

    private void append(RiskItem item, Employee operator, RiskAction action,
                        RiskStatus from, RiskStatus to, String note, List<String> evidence, LocalDateTime now) {
        RiskDispositionLog log = new RiskDispositionLog();
        log.setCompany(item.getCompany());
        log.setRisk(item);
        log.setAction(action);
        log.setFromStatus(from);
        log.setToStatus(to);
        log.setOperator(operator);
        log.setNote(note);
        log.setEvidenceUrls(evidence);
        log.setOccurredAt(now);
        logRepository.save(log);
    }

    private RiskLogView toView(RiskDispositionLog log) {
        RiskLogView view = new RiskLogView();
        view.setLogId(log.getLogId());
        view.setAction(log.getAction() == null ? null : log.getAction().name());
        view.setActionLabel(log.getAction() == null ? null : log.getAction().getName());
        view.setFromStatusLabel(log.getFromStatus() == null ? null : log.getFromStatus().getName());
        view.setToStatusLabel(log.getToStatus() == null ? null : log.getToStatus().getName());
        view.setOperatorName(log.getOperator() == null ? null : log.getOperator().getName());
        view.setNote(log.getNote());
        view.setEvidenceUrls(log.getEvidenceUrls());
        view.setOccurredAt(log.getOccurredAt());
        return view;
    }

    /** 稳定预警键：不含日期，跨周期保持不变。 */
    public String alertKey(BusinessAlert alert) {
        return alert.getType().name() + ":" + alert.getBusinessType() + ":" + alert.getBusinessId();
    }

    /** 处置 SLA：高 24h、中 48h、低 7d（响应时限 4h/24h/48h 由看板提示，不落库）。 */
    private LocalDateTime dueAt(BusinessAlertLevel level, LocalDateTime from) {
        Duration sla = switch (level == null ? BusinessAlertLevel.Low : level) {
            case High -> Duration.ofHours(24);
            case Medium -> Duration.ofHours(48);
            case Low -> Duration.ofDays(7);
        };
        return from.plus(sla);
    }
}
