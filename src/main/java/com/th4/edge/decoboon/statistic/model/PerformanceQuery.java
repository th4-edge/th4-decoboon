package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.tenant.user.configure.EmployeeRole;

import java.time.LocalDate;

/** 人员绩效考核查询条件。 */
@Note("人员绩效查询")
public class PerformanceQuery {

    @Note("考核月份，任意一天代表所在月，默认当前月")
    private LocalDate month;

    @Note("员工角色，可选 Designer、Marketer 或 Pm")
    private EmployeeRole role;

    @Note("门店范围；为空表示企业全部门店（仅企业负责人可跨门店）")
    private Long storeId;

    public LocalDate getMonth() { return month; }
    public void setMonth(LocalDate month) { this.month = month; }
    public EmployeeRole getRole() { return role; }
    public void setRole(EmployeeRole role) { this.role = role; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
}
