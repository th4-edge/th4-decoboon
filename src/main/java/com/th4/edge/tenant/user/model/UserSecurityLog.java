package com.th4.edge.tenant.user.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.date.model.DateTime;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.framework.data.model.AbstractModel;
import com.th4.edge.framework.data.model.prepare.annotation.CreateTime;
import com.th4.edge.tenant.user.configure.SecurityAction;
import com.th4.edge.tenant.user.configure.SecurityResult;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/** 用户登录、绑定、加入、退出和安全限制日志。 */
@Note("用户安全日志")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_user_security_log", indexes = {
        @Index(name = "idx_security_log_user", columnList = "user_id, create_date_time"),
        @Index(name = "idx_security_log_open", columnList = "open_id, create_date_time"),
        @Index(name = "idx_security_log_phone", columnList = "phone, create_date_time")
})
public class UserSecurityLog extends AbstractModel {

    @Note("日志索引")
    @Id
    @SnowflakeId
    private Long logId;

    @Note("用户索引快照")
    @Column(name = "user_id")
    private Long userId;

    @Note("微信 openId 快照")
    @Column(name = "open_id", length = 128)
    private String openId;

    @Note("手机号快照")
    @Column(length = 20)
    private String phone;

    @Note("企业索引快照")
    @Column(name = "com_id")
    private Long companyId;

    @Note("员工索引快照")
    @Column(name = "emp_id")
    private Long empId;

    @Note("动作")
    @Enumerated(EnumType.STRING)
    @Column(length = 24, nullable = false)
    private SecurityAction action;

    @Note("结果")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private SecurityResult result;

    @Note("客户端 IP")
    @Column(length = 64)
    private String ip;

    @Note("User-Agent")
    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Note("说明")
    @Column(length = 500)
    private String remark;

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

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getOpenId() { return openId; }
    public void setOpenId(String openId) { this.openId = openId; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public Long getEmpId() { return empId; }
    public void setEmpId(Long empId) { this.empId = empId; }
    public SecurityAction getAction() { return action; }
    public void setAction(SecurityAction action) { this.action = action; }
    public SecurityResult getResult() { return result; }
    public void setResult(SecurityResult result) { this.result = result; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public DateTime getCreateTime() { return createTime; }
    public void setCreateTime(DateTime createTime) { this.createTime = createTime; }
}
