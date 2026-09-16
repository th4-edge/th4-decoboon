package com.th4.edge.tenant.user.model;
import com.th4.edge.components.data.listener.SnowflakeEntityListener;

import com.th4.edge.decoboon.customer.model.CustomerAccess;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.framework.data.model.AbstractModel;
import com.th4.edge.tenant.user.configure.CredType;
import com.th4.edge.tenant.user.configure.SessionType;
import com.th4.edge.tenant.user.configure.UserStatus;
import jakarta.persistence.*;

import com.th4.edge.framework.data.date.model.DateTime;
import com.th4.edge.framework.data.model.prepare.annotation.CreateTime;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 平台用户身份。
 *
 * <p>微信凭证使用 idKey=openId、idSecret=unionId；小程序员工可在已登录状态下为
 * 当前凭证设置 Web 登录密码，Web 首次设密也可先创建 Password 凭证并以手机号
 * 作为 idKey，之后绑定微信时升级为 Wx 凭证。未加入企业时 employee 为空，
 * 加入后指向当前有效 Employee。</p>
 */
@Note("登陆凭证")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_tenant_credentials")
public class Credentials extends AbstractModel implements com.th4.edge.framework.provider.security.User<Long> {

    @Note("凭证索引")
    @Id
    @SnowflakeId
    private Long creId;

    @Note("类型")
    @Enumerated(EnumType.STRING)
    @Column(length = 12,nullable = false)
    private CredType type=CredType.Wx;

    @Note("关联员工；微信用户首次登录时可暂未加入企业")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id")
    private Employee employee;

    @Note("微信授权手机号")
    @Column(length = 20, unique = true)
    private String phone;

    @Note("用户状态")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false, columnDefinition = "varchar(16) default 'Active'")
    private UserStatus status = UserStatus.Active;

    @Note("凭证标识")
    @Column(length = 64,nullable = false,unique = true)
    private String idKey;

    @Note("凭证密钥")
    @Column(length = 128)
    private String idSecret;

    @Note("上次登陆时间")
    @Column
    private java.time.LocalDateTime lastTime;

    @Note("密码哈希；仅用于登录校验，不进入响应")
    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Note("密码最近更新时间")
    @Column(name = "password_updated_at")
    private LocalDateTime passwordUpdatedAt;

    @Note("连续登录失败次数")
    @Column(name = "failed_attempts", nullable = false, columnDefinition = "int default 0")
    private Integer failedAttempts = 0;

    @Note("锁定截止时间")
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Note("是否必须修改密码")
    @Column(name = "must_change_password", nullable = false, columnDefinition = "bit default 0")
    private Boolean mustChangePassword = false;

    @Note("当前会话身份；仅用于会话响应，不落库")
    @Transient
    private SessionType sessionType = SessionType.User;

    @Note("当前会话可见的业主项目授权；仅用于会话响应，不落库")
    @Transient
    private List<CustomerAccess> customerAccesses;

    @Note("创建时间")
    @CreateTime
    @Embedded
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

    public Long getCreId() {
        return creId;
    }

    public void setCreId(Long creId) {
        this.creId = creId;
    }

    public CredType getType() {
        return type;
    }

    public void setType(CredType type) {
        this.type = type;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public String getIdKey() {
        return idKey;
    }

    public void setIdKey(String idKey) {
        this.idKey = idKey;
    }

    public String getIdSecret() {
        return idSecret;
    }

    public String getOpenId() {
        return type == CredType.Wx ? idKey : null;
    }

    public String getUnionId() {
        return type == CredType.Wx ? idSecret : null;
    }

    public void setIdSecret(String idSecret) {
        this.idSecret = idSecret;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public LocalDateTime getLastTime() {
        return lastTime;
    }

    public void setLastTime(LocalDateTime lastTime) {
        this.lastTime = lastTime;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /** 是否已设置密码；仅用于响应，不暴露哈希。 */
    public Boolean getPasswordSet() {
        return passwordHash != null && !passwordHash.isBlank();
    }

    public LocalDateTime getPasswordUpdatedAt() {
        return passwordUpdatedAt;
    }

    public void setPasswordUpdatedAt(LocalDateTime passwordUpdatedAt) {
        this.passwordUpdatedAt = passwordUpdatedAt;
    }

    public Integer getFailedAttempts() {
        return failedAttempts;
    }

    public void setFailedAttempts(Integer failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(LocalDateTime lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public Boolean getMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(Boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    public SessionType getSessionType() {
        return sessionType;
    }

    public void setSessionType(SessionType sessionType) {
        this.sessionType = sessionType;
    }

    public List<CustomerAccess> getCustomerAccesses() {
        return customerAccesses;
    }

    public void setCustomerAccesses(List<CustomerAccess> customerAccesses) {
        this.customerAccesses = customerAccesses;
    }

    @Override
    public Long getUserId() {
        return creId;
    }

    @Override
    public void setUserId(Long userId) {
        this.creId = userId;
    }

    public DateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(DateTime createTime) {
        this.createTime = createTime;
    }
}
