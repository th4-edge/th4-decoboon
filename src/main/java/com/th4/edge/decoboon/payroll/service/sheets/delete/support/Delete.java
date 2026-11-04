package com.th4.edge.decoboon.payroll.service.sheets.delete.support;

import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.payroll.configure.AttendanceStatus;
import com.th4.edge.decoboon.payroll.configure.PayrollItemStatus;
import com.th4.edge.decoboon.payroll.configure.PayrollSheetStatus;
import com.th4.edge.decoboon.payroll.context.PayrollAccessContext;
import com.th4.edge.decoboon.payroll.context.PayrollAttendanceAuditContext;
import com.th4.edge.decoboon.payroll.model.AttendanceSummary;
import com.th4.edge.decoboon.payroll.model.PayrollItem;
import com.th4.edge.decoboon.payroll.model.PayrollSheet;
import com.th4.edge.decoboon.payroll.repository.AttendanceSummaryRepository;
import com.th4.edge.decoboon.payroll.repository.PayrollItemRepository;
import com.th4.edge.decoboon.payroll.repository.PayrollSheetRepository;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** 删除尚未形成财务事实的工资单草稿。 */
@Note("工资单草稿删除")
@Properties(allow = @AllowProperties(model = PayrollSheet.class,
        properties = {"sheetId", "period", "status", "attendanceComplete", "commissionComplete",
                "totalGrossAmount", "totalDeductionAmount", "totalNetAmount", "createTime"}))
@Service("payroll/sheet/delete")
@Scope(ScopeTypes.PROTOTYPE)
public class Delete implements com.th4.edge.decoboon.payroll.service.sheets.delete.Delete {

    @User private Employee employee;
    @AutoInject @NotNull private Long sheetId;

    private final PayrollSheetRepository sheetRepository;
    private final PayrollItemRepository itemRepository;
    private final AttendanceSummaryRepository attendanceRepository;
    private final PayableRepository payableRepository;
    private final PayrollAccessContext accessContext;
    private final PayrollAttendanceAuditContext attendanceAuditContext;

    public Delete(PayrollSheetRepository sheetRepository,
                  PayrollItemRepository itemRepository,
                  AttendanceSummaryRepository attendanceRepository,
                  PayableRepository payableRepository,
                  PayrollAccessContext accessContext,
                  PayrollAttendanceAuditContext attendanceAuditContext) {
        this.sheetRepository = sheetRepository;
        this.itemRepository = itemRepository;
        this.attendanceRepository = attendanceRepository;
        this.payableRepository = payableRepository;
        this.accessContext = accessContext;
        this.attendanceAuditContext = attendanceAuditContext;
    }

    @Override
    @Transactional
    public PayrollSheet call(PayrollSheet request) {
        Employee current = accessContext.requireDeleteActor(employee);
        Long comId = current.getCompany().getComId();
        Specification<PayrollSheet> byId = (root, query, builder) -> builder.equal(root.get("sheetId"), sheetId);
        PayrollSheet sheet = sheetRepository.findOne(byId.and(accessContext.visibleSheets(current)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "工资单不存在或无权访问")));
        if (sheet.getStatus() != PayrollSheetStatus.Draft) {
            throw new DefinedException(new DefaultErrorCode("409", "仅草稿状态工资单可以删除"));
        }
        if (sheet.getConfirmedBy() != null || sheet.getConfirmedAt() != null) {
            throw new DefinedException(new DefaultErrorCode("409", "工资单已确认，不能删除"));
        }
        List<PayrollItem> items = itemRepository
                .findByCompany_ComIdAndSheet_SheetIdOrderByEmployee_NameAsc(comId, sheet.getSheetId());
        accessContext.requireStoreItemsInStore(current, items);
        for (PayrollItem item : items) {
            if (item.getStatus() != PayrollItemStatus.Draft) {
                throw new DefinedException(new DefaultErrorCode("409", "工资项已形成正式事实，不能删除草稿"));
            }
            if (payableRepository.findByCompany_ComIdAndSourceTypeAndSourceId(
                    comId, "PayrollItem", item.getItemId()).isPresent()) {
                throw new DefinedException(new DefaultErrorCode("409", "工资项已生成应付，不能删除草稿"));
            }
        }
        if (!items.isEmpty()) {
            Set<Long> employeeIds = items.stream()
                    .map(item -> item.getEmployee().getEmpId())
                    .collect(Collectors.toSet());
            List<AttendanceSummary> attendances = attendanceRepository
                    .findByCompany_ComIdAndPayrollMonthOrderByEmployee_NameAsc(comId, sheet.getPeriod());
            List<AttendanceSummary> restoredAttendances = new ArrayList<>();
            for (AttendanceSummary attendance : attendances) {
                if (employeeIds.contains(attendance.getEmployee().getEmpId())
                        && attendance.getStatus() == AttendanceStatus.InPayroll) {
                    String beforeSnapshot = attendanceAuditContext.snapshot(attendance);
                    attendance.setStatus(AttendanceStatus.Confirmed);
                    restoredAttendances.add(attendance);
                    attendanceAuditContext.record(current, attendance, "RESTORE_TO_CONFIRMED",
                            beforeSnapshot, "删除工资单草稿后恢复考勤");
                }
            }
            if (!restoredAttendances.isEmpty()) {
                attendanceRepository.saveAll(restoredAttendances);
            }
            itemRepository.deleteAll(items);
        }
        sheetRepository.delete(sheet);
        return sheet;
    }
}
