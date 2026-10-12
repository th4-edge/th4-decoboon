package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 业务分析报表（企业负责人视角）：渠道 · 区域 · 客单 · 转化全链路。
 *
 * <p>口径边界（设计方案 §10.2 / §10.3）：</p>
 * <ul>
 *   <li>报价接受率按 {@code MET-001}/{@code DEC-001} 冻结的<strong>双指标</strong>实现，
 *       按「客户 + 房屋」机会去重，草稿、待决策与替代报价不计入分母；</li>
 *   <li>不含获客成本（系统无投放费用采集）；</li>
 *   <li>不含业务线结构（系统无业务线对象）；</li>
 *   <li>渠道维度取 {@code CustomerSource} 现有枚举值，未扩充枚举前不虚构渠道。</li>
 * </ul>
 */
@Note("业务分析报表")
public class BusinessAnalysisReport {

    @Note("统计月份")
    private LocalDate month;

    /** 门店维度范围标注（DEC-067 BOSS-STORE-03）。 */
    private StoreScopeInfo storeScope = new StoreScopeInfo();

    public StoreScopeInfo getStoreScope() { return storeScope; }
    public void setStoreScope(StoreScopeInfo storeScope) { this.storeScope = storeScope; }


    @Note("转化漏斗（线索 → 到店 → 量房 → 报价 → 签约）")
    private List<FunnelStage> funnel = List.of();

    @Note("整体转化率（%）= 本月签约数 ÷ 本月线索数")
    private BigDecimal overallConversionRate = BigDecimal.ZERO;

    @Note("报价转化双指标（MET-001）")
    private QuoteConversion quoteConversion = new QuoteConversion();

    @Note("渠道获客与转化")
    private List<ChannelMetric> channels = List.of();

    @Note("区域/小区签约分布（按签约额倒序）")
    private List<RegionMetric> regions = List.of();

    @Note("客单分布")
    private List<OrderSizeBucket> orderSizes = List.of();

    @Unit("元")
    @Note("中位数客单")
    private BigDecimal medianOrderAmount = BigDecimal.ZERO;

    @Unit("元")
    @Note("最高客单")
    private BigDecimal maxOrderAmount = BigDecimal.ZERO;

    public LocalDate getMonth() { return month; }
    public void setMonth(LocalDate month) { this.month = month; }
    public List<FunnelStage> getFunnel() { return funnel; }
    public void setFunnel(List<FunnelStage> funnel) { this.funnel = funnel; }
    public BigDecimal getOverallConversionRate() { return overallConversionRate; }
    public void setOverallConversionRate(BigDecimal overallConversionRate) { this.overallConversionRate = overallConversionRate; }
    public QuoteConversion getQuoteConversion() { return quoteConversion; }
    public void setQuoteConversion(QuoteConversion quoteConversion) { this.quoteConversion = quoteConversion; }
    public List<ChannelMetric> getChannels() { return channels; }
    public void setChannels(List<ChannelMetric> channels) { this.channels = channels; }
    public List<RegionMetric> getRegions() { return regions; }
    public void setRegions(List<RegionMetric> regions) { this.regions = regions; }
    public List<OrderSizeBucket> getOrderSizes() { return orderSizes; }
    public void setOrderSizes(List<OrderSizeBucket> orderSizes) { this.orderSizes = orderSizes; }
    public BigDecimal getMedianOrderAmount() { return medianOrderAmount; }
    public void setMedianOrderAmount(BigDecimal medianOrderAmount) { this.medianOrderAmount = medianOrderAmount; }
    public BigDecimal getMaxOrderAmount() { return maxOrderAmount; }
    public void setMaxOrderAmount(BigDecimal maxOrderAmount) { this.maxOrderAmount = maxOrderAmount; }

    /** 漏斗单段。 */
    @Note("转化漏斗单段")
    public static class FunnelStage {
        private String key;
        private String label;
        private Integer count = 0;
        private String unit;

        public FunnelStage() { }

        public FunnelStage(String key, String label, Integer count, String unit) {
            this.key = key;
            this.label = label;
            this.count = count;
            this.unit = unit;
        }

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public Integer getCount() { return count; }
        public void setCount(Integer count) { this.count = count; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
    }

