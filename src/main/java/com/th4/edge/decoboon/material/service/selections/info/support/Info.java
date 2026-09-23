package com.th4.edge.decoboon.material.service.selections.info.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.material.context.MaterialSelectionContext;
import com.th4.edge.decoboon.material.model.MaterialSelection;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 主材选材记录详情。 */
@Note("主材选材记录详情")
@Properties(allow = {
        @AllowProperties(model = MaterialSelection.class, properties = {
                "selectionId", "version", "status", "title", "content", "attachments", "remark",
                "customer", "property", "contract", "project", "designer", "owner",
                "confirmerName", "confirmedBy", "confirmedAt", "createTime", "updateTime"}),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout", "status"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "telNo"}),
        @AllowProperties(model = ProjectContract.class, properties = {"contractId", "no", "baselineAmount", "signedAmount"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("material/selection/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.material.service.selections.info.Info {

    @AutoInject @NotNull private Long selectionId;
    @User private Employee employee;
    private final MaterialSelectionContext materialSelectionContext;

    public Info(MaterialSelectionContext materialSelectionContext) {
        this.materialSelectionContext = materialSelectionContext;
    }

    @Override
    @Transactional(readOnly = true)
    public MaterialSelection call(MaterialSelection request) {
        Employee current = materialSelectionContext.requireEmployee(employee);
        return materialSelectionContext.require(selectionId, current);
    }
}
