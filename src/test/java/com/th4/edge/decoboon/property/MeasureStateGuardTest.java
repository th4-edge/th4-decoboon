package com.th4.edge.decoboon.property;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.property.configure.MeasureStatus;
import com.th4.edge.decoboon.property.configure.PropertyStatus;
import com.th4.edge.decoboon.property.context.PropertyContext;
import com.th4.edge.decoboon.property.model.Measure;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void designerOwnedCustomerWithoutDesignerDefaultsMeasureToCurrentDesigner() {
        Employee designer = new Employee();
        designer.setEmpId(9L);
        designer.setRole(EmployeeRole.Designer);
        Customer customer = new Customer();
        customer.setOwner(designer);
        Property property = new Property();
        property.setPropertyId(3L);
        property.setOwner(designer);
        property.setSalesman(designer);
        property.setCustomer(customer);
        property.setStatus(PropertyStatus.PendingMeasure);
        when(propertyContext.requireEmployee(employee)).thenReturn(designer);
        when(propertyContext.require(nullable(Long.class), nullable(Employee.class))).thenReturn(property);
        when(measureRepository.save(any(Measure.class))).thenAnswer(invocation -> invocation.getArgument(0));
        com.th4.edge.decoboon.property.service.measures.save.support.Save save =
                new com.th4.edge.decoboon.property.service.measures.save.support.Save(
                        propertyContext, propertyRepository, measureRepository);
        ReflectionTestUtils.setField(save, "employee", employee);
        ReflectionTestUtils.setField(save, "propertyId", 3L);
        ReflectionTestUtils.setField(save, "status", MeasureStatus.Completed);

        Measure result = save.call(new Measure());

        assertEquals(designer, result.getDesigner());
        assertEquals(designer, property.getDesigner());
        assertEquals(designer, customer.getDesigner());
    }

    @Test
    void directCompletionDoesNotFabricateScheduledAt() {
        Employee designer = new Employee();
        designer.setEmpId(9L);
        designer.setRole(EmployeeRole.Designer);
        Customer customer = new Customer();
        customer.setDesigner(designer);
        Property property = new Property();
        property.setPropertyId(4L);
        property.setCustomer(customer);
        property.setDesigner(designer);
        property.setStatus(PropertyStatus.PendingMeasure);
        when(propertyContext.requireEmployee(employee)).thenReturn(designer);
        when(propertyContext.require(nullable(Long.class), nullable(Employee.class))).thenReturn(property);
        when(measureRepository.findByPropertyOrderByMeasureTimeDesc(property)).thenReturn(List.of());
        when(measureRepository.save(any(Measure.class))).thenAnswer(invocation -> invocation.getArgument(0));
        com.th4.edge.decoboon.property.service.measures.save.support.Save save =
                new com.th4.edge.decoboon.property.service.measures.save.support.Save(
                        propertyContext, propertyRepository, measureRepository);
        LocalDateTime completedAt = LocalDateTime.of(2026, 10, 1, 9, 8);
        ReflectionTestUtils.setField(save, "employee", employee);
        ReflectionTestUtils.setField(save, "propertyId", 4L);
        ReflectionTestUtils.setField(save, "status", MeasureStatus.Completed);
        ReflectionTestUtils.setField(save, "measureTime", completedAt);

        Measure result = save.call(new Measure());

        assertNull(result.getScheduledAt());
        assertEquals(completedAt, result.getCompletedAt());
        assertEquals(completedAt, result.getMeasureTime());
        assertEquals(PropertyStatus.Measured, property.getStatus());
    }

    @Test
    void completionRejectsScheduledAtAfterCompletedAt() {
        Employee designer = new Employee();
        designer.setEmpId(9L);
        designer.setRole(EmployeeRole.Designer);
        Customer customer = new Customer();
        customer.setDesigner(designer);
        Property property = new Property();
        property.setPropertyId(5L);
        property.setCustomer(customer);
        property.setDesigner(designer);
        property.setStatus(PropertyStatus.Measured);
        Measure scheduled = new Measure();
        scheduled.setMeasureId(6L);
        scheduled.setProperty(property);
        scheduled.setCustomer(customer);
        scheduled.setStatus(MeasureStatus.Scheduled);
        LocalDateTime completedAt = LocalDateTime.of(2026, 10, 1, 9, 8);
        scheduled.setScheduledAt(completedAt.plusDays(2));
        when(propertyContext.requireEmployee(employee)).thenReturn(designer);
        when(propertyContext.require(nullable(Long.class), nullable(Employee.class))).thenReturn(property);
        when(measureRepository.findByPropertyOrderByMeasureTimeDesc(property)).thenReturn(List.of(scheduled));
        com.th4.edge.decoboon.property.service.measures.save.support.Save save =
                new com.th4.edge.decoboon.property.service.measures.save.support.Save(
                        propertyContext, propertyRepository, measureRepository);
        ReflectionTestUtils.setField(save, "employee", employee);
        ReflectionTestUtils.setField(save, "propertyId", 5L);
        ReflectionTestUtils.setField(save, "status", MeasureStatus.Completed);
        ReflectionTestUtils.setField(save, "completedAt", completedAt);
        ReflectionTestUtils.setField(save, "measureTime", completedAt);

        assertThrows(DefinedException.class, () -> save.call(new Measure()));
    }
}
