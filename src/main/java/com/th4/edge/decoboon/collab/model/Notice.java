package com.th4.edge.decoboon.collab.model;
import jakarta.persistence.EntityListeners;
import com.th4.edge.components.data.listener.SnowflakeEntityListener;

import com.th4.edge.decoboon.collab.configure.NoticeType;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Note("业务通知")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_decoboon_notice", indexes = {
        @Index(name = "idx_notice_receiver_read", columnList = "receiver_id, read_flag"),
        @Index(name = "idx_notice_receiver_biz", columnList = "receiver_id, biz_key", unique = true)
})
public class Notice extends AuditModel {

    @Note("通知索引")
    @Id
    @SnowflakeId
    private Long noticeId;

    @Note("所属企业")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @Note("接收员工")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private Employee receiver;

    @Note("关联项目")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Note("关联客户")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Note("关联报价")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_id")
    private Quote quote;

    @Note("业务去重键")
    @Column(name = "biz_key", length = 96, nullable = false)
    private String bizKey;

    @Note("通知类型")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private NoticeType type = NoticeType.System;

    @Note("标题")
    @Column(length = 128, nullable = false)
    private String title;

    @Note("内容")
    @Column(length = 512, nullable = false)
    private String content;

    @Note("是否已读")
    @Column(name = "read_flag", nullable = false)
    private Boolean read = Boolean.FALSE;

    public Long getNoticeId() { return noticeId; }
    public void setNoticeId(Long noticeId) { this.noticeId = noticeId; }

    /** 对前端提供字符串 ID，避免雪花 ID 在 JavaScript 中丢失精度。 */
    public String getId() { return noticeId == null ? null : noticeId.toString(); }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Employee getReceiver() { return receiver; }
    public void setReceiver(Employee receiver) { this.receiver = receiver; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public Quote getQuote() { return quote; }
    public void setQuote(Quote quote) { this.quote = quote; }
    public String getBizKey() { return bizKey; }
    public void setBizKey(String bizKey) { this.bizKey = bizKey; }
    public NoticeType getType() { return type; }
    public void setType(NoticeType type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Boolean getRead() { return read; }
    public void setRead(Boolean read) { this.read = read; }
}
