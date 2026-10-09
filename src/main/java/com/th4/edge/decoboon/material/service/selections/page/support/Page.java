package com.th4.edge.decoboon.material.service.selections.page.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.material.context.MaterialSelectionContext;
import com.th4.edge.decoboon.material.model.MaterialSelection;
import com.th4.edge.decoboon.material.repository.MaterialSelectionRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 主材选材版本列表：按房屋（或指定项目）返回全部历史版本，最新版本在最前。 */
@Note("主材选材记录分页")
@Description("按房屋返回主材选材全部历史版本，已确认版本不被覆盖，最新版本在最前")
@Properties(allow = {
        @AllowProperties(model = MaterialSelection.class, properties = {
                "selectionId", "version", "status", "title", "content", "attachments", "remark",
                "customer", "property", "contract", "project", "designer", "owner",
                "createTime", "updateTime"}),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout", "status"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "telNo"}),
        @AllowProperties(model = ProjectContract.class, properties = {"contractId", "no", "baselineAmount", "signedAmount"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("material/selection/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.material.service.selections.page.Page {

    @AutoInject @NotNull private Long propertyId;
    @AutoInject private Long projectId;
    @User private Employee employee;
    private final MaterialSelectionContext materialSelectionContext;
    private final MaterialSelectionRepository materialSelectionRepository;

    public Page(MaterialSelectionContext materialSelectionContext,
                MaterialSelectionRepository materialSelectionRepository) {
        this.materialSelectionContext = materialSelectionContext;
        this.materialSelectionRepository = materialSelectionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<MaterialSelection> call(MaterialSelection request) {
        Employee current = materialSelectionContext.requireEmployee(employee);
        Property property = materialSelectionContext.requireViewableProperty(propertyId, current);
        Long comId = property.getCompany().getComId();
        List<MaterialSelection> versions = projectId == null
                ? materialSelectionRepository.findByCompany_ComIdAndProperty_PropertyIdOrderByVersionDesc(
                        comId, property.getPropertyId())
                : materialSelectionRepository.findByCompany_ComIdAndProject_ProjectIdOrderByVersionDesc(
                        comId, projectId);
        return new PageImpl<>(versions);
    }
}
