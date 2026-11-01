package com.th4.edge.decoboon.payroll.context;

import com.th4.edge.decoboon.common.configure.BusinessOrigin;
import com.th4.edge.decoboon.finance.configure.PayableStatus;
import com.th4.edge.decoboon.finance.configure.PayableType;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.payroll.configure.PayrollItemStatus;
import com.th4.edge.decoboon.payroll.configure.PayrollSheetStatus;
import com.th4.edge.decoboon.payroll.configure.PayslipStatus;
import com.th4.edge.decoboon.payroll.model.PayrollItem;
import com.th4.edge.decoboon.payroll.model.PayrollSheet;
import com.th4.edge.decoboon.payroll.model.Payslip;
import com.th4.edge.decoboon.payroll.repository.PayrollItemRepository;
import com.th4.edge.decoboon.payroll.repository.PayrollSheetRepository;
import com.th4.edge.decoboon.payroll.repository.PayslipRepository;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import org.springframework.data.jpa.domain.Specification;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 工资单确认、员工工资应付生成和工资条准备。 */
@Component
public class PayrollSettlementContext {

    private final PayrollAccessContext accessContext;
    private final PayrollSheetRepository sheetRepository;
    private final PayrollItemRepository itemRepository;
    private final PayslipRepository payslipRepository;
    private final PayableRepository payableRepository;
    private final PayrollSettlementAuditContext auditContext;

    public PayrollSettlementContext(PayrollAccessContext accessContext,
                                    PayrollSheetRepository sheetRepository,
                                    PayrollItemRepository itemRepository,
                                    PayslipRepository payslipRepository,
                                    PayableRepository payableRepository,
                                    PayrollSettlementAuditContext auditContext) {
        this.accessContext = accessContext;
        this.sheetRepository = sheetRepository;
        this.itemRepository = itemRepository;
        this.payslipRepository = payslipRepository;
        this.payableRepository = payableRepository;
        this.auditContext = auditContext;
    }

    /** 财务/BOSS 可选复核提交：仅 Draft → Pending，重复提交幂等。 */
    @Transactional
    public PayrollSheet submitReview(Employee principal, Long sheetId) {
        Employee current = accessContext.requireEditor(principal);
        PayrollSheet sheet = requireVisibleSheet(current, sheetId);
        if (sheet.getStatus() == PayrollSheetStatus.Pending) {
            return sheet;
        }
        if (sheet.getStatus() != PayrollSheetStatus.Draft) {
            throw new DefinedException(new DefaultErrorCode("400", "当前工资单状态不允许提交复核"));
        }
        List<PayrollItem> items = itemRepository
                .findByCompany_ComIdAndSheet_SheetIdOrderByEmployee_NameAsc(
                        current.getCompany().getComId(), sheet.getSheetId());
        validateConfirmable(sheet, items);
        PayrollSheetStatus before = sheet.getStatus();
        sheet.setStatus(PayrollSheetStatus.Pending);
        PayrollSheet saved = sheetRepository.save(sheet);
        auditContext.record(current, saved, "SUBMIT_REVIEW", before, PayrollSheetStatus.Pending, null);
        return saved;
    }

    /** BOSS 退回复核工资单：仅 Pending → Draft，原因必填并留痕。 */
    @Transactional
    public PayrollSheet returnToDraft(Employee principal, Long sheetId, String reason) {
        Employee current = accessContext.requireReturnActor(principal);
        PayrollSheet sheet = requireVisibleSheet(current, sheetId);
        String normalizedReason = reason == null ? "" : reason.trim();
        if (normalizedReason.isEmpty()) {
            throw new DefinedException(new DefaultErrorCode("400", "退回原因不能为空"));
        }
        if (sheet.getStatus() == PayrollSheetStatus.Draft) {
            return sheet;
        }
        if (sheet.getStatus() != PayrollSheetStatus.Pending) {
            throw new DefinedException(new DefaultErrorCode("400", "当前工资单状态不允许退回"));
        }
        Long comId = current.getCompany().getComId();
        List<PayrollItem> items = itemRepository
                .findByCompany_ComIdAndSheet_SheetIdOrderByEmployee_NameAsc(comId, sheet.getSheetId());
        accessContext.requireStoreItemsInStore(current, items);
        PayrollSheetStatus before = sheet.getStatus();
        sheet.setStatus(PayrollSheetStatus.Draft);
        sheet.setRemark("退回原因：" + normalizedReason);
        PayrollSheet saved = sheetRepository.save(sheet);
        auditContext.record(current, saved, "RETURN_TO_DRAFT", before, PayrollSheetStatus.Draft, normalizedReason);
        return saved;
    }

