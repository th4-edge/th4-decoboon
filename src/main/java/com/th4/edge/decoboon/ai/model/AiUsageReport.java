package com.th4.edge.decoboon.ai.model;

import com.th4.edge.decoboon.ai.configure.AiStoreAllocation;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** 企业 AI 资源用量只读报表。 */
@Note("AI资源用量报表")
public class AiUsageReport {

    @Note("数据截止日")
    private LocalDate asOf;

    @Note("统计开始日期")
    private LocalDate from;

    @Note("统计结束日期")
    private LocalDate to;

    @Note("请求次数")
    private Long requestCount = 0L;

    @Note("有效调用次数")
    private Long validCallCount = 0L;

    @Note("成功次数")
    private Long successCount = 0L;

    @Note("失败次数")
    private Long failureCount = 0L;

    @Unit("单位")
    @Note("资源单位合计")
    private Long resourceUnits = 0L;

    @Unit("点")
    @Note("影子计费点数")
    private Long shadowBillableUnits = 0L;

    @Unit("点")
    @Note("返还点数")
    private Long refundedUnits = 0L;

    @Unit("点")
    @Note("净影子点数")
    private Long netBillableUnits = 0L;

    @Unit("%")
    @Note("成功率")
    private BigDecimal successRate = BigDecimal.ZERO;

    @Note("调用趋势")
    private List<AiUsageTrendPoint> trend = new ArrayList<>();

    @Note("门店分布")
    private List<AiUsageStoreSummary> stores = new ArrayList<>();

    @Note("能力分布")
    private List<AiUsageCapabilitySummary> capabilities = new ArrayList<>();

    public LocalDate getAsOf() { return asOf; }
    public void setAsOf(LocalDate asOf) { this.asOf = asOf; }
    public LocalDate getFrom() { return from; }
    public void setFrom(LocalDate from) { this.from = from; }
    public LocalDate getTo() { return to; }
    public void setTo(LocalDate to) { this.to = to; }
    public Long getRequestCount() { return requestCount; }
    public void setRequestCount(Long requestCount) { this.requestCount = requestCount; }
    public Long getValidCallCount() { return validCallCount; }
    public void setValidCallCount(Long validCallCount) { this.validCallCount = validCallCount; }
    public Long getSuccessCount() { return successCount; }
    public void setSuccessCount(Long successCount) { this.successCount = successCount; }
    public Long getFailureCount() { return failureCount; }
    public void setFailureCount(Long failureCount) { this.failureCount = failureCount; }
    public Long getResourceUnits() { return resourceUnits; }
    public void setResourceUnits(Long resourceUnits) { this.resourceUnits = resourceUnits; }
    public Long getShadowBillableUnits() { return shadowBillableUnits; }
    public void setShadowBillableUnits(Long shadowBillableUnits) { this.shadowBillableUnits = shadowBillableUnits; }
    public Long getRefundedUnits() { return refundedUnits; }
    public void setRefundedUnits(Long refundedUnits) { this.refundedUnits = refundedUnits; }
    public Long getNetBillableUnits() { return netBillableUnits; }
    public void setNetBillableUnits(Long netBillableUnits) { this.netBillableUnits = netBillableUnits; }
    public BigDecimal getSuccessRate() { return successRate; }
    public void setSuccessRate(BigDecimal successRate) { this.successRate = successRate; }
    public List<AiUsageTrendPoint> getTrend() { return trend; }
    public void setTrend(List<AiUsageTrendPoint> trend) { this.trend = trend; }
    public List<AiUsageStoreSummary> getStores() { return stores; }
    public void setStores(List<AiUsageStoreSummary> stores) { this.stores = stores; }
    public List<AiUsageCapabilitySummary> getCapabilities() { return capabilities; }
    public void setCapabilities(List<AiUsageCapabilitySummary> capabilities) { this.capabilities = capabilities; }

    /** 按日期汇总。 */
    @Note("AI用量趋势点")
    public static class AiUsageTrendPoint {
        private LocalDate date;
        private Long requestCount = 0L;
        private Long successCount = 0L;
        private Long shadowBillableUnits = 0L;

        public AiUsageTrendPoint() { }

        public AiUsageTrendPoint(LocalDate date, Long requestCount, Long successCount, Long shadowBillableUnits) {
            this.date = date;
            this.requestCount = requestCount;
            this.successCount = successCount;
            this.shadowBillableUnits = shadowBillableUnits;
        }

        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        public Long getRequestCount() { return requestCount; }
        public void setRequestCount(Long requestCount) { this.requestCount = requestCount; }
        public Long getSuccessCount() { return successCount; }
        public void setSuccessCount(Long successCount) { this.successCount = successCount; }
        public Long getShadowBillableUnits() { return shadowBillableUnits; }
        public void setShadowBillableUnits(Long shadowBillableUnits) { this.shadowBillableUnits = shadowBillableUnits; }
    }

    /** 按门店与企业公共汇总。 */
    @Note("AI用量门店汇总")
    public static class AiUsageStoreSummary {
        private Long storeId;
        private String storeName;
        private AiStoreAllocation allocationType;
        private Long requestCount = 0L;
        private Long successCount = 0L;
        private Long failureCount = 0L;
        private Long resourceUnits = 0L;
        private Long shadowBillableUnits = 0L;

        public Long getStoreId() { return storeId; }
        public void setStoreId(Long storeId) { this.storeId = storeId; }
        public String getStoreName() { return storeName; }
        public void setStoreName(String storeName) { this.storeName = storeName; }
        public AiStoreAllocation getAllocationType() { return allocationType; }
        public void setAllocationType(AiStoreAllocation allocationType) { this.allocationType = allocationType; }
        public Long getRequestCount() { return requestCount; }
        public void setRequestCount(Long requestCount) { this.requestCount = requestCount; }
        public Long getSuccessCount() { return successCount; }
        public void setSuccessCount(Long successCount) { this.successCount = successCount; }
        public Long getFailureCount() { return failureCount; }
        public void setFailureCount(Long failureCount) { this.failureCount = failureCount; }
        public Long getResourceUnits() { return resourceUnits; }
        public void setResourceUnits(Long resourceUnits) { this.resourceUnits = resourceUnits; }
        public Long getShadowBillableUnits() { return shadowBillableUnits; }
        public void setShadowBillableUnits(Long shadowBillableUnits) { this.shadowBillableUnits = shadowBillableUnits; }
    }

    /** 按 AI 能力汇总。 */
    @Note("AI用量能力汇总")
    public static class AiUsageCapabilitySummary {
        private String capabilityCode;
        private Long requestCount = 0L;
        private Long successCount = 0L;
        private Long failureCount = 0L;
        private Long shadowBillableUnits = 0L;

        public String getCapabilityCode() { return capabilityCode; }
        public void setCapabilityCode(String capabilityCode) { this.capabilityCode = capabilityCode; }
        public Long getRequestCount() { return requestCount; }
        public void setRequestCount(Long requestCount) { this.requestCount = requestCount; }
        public Long getSuccessCount() { return successCount; }
        public void setSuccessCount(Long successCount) { this.successCount = successCount; }
        public Long getFailureCount() { return failureCount; }
        public void setFailureCount(Long failureCount) { this.failureCount = failureCount; }
        public Long getShadowBillableUnits() { return shadowBillableUnits; }
        public void setShadowBillableUnits(Long shadowBillableUnits) { this.shadowBillableUnits = shadowBillableUnits; }
    }
}
