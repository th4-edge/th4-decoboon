package com.th4.edge.decoboon.common.file.context;

import com.th4.edge.decoboon.carryover.model.CarryoverAttachment;
import com.th4.edge.decoboon.carryover.repository.CarryoverAttachmentRepository;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
import com.th4.edge.decoboon.common.file.configure.FileScope;
import com.th4.edge.decoboon.customer.context.CustomerAccessContext;
import com.th4.edge.decoboon.customer.model.CustomerFeedback;
import com.th4.edge.decoboon.customer.repository.CustomerFeedbackRepository;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.DesignItem;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.design.model.Talk;
import com.th4.edge.decoboon.design.model.TalkSection;
import com.th4.edge.decoboon.design.repository.DesignItemRepository;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import com.th4.edge.decoboon.design.repository.SpaceRepository;
import com.th4.edge.decoboon.design.repository.TalkRepository;
import com.th4.edge.decoboon.design.repository.TalkSectionRepository;
import com.th4.edge.decoboon.property.model.Measure;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.Acceptance;
import com.th4.edge.decoboon.project.model.ContractChange;
import com.th4.edge.decoboon.project.model.GoodsReceipt;
import com.th4.edge.decoboon.project.model.ProjectReport;
import com.th4.edge.decoboon.project.model.ProjectTask;
import com.th4.edge.decoboon.project.model.TaskNode;
import com.th4.edge.decoboon.project.repository.AcceptanceRepository;
import com.th4.edge.decoboon.project.repository.ContractChangeRepository;
import com.th4.edge.decoboon.project.repository.GoodsReceiptRepository;
import com.th4.edge.decoboon.project.repository.ProjectReportRepository;
import com.th4.edge.decoboon.project.repository.ProjectTaskRepository;
import com.th4.edge.decoboon.project.repository.TaskNodeRepository;
import com.th4.edge.decoboon.property.model.Measure;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
import com.th4.edge.decoboon.supply.model.StockMovement;
import com.th4.edge.decoboon.supply.repository.StockMovementRepository;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.security.User;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.DataScopeSpecification;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 私有文件授权：员工使用四级数据范围，业主只允许显式项目中的客户可见文件。 */
@Component
public class FileAuthorizationContext {

    private final FileSignatureContext signatureContext;
    private final TenantContext tenantContext;
    private final DataScopeContext dataScopeContext;
    private final ProjectAccessContext projectAccessContext;
    private final CustomerAccessContext customerAccessContext;
    private final ProjectReportRepository reportRepository;
    private final AcceptanceRepository acceptanceRepository;
    private final CustomerFeedbackRepository feedbackRepository;
    private final ContractChangeRepository changeRepository;
    private final DesignRepository designRepository;
    private final DesignItemRepository designItemRepository;
    private final SpaceRepository spaceRepository;
    private final TalkRepository talkRepository;
    private final TalkSectionRepository talkSectionRepository;
    private final MeasureRepository measureRepository;
    private final ProjectTaskRepository taskRepository;
    private final TaskNodeRepository taskNodeRepository;
    private final GoodsReceiptRepository goodsReceiptRepository;
    private final StockMovementRepository stockMovementRepository;
    private final CatalogItemRepository catalogItemRepository;
    private final CarryoverAttachmentRepository carryoverAttachmentRepository;

    public FileAuthorizationContext(FileSignatureContext signatureContext,
                                    TenantContext tenantContext,
                                    DataScopeContext dataScopeContext,
                                    ProjectAccessContext projectAccessContext,
                                    CustomerAccessContext customerAccessContext,
                                    ProjectReportRepository reportRepository,
                                    AcceptanceRepository acceptanceRepository,
                                    CustomerFeedbackRepository feedbackRepository,
                                    ContractChangeRepository changeRepository,
                                    DesignRepository designRepository,
                                    DesignItemRepository designItemRepository,
                                    SpaceRepository spaceRepository,
                                    TalkRepository talkRepository,
                                    TalkSectionRepository talkSectionRepository,
                                    MeasureRepository measureRepository,
                                    ProjectTaskRepository taskRepository,
                                    TaskNodeRepository taskNodeRepository,
                                    GoodsReceiptRepository goodsReceiptRepository,
                                    StockMovementRepository stockMovementRepository,
                                    CatalogItemRepository catalogItemRepository,
                                    CarryoverAttachmentRepository carryoverAttachmentRepository) {
        this.signatureContext = signatureContext;
        this.tenantContext = tenantContext;
        this.dataScopeContext = dataScopeContext;
        this.projectAccessContext = projectAccessContext;
        this.customerAccessContext = customerAccessContext;
        this.reportRepository = reportRepository;
        this.acceptanceRepository = acceptanceRepository;
        this.feedbackRepository = feedbackRepository;
        this.changeRepository = changeRepository;
        this.designRepository = designRepository;
        this.designItemRepository = designItemRepository;
        this.spaceRepository = spaceRepository;
        this.talkRepository = talkRepository;
        this.talkSectionRepository = talkSectionRepository;
        this.measureRepository = measureRepository;
        this.taskRepository = taskRepository;
        this.taskNodeRepository = taskNodeRepository;
        this.goodsReceiptRepository = goodsReceiptRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.catalogItemRepository = catalogItemRepository;
        this.carryoverAttachmentRepository = carryoverAttachmentRepository;
    }

