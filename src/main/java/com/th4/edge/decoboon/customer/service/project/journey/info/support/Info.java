package com.th4.edge.decoboon.customer.service.project.journey.info.support;

import com.th4.edge.decoboon.customer.context.CustomerAccessContext;
import com.th4.edge.decoboon.customer.context.CustomerJourneyContext;
import com.th4.edge.decoboon.customer.model.CustomerJourney;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Credentials;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 业主项目生命周期旅程实现。 */
@Note("业主项目生命周期旅程")
@Properties(allow = {
        @AllowProperties(model = CustomerJourney.class, properties = {
                "projectId", "projectNo", "projectName", "projectStatus", "projectStatusName", "progress",
                "currentStageName", "propertyAddress", "asOf", "sections", "actions", "openFeedbackCount",
                "pendingActionCount", "vendor", "team"}),
        // 服务主体知情权（USER-DECISION-20261014-002）：
        // 企业对外信息 + 为这一户服务的人。team 只含姓名与岗位，不含个人手机号。
        @AllowProperties(model = CustomerJourney.Vendor.class, properties = {
                "name", "logo", "color", "custodianCredit", "serviceArea", "serviceTel", "afterSaleTel",
                "uscc", "qualification", "intro"}),
        @AllowProperties(model = CustomerJourney.Member.class, properties = {"name", "roleName"}),
        @AllowProperties(model = CustomerJourney.Section.class, properties = {
                "key", "title", "subtitle", "status", "statusName", "summary", "facts", "actions", "items"}),
        @AllowProperties(model = CustomerJourney.Item.class, properties = {
                "key", "title", "subtitle", "status", "statusName", "amount", "date", "businessType",
                "businessId", "images", "lines", "facts", "media", "regions", "priceGroups"}),
        @AllowProperties(model = CustomerJourney.Media.class, properties = {
                "key", "url", "kind", "title", "time", "room"}),
        @AllowProperties(model = CustomerJourney.Region.class, properties = {
                "key", "name", "type", "length", "width", "height", "area", "perimeter", "areaSource", "valueOrigin", "sourceMeasureRegionId", "itemCount",
                "images", "remark"}),
        @AllowProperties(model = CustomerJourney.PriceGroup.class, properties = {
                "key", "name", "total", "summary", "rows"}),
        @AllowProperties(model = CustomerJourney.PriceRow.class, properties = {
                "key", "name", "category", "brand", "spec", "unit", "quantity", "price", "amount", "role", "type",
                "craft", "criterion"}),
        @AllowProperties(model = CustomerJourney.Fact.class, properties = {"key", "label", "value", "valueType"}),
        @AllowProperties(model = CustomerJourney.Action.class, properties = {
                "key", "type", "title", "description", "targetType", "targetId"})
})
@Service("customer/project/journey/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.customer.service.project.journey.info.Info {

    @com.th4.edge.framework.provider.security.annotation.User
    private User<?> principal;

    @AutoInject
    @NotNull
    private Long projectId;

    private final CustomerAccessContext accessContext;
    private final CustomerJourneyContext journeyContext;

    public Info(CustomerAccessContext accessContext, CustomerJourneyContext journeyContext) {
        this.accessContext = accessContext;
        this.journeyContext = journeyContext;
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerJourney call(Project request) {
        Credentials credential = accessContext.requireCredential(principal);
        Project project = accessContext.requireProject(credential, projectId);
        return journeyContext.build(project);
    }
}
