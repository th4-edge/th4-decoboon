package com.th4.edge.decoboon.property.service.properties.save.support;

import com.th4.edge.decoboon.customer.context.CustomerContext;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.property.configure.PropertyStatus;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.DataScopeSpecification;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/** 创建客户房屋。 */
@Note("房屋创建")
@Description("将客户地址固化为可独立开展设计、报价、合同和项目的房屋")
@Properties(allow = {
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "customer", "community", "building", "roomNo", "address",
                        "area", "layout", "status", "salesman", "designer", "latestMeasureAt", "remark", "createTime"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "telNo"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("property/property/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.property.service.properties.save.Save {

    @AutoInject @NotNull private Long customerId;
    @AutoInject @NotEmpty private String address;
    @AutoInject private String community;
    @AutoInject private String building;
    @AutoInject private String roomNo;
    @AutoInject private BigDecimal area;
    @AutoInject private String layout;
    @AutoInject private String remark;

    @User private Employee employee;
    private final CustomerRepository customerRepository;
    private final PropertyRepository propertyRepository;
    private final CustomerContext customerContext;
    private final DataScopeContext dataScopeContext;

    public Save(CustomerRepository customerRepository, PropertyRepository propertyRepository,
                CustomerContext customerContext, DataScopeContext dataScopeContext) {
        this.customerRepository = customerRepository;
        this.propertyRepository = propertyRepository;
        this.customerContext = customerContext;
        this.dataScopeContext = dataScopeContext;
    }

    @Override
    @Transactional
    public Property call(Property request) {
        Employee current = dataScopeContext.requireEmployee(employee);
        DataScope scope = dataScopeContext.resolve(current);
        Specification<Customer> byId = (root, query, builder) -> builder.equal(root.get("custId"), customerId);
        Customer customer = customerRepository.findOne(byId.and(DataScopeSpecification.visible(Customer.class, scope)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "客户不存在或无权访问")));
        if (propertyRepository.existsByCompany_ComIdAndCustomer_CustIdAndAddress(
                current.getCompany().getComId(), customerId, address.trim())) {
            throw new DefinedException(new DefaultErrorCode("400", "该客户已存在相同地址的房屋"));
        }
        Property property = new Property();
        property.setCompany(current.getCompany());
        property.setStore(customer.getStore());
        property.setTeam(customer.getTeam());
        property.setOwner(customerContext.resolveOwner(current, customer.getOwner().getEmpId(), customer.getOwner()));
        property.setCustomer(customer);
        property.setPropertyNo("H" + System.currentTimeMillis());
        property.setCommunity(community == null ? customer.getCommunity() : community);
        property.setBuilding(building);
        property.setRoomNo(roomNo);
        property.setAddress(address.trim());
        property.setArea(area == null ? customer.getArea() : area);
        property.setLayout(layout);
        property.setStatus(PropertyStatus.PendingMeasure);
        property.setSalesman(customer.getOwner());
        property.setDesigner(customer.getDesigner());
        property.setRemark(remark);
        return propertyRepository.save(property);
    }
}
