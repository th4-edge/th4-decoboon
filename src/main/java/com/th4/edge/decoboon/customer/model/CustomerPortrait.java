package com.th4.edge.decoboon.customer.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.customer.configure.PortraitCompetitor;
import com.th4.edge.decoboon.customer.configure.PortraitConcern;
import com.th4.edge.decoboon.customer.configure.PortraitDecisionRole;
import com.th4.edge.decoboon.customer.configure.PortraitEco;
import com.th4.edge.decoboon.customer.configure.PortraitHouseCondition;
import com.th4.edge.decoboon.customer.configure.PortraitHousehold;
import com.th4.edge.decoboon.customer.configure.PortraitPayment;
import com.th4.edge.decoboon.customer.configure.PortraitPurpose;
import com.th4.edge.decoboon.customer.configure.PortraitStartWindow;
import com.th4.edge.decoboon.customer.configure.PortraitStyle;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户画像（与客户一对一）。
 *
 * <p>承载 P0-b 冻结的 10 个营销画像字段，全部可空，缺失不阻断主链。
 * 数据范围继承 {@link Customer} 的经营归属，由 Provider 显式校验。</p>
 */
@Note("客户画像")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "cust_portrait", indexes = {
        @Index(name = "idx_cust_portrait_company", columnList = "com_id, portrait_id"),
        @Index(name = "idx_cust_portrait_company_customer", columnList = "com_id, cust_id", unique = true)
})
public class CustomerPortrait extends AuditModel {

    @Id
    @SnowflakeId
    @Note("画像索引")
    private Long portraitId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    @Note("所属企业")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cust_id", nullable = false)
    @Note("客户")
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    @Note("决策角色")
    private PortraitDecisionRole decisionRole;

    @Enumerated(EnumType.STRING)
    @Column(length = 24)
    @Note("装修动机")
    private PortraitPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    @Note("房屋现状")
    private PortraitHouseCondition houseCondition;

    @Enumerated(EnumType.STRING)
    @Column(length = 24)
    @Note("计划开工时间")
    private PortraitStartWindow startWindow;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    @Note("风格偏好")
    private PortraitStyle style;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    @Note("环保健康要求")
    private PortraitEco eco;

    @Enumerated(EnumType.STRING)
    @Column(length = 24)
    @Note("居住人口结构")
    private PortraitHousehold household;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    @Note("付款方式偏好")
    private PortraitPayment payment;

    @Note("主要顾虑")
    @ElementCollection(fetch = FetchType.LAZY)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "cust_portrait_concern", joinColumns = @JoinColumn(name = "portrait_id"))
    @Column(name = "concern", length = 16)
    private List<PortraitConcern> concerns = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(length = 24)
    @Note("竞品对比状态")
    private PortraitCompetitor competitor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id")
    @Note("最后修改人")
    private Employee updatedBy;

    public Long getPortraitId() { return portraitId; }
    public void setPortraitId(Long portraitId) { this.portraitId = portraitId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public PortraitDecisionRole getDecisionRole() { return decisionRole; }
    public void setDecisionRole(PortraitDecisionRole decisionRole) { this.decisionRole = decisionRole; }
    public PortraitPurpose getPurpose() { return purpose; }
    public void setPurpose(PortraitPurpose purpose) { this.purpose = purpose; }
    public PortraitHouseCondition getHouseCondition() { return houseCondition; }
    public void setHouseCondition(PortraitHouseCondition houseCondition) { this.houseCondition = houseCondition; }
    public PortraitStartWindow getStartWindow() { return startWindow; }
    public void setStartWindow(PortraitStartWindow startWindow) { this.startWindow = startWindow; }
    public PortraitStyle getStyle() { return style; }
    public void setStyle(PortraitStyle style) { this.style = style; }
    public PortraitEco getEco() { return eco; }
    public void setEco(PortraitEco eco) { this.eco = eco; }
    public PortraitHousehold getHousehold() { return household; }
    public void setHousehold(PortraitHousehold household) { this.household = household; }
    public PortraitPayment getPayment() { return payment; }
    public void setPayment(PortraitPayment payment) { this.payment = payment; }
    public List<PortraitConcern> getConcerns() { return concerns; }
    public void setConcerns(List<PortraitConcern> concerns) { this.concerns = concerns; }
    public PortraitCompetitor getCompetitor() { return competitor; }
    public void setCompetitor(PortraitCompetitor competitor) { this.competitor = competitor; }
    public Employee getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Employee updatedBy) { this.updatedBy = updatedBy; }
}
