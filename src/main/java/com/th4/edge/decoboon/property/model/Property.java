package com.th4.edge.decoboon.property.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.property.configure.PropertyStatus;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.framework.data.jpa.keyword.annotation.Keyword;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import jakarta.persistence.Column;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 客户名下的装修房屋，是设计、报价、合同、项目和账务的唯一业务根。 */
@Note("装修房屋")
@Description("客户名下可独立开展设计、报价、签约和施工的装修标的")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_decoboon_property", indexes = {
        @Index(name = "idx_property_company", columnList = "com_id, property_id"),
        @Index(name = "idx_property_company_customer", columnList = "com_id, customer_id, property_id"),
        @Index(name = "idx_property_company_store", columnList = "com_id, store_id, property_id"),
        @Index(name = "idx_property_company_team", columnList = "com_id, team_id, property_id"),
        @Index(name = "idx_property_company_owner", columnList = "com_id, emp_id, property_id")
})
@DataPermission
public class Property extends AuditModel {

    @Id
    @SnowflakeId
    @Note("房屋索引")
    private Long propertyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    @Note("所属企业")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    @Note("归属门店")
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    @Note("归属团队")
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id", nullable = false)
    @Note("数据负责人")
    private Employee owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @Note("所属客户")
    private Customer customer;

    @Keyword
    @Column(length = 64, nullable = false)
    @Note("房屋编号")
    private String propertyNo;

    @Keyword
    @Column(length = 128)
    @Note("小区")
    private String community;

    @Column(length = 64)
    @Note("楼栋")
    private String building;

    @Column(length = 64)
    @Note("房号")
    private String roomNo;

    @Keyword
    @Column(length = 256, nullable = false)
    @Note("完整地址")
    private String address;

    @Unit("m²")
    @Column(precision = 10, scale = 2)
    @Note("房屋面积")
    private BigDecimal area = BigDecimal.ZERO;

    @Column(length = 128)
    @Note("户型")
    private String layout;

    @Enumerated(EnumType.STRING)
    @Column(length = 24, nullable = false)
    @Note("房屋状态")
    private PropertyStatus status = PropertyStatus.PendingMeasure;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salesman_id")
    @Note("营销负责人")
    private Employee salesman;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "designer_id")
    @Note("设计师")
    private Employee designer;

    @Note("最近量房时间")
    private LocalDateTime latestMeasureAt;

    @Column(length = 512)
    @Note("备注")
    private String remark;

    public Long getPropertyId() { return propertyId; }
    public void setPropertyId(Long propertyId) { this.propertyId = propertyId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public Employee getOwner() { return owner; }
    public void setOwner(Employee owner) { this.owner = owner; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public String getPropertyNo() { return propertyNo; }
    public void setPropertyNo(String propertyNo) { this.propertyNo = propertyNo; }
    public String getCommunity() { return community; }
    public void setCommunity(String community) { this.community = community; }
    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }
    public String getRoomNo() { return roomNo; }
    public void setRoomNo(String roomNo) { this.roomNo = roomNo; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public BigDecimal getArea() { return area; }
    public void setArea(BigDecimal area) { this.area = area; }
    public String getLayout() { return layout; }
    public void setLayout(String layout) { this.layout = layout; }
    public PropertyStatus getStatus() { return status; }
    public void setStatus(PropertyStatus status) { this.status = status; }
    public Employee getSalesman() { return salesman; }
    public void setSalesman(Employee salesman) { this.salesman = salesman; }
    public Employee getDesigner() { return designer; }
    public void setDesigner(Employee designer) { this.designer = designer; }
    public LocalDateTime getLatestMeasureAt() { return latestMeasureAt; }
    public void setLatestMeasureAt(LocalDateTime latestMeasureAt) { this.latestMeasureAt = latestMeasureAt; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