    public Authorization authorize(User<?> principal, FileScope scope, Long businessId,
                                   Set<String> requestedPaths) {
        if (principal instanceof Employee employee) {
            return employeeAuthorize(tenantContext.requireEmployee(employee), scope, businessId, requestedPaths);
        }
        if (principal instanceof Credentials) {
            return ownerAuthorize(principal, scope, businessId, requestedPaths);
        }
        throw notFound();
    }

    private Authorization employeeAuthorize(Employee employee, FileScope scope, Long businessId,
                                            Set<String> requestedPaths) {
        Set<String> allowed = new LinkedHashSet<>();
        Long projectId = null;
        switch (scope) {
            case Report -> {
                ProjectReport report = reportRepository.findOne(byId(ProjectReport.class, "reportId", businessId)
                                .and(projectAccessContext.visibleChild(employee, ProjectReport.class, "project")))
                        .orElseThrow(this::notFound);
                add(allowed, report.getImages());
                projectId = report.getProject().getProjectId();
            }
            case Acceptance -> {
                Acceptance acceptance = acceptanceRepository.findOne(
                                byId(Acceptance.class, "acceptanceId", businessId)
                                        .and(projectAccessContext.visibleChild(employee, Acceptance.class, "project")))
                        .orElseThrow(this::notFound);
                add(allowed, acceptance.getEvidenceUrls());
                add(allowed, acceptance.getRectificationIssueImages());
                projectId = acceptance.getProject().getProjectId();
            }
            case Feedback -> {
                CustomerFeedback feedback = feedbackRepository.findOne(
                                byId(CustomerFeedback.class, "feedbackId", businessId)
                                        .and(projectAccessContext.visibleChild(employee, CustomerFeedback.class, "project")))
                        .orElseThrow(this::notFound);
                add(allowed, feedback.getImages());
                projectId = feedback.getProject().getProjectId();
            }
            case ContractChange -> {
                ContractChange change = changeRepository.findOne(
                                byId(ContractChange.class, "changeId", businessId)
                                        .and(projectAccessContext.visibleChild(employee, ContractChange.class, "project")))
                        .orElseThrow(this::notFound);
                add(allowed, change.getEvidenceImages());
                add(allowed, change.getSignatureUrl());
                projectId = change.getProject().getProjectId();
            }
            case Design -> {
                DataScope scopeData = dataScopeContext.resolve(employee);
                Design design = designRepository.findOne(byId(Design.class, "designId", businessId)
                                .and(DataScopeSpecification.visible(Design.class, scopeData)))
                        .orElseThrow(this::notFound);
                add(allowed, design.getCoverUrl(), design.getCadUrl(), design.getCadPreviewUrl(),
                        design.getOriginalPlanUrl(), design.getDemolitionUrl(), design.getMasonryUrl(),
                        design.getOptimizationUrl());
                add(allowed, design.getSitePhotos());
                add(allowed, design.getSurveyPhotos());
                for (Space space : spaceRepository.findByDesign_DesignIdOrderBySortAsc(businessId)) {
                    add(allowed, space.getImages());
                }
                for (DesignItem item : designItemRepository.findByDesign_DesignIdOrderBySortAsc(businessId)) {
                    add(allowed, item.getImages());
                }
            }
            case Talk -> {
                DataScope scopeData = dataScopeContext.resolve(employee);
                Talk talk = talkRepository.findOne(byId(Talk.class, "talkId", businessId)
                                .and(DataScopeSpecification.visible(Talk.class, scopeData)))
                        .orElseThrow(this::notFound);
                add(allowed, talk.getCoverUrl(), talk.getOutputUrl());
                for (TalkSection section : talkSectionRepository.findByTalk_TalkIdOrderBySortAsc(businessId)) {
                    add(allowed, section.getImages());
                }
            }
            case Measure -> {
                DataScope scopeData = dataScopeContext.resolve(employee);
                Measure measure = measureRepository.findOne(byId(Measure.class, "measureId", businessId)
                                .and(DataScopeSpecification.visible(Measure.class, scopeData)))
                        .orElseThrow(this::notFound);
                add(allowed, measure.getSitePhotos());
            }
            case Task -> {
                ProjectTask task = taskRepository.findOne(byId(ProjectTask.class, "taskId", businessId)
                                .and(projectAccessContext.visibleChild(employee, ProjectTask.class, "project")))
                        .orElseThrow(this::notFound);
                add(allowed, task.getEvidenceUrls());
                for (TaskNode node : taskNodeRepository.findByTaskOrderByCreateTime_DateTimeAsc(task)) {
                    add(allowed, node.getMedias());
                }
                projectId = task.getProject().getProjectId();
            }
            case GoodsReceipt -> {
                GoodsReceipt receipt = goodsReceiptRepository.findOne(
                                byId(GoodsReceipt.class, "receiptId", businessId)
                                        .and(projectAccessContext.visibleChild(employee, GoodsReceipt.class, "project")))
                        .orElseThrow(this::notFound);
                add(allowed, receipt.getImages());
                projectId = receipt.getProject().getProjectId();
            }
            case StockMovement -> {
                StockMovement movement = stockMovementRepository.findById(businessId).orElseThrow(this::notFound);
                if (movement.getCompany() == null
                        || !employee.getCompany().getComId().equals(movement.getCompany().getComId())) {
                    throw notFound();
                }
                if (movement.getProject() != null) {
                    projectId = movement.getProject().getProjectId();
                    projectAccessContext.requireProject(projectId, employee);
                }
                add(allowed, movement.getImages());
            }
            case Catalog -> {
                CatalogItem item = catalogItemRepository.findById(businessId).orElseThrow(this::notFound);
                if (item.getCompany() == null
                        || !employee.getCompany().getComId().equals(item.getCompany().getComId())) {
                    throw notFound();
                }
                add(allowed, item.getImages());
            }
            case Carryover -> {
                CarryoverAttachment attachment = carryoverAttachmentRepository.findById(businessId)
                        .orElseThrow(this::notFound);
                if (attachment.getCompany() == null
                        || !employee.getCompany().getComId().equals(attachment.getCompany().getComId())) {
                    throw notFound();
                }
                add(allowed, attachment.getFileUrl());
            }
        }
        return new Authorization(projectId, requested(allowed, requestedPaths));
    }

