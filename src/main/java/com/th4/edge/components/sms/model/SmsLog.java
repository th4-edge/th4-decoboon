package com.th4.edge.components.sms.model;

import com.th4.edge.components.sms.configure.SmsEngine;
import com.th4.edge.components.sms.configure.VCodeScene;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.date.model.DateTime;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.keyword.annotation.Keyword;
import com.th4.edge.framework.data.model.AbstractModel;
import com.th4.edge.framework.data.model.prepare.annotation.CreateTime;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * 短信发送与验证码校验审计日志。
 *
 * <p>该实体同时作为短信 Provider 的请求/响应实体。验证码明文和摘要均不落库，
 * Redis 中的摘要仅用于验证码有效期内的短时校验。</p>
 */
@Note("短信日志")
@Description("短信验证码发送与校验审计日志，禁止保存验证码明文")
@Model
@Entity
@Table(name = "th4_sms_log")
public class Log extends AbstractModel {

    @Note("日志索引")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;

    @Note("手机号码")
    @Keyword
    @Column(name = "tel_no", length = 11, nullable = false)
    private String telNo;

    @Note("业务场景")
    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private VCodeScene scene;

    @Note("短信厂商")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private SmsEngine engine = SmsEngine.Aliyun;

    @Note("扩展信息")
    @Description("记录模板编码、三方请求 ID 等非敏感排障信息")
    @Lob
    private String extra;

    @Note("用户输入验证码")
    @Description("仅用于校验请求，不写入数据库、不返回前端")
    @Transient
    private String vcode;

    @Note("操作内容")
    @Column(length = 128, nullable = false)
    private String content;

    @Note("是否成功")
    @Column(nullable = false)
    private Boolean success = false;

    @Note("错误代码")
    @Description("短信平台或验证码校验返回的错误代码")
    @Column(name = "error_code", length = 64)
    private String errorCode;

    @Note("错误信息")
    @Description("短信平台或验证码校验返回的错误信息")
    @Lob
    @Column(name = "error_msg")
    private String errorMsg;

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

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public String getTelNo() {
        return telNo;
    }

    public void setTelNo(String telNo) {
        this.telNo = telNo;
    }

    public VCodeScene getScene() {
        return scene;
    }

    public void setScene(VCodeScene scene) {
        this.scene = scene;
    }

    public SmsEngine getEngine() {
        return engine;
    }

    public void setEngine(SmsEngine engine) {
        this.engine = engine;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    public String getVcode() {
        return vcode;
    }

    public void setVcode(String vcode) {
        this.vcode = vcode;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public DateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(DateTime createTime) {
        this.createTime = createTime;
    }
}
