package com.th4.edge.decoboon.ai.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.ai.configure.AiBillingStatus;
import com.th4.edge.decoboon.ai.configure.AiStoreAllocation;
import com.th4.edge.decoboon.ai.configure.AiUsageStatus;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
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
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/**
 * AI 调用底账。
 *
 * <p>该实体记录调用次数、资源单位、影子点数和企业与门店归属，
 * P0 不产生真实扣点，不改写订阅天数。提示词、客户正文、图片和模型输出不落本表。</p>
 */
@Note("AI调用记录")
@Description("AI调用的企业、门店、能力、资源单位、影子点数、状态和幂等底账")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_ai_usage_call",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ai_usage_call_idempotency",
                        columnNames = {"com_id", "idempotency_key"})
        },
        indexes = {
                @Index(name = "idx_ai_usage_call_company_time", columnList = "com_id, started_at, call_id"),
                @Index(name = "idx_ai_usage_call_store_time", columnList = "com_id, allocation_store_id, started_at"),
                @Index(name = "idx_ai_usage_call_capability_time", columnList = "com_id, capability_code, started_at"),
                @Index(name = "idx_ai_usage_call_module_time", columnList = "com_id, business_module, started_at"),
                @Index(name = "idx_ai_usage_call_business", columnList = "com_id, business_type, business_id")
        })
public class AiUsageCall extends AuditModel {

    @Id
    @SnowflakeId
    @Note("AI调用记录索引")
    private Long callId;

    @Note("所属企业")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @Note("业务对象所属门店")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_store_id")
    private Store businessStore;

    @Note("执行员工所属门店")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_store_id")
    private Store employeeStore;

    @Note("统计分摊门店")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocation_store_id")
    private Store allocationStore;

    @Enumerated(EnumType.STRING)
    @Column(name = "allocation_type", length = 24, nullable = false)
    @Note("门店归属方式")
    private AiStoreAllocation allocationType;

    @Note("门店归属规则版本")
    @Column(name = "allocation_rule_version", length = 24)
    private String allocationRuleVersion;

    @Note("触发员工；系统调用可为空")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Note("外部或上游请求标识")
    @Column(name = "request_id", length = 96)
    private String requestId;

    @Note("幂等键，同一企业唯一")
    @Column(name = "idempotency_key", length = 160, nullable = false)
    private String idempotencyKey;

    @Note("业务模块")
    @Column(name = "business_module", length = 32, nullable = false)
    private String businessModule;

    @Note("AI能力编码")
    @Column(name = "capability_code", length = 64, nullable = false)
    private String capabilityCode;

    @Note("业务场景编码")
    @Column(name = "scene_code", length = 64)
    private String sceneCode;

    @Note("关联业务对象类型")
    @Column(name = "business_type", length = 40)
    private String businessType;

    @Note("关联业务对象主键")
    @Column(name = "business_id")
    private Long businessId;

    @Note("模型或服务供应方")
    @Column(name = "provider_code", length = 64)
    private String providerCode;

    @Note("模型编码")
    @Column(name = "model_code", length = 64)
    private String modelCode;

    @Note("模型版本")
    @Column(name = "model_version", length = 64)
    private String modelVersion;

    @Note("能力版本")
    @Column(name = "capability_version", length = 64)
    private String capabilityVersion;

    @Note("影子费率版本；P0 不代表正式收费费率")
    @Column(name = "rate_card_version", length = 32)
    private String rateCardVersion;

    @Unit("单位")
    @Note("输入资源单位")
    @Column(name = "input_units", nullable = false)
    private Long inputUnits;

    @Unit("单位")
    @Note("输出资源单位")
    @Column(name = "output_units", nullable = false)
    private Long outputUnits;

    @Unit("单位")
    @Note("资源单位合计")
    @Column(name = "resource_units", nullable = false)
    private Long resourceUnits;

    @Unit("点")
    @Note("影子计费点数")
    @Column(name = "shadow_billable_units", nullable = false)
    private Long shadowBillableUnits;

    @Unit("点")
    @Note("已返还影子点数")
    @Column(name = "refunded_units", nullable = false)
    private Long refundedUnits;

    @Unit("分")
    @Note("平台实际成本；仅平台运营使用，不返回租户")
    @Column(name = "platform_cost")
    private Long platformCost;

