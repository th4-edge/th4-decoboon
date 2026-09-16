package com.th4.edge.tenant.user.model;
import com.th4.edge.components.data.listener.SnowflakeEntityListener;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.framework.data.model.AbstractModel;
import com.th4.edge.tenant.user.configure.Size;
import jakarta.persistence.*;

import com.th4.edge.framework.data.date.model.DateTime;
import com.th4.edge.framework.data.model.prepare.annotation.CreateTime;

@Note("企业")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_tenant_company")
public class Company extends AbstractModel {

    @Note("企业索引")
    @Id
    @SnowflakeId
    private Long comId;

    @Note("名称")
    @Column(length = 128,nullable = false)
    private String title;

    @Note("简称")
    @Column(length = 32,nullable = false)
    private String name;

    @Note("Logo")
    @Column(length = 256)
    private String logo;

    @Note("品牌色")
    @Column(length = 12)
    private String color;

    @Note("规模")
    @Enumerated(EnumType.STRING)
    @Column(length = 12,nullable = false)
    private Size size=Size.A;

    @Note("地址")
    @Column(length = 256)
    private String addr;

    @Note("配置")
    @Embedded
    private Setting setting;

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

    public Long getComId() {
        return comId;
    }

    public void setComId(Long comId) {
        this.comId = comId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Size getSize() {
        return size;
    }

    public void setSize(Size size) {
        this.size = size;
    }

    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }

    public Setting getSetting() {
        return setting;
    }

    public void setSetting(Setting setting) {
        this.setting = setting;
    }

    public DateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(DateTime createTime) {
        this.createTime = createTime;
    }
}
