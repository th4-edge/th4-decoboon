package com.th4.edge.decoboon.carryover.service.project.page.support;

import com.th4.edge.decoboon.carryover.context.CarryoverAccessContext;
import com.th4.edge.decoboon.carryover.context.CarryoverProjectContext;
import com.th4.edge.decoboon.carryover.model.CarryoverBatch;
import com.th4.edge.decoboon.carryover.model.CarryoverProject;
import com.th4.edge.decoboon.carryover.repository.CarryoverProjectRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Note("承接项目分页")
@Properties(allow = {
        @AllowProperties(model = CarryoverProject.class,
                properties = {"itemId", "batch", "status", "version", "customerName", "customerPhone", "community",
                        "address", "area", "owner", "manager", "designer", "contractNo", "signedAmount",
                        "currentAmount", "signedAt", "startDate", "planEndDate", "projectName", "projectStatus",
                        "currentStageName", "progress", "remainingWork", "currentIssue",
                        "planStatus", "planVersion", "planConfirmedAt", "planConfirmedBy",
                        "receivableOutstanding",
                        "receivableDueDate", "receivableSourceNo", "receivableConfirmed", "payableOutstanding",
                        "payableDueDate", "payablePayeeName", "payableSourceNo", "payableConfirmed", "costTotal",
                        "costStatus", "costMissingNote", "legacyId", "legacyProjectNo", "quote", "contract",
                        "project", "receivable", "payable", "appliedAt", "lockedAt", "firstBusinessAt", "firstBusinessType",
                        "validationBlockers", "carryoverReady", "createTime"}),
        @AllowProperties(model = CarryoverBatch.class,
                properties = {"batchId", "asOfDate", "status", "createTime"}),
        @AllowProperties(model = com.th4.edge.decoboon.quote.model.Quote.class,
                properties = {"quoteId", "no", "amount", "status", "businessOrigin"}),
        @AllowProperties(model = com.th4.edge.decoboon.project.model.ProjectContract.class,
                properties = {"contractId", "no", "baselineAmount", "signedAmount", "currentAmount", "status", "businessOrigin"}),
        @AllowProperties(model = com.th4.edge.decoboon.project.model.Project.class,
                properties = {"projectId", "no", "name", "status", "businessOrigin"}),
        @AllowProperties(model = com.th4.edge.decoboon.finance.model.Receivable.class,
                properties = {"receivableId", "name", "amount", "receivedAmount", "dueDate", "status", "businessOrigin"}),
        @AllowProperties(model = com.th4.edge.decoboon.finance.model.Payable.class,
                properties = {"payableId", "name", "amount", "paidAmount", "dueDate", "status", "sourceNo", "businessOrigin"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("carryover/project/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.carryover.service.project.page.Page {

    @User
    private Employee employee;

    @AutoInject
    private Integer current;

    @AutoInject
    private Integer size;

    private final CarryoverAccessContext accessContext;
    private final CarryoverProjectContext projectContext;
    private final CarryoverProjectRepository projectRepository;

    public Page(CarryoverAccessContext accessContext, CarryoverProjectContext projectContext,
                CarryoverProjectRepository projectRepository) {
        this.accessContext = accessContext;
        this.projectContext = projectContext;
        this.projectRepository = projectRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<CarryoverProject> call(CarryoverProject request) {
        Employee currentEmployee = accessContext.requireEditor(employee);
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 100 : Math.min(size, 200);
        var result = projectRepository.findAll(accessContext.visible(currentEmployee), PageRequest.of(page, pageSize));
        List<CarryoverProject> items = result.getContent();
        items.forEach(this::fillValidation);
        return new PageImpl<>(items, result.getPageable(), result.getTotalElements());
    }

    private void fillValidation(CarryoverProject item) {
        List<String> blockers = projectContext.validate(item);
        item.setValidationBlockers(blockers);
        item.setCarryoverReady(blockers.isEmpty());
    }
}
