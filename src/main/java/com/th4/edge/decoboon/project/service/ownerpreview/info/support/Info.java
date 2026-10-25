package com.th4.edge.decoboon.project.service.ownerpreview.info.support;

import com.th4.edge.decoboon.customer.configure.JourneyViewMode;
import com.th4.edge.decoboon.customer.context.CustomerJourneyContext;
import com.th4.edge.decoboon.customer.model.CustomerJourney;
import com.th4.edge.decoboon.project.configure.OwnerPreviewResult;
import com.th4.edge.decoboon.project.context.OwnerPreviewAuditContext;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.quote.model.QuoteComponent;
import com.th4.edge.decoboon.quote.model.QuoteGroup;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;

/** 员工侧业主视角只读预览实现。 */
@Note("员工业主视角只读预览")
@Properties(allow = {
        @AllowProperties(model = CustomerJourney.class, properties = {
                "projectId", "projectNo", "projectName", "projectStatus", "projectStatusName", "progress",
                "currentStageName", "propertyAddress", "asOf", "sections", "actions", "openFeedbackCount",
                "pendingActionCount", "vendor", "team", "previewMeta"}),
        @AllowProperties(model = CustomerJourney.PreviewMeta.class, properties = {
                "mode", "readOnly", "notice", "watermark", "asOf"}),
        @AllowProperties(model = CustomerJourney.Vendor.class, properties = {
                "name", "logo", "color", "custodianCredit", "serviceArea", "serviceTel", "afterSaleTel",
                "uscc", "qualification", "intro"}),
        @AllowProperties(model = CustomerJourney.Member.class, properties = {"name", "roleName"}),
        @AllowProperties(model = CustomerJourney.Section.class, properties = {
                "key", "title", "subtitle", "status", "statusName", "summary", "facts", "actions", "items"}),
        @AllowProperties(model = CustomerJourney.Item.class, properties = {
                "key", "title", "subtitle", "status", "statusName", "amount", "date", "businessType",
                "businessId", "images", "lines", "facts", "media", "regions", "priceGroups", "regionGroups", "typeGroups"}),
        @AllowProperties(model = CustomerJourney.Media.class, properties = {
                "key", "url", "kind", "title", "time", "room"}),
        @AllowProperties(model = CustomerJourney.Region.class, properties = {
                "key", "name", "type", "length", "width", "height", "area", "perimeter", "areaSource",
                "valueOrigin", "sourceMeasureRegionId", "itemCount", "images", "remark"}),
        @AllowProperties(model = CustomerJourney.PriceGroup.class, properties = {
                "key", "name", "total", "summary", "rows"}),
        @AllowProperties(model = CustomerJourney.PriceRow.class, properties = {
                "key", "name", "category", "brand", "spec", "unit", "quantity", "price", "amount", "role", "type",
                "craft", "criterion"}),
        @AllowProperties(model = QuoteGroup.class, properties = {
                "key", "name", "groupMode", "type", "category", "unit", "quantity", "amount", "items"}),
        @AllowProperties(model = QuoteLine.class, properties = {
                "itemId", "space", "type", "category", "name", "brand", "spec", "unit", "quantity", "price", "amount",
                "role", "differenceType", "differenceStatus", "included", "discountAmount", "components", "craft",
                "typeRowId", "sourceLineIds", "sourceRegionIds", "sourceRegionNames", "sourceCount"}),
        @AllowProperties(model = QuoteComponent.class, properties = {"type", "name", "saleAmount", "sort"}),
        @AllowProperties(model = Space.class, properties = {"spaceId", "type", "name", "area", "areaSource"}),
        @AllowProperties(model = CustomerJourney.Fact.class, properties = {"key", "label", "value", "valueType"}),
        @AllowProperties(model = CustomerJourney.Action.class, properties = {
                "key", "type", "title", "description", "targetType", "targetId"})
})
@Service("project/owner-preview/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.project.service.ownerpreview.info.Info {

    private static final DateTimeFormatter WATERMARK_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @com.th4.edge.framework.provider.security.annotation.User
    private Employee employee;

    @AutoInject
    @NotNull
    private Long projectId;

    private final TenantContext tenantContext;
    private final ProjectAccessContext projectAccessContext;
    private final CustomerJourneyContext journeyContext;
    private final OwnerPreviewAuditContext auditContext;

    public Info(TenantContext tenantContext,
                ProjectAccessContext projectAccessContext,
                CustomerJourneyContext journeyContext,
                OwnerPreviewAuditContext auditContext) {
        this.tenantContext = tenantContext;
        this.projectAccessContext = projectAccessContext;
        this.journeyContext = journeyContext;
        this.auditContext = auditContext;
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerJourney call(Project request) {
        Employee current = tenantContext.requireEmployee(employee);
        try {
            current = tenantContext.requireInternalRole(current,
                    EmployeeRole.StoreManager, EmployeeRole.TeamLeader, EmployeeRole.Designer, EmployeeRole.Pm);
        } catch (DefinedException error) {
            auditContext.recordFailure(current, projectId, OwnerPreviewResult.Forbidden);
            throw error;
        }

        Project project;
        try {
            project = projectAccessContext.requireProject(projectId, current);
        } catch (DefinedException error) {
            auditContext.recordFailure(current, projectId, OwnerPreviewResult.NotFound);
            throw error;
        }

        CustomerJourney journey;
        try {
            journey = journeyContext.build(project, JourneyViewMode.EmployeeOwnerPreview);
        } catch (RuntimeException error) {
            auditContext.recordFailure(current, projectId, OwnerPreviewResult.Error);
            throw error;
        }

        CustomerJourney.PreviewMeta meta = journey.getPreviewMeta();
        meta.setWatermark("业主视角预览 · 仅内部使用 · " + current.getName() + " · 工号"
                + current.getEmpId() + " · " + meta.getAsOf().format(WATERMARK_TIME));
        auditContext.recordSuccess(current, project);
        return journey;
    }
}
