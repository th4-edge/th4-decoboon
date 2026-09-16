package com.th4.edge.decoboon.property;

import com.th4.edge.decoboon.property.configure.PropertyStatus;
import com.th4.edge.decoboon.property.context.PropertyContext;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 客户交接后房屋责任归属回归测试。 */
class PropertyHandoffOwnershipTest {

    @Test
    void assignedDesignerMustBecomePropertyOwner() {
        PropertyRepository repository = mock(PropertyRepository.class);
        PropertyContext context = new PropertyContext(repository, mock(DataScopeContext.class));
        Property property = new Property();
        property.setStatus(PropertyStatus.PendingMeasure);
        Employee marketer = new Employee();
        marketer.setEmpId(10L);
        Employee designer = new Employee();
        designer.setEmpId(100L);
        property.setOwner(marketer);
        property.setSalesman(marketer);
        when(repository.save(property)).thenReturn(property);

        Property saved = context.assignDesigner(property, designer);

        assertThat(saved.getDesigner()).isSameAs(designer);
        assertThat(saved.getOwner()).isSameAs(marketer);
        assertThat(saved.getSalesman()).isSameAs(marketer);
        assertThat(saved.getStatus()).isEqualTo(PropertyStatus.Measuring);
    }
}
