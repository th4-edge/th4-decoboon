package com.th4.edge.decoboon.customer.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.customer.configure.ShareScope;
import com.th4.edge.decoboon.customer.configure.ShareStatus;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
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

import java.time.LocalDateTime;

/**
 * 项目分享链接（亲友只读通道）。
 *
 * 背景：{@code journey} 页原先强制登录，家人/好友点开分享链接会被踢到登录页，
 * 分享功能实际不可用。本对象提供**带时效的只读令牌**，使未登录访客可查看脱敏内容。
 *
 * 安全边界：
 * <ul>
 *   <li>令牌唯一且可撤销；撤销/过期后立即失效，不缓存；</li>
 *   <li>{@code scope = Friend} 时强制不返回金额与房号；</li>
 *   <li>不保存任何登录凭证，不做账号代管。</li>
 * </ul>
 */
@Note("项目分享链接")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "cust_project_share_link", indexes = {
        @Index(name = "idx_share_link_company_project", columnList = "com_id, project_id"),
        @Index(name = "idx_share_link_company_status_expire", columnList = "com_id, status, expire_at")
})
public class ProjectShareLink extends AuditModel {

    @Id
    @SnowflakeId
    @Note("分享索引")
    private Long shareId;

    @Note("所属企业")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @Note("项目")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Note("创建人（业主）")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Employee createdBy;

    @Note("令牌")
    @Column(name = "token", length = 32, nullable = false, unique = true)
    private String token;

    @Note("分享范围")
    @Enumerated(EnumType.STRING)
    @Column(name = "scope", length = 16, nullable = false)
    private ShareScope scope = ShareScope.Family;

    @Note("是否向家人展示金额（Friend 恒为否）")
    @Column(name = "show_amount", nullable = false)
    private Boolean showAmount = Boolean.FALSE;

    @Note("状态")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 16, nullable = false)
    private ShareStatus status = ShareStatus.Active;

    @Note("失效时间")
    @Column(name = "expire_at")
    private LocalDateTime expireAt;

    public Long getShareId() { return shareId; }
    public void setShareId(Long shareId) { this.shareId = shareId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public Employee getCreatedBy() { return createdBy; }
    public void setCreatedBy(Employee createdBy) { this.createdBy = createdBy; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public ShareScope getScope() { return scope; }
    public void setScope(ShareScope scope) { this.scope = scope; }
    public Boolean getShowAmount() { return showAmount; }
    public void setShowAmount(Boolean showAmount) { this.showAmount = showAmount; }
    public ShareStatus getStatus() { return status; }
    public void setStatus(ShareStatus status) { this.status = status; }
    public LocalDateTime getExpireAt() { return expireAt; }
    public void setExpireAt(LocalDateTime expireAt) { this.expireAt = expireAt; }
}