    private Authorization ownerAuthorize(User<?> principal, FileScope scope, Long businessId,
                                         Set<String> requestedPaths) {
        Credentials credential = customerAccessContext.requireCredential(principal);
        Set<String> allowed = new LinkedHashSet<>();
        Long projectId;
        switch (scope) {
            case Report -> {
                ProjectReport report = reportRepository.findById(businessId).orElseThrow(this::notFound);
                projectId = report.getProject().getProjectId();
                customerAccessContext.requireAccess(credential, projectId);
                if (!Boolean.TRUE.equals(report.getVisibleToCustomer())) {
                    throw notFound();
                }
                add(allowed, report.getImages());
            }
            case Acceptance -> {
                Acceptance acceptance = acceptanceRepository.findById(businessId).orElseThrow(this::notFound);
                projectId = acceptance.getProject().getProjectId();
                customerAccessContext.requireAccess(credential, projectId);
                add(allowed, acceptance.getEvidenceUrls());
                add(allowed, acceptance.getRectificationIssueImages());
            }
            case Measure -> {
                Measure measure = measureRepository.findById(businessId).orElseThrow(this::notFound);
                if (!ownerHasCustomerOrPropertyAccess(credential, measure.getCustomer(), measure.getProperty())) {
                    throw notFound();
                }
                add(allowed, measure.getSitePhotos());
                add(allowed, measure.getStructureImages());
                add(allowed, measure.getAttachments());
                projectId = null;
            }
            case Design -> {
                Design design = designRepository.findById(businessId).orElseThrow(this::notFound);
                if (!ownerHasCustomerOrPropertyAccess(credential, design.getCustomer(), design.getProperty())) {
                    throw notFound();
                }
                add(allowed, design.getCoverUrl());
                add(allowed, design.getCadUrl());
                add(allowed, design.getCadPreviewUrl());
                add(allowed, design.getOriginalPlanUrl());
                add(allowed, design.getDemolitionUrl());
                add(allowed, design.getMasonryUrl());
                add(allowed, design.getOptimizationUrl());
                add(allowed, design.getOriginalPlanImages());
                add(allowed, design.getSitePhotos());
                add(allowed, design.getSurveyPhotos());
                for (Space space : spaceRepository.findByDesign_DesignIdOrderBySortAsc(businessId)) {
                    add(allowed, space.getImages());
                }
                for (DesignItem item : designItemRepository.findByDesign_DesignIdOrderBySortAsc(businessId)) {
                    add(allowed, item.getImages());
                }
                projectId = null;
            }
            case Feedback -> {
                CustomerFeedback feedback = feedbackRepository.findById(businessId).orElseThrow(this::notFound);
                projectId = feedback.getProject().getProjectId();
                customerAccessContext.requireAccess(credential, projectId);
                add(allowed, feedback.getImages());
            }
            case ContractChange -> {
                ContractChange change = changeRepository.findById(businessId).orElseThrow(this::notFound);
                projectId = change.getProject().getProjectId();
                customerAccessContext.requireAccess(credential, projectId);
                add(allowed, change.getEvidenceImages());
                add(allowed, change.getSignatureUrl());
            }
            default -> throw notFound();
        }
        return new Authorization(projectId, requested(allowed, requestedPaths));
    }