    /** 报价接受率双指标。 */
    @Note("报价转化双指标")
    public static class QuoteConversion {
        @Note("最终报价接受率（%）= 最终接受或转项目的机会 ÷ 已产生决策的机会")
        private BigDecimal finalQuoteAcceptanceRate = BigDecimal.ZERO;
        @Note("首版报价接受率（%）= 首版被接受的机会 ÷ 首版已产生决策的机会")
        private BigDecimal firstQuoteAcceptanceRate = BigDecimal.ZERO;
        @Note("已产生客户决策的有效报价机会数（分母）")
        private Integer decidedOpportunityCount = 0;
        @Note("待客户决策的报价数量（单独展示，不混入接受率）")
        private Integer pendingDecisionQuoteCount = 0;
        @Note("已提交报价总数（本月口径）")
        private Integer submittedQuoteCount = 0;

        public BigDecimal getFinalQuoteAcceptanceRate() { return finalQuoteAcceptanceRate; }
        public void setFinalQuoteAcceptanceRate(BigDecimal value) { this.finalQuoteAcceptanceRate = value; }
        public BigDecimal getFirstQuoteAcceptanceRate() { return firstQuoteAcceptanceRate; }
        public void setFirstQuoteAcceptanceRate(BigDecimal value) { this.firstQuoteAcceptanceRate = value; }
        public Integer getDecidedOpportunityCount() { return decidedOpportunityCount; }
        public void setDecidedOpportunityCount(Integer value) { this.decidedOpportunityCount = value; }
        public Integer getPendingDecisionQuoteCount() { return pendingDecisionQuoteCount; }
        public void setPendingDecisionQuoteCount(Integer value) { this.pendingDecisionQuoteCount = value; }
        public Integer getSubmittedQuoteCount() { return submittedQuoteCount; }
        public void setSubmittedQuoteCount(Integer value) { this.submittedQuoteCount = value; }
    }

    /** 渠道指标。 */
    @Note("渠道获客与转化")
    public static class ChannelMetric {
        private String source;
        private String label;
        private Integer leadCount = 0;
        private Integer visitCount = 0;
        private Integer signedCount = 0;
        private BigDecimal signRate = BigDecimal.ZERO;
        @Unit("元")
        private BigDecimal avgOrderAmount = BigDecimal.ZERO;

        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public Integer getLeadCount() { return leadCount; }
        public void setLeadCount(Integer leadCount) { this.leadCount = leadCount; }
        public Integer getVisitCount() { return visitCount; }
        public void setVisitCount(Integer visitCount) { this.visitCount = visitCount; }
        public Integer getSignedCount() { return signedCount; }
        public void setSignedCount(Integer signedCount) { this.signedCount = signedCount; }
        public BigDecimal getSignRate() { return signRate; }
        public void setSignRate(BigDecimal signRate) { this.signRate = signRate; }
        public BigDecimal getAvgOrderAmount() { return avgOrderAmount; }
        public void setAvgOrderAmount(BigDecimal avgOrderAmount) { this.avgOrderAmount = avgOrderAmount; }
    }

    /** 区域/小区指标。 */
    @Note("区域签约分布")
    public static class RegionMetric {
        private String community;
        private Integer signedCount = 0;
        @Unit("元")
        private BigDecimal signedAmount = BigDecimal.ZERO;

        public RegionMetric() { }

        public RegionMetric(String community, Integer signedCount, BigDecimal signedAmount) {
            this.community = community;
            this.signedCount = signedCount;
            this.signedAmount = signedAmount;
        }

        public String getCommunity() { return community; }
        public void setCommunity(String community) { this.community = community; }
        public Integer getSignedCount() { return signedCount; }
        public void setSignedCount(Integer signedCount) { this.signedCount = signedCount; }
        public BigDecimal getSignedAmount() { return signedAmount; }
        public void setSignedAmount(BigDecimal signedAmount) { this.signedAmount = signedAmount; }
    }

    /** 客单分桶。 */
    @Note("客单分布")
    public static class OrderSizeBucket {
        private String key;
        private String label;
        private Integer count = 0;

        public OrderSizeBucket() { }

        public OrderSizeBucket(String key, String label, Integer count) {
            this.key = key;
            this.label = label;
            this.count = count;
        }

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public Integer getCount() { return count; }
        public void setCount(Integer count) { this.count = count; }
    }
}
