package com.th4.edge.decoboon.property.service.properties.page.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.property.context.PropertyContext;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/** 当前角色可见房屋分页。 */
@Note("房屋分页")
@Properties(allow = {
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "customer", "community", "building", "roomNo", "address",
                        "area", "layout", "status", "salesman", "designer", "latestMeasureAt", "remark", "createTime"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "telNo"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("property/property/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.property.service.properties.page.Page {

    @AutoInject
    private Long customerId;

    @User
    private Employee employee;

    private final PropertyRepository propertyRepository;
    private final TenantContext tenantContext;
    private final PropertyContext propertyContext;

    public Page(PropertyRepository propertyRepository, TenantContext tenantContext, PropertyContext propertyContext) {
        this.propertyRepository = propertyRepository;
        this.tenantContext = tenantContext;
        this.propertyContext = propertyContext;
    }

    @Override
    public org.springframework.data.domain.Page<Property> call(Property request) {
        Employee current = tenantContext.requireEmployee(employee);
        Long comId = current.getCompany().getComId();
        Long empId = current.getEmpId();
        List<Property> list;
        if (customerId != null) {
            list = propertyRepository.findByCompany_ComIdAndCustomer_CustIdOrderByPropertyIdAsc(comId, customerId)
                    .stream().filter(item -> propertyContext.canView(item, current)).toList();
        } else {
            list = switch (current.getRole()) {
                case Boss, Finance -> propertyRepository.findAll().stream()
                        .filter(item -> item.getCompany().getComId().equals(comId)).toList();
                case StoreManager -> current.getStore() == null ? List.of()
                        : propertyRepository.findByCompany_ComIdAndStore_StoreIdOrderByPropertyIdDesc(
                                comId, current.getStore().getStoreId());
                case TeamLeader -> current.getTeam() == null ? List.of()
                        : propertyRepository.findByCompany_ComIdAndTeam_TeamIdOrderByPropertyIdDesc(
                                comId, current.getTeam().getTeamId());
                case Designer -> propertyRepository.findByCompany_ComIdAndDesigner_EmpIdOrderByPropertyIdDesc(comId, empId);
                case Marketer -> propertyRepository.findByCompany_ComIdAndSalesman_EmpIdOrderByPropertyIdDesc(comId, empId);
                default -> propertyRepository.findByCompany_ComIdAndOwner_EmpIdOrderByPropertyIdDesc(comId, empId);
            };
        }
        return new PageImpl<>(list);
    }
}
