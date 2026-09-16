package com.th4.edge.tenant.user.model;

import com.th4.edge.framework.core.annotation.Note;

/** 员工退出前的业务交接清单。 */
@Note("成员交接清单")
public class MemberHandover {

    private Employee employee;
    private Integer customerCount = 0;
    private Integer propertyCount = 0;
    private Integer quoteCount = 0;
    private Integer projectCount = 0;
    private Integer taskCount = 0;
    private Integer receivableCount = 0;

    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public Integer getCustomerCount() { return customerCount; }
    public void setCustomerCount(Integer customerCount) { this.customerCount = customerCount; }
    public Integer getPropertyCount() { return propertyCount; }
    public void setPropertyCount(Integer propertyCount) { this.propertyCount = propertyCount; }
    public Integer getQuoteCount() { return quoteCount; }
    public void setQuoteCount(Integer quoteCount) { this.quoteCount = quoteCount; }
    public Integer getProjectCount() { return projectCount; }
    public void setProjectCount(Integer projectCount) { this.projectCount = projectCount; }
    public Integer getTaskCount() { return taskCount; }
    public void setTaskCount(Integer taskCount) { this.taskCount = taskCount; }
    public Integer getReceivableCount() { return receivableCount; }
    public void setReceivableCount(Integer receivableCount) { this.receivableCount = receivableCount; }
    public int total() {
        return customerCount + propertyCount + quoteCount + projectCount + taskCount + receivableCount;
    }
}
