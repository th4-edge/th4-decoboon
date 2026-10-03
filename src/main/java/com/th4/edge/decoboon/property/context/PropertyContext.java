package com.th4.edge.decoboon.property.context;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.property.configure.PropertyStatus;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import org.springframework.stereotype.Component;

import java.util.List;

/** 客户房屋根模型上下文。 */
@Note("房屋上下文")
@Component
public class PropertyContext {

    private final PropertyRepository propertyRepository;
    private final ProjectRepository projectRepository;
    private final DataScopeContext dataScopeContext;

    public PropertyContext(PropertyRepository propertyRepository,
                           ProjectRepository projectRepository,
                           DataScopeContext dataScopeContext) {
        this.propertyRepository = propertyRepository;
        this.projectRepository = projectRepository;
        this.dataScopeContext = dataScopeContext;
    }

    public Property ensureForCustomer(Customer customer, Employee owner) {
        if (customer == null || customer.getCustId() == null || customer.getCompany() == null) {
            throw new DefinedException(new DefaultErrorCode("400", "房屋必须关联有效客户"));
        }
        List<Property> properties = propertyRepository.findByCompany_ComIdAndCustomer_CustIdOrderByPropertyIdAsc(
                customer.getCompany().getComId(), customer.getCustId());
        if (properties.isEmpty()) {
            throw new DefinedException(new DefaultErrorCode("400", "请先建立房屋档案"));
        }
        if (properties.size() > 1) {
            throw new DefinedException(new DefaultErrorCode("400", "客户存在多套房屋，请明确选择房屋"));
        }
        return properties.get(0);
    }

    /**
     * 重新加载完整员工实体。
     *
     * <p>JWT 主体是框架用 BeanUtils 新建的 Employee 占位对象（只有 empId，
     * role 为字段默认值 Marketer、name 为空），因此业务归属与响应关联必须使用
     * 数据库中的完整员工实体，禁止把占位对象直接写入实体关系或返回前端。</p>
     */
    public Employee requireEmployee(Employee principal) {
        return dataScopeContext.requireEmployee(principal);
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
            throw new DefinedException(new DefaultErrorCode("400", "请先选择客户房屋"));
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
        if (current.getRole() == EmployeeRole.Pm && property.getPropertyId() != null) {
            // 项目经理通过项目参与关系只读本项目房屋，不获得量房/设计/报价写权限。
            return projectRepository.existsByCompany_ComIdAndProperty_PropertyIdAndManager_EmpId(
                    current.getCompany().getComId(), property.getPropertyId(), current.getEmpId());
        }
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

}
