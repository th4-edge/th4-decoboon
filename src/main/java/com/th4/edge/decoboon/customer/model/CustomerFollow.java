package com.th4.edge.decoboon.customer.model;

import jakarta.persistence.EntityListeners;
import com.th4.edge.components.data.listener.SnowflakeEntityListener;

import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;

import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.customer.configure.FollowWay;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.keyword.annotation.Keyword;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** 客户跟进记录。 */
@Note("客户跟进记录")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission(owner = "emp")
@Table(name = "cust_customer_follow", indexes = {
        @Index(name = "idx_customer_follow_company", columnList = "com_id, follow_id"),
        @Index(name = "idx_customer_follow_company_customer", columnList = "com_id, customer_id, follow_id"),
        @Index(name = "idx_customer_follow_company_owner", columnList = "com_id, emp_id, follow_id")
})
public class CustomerFollow extends AuditModel {

    @Id
    @SnowflakeId
    @Note("跟进索引")
    private Long followId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    @Note("所属企业")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    @Note("所属门店")
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    @Note("所属团队")
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @Note("客户")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id", nullable = false)
    @Note("跟进员工")
    private Employee emp;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    @Note("跟进方式")
    private FollowWay way = FollowWay.Other;

    @Keyword
    @Column(length = 512, nullable = false)
    @Note("跟进内容")
    private String content;

    @Note("下次跟进时间")
    private LocalDateTime nextFollowAt;

    public Long getFollowId() { return followId; }
    public void setFollowId(Long followId) { this.followId = followId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public Employee getEmp() { return emp; }
    public void setEmp(Employee emp) { this.emp = emp; }
    public FollowWay getWay() { return way; }
    public void setWay(FollowWay way) { this.way = way; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getNextFollowAt() { return nextFollowAt; }
    public void setNextFollowAt(LocalDateTime nextFollowAt) { this.nextFollowAt = nextFollowAt; }
}
