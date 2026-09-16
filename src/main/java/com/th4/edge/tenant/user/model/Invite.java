package com.th4.edge.tenant.user.model;
import com.th4.edge.components.data.listener.SnowflakeEntityListener;

import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.date.model.DateTime;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.framework.data.model.AbstractModel;
import com.th4.edge.framework.data.model.prepare.annotation.CreateTime;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.InviteStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 员工邀请。
 *
 * <p>老板邀请业务员加入企业，邀请记录包含租户、门店、目标手机号和角色。
 * 被邀请人通过 inviteId 与微信授权手机号完成校验。</p>
 */
@Note("员工邀请")
@Description("老板邀请业务员加入企业的凭证")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_tenant_invite")
public class Invite extends AbstractModel {

    @Note("邀请索引")
    @Id
    @SnowflakeId
    private Long inviteId;

    @Note("邀请企业")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @Note("邀请门店")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Note("邀请团队")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Note("邀请人")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inviter_id", nullable = false)
    private Employee inviter;

    @Note("被邀请人姓名")
    @Column(name = "invite_name", length = 15)
    private String inviteName;

    @Note("目标手机号")
    @Column(name = "tel_no", length = 11, nullable = false)
    private String telNo;

    @Note("邀请角色")
    @Enumerated(EnumType.STRING)
    @Column(length = 24, nullable = false)
    private EmployeeRole role = EmployeeRole.Marketer;

    @Note("邀请状态")
    @Enumerated(EnumType.STRING)
    @Column(length = 12, nullable = false)
    private InviteStatus status = InviteStatus.Pending;

    @Note("过期时间")
    @Column(name = "expire_time")
    private LocalDateTime expireTime;

    @Note("接受时间")
    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Note("接受用户")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_user_id")
    private Credentials acceptedBy;

    @Note("取消时间")
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Note("取消人")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by_id")
    private Employee cancelledBy;

    @Note("拒绝时间")
    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

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

    public Long getInviteId() {
        return inviteId;
    }

    public void setInviteId(Long inviteId) {
        this.inviteId = inviteId;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public Store getStore() {
        return store;
    }

    public void setStore(Store store) {
        this.store = store;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public Employee getInviter() {
        return inviter;
    }

    public void setInviter(Employee inviter) {
        this.inviter = inviter;
    }

    public String getInviteName() {
        return inviteName;
    }

    public void setInviteName(String inviteName) {
        this.inviteName = inviteName;
    }

    public String getInvitePhone() {
        return telNo;
    }

    public void setInvitePhone(String invitePhone) {
        this.telNo = invitePhone;
    }

    public String getTelNo() {
        return telNo;
    }

    public void setTelNo(String telNo) {
        this.telNo = telNo;
    }

    public EmployeeRole getRole() {
        return role;
    }

    public void setRole(EmployeeRole role) {
        this.role = role;
    }

    public InviteStatus getStatus() {
        return status;
    }

    public void setStatus(InviteStatus status) {
        this.status = status;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }

    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(LocalDateTime acceptedAt) { this.acceptedAt = acceptedAt; }
    public Credentials getAcceptedBy() { return acceptedBy; }
    public void setAcceptedBy(Credentials acceptedBy) { this.acceptedBy = acceptedBy; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public Employee getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(Employee cancelledBy) { this.cancelledBy = cancelledBy; }
    public LocalDateTime getRejectedAt() { return rejectedAt; }
    public void setRejectedAt(LocalDateTime rejectedAt) { this.rejectedAt = rejectedAt; }

    public DateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(DateTime createTime) {
        this.createTime = createTime;
    }
}
