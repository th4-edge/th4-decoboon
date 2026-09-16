package com.th4.edge.decoboon.common.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.date.model.DateTime;
import com.th4.edge.framework.data.model.AbstractModel;
import com.th4.edge.framework.data.model.prepare.annotation.CreateTime;
import com.th4.edge.framework.data.model.prepare.annotation.UpdateTime;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;

/**
 * 家装业务实体审计基类。
 *
 * <p>统一维护创建时间和更新时间，供项目、报价、材料和验收等服务复用。
 * 该类不承载业务逻辑，只提供可审计的数据基础。</p>
 */
@MappedSuperclass
public abstract class AuditModel extends AbstractModel {

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

    @Note("更新时间")
    @UpdateTime
    @Embedded
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

    public DateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(DateTime createTime) {
        this.createTime = createTime;
    }

    public DateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(DateTime updateTime) {
        this.updateTime = updateTime;
    }
}