    private <T> Specification<T> byId(Class<T> model, String property, Long id) {
        if (id == null) {
            throw notFound();
        }
        return (root, query, builder) -> builder.equal(root.get(property), id);
    }

    private boolean ownerHasCustomerOrPropertyAccess(Credentials credential,
                                                     com.th4.edge.decoboon.customer.model.Customer customer,
                                                     com.th4.edge.decoboon.property.model.Property property) {
        Long customerId = customer == null ? null : customer.getCustId();
        Long propertyId = property == null ? null : property.getPropertyId();
        return customerAccessContext.activeAccesses(credential).stream().anyMatch(access ->
                (customerId != null && access.getCustomer() != null && customerId.equals(access.getCustomer().getCustId()))
                        || (propertyId != null && access.getProject() != null && access.getProject().getProperty() != null
                        && propertyId.equals(access.getProject().getProperty().getPropertyId())));
    }

    private Set<String> requested(Set<String> allowed, Set<String> requestedPaths) {
        if (requestedPaths == null || requestedPaths.isEmpty()) {
            throw new DefinedException(new DefaultErrorCode("400", "文件路径不能为空"));
        }
        Set<String> normalizedAllowed = new LinkedHashSet<>();
        for (String path : allowed) {
            if (!signatureContext.managed(path)) {
                continue;
            }
            try {
                normalizedAllowed.add(signatureContext.normalize(path));
            } catch (DefinedException ignored) {
                // 外部链接不进入本地签名范围。
            }
        }
        Set<String> result = new LinkedHashSet<>();
        for (String path : requestedPaths) {
            String normalized = signatureContext.normalize(path);
            if (!normalizedAllowed.contains(normalized)) {
                throw notFound();
            }
            result.add(normalized);
        }
        return result;
    }

    private void add(Set<String> target, String... paths) {
        if (paths == null) {
            return;
        }
        for (String path : paths) {
            if (path != null && !path.isBlank()) {
                target.add(path);
            }
        }
    }

    private void add(Set<String> target, List<String> paths) {
        if (paths == null) {
            return;
        }
        for (String path : paths) {
            if (path != null && !path.isBlank()) {
                target.add(path);
            }
        }
    }

    private DefinedException notFound() {
        return new DefinedException(new DefaultErrorCode("404", "文件不存在或无权访问"));
    }

    public record Authorization(Long projectId, Set<String> paths) {
    }
}
