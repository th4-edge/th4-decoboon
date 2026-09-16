package com.th4.edge.tenant.user.model;
import com.th4.edge.components.data.listener.SnowflakeEntityListener;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.framework.data.model.AbstractModel;
import com.th4.edge.framework.data.name.annotation.Name;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import jakarta.persistence.*;

import com.th4.edge.framework.data.date.model.DateTime;
import com.th4.edge.framework.data.model.prepare.annotation.CreateTime;

import java.time.LocalDateTime;

@Note("员工")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_tenant_employee", indexes = {
        @Index(name = "idx_tenant_employee_company", columnList = "com_id, emp_id"),
        @Index(name = "idx_tenant_employee_store", columnList = "com_id, store_id, emp_id"),
        @Index(name = "idx_tenant_employee_team", columnList = "com_id, team_id, emp_id"),
        @Index(name = "idx_tenant_employee_company_phone_status", columnList = "com_id, tel_no, status")
})
public class Employee extends AbstractModel implements com.th4.edge.framework.provider.security.User<Long> {

    @Note("员工索引")
    @Id
    @SnowflakeId
    private Long empId;

    @Note("姓名")
    @Name
    @Column(length = 15,nullable = false)
    private String name;

    @Note("手机号码")
    @Column(length = 12,nullable = false)
    private String telNo;

    @Note("微信头像")
    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    @Note("门店")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Note("团队")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Note("企业")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @Note("角色")
    @Enumerated(EnumType.STRING)
    @Column(length = 24, nullable = false)
    private EmployeeRole role = EmployeeRole.Marketer;

    @Note("员工状态")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false, columnDefinition = "varchar(16) default 'Active'")
    private EmployeeStatus status = EmployeeStatus.Active;

    @Note("加入企业时间")
    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Note("离职时间")
    @Column(name = "resigned_at")
    private LocalDateTime resignedAt;

    @Note("离职原因")
    @Column(name = "resign_reason", length = 500)
    private String resignReason;

    @Note("来源邀请")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invite_id")
    private Invite invite;

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

    public Employee() {
    }

    /**
     * JPQL 构造登录主体，确保返回普通 Employee 实体而不是 Hibernate 代理。
     */
    public Employee(Long empId, String name, String telNo, Store store,
                    Company company, EmployeeRole role, DateTime createTime) {
        this.empId = empId;
        this.name = name;
        this.telNo = telNo;
        this.store = store;
        this.company = company;
        this.role = role;
        this.createTime = createTime;
    }

    public Long getEmpId() {
        return empId;
    }

    public void setEmpId(Long empId) {
        this.empId = empId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTelNo() {
        return telNo;
    }

    public void setTelNo(String telNo) {
        this.telNo = telNo;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
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

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public EmployeeRole getRole() {
        return role;
    }

    public void setRole(EmployeeRole role) {
        this.role = role;
    }

    public EmployeeStatus getStatus() {
        return status;
    }

    public void setStatus(EmployeeStatus status) {
        this.status = status;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public LocalDateTime getResignedAt() {
        return resignedAt;
    }

    public void setResignedAt(LocalDateTime resignedAt) {
        this.resignedAt = resignedAt;
    }

    public String getResignReason() {
        return resignReason;
    }

    public void setResignReason(String resignReason) {
        this.resignReason = resignReason;
    }

    public Invite getInvite() {
        return invite;
    }

    public void setInvite(Invite invite) {
        this.invite = invite;
    }

    public DateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(DateTime createTime) {
        this.createTime = createTime;
    }

    @Override
    public Long getUserId() {
        return getEmpId();
    }

    @Override
    public void setUserId(Long userId) {
        setEmpId(userId);
    }
}
