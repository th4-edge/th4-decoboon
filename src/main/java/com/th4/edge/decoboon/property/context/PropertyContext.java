package com.th4.edge.decoboon.property.context;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.property.configure.PropertyStatus;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** 客户房屋根模型上下文。 */
@Note("房屋上下文")
@Component
public class PropertyContext {

    private final PropertyRepository propertyRepository;
    private final DataScopeContext dataScopeContext;

    public PropertyContext(PropertyRepository propertyRepository, DataScopeContext dataScopeContext) {
        this.propertyRepository = propertyRepository;
        this.dataScopeContext = dataScopeContext;
    }

    public Property ensureForCustomer(Customer customer, Employee owner) {
        if (customer == null || customer.getCustId() == null || customer.getCompany() == null) {
            throw new DefinedException(new DefaultErrorCode("400", "房屋必须关联有效客户"));
        }
        Long comId = customer.getCompany().getComId();
        Employee principal = owner == null ? customer.getOwner() : owner;
        if (principal == null) {
            throw new DefinedException(new DefaultErrorCode("400", "房屋必须指定负责人"));
        }
        String address = customer.getAddr() == null || customer.getAddr().isBlank()
                ? customer.getCommunity() : customer.getAddr();
        if (address == null || address.isBlank()) {
            throw new DefinedException(new DefaultErrorCode("400", "请先补充房屋地址，再建立房屋档案"));
        }
        return propertyRepository.findFirstByCompany_ComIdAndCustomer_CustIdOrderByPropertyIdAsc(
                        comId, customer.getCustId())
                .map(property -> refresh(property, customer, principal))
                .orElseGet(() -> create(customer, principal, address));
    }

    public Property require(Long propertyId, Employee principal) {
        Employee current = dataScopeContext.requireEmployee(principal);
        Property property = propertyRepository.findById(propertyId)
                .filter(item -> item.getCompany() != null
                        && current.getCompany() != null
                        && item.getCompany().getComId().equals(current.getCompany().getComId()))
                .filter(item -> canView(item, current))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "房屋不存在或无权访问")));
        return property;
    }

    public Property resolve(Long propertyId, Customer customer, Employee owner) {
        if (propertyId == null) {
            return ensureForCustomer(customer, owner);
        }
        Property property = require(propertyId, owner);
        if (!property.getCustomer().getCustId().equals(customer.getCustId())) {
            throw new DefinedException(new DefaultErrorCode("400", "房屋与客户不匹配"));
        }
        return property;
    }

    /** 业务员、设计师和各级管理者按职责共同可见同一房屋。 */
    public boolean canView(Property property, Employee current) {
        if (property == null || current == null || property.getCompany() == null || current.getCompany() == null
                || !property.getCompany().getComId().equals(current.getCompany().getComId())) {
            return false;
        }
        if (current.getRole() == EmployeeRole.Boss || current.getRole() == EmployeeRole.Finance) {
            return true;
        }
        if (current.getRole() == EmployeeRole.StoreManager) {
            return current.getStore() != null && property.getStore() != null
                    && current.getStore().getStoreId().equals(property.getStore().getStoreId());
        }
        if (current.getRole() == EmployeeRole.TeamLeader) {
            return current.getTeam() != null && property.getTeam() != null
                    && current.getTeam().getTeamId().equals(property.getTeam().getTeamId());
        }
        if (sameEmployee(property.getOwner(), current)) return true;
        if (current.getRole() == EmployeeRole.Marketer && sameEmployee(property.getSalesman(), current)) return true;
        return current.getRole() == EmployeeRole.Designer && sameEmployee(property.getDesigner(), current);
    }

    private boolean sameEmployee(Employee target, Employee current) {
        return target != null && target.getEmpId() != null && target.getEmpId().equals(current.getEmpId());
    }

    public Property assignDesigner(Property property, Employee designer) {
        if (property == null || designer == null) return property;
        property.setDesigner(designer);
        if (property.getStatus() == PropertyStatus.PendingMeasure) {
            property.setStatus(PropertyStatus.Measuring);
        }
        return propertyRepository.save(property);
    }

    public List<Property> list(Customer customer) {
        return propertyRepository.findByCompany_ComIdAndCustomer_CustIdOrderByPropertyIdAsc(
                customer.getCompany().getComId(), customer.getCustId());
    }

    public void updateStatus(Property property, PropertyStatus status) {
        if (property != null && status != null) {
            property.setStatus(status);
            propertyRepository.save(property);
        }
    }

    private Property create(Customer customer, Employee owner, String address) {
        Property property = new Property();
        property.setCompany(customer.getCompany());
        property.setStore(owner.getStore());
        property.setTeam(owner.getTeam());
        property.setOwner(owner);
        property.setCustomer(customer);
        property.setPropertyNo("H" + System.currentTimeMillis());
        property.setCommunity(customer.getCommunity());
        property.setAddress(address);
        property.setArea(customer.getArea());
        property.setStatus(PropertyStatus.PendingMeasure);
        property.setSalesman(customer.getOwner());
        property.setDesigner(customer.getDesigner());
        return propertyRepository.save(property);
    }

    private Property refresh(Property property, Customer customer, Employee owner) {
        boolean changed = false;
        if ((property.getAddress() == null || property.getAddress().isBlank())
                && customer.getAddr() != null && !customer.getAddr().isBlank()) {
            property.setAddress(customer.getAddr());
            changed = true;
        }
        if ((property.getCommunity() == null || property.getCommunity().isBlank())
                && customer.getCommunity() != null) {
            property.setCommunity(customer.getCommunity());
            changed = true;
        }
        BigDecimal area = customer.getArea();
        if ((property.getArea() == null || property.getArea().signum() == 0) && area != null) {
            property.setArea(area);
            changed = true;
        }
        if (property.getOwner() == null && owner != null) {
            property.setOwner(owner);
            property.setStore(owner.getStore());
            property.setTeam(owner.getTeam());
            changed = true;
        }
        return changed ? propertyRepository.save(property) : property;
    }
}
