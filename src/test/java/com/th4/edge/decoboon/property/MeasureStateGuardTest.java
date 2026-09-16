package com.th4.edge.decoboon.property;

import com.th4.edge.decoboon.property.configure.MeasureStatus;
import com.th4.edge.decoboon.property.configure.PropertyStatus;
import com.th4.edge.decoboon.property.context.PropertyContext;
import com.th4.edge.decoboon.property.model.Measure;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeasureStateGuardTest {

    @Mock private PropertyContext propertyContext;
    @Mock private PropertyRepository propertyRepository;
    @Mock private MeasureRepository measureRepository;
    @Mock private Employee employee;

    @Test
    void completedMeasureDoesNotRegressServingProperty() {
        Property property = new Property();
        property.setStatus(PropertyStatus.Serving);
        when(propertyContext.require(nullable(Long.class), nullable(Employee.class))).thenReturn(property);
        when(measureRepository.save(any(Measure.class))).thenAnswer(invocation -> {
            Measure saved = invocation.getArgument(0);
            saved.setMeasureId(1L);
            return saved;
        });
        com.th4.edge.decoboon.property.service.measures.save.support.Save save =
                new com.th4.edge.decoboon.property.service.measures.save.support.Save(
                        propertyContext, propertyRepository, measureRepository);

        Measure result = save.call(new Measure());

        assertEquals(MeasureStatus.Completed, result.getStatus());
        assertNotNull(result.getMeasureTime());
        assertEquals(PropertyStatus.Serving, property.getStatus());
        verify(propertyRepository).save(property);
    }

    @Test
    void completedMeasureAdvancesPendingProperty() {
        Property property = new Property();
        property.setStatus(PropertyStatus.PendingMeasure);
        when(propertyContext.require(nullable(Long.class), nullable(Employee.class))).thenReturn(property);
        when(measureRepository.save(any(Measure.class))).thenAnswer(invocation -> {
            Measure saved = invocation.getArgument(0);
            saved.setMeasureId(2L);
            return saved;
        });
        com.th4.edge.decoboon.property.service.measures.save.support.Save save =
                new com.th4.edge.decoboon.property.service.measures.save.support.Save(
                        propertyContext, propertyRepository, measureRepository);

        save.call(new Measure());

        assertEquals(PropertyStatus.Measured, property.getStatus());
    }
}
