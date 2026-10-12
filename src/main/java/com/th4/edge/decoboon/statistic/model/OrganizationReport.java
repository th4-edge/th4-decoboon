package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 企业组织效能报表（企业负责人视角）。
 *
 * <p>口径：</p>
 * <ul>
 *   <li>人均产值 = 本月签约额 ÷ 统计期末在职人数；</li>
 *   <li>人均单产 = 本月新签项目数 ÷ 统计期末在职人数；</li>
 *   <li>在职 = {@code EmployeeStatus.Active}；本月新增 = {@code joinedAt} 落在统计月内；</li>
 *   <li>人员流动率、待优化岗位依赖人事事件记录，本批不返回（设计方案 §10.4）。</li>
 * </ul>
 */
@Note("企业组织效能报表")
public class OrganizationReport {

    @Note("统计月份")
    private LocalDate month;

    /** 门店维度范围标注（DEC-067 BOSS-STORE-03）。 */
    private StoreScopeInfo storeScope = new StoreScopeInfo();

    public StoreScopeInfo getStoreScope() { return storeScope; }
    public void setStoreScope(StoreScopeInfo storeScope) { this.storeScope = storeScope; }


    @Note("在职员工数")
    private Integer headcount = 0;

    @Note("本月新增员工数")
    private Integer newHires = 0;

    @Unit("元")
    @Note("本月签约额（与负责人驾驶舱同口径）")
    private BigDecimal signedAmount = BigDecimal.ZERO;

    @Note("本月新签项目数")
    private Integer newProjectCount = 0;

    @Unit("元")
    @Note("人均产值 = 本月签约额 ÷ 在职人数")
    private BigDecimal perCapitaOutput = BigDecimal.ZERO;

    @Note("人均单产 = 本月新签项目数 ÷ 在职人数")
    private BigDecimal perCapitaOrders = BigDecimal.ZERO;

    @Note("角色产能（角色 + 人数 + 该角色绩效均值）")
    private List<RoleCapacity> roleCapacities = List.of();

    @Note("设计师效能排行（按报价接受率倒序，前若干名）")
    private List<EmployeePerformance> designerRanking = List.of();

    @Note("业务员效能排行（按签约率倒序，前若干名）")
    private List<EmployeePerformance> marketerRanking = List.of();

    @Note("项目经理交付排行（按准时交付率倒序，前若干名）")
    private List<EmployeePerformance> pmRanking = List.of();

    @Note("绩效样本是否充足（角色样本 < 3 时为 false，前端需提示仅供参考）")
    private Boolean sampleSufficient = Boolean.TRUE;

    public LocalDate getMonth() { return month; }
    public void setMonth(LocalDate month) { this.month = month; }
    public Integer getHeadcount() { return headcount; }
    public void setHeadcount(Integer headcount) { this.headcount = headcount; }
    public Integer getNewHires() { return newHires; }
    public void setNewHires(Integer newHires) { this.newHires = newHires; }
    public BigDecimal getSignedAmount() { return signedAmount; }
    public void setSignedAmount(BigDecimal signedAmount) { this.signedAmount = signedAmount; }
    public Integer getNewProjectCount() { return newProjectCount; }
    public void setNewProjectCount(Integer newProjectCount) { this.newProjectCount = newProjectCount; }
    public BigDecimal getPerCapitaOutput() { return perCapitaOutput; }
    public void setPerCapitaOutput(BigDecimal perCapitaOutput) { this.perCapitaOutput = perCapitaOutput; }
    public BigDecimal getPerCapitaOrders() { return perCapitaOrders; }
    public void setPerCapitaOrders(BigDecimal perCapitaOrders) { this.perCapitaOrders = perCapitaOrders; }
    public List<RoleCapacity> getRoleCapacities() { return roleCapacities; }
    public void setRoleCapacities(List<RoleCapacity> roleCapacities) { this.roleCapacities = roleCapacities; }
    public List<EmployeePerformance> getDesignerRanking() { return designerRanking; }
    public void setDesignerRanking(List<EmployeePerformance> designerRanking) { this.designerRanking = designerRanking; }
    public List<EmployeePerformance> getMarketerRanking() { return marketerRanking; }
    public void setMarketerRanking(List<EmployeePerformance> marketerRanking) { this.marketerRanking = marketerRanking; }
    public List<EmployeePerformance> getPmRanking() { return pmRanking; }
    public void setPmRanking(List<EmployeePerformance> pmRanking) { this.pmRanking = pmRanking; }
    public Boolean getSampleSufficient() { return sampleSufficient; }
    public void setSampleSufficient(Boolean sampleSufficient) { this.sampleSufficient = sampleSufficient; }
}
