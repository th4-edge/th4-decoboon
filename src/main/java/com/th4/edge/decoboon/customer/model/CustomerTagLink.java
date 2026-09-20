package com.th4.edge.decoboon.customer.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * 客户与标签的关联。
 *
 * <p>只允许绑定人工标签；系统标签由服务端按客户事实装配，不落关联表。
 * 数据范围继承 {@link Customer} 的经营归属，由 Provider 显式校验，不在此冗余快照，
 * 避免客户转交后关联范围失真。</p>
 */
@Note("客户标签关联")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "cust_tag_link", indexes = {
        @Index(name = "idx_cust_tag_link_company", columnList = "com_id, link_id"),
        @Index(name = "idx_cust_tag_link_company_customer", columnList = "com_id, cust_id, link_id"),
        @Index(name = "idx_cust_tag_link_company_tag", columnList = "com_id, tag_id, link_id"),
        @Index(name = "idx_cust_tag_link_unique", columnList = "com_id, cust_id, tag_id", unique = true)
})
public class CustomerTagLink extends AuditModel {

    @Id
    @SnowflakeId
    @Note("关联索引")
    private Long linkId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    @Note("所属企业")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cust_id", nullable = false)
    @Note("客户")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    @Note("标签")
    private CustomerTag tag;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id", nullable = false)
    @Note("打标签员工")
    private Employee createdBy;

    public Long getLinkId() { return linkId; }
    public void setLinkId(Long linkId) { this.linkId = linkId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public CustomerTag getTag() { return tag; }
    public void setTag(CustomerTag tag) { this.tag = tag; }
    public Employee getCreatedBy() { return createdBy; }
    public void setCreatedBy(Employee createdBy) { this.createdBy = createdBy; }
}
