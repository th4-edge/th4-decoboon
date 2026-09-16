package com.th4.edge.platform.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.date.model.DateTime;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.framework.data.model.AbstractModel;
import com.th4.edge.framework.data.model.prepare.annotation.CreateTime;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import jakarta.persistence.*;

/** 平台运营人工操作审计。 */
@Note("平台操作审计")
@Description("平台运营人员对企业订阅执行人工动作时留下的不可变审计记录")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_platform_operation_log", indexes = {
        @Index(name = "idx_platform_log_company", columnList = "com_id, log_id"),
        @Index(name = "idx_platform_log_company_time", columnList = "com_id, create_date_time, log_id"),
        @Index(name = "idx_platform_log_operator_time", columnList = "emp_id, create_date_time, log_id")
})
public class PlatformOperationLog extends AbstractModel {

    @Note("审计索引")
    @Id
    @SnowflakeId
    private Long logId;

    @Note("目标企业")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @Note("操作人")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id", nullable = false)
    private Employee operator;

    @Note("操作动作")
    @Enumerated(EnumType.STRING)
    @Column(length = 24, nullable = false)
    private PlatformAction action;

    @Note("目标类型")
    @Column(name = "target_type", length = 32, nullable = false)
    private String targetType;

    @Note("目标标识")
    @Column(name = "target_key", length = 64, nullable = false)
    private String targetKey;

    @Note("操作摘要")
    @Column(length = 256, nullable = false)
    private String summary;

    @Note("变更前")
    @Column(name = "before_value", length = 2048)
    private String beforeValue;

    @Note("变更后")
    @Column(name = "after_value", length = 2048)
    private String afterValue;

    @Note("操作原因")
    @Column(length = 500, nullable = false)
    private String reason;

    @Note("操作时间")
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
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Employee getOperator() { return operator; }
    public void setOperator(Employee operator) { this.operator = operator; }
    public PlatformAction getAction() { return action; }
    public void setAction(PlatformAction action) { this.action = action; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public String getTargetKey() { return targetKey; }
    public void setTargetKey(String targetKey) { this.targetKey = targetKey; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getBeforeValue() { return beforeValue; }
    public void setBeforeValue(String beforeValue) { this.beforeValue = beforeValue; }
    public String getAfterValue() { return afterValue; }
    public void setAfterValue(String afterValue) { this.afterValue = afterValue; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public DateTime getCreateTime() { return createTime; }
    public void setCreateTime(DateTime createTime) { this.createTime = createTime; }
}
