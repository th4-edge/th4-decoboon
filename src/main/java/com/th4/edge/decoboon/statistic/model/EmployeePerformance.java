package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;

import java.math.BigDecimal;

/** 单个员工的自动绩效指标。 */
@Note("员工绩效指标")
public class EmployeePerformance {

    private Employee employee;
    private EmployeeRole role;
    private Integer quoteCount = 0;
    private Integer acceptedQuoteCount = 0;
    private BigDecimal designerConversionRate = BigDecimal.ZERO;
    private Integer customerCount = 0;
    private Integer signedCustomerCount = 0;
    private BigDecimal marketerSignRate = BigDecimal.ZERO;
    private Integer deliveredProjectCount = 0;
    private Integer onTimeProjectCount = 0;
    private BigDecimal pmOnTimeDeliveryRate = BigDecimal.ZERO;
    private Integer acceptanceCount = 0;
    private Integer acceptedAcceptanceCount = 0;
    private BigDecimal pmAcceptancePassRate = BigDecimal.ZERO;

    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public EmployeeRole getRole() { return role; }
    public void setRole(EmployeeRole role) { this.role = role; }
    public Integer getQuoteCount() { return quoteCount; }
    public void setQuoteCount(Integer quoteCount) { this.quoteCount = quoteCount; }
    public Integer getAcceptedQuoteCount() { return acceptedQuoteCount; }
    public void setAcceptedQuoteCount(Integer acceptedQuoteCount) { this.acceptedQuoteCount = acceptedQuoteCount; }
    public BigDecimal getDesignerConversionRate() { return designerConversionRate; }
    public void setDesignerConversionRate(BigDecimal designerConversionRate) { this.designerConversionRate = designerConversionRate; }
    public Integer getCustomerCount() { return customerCount; }
    public void setCustomerCount(Integer customerCount) { this.customerCount = customerCount; }
    public Integer getSignedCustomerCount() { return signedCustomerCount; }
    public void setSignedCustomerCount(Integer signedCustomerCount) { this.signedCustomerCount = signedCustomerCount; }
    public BigDecimal getMarketerSignRate() { return marketerSignRate; }
    public void setMarketerSignRate(BigDecimal marketerSignRate) { this.marketerSignRate = marketerSignRate; }
    public Integer getDeliveredProjectCount() { return deliveredProjectCount; }
    public void setDeliveredProjectCount(Integer deliveredProjectCount) { this.deliveredProjectCount = deliveredProjectCount; }
    public Integer getOnTimeProjectCount() { return onTimeProjectCount; }
    public void setOnTimeProjectCount(Integer onTimeProjectCount) { this.onTimeProjectCount = onTimeProjectCount; }
    public BigDecimal getPmOnTimeDeliveryRate() { return pmOnTimeDeliveryRate; }
    public void setPmOnTimeDeliveryRate(BigDecimal pmOnTimeDeliveryRate) { this.pmOnTimeDeliveryRate = pmOnTimeDeliveryRate; }
    public Integer getAcceptanceCount() { return acceptanceCount; }
    public void setAcceptanceCount(Integer acceptanceCount) { this.acceptanceCount = acceptanceCount; }
    public Integer getAcceptedAcceptanceCount() { return acceptedAcceptanceCount; }
    public void setAcceptedAcceptanceCount(Integer acceptedAcceptanceCount) { this.acceptedAcceptanceCount = acceptedAcceptanceCount; }
    public BigDecimal getPmAcceptancePassRate() { return pmAcceptancePassRate; }
    public void setPmAcceptancePassRate(BigDecimal pmAcceptancePassRate) { this.pmAcceptancePassRate = pmAcceptancePassRate; }
}