    @Note("资源单位是否为估算值")
    @Column(nullable = false)
    private Boolean estimated = Boolean.FALSE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 24, nullable = false)
    @Note("调用状态")
    private AiUsageStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_status", length = 24, nullable = false)
    @Note("影子账本状态")
    private AiBillingStatus billingStatus;

    @Note("失败分类")
    @Column(name = "failure_code", length = 64)
    private String failureCode;

    @Note("失败说明，不记录敏感正文")
    @Column(name = "failure_message", length = 300)
    private String failureMessage;

    @Note("调用开始时间")
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Note("调用完成时间")
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Unit("毫秒")
    @Note("调用耗时")
    @Column(name = "duration_ms")
    private Long durationMs;

    @Note("重试来源记录")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retry_of_call_id")
    private AiUsageCall retryOfCall;

    @Note("取消原因")
    @Column(name = "cancel_reason", length = 300)
    private String cancelReason;

    public Long getCallId() { return callId; }
    public void setCallId(Long callId) { this.callId = callId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Store getBusinessStore() { return businessStore; }
    public void setBusinessStore(Store businessStore) { this.businessStore = businessStore; }
    public Store getEmployeeStore() { return employeeStore; }
    public void setEmployeeStore(Store employeeStore) { this.employeeStore = employeeStore; }
    public Store getAllocationStore() { return allocationStore; }
    public void setAllocationStore(Store allocationStore) { this.allocationStore = allocationStore; }
    public AiStoreAllocation getAllocationType() { return allocationType; }
    public void setAllocationType(AiStoreAllocation allocationType) { this.allocationType = allocationType; }
    public String getAllocationRuleVersion() { return allocationRuleVersion; }
    public void setAllocationRuleVersion(String allocationRuleVersion) { this.allocationRuleVersion = allocationRuleVersion; }
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getBusinessModule() { return businessModule; }
    public void setBusinessModule(String businessModule) { this.businessModule = businessModule; }
    public String getCapabilityCode() { return capabilityCode; }
    public void setCapabilityCode(String capabilityCode) { this.capabilityCode = capabilityCode; }
    public String getSceneCode() { return sceneCode; }
    public void setSceneCode(String sceneCode) { this.sceneCode = sceneCode; }
    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }
    public Long getBusinessId() { return businessId; }
    public void setBusinessId(Long businessId) { this.businessId = businessId; }
    public String getProviderCode() { return providerCode; }
    public void setProviderCode(String providerCode) { this.providerCode = providerCode; }
    public String getModelCode() { return modelCode; }
    public void setModelCode(String modelCode) { this.modelCode = modelCode; }
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    public String getCapabilityVersion() { return capabilityVersion; }
    public void setCapabilityVersion(String capabilityVersion) { this.capabilityVersion = capabilityVersion; }
    public String getRateCardVersion() { return rateCardVersion; }
    public void setRateCardVersion(String rateCardVersion) { this.rateCardVersion = rateCardVersion; }
    public Long getInputUnits() { return inputUnits; }
    public void setInputUnits(Long inputUnits) { this.inputUnits = inputUnits; }
    public Long getOutputUnits() { return outputUnits; }
    public void setOutputUnits(Long outputUnits) { this.outputUnits = outputUnits; }
    public Long getResourceUnits() { return resourceUnits; }
    public void setResourceUnits(Long resourceUnits) { this.resourceUnits = resourceUnits; }
    public Long getShadowBillableUnits() { return shadowBillableUnits; }
    public void setShadowBillableUnits(Long shadowBillableUnits) { this.shadowBillableUnits = shadowBillableUnits; }
    public Long getRefundedUnits() { return refundedUnits; }
    public void setRefundedUnits(Long refundedUnits) { this.refundedUnits = refundedUnits; }
    public Long getPlatformCost() { return platformCost; }
    public void setPlatformCost(Long platformCost) { this.platformCost = platformCost; }
    public Boolean getEstimated() { return estimated; }
    public void setEstimated(Boolean estimated) { this.estimated = estimated; }
    public AiUsageStatus getStatus() { return status; }
    public void setStatus(AiUsageStatus status) { this.status = status; }
    public AiBillingStatus getBillingStatus() { return billingStatus; }
    public void setBillingStatus(AiBillingStatus billingStatus) { this.billingStatus = billingStatus; }
    public String getFailureCode() { return failureCode; }
    public void setFailureCode(String failureCode) { this.failureCode = failureCode; }
    public String getFailureMessage() { return failureMessage; }
    public void setFailureMessage(String failureMessage) { this.failureMessage = failureMessage; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public AiUsageCall getRetryOfCall() { return retryOfCall; }
    public void setRetryOfCall(AiUsageCall retryOfCall) { this.retryOfCall = retryOfCall; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
}
