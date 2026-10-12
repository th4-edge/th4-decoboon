package com.th4.edge.decoboon.design;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.design.context.DesignContext;
import com.th4.edge.decoboon.design.model.Requirement;
import com.th4.edge.decoboon.property.context.PropertyContext;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.decoboon.common.access.StoreAttributionTestSupport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class DesignContextMultiPropertyTest {

    @Test
    void requirementApplyShouldKeepExplicitPropertyWhenCustomerHasMultipleProperties() {
        PropertyContext propertyContext = mock(PropertyContext.class);
        DesignContext context = StoreAttributionTestSupport.inject(
                new DesignContext(null, null, null, null, null, null, propertyContext));

        Company company = new Company();
        company.setComId(1L);
        Employee owner = new Employee();
        owner.setEmpId(10L);
        owner.setCompany(company);
        Customer customer = new Customer();
        customer.setCustId(20L);
        customer.setCompany(company);
        Property property = new Property();
        property.setPropertyId(30L);
        property.setCustomer(customer);
        Requirement requirement = new Requirement();

        context.apply(requirement, customer, owner, property);

        assertThat(requirement.getProperty()).isSameAs(property);
        assertThat(requirement.getCustomer()).isSameAs(customer);
        assertThat(requirement.getOwner()).isSameAs(owner);
        verifyNoInteractions(propertyContext);
    }
}
