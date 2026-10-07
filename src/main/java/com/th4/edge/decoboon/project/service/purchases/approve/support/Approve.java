package com.th4.edge.decoboon.project.service.purchases.approve.support;

import com.th4.edge.decoboon.project.configure.PurchaseStatus;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
import com.th4.edge.decoboon.project.repository.PurchaseOrderRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 采购单审批。 */
@Note("采购单审批")
@Properties(allow = @AllowProperties(model = PurchaseOrder.class,
        properties = {"purchaseId", "no", "status", "approvedAt", "approvalRemark", "createTime"}))
@Service("project/purchase/approve")
@Scope(ScopeTypes.PROTOTYPE)
public class Approve implements com.th4.edge.decoboon.project.service.purchases.approve.Approve {
    @User private Employee employee;
    @AutoInject @NotNull private Long purchaseId;
    @AutoInject @NotNull private Boolean approved;
    @AutoInject private String conclusion;
    private final PurchaseOrderRepository purchaseRepository;
    private final ProjectAccessContext accessContext;

    public Approve(PurchaseOrderRepository purchaseRepository, ProjectAccessContext accessContext) {
        this.purchaseRepository = purchaseRepository;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional
    public PurchaseOrder call(PurchaseOrder request) {
        Employee current = accessContext.requireEmployee(employee);
        if (current.getRole() != EmployeeRole.Boss
                && current.getRole() != EmployeeRole.StoreManager
                && current.getRole() != EmployeeRole.Finance) {
            throw new DefinedException(new DefaultErrorCode("403", "只有企业负责人、店长或财务可以审批采购单"));
        }
        Specification<PurchaseOrder> byId = (root, query, builder) ->
                builder.equal(root.get("purchaseId"), purchaseId);
        PurchaseOrder purchase = purchaseRepository.findOne(
                        byId.and(accessContext.visibleChild(employee, PurchaseOrder.class, "project")))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "采购单不存在或无权访问")));
        if (purchase.getStatus() != PurchaseStatus.Submitted) {
            throw new DefinedException(new DefaultErrorCode("400", "采购单当前状态不能审批"));
        }
        purchase.setStatus(Boolean.TRUE.equals(approved) ? PurchaseStatus.Approved : PurchaseStatus.Rejected);
        purchase.setApprovedBy(current);
        purchase.setApprovedAt(LocalDateTime.now());
        purchase.setApprovalRemark(conclusion);
        return purchaseRepository.save(purchase);
    }
}