    private PayrollSheet requireVisibleSheet(Employee current, Long sheetId) {
        Specification<PayrollSheet> byId = (root, query, cb) -> cb.equal(root.get("sheetId"), sheetId);
        return sheetRepository.findOne(byId.and(accessContext.visibleSheets(current)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "工资单不存在或无权访问")));
    }

    private void validateConfirmable(PayrollSheet sheet, List<PayrollItem> items) {
        if (!Boolean.TRUE.equals(sheet.getAttendanceComplete())) {
            throw new DefinedException(new DefaultErrorCode("400", "考勤未确认，工资单不可确认"));
        }
        if (items.isEmpty()) {
            throw new DefinedException(new DefaultErrorCode("400", "工资单没有工资项，不可确认"));
        }
        for (PayrollItem item : items) {
            if (item.getNetAmount() == null || item.getNetAmount().signum() < 0) {
                throw new DefinedException(new DefaultErrorCode("400", "实发为负，必须人工处理后才能确认"));
            }
        }
    }

    @Transactional
    public PayrollSheet confirm(Employee principal, Long sheetId) {
        // DEC-097：BOSS 和本店店长可确认；财务仍只负责制单/复核与发放。
        Employee current = accessContext.requireConfirmActor(principal);
        Long comId = current.getCompany().getComId();
        PayrollSheet sheet = requireVisibleSheet(current, sheetId);
        if (sheet.getStatus() != PayrollSheetStatus.Draft && sheet.getStatus() != PayrollSheetStatus.Pending) {
            if (sheet.getStatus() == PayrollSheetStatus.Confirmed) {
                return sheet;
            }
            throw new DefinedException(new DefaultErrorCode("400", "当前工资单状态不允许确认"));
        }
        // DEC-047 / BR-039：员工工资与员工提成解耦，工资单确认不校验提成是否结清。
        // commissionComplete 仅作管理侧提示，不再作为阻断条件。
        List<PayrollItem> items = itemRepository
                .findByCompany_ComIdAndSheet_SheetIdOrderByEmployee_NameAsc(comId, sheet.getSheetId());
        accessContext.requireStoreItemsInStore(current, items);
        validateConfirmable(sheet, items);
        PayrollSheetStatus before = sheet.getStatus();
        for (PayrollItem item : items) {
            createPayableIfAbsent(current, sheet, item);
            item.setStatus(PayrollItemStatus.Confirmed);
            itemRepository.save(item);
        }
        preparePayslips(current, sheet, items);
        sheet.setStatus(PayrollSheetStatus.Confirmed);
        sheet.setConfirmedBy(current);
        sheet.setConfirmedAt(LocalDateTime.now());
        PayrollSheet saved = sheetRepository.save(sheet);
        auditContext.record(current, saved, "CONFIRM", before, PayrollSheetStatus.Confirmed, null);
        return saved;
    }

    private void createPayableIfAbsent(Employee current, PayrollSheet sheet, PayrollItem item) {
        Long comId = current.getCompany().getComId();
        if (payableRepository.findByCompany_ComIdAndSourceTypeAndSourceId(comId, "PayrollItem", item.getItemId()).isPresent()) {
            return;
        }
        Payable payable = new Payable();
        payable.setBusinessOrigin(BusinessOrigin.New);
        payable.setCompany(item.getCompany());
        payable.setStore(item.getStore());
        payable.setTeam(item.getTeam());
        payable.setOwner(item.getEmployee());
        payable.setProject(item.getProject());
        payable.setProperty(item.getProject() == null ? null : item.getProject().getProperty());
        payable.setName(sheet.getPeriod() + " 员工实发工资");
        payable.setType(PayableType.EmployeeSalary);
        payable.setPayeeEmployee(item.getEmployee());
        payable.setPayeeName(item.getEmployee().getName());
        payable.setPeriod(sheet.getPeriod());
        payable.setAmount(money(item.getNetAmount()));
        payable.setPaidAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        payable.setStatus(PayableStatus.Pending);
        payable.setUrgent(Boolean.FALSE);
        payable.setSourceType("PayrollItem");
        payable.setSourceId(item.getItemId());
        payable.setRemark("工资项实发金额");
        payable.setCreatedBy(current);
        payableRepository.save(payable);
    }

    private void preparePayslips(Employee current, PayrollSheet sheet, List<PayrollItem> items) {
        Map<Long, List<PayrollItem>> byEmployee = new LinkedHashMap<>();
        for (PayrollItem item : items) {
            byEmployee.computeIfAbsent(item.getEmployee().getEmpId(), key -> new java.util.ArrayList<>()).add(item);
        }
        for (Map.Entry<Long, List<PayrollItem>> entry : byEmployee.entrySet()) {
            Long empId = entry.getKey();
            Payslip payslip = payslipRepository
                    .findByCompany_ComIdAndSheet_SheetIdAndEmployee_EmpId(
                            current.getCompany().getComId(), sheet.getSheetId(), empId)
                    .orElseGet(Payslip::new);
            if (payslip.getPayslipId() == null) {
                Employee employee = entry.getValue().get(0).getEmployee();
                accessContext.applyOwnership(payslip, current, employee, sheet);
                payslip.setPeriod(sheet.getPeriod());
                payslip.setStatus(PayslipStatus.PendingPayment);
            }
            payslip.setGrossAmount(sum(entry.getValue(), "gross"));
            payslip.setDeductionAmount(sum(entry.getValue(), "deduction"));
            payslip.setNetAmount(sum(entry.getValue(), "net"));
            payslipRepository.save(payslip);
        }
    }

    private BigDecimal sum(List<PayrollItem> items, String field) {
        BigDecimal result = BigDecimal.ZERO;
        for (PayrollItem item : items) {
            result = result.add(switch (field) {
                case "gross" -> money(item.getGrossAmount());
                case "deduction" -> money(item.getDeductionAmount());
                default -> money(item.getNetAmount());
            });
        }
        return result.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value;
    }
}
