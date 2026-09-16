package com.th4.edge.platform.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.date.model.DateTime;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.framework.data.jpa.keyword.annotation.Keyword;
import com.th4.edge.framework.data.model.AbstractModel;
import com.th4.edge.framework.data.model.prepare.annotation.CreateTime;
import com.th4.edge.framework.data.model.prepare.annotation.UpdateTime;
import com.th4.edge.framework.provider.security.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/** 独立平台运营用户。 */
@Note("平台运营用户")
@Description("平台运营团队的登录和岗位主体，不从租户 Employee/Credentials 派生")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_platform_user", uniqueConstraints = {
        @UniqueConstraint(name = "uk_platform_user_phone", columnNames = "phone")
}, indexes = {
        @Index(name = "idx_platform_user_org", columnList = "org_id, user_id"),
        @Index(name = "idx_platform_user_department", columnList = "org_id, dept_id, user_id"),
        @Index(name = "idx_platform_user_phone", columnList = "phone, status")
})
public class PlatformUser extends AbstractModel implements User<Long> {
    @Note("用户索引") @Id @SnowflakeId private Long userId;
    @Note("平台组织") @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "org_id", nullable = false) private PlatformOrg org;
    @Note("平台部门") @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "dept_id") private PlatformDepartment department;
    @Note("姓名") @Keyword @Column(length = 32, nullable = false) private String name;
    @Note("手机号") @Keyword @Column(length = 20, nullable = false) private String phone;
    @Note("密码哈希") @Column(name = "password_hash", length = 100, nullable = false) private String passwordHash;
    @Note("岗位") @Enumerated(EnumType.STRING) @Column(length = 32, nullable = false) private PlatformRole role = PlatformRole.CustomerSuccess;
    @Note("状态") @Enumerated(EnumType.STRING) @Column(length = 16, nullable = false) private PlatformUserStatus status = PlatformUserStatus.Active;
    @Note("最近登录时间") @Column(name = "last_login_at") private LocalDateTime lastLoginAt;
    @Note("当前岗位能力；仅响应使用") @Transient private java.util.Set<PlatformPermission> permissions;
    @Note("创建时间")
    @CreateTime @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "dateTime", column = @Column(name = "create_date_time")),
            @AttributeOverride(name = "date", column = @Column(name = "create_date")),
            @AttributeOverride(name = "time", column = @Column(name = "create_time")),
            @AttributeOverride(name = "year", column = @Column(name = "create_year")),
            @AttributeOverride(name = "month", column = @Column(name = "create_month")),
            @AttributeOverride(name = "day", column = @Column(name = "create_day")),
            @AttributeOverride(name = "hour", column = @Column(name = "create_hour")),
            @AttributeOverride(name = "minute", column = @Column(name = "create_minute")),
            @AttributeOverride(name = "second", column = @Column(name = "create_second")),
            @AttributeOverride(name = "week", column = @Column(name = "create_week"))
    })
    private DateTime createTime;
    @Note("更新时间")
    @UpdateTime @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "dateTime", column = @Column(name = "update_date_time")),
            @AttributeOverride(name = "date", column = @Column(name = "update_date")),
            @AttributeOverride(name = "time", column = @Column(name = "update_time")),
            @AttributeOverride(name = "year", column = @Column(name = "update_year")),
            @AttributeOverride(name = "month", column = @Column(name = "update_month")),
            @AttributeOverride(name = "day", column = @Column(name = "update_day")),
            @AttributeOverride(name = "hour", column = @Column(name = "update_hour")),
            @AttributeOverride(name = "minute", column = @Column(name = "update_minute")),
            @AttributeOverride(name = "second", column = @Column(name = "update_second")),
            @AttributeOverride(name = "week", column = @Column(name = "update_week"))
    })
    private DateTime updateTime;

    @Override public Long getUserId() { return userId; }
    @Override public void setUserId(Long userId) { this.userId = userId; }
    public PlatformOrg getOrg() { return org; }
    public void setOrg(PlatformOrg org) { this.org = org; }
    public PlatformDepartment getDepartment() { return department; }
    public void setDepartment(PlatformDepartment department) { this.department = department; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public PlatformRole getRole() { return role; }
    public void setRole(PlatformRole role) { this.role = role; }
    public PlatformUserStatus getStatus() { return status; }
    public void setStatus(PlatformUserStatus status) { this.status = status; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public java.util.Set<PlatformPermission> getPermissions() { return permissions; }
    public void setPermissions(java.util.Set<PlatformPermission> permissions) { this.permissions = permissions; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    public DateTime getCreateTime() { return createTime; }
    public void setCreateTime(DateTime createTime) { this.createTime = createTime; }
    public DateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(DateTime updateTime) { this.updateTime = updateTime; }
}
