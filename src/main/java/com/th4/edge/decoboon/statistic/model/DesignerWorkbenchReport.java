package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 设计师业务工作台只读聚合。
 *
 * <p>只返回聚合数量、金额概览和时间趋势，不返回客户、报价、项目等实体明细。
 * 前端继续以工作台注册表和 Dashboard 配置提供名称、Icon 与路由。</p>
 */
@Note("设计师业务工作台")
public class DesignerWorkbenchReport {

    @Note("角色")
    private String role;

    @Note("当前员工编号")
    private Long employeeId;

    @Note("当前员工姓名")
    private String employeeName;

    @Note("所属企业编号")
    private Long companyId;

    @Note("所属门店编号")
    private Long storeId;

    @Note("数据范围")
    private String dataScope;

    @Note("生成时间")
    private LocalDateTime generatedAt;

    @Note("当前自然月")
    private String month;

    @Note("趋势开始日期")
    private LocalDate trendStart;

    @Note("趋势结束日期")
    private LocalDate trendEnd;

    @Note("响应状态：ready/partial")
    private String status = "ready";

    @Note("当月概览")
    private List<SummaryMetric> summary = new ArrayList<>();

    @Note("业务工作卡")
    private List<CardMetric> cards = new ArrayList<>();

    @Note("方案报价主链")
    private List<PipelineStage> pipeline = new ArrayList<>();

    @Note("趋势图")
    private List<Chart> charts = new ArrayList<>();

    @Note("数据警告")
    private List<Warning> warnings = new ArrayList<>();

    @Note("本企业设计师本月签约额排名投影")
    private PerformanceRanking performanceRanking = new PerformanceRanking();

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public String getDataScope() { return dataScope; }
    public void setDataScope(String dataScope) { this.dataScope = dataScope; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }
    public LocalDate getTrendStart() { return trendStart; }
    public void setTrendStart(LocalDate trendStart) { this.trendStart = trendStart; }
    public LocalDate getTrendEnd() { return trendEnd; }
    public void setTrendEnd(LocalDate trendEnd) { this.trendEnd = trendEnd; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<SummaryMetric> getSummary() { return summary; }
    public void setSummary(List<SummaryMetric> summary) { this.summary = summary == null ? new ArrayList<>() : new ArrayList<>(summary); }
    public List<CardMetric> getCards() { return cards; }
    public void setCards(List<CardMetric> cards) { this.cards = cards == null ? new ArrayList<>() : new ArrayList<>(cards); }
    public List<PipelineStage> getPipeline() { return pipeline; }
    public void setPipeline(List<PipelineStage> pipeline) { this.pipeline = pipeline == null ? new ArrayList<>() : new ArrayList<>(pipeline); }
    public List<Chart> getCharts() { return charts; }
    public void setCharts(List<Chart> charts) { this.charts = charts == null ? new ArrayList<>() : new ArrayList<>(charts); }
    public List<Warning> getWarnings() { return warnings; }
    public void setWarnings(List<Warning> warnings) { this.warnings = warnings == null ? new ArrayList<>() : new ArrayList<>(warnings); }
    public PerformanceRanking getPerformanceRanking() { return performanceRanking; }
    public void setPerformanceRanking(PerformanceRanking performanceRanking) {
        this.performanceRanking = performanceRanking == null ? new PerformanceRanking() : performanceRanking;
    }

    /** 当月概览指标。 */
    @Note("当月概览指标")
    public static class SummaryMetric {
        @Note("指标 key") private String key;
        @Note("指标值") private BigDecimal value = BigDecimal.ZERO;
        @Note("单位：count/amount") private String unit;
        @Note("上一周期值；无基期时为空") private BigDecimal previousValue;
        @Note("变化率；无基期时为空") private BigDecimal changeRate;
        @Note("对比状态：up/down/flat/unavailable") private String comparisonState = "unavailable";
        @Note("对比说明") private String comparisonLabel = "暂无基期";

        public SummaryMetric() { }

        public SummaryMetric(String key, BigDecimal value, String unit) {
            this.key = key;
            this.value = value;
            this.unit = unit;
        }

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public BigDecimal getValue() { return value; }
        public void setValue(BigDecimal value) { this.value = value; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        public BigDecimal getPreviousValue() { return previousValue; }
        public void setPreviousValue(BigDecimal previousValue) { this.previousValue = previousValue; }
        public BigDecimal getChangeRate() { return changeRate; }
        public void setChangeRate(BigDecimal changeRate) { this.changeRate = changeRate; }
        public String getComparisonState() { return comparisonState; }
        public void setComparisonState(String comparisonState) { this.comparisonState = comparisonState; }
        public String getComparisonLabel() { return comparisonLabel; }
        public void setComparisonLabel(String comparisonLabel) { this.comparisonLabel = comparisonLabel; }
    }

    /** 方案报价主链阶段。 */
    @Note("方案报价主链阶段")
    public static class PipelineStage {
        @Note("阶段 key") private String key;
        @Note("阶段名称") private String label;
        @Note("阶段数量") private Integer count = 0;
        @Note("转化率；未裁决时为空") private BigDecimal rate;
        @Note("上一阶段数量") private Integer previousCount;
        @Note("阶段状态：done/current/pending") private String state = "pending";
        @Note("说明") private String description;
        @Note("进入路由") private String route;

        public PipelineStage() { }

        public PipelineStage(String key, String label, Integer count, BigDecimal rate, Integer previousCount,
                             String state, String description, String route) {
            this.key = key;
            this.label = label;
            this.count = count;
            this.rate = rate;
            this.previousCount = previousCount;
            this.state = state;
            this.description = description;
            this.route = route;
        }

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public Integer getCount() { return count; }
        public void setCount(Integer count) { this.count = count; }
        public BigDecimal getRate() { return rate; }
        public void setRate(BigDecimal rate) { this.rate = rate; }
        public Integer getPreviousCount() { return previousCount; }
        public void setPreviousCount(Integer previousCount) { this.previousCount = previousCount; }
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getRoute() { return route; }
        public void setRoute(String route) { this.route = route; }
    }

    /** 业务工作卡。 */
    @Note("业务工作卡")
    public static class CardMetric {
        @Note("指标 key") private String key;
        @Note("主数量") private Integer count = 0;
        @Note("风险数量；未计算结果时为空") private Integer riskCount;
        @Note("卡片状态：ready/partial") private String status = "ready";
        @Note("细分数量") private List<Breakdown> breakdown = new ArrayList<>();

        public CardMetric() { }

        public CardMetric(String key, Integer count, List<Breakdown> breakdown) {
            this.key = key;
            this.count = count;
            this.breakdown = breakdown == null ? new ArrayList<>() : new ArrayList<>(breakdown);
        }

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public Integer getCount() { return count; }
        public void setCount(Integer count) { this.count = count; }
        public Integer getRiskCount() { return riskCount; }
        public void setRiskCount(Integer riskCount) { this.riskCount = riskCount; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public List<Breakdown> getBreakdown() { return breakdown; }
        public void setBreakdown(List<Breakdown> breakdown) { this.breakdown = breakdown == null ? new ArrayList<>() : new ArrayList<>(breakdown); }
    }

    /** 卡片细分数量。 */
    @Note("卡片细分数量")
    public static class Breakdown {
        @Note("细分 key") private String key;
        @Note("细分数量") private Integer count = 0;

        public Breakdown() { }

        public Breakdown(String key, Integer count) {
            this.key = key;
            this.count = count;
        }

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public Integer getCount() { return count; }
        public void setCount(Integer count) { this.count = count; }
    }

    /** 趋势图。 */
    @Note("趋势图")
    public static class Chart {
        @Note("图表 key") private String key;
        @Note("图表类型：line/bar") private String type;
        @Note("图表系列") private List<ChartSeries> series = new ArrayList<>();

        public Chart() { }

        public Chart(String key, String type, List<ChartSeries> series) {
            this.key = key;
            this.type = type;
            this.series = series == null ? new ArrayList<>() : new ArrayList<>(series);
        }

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public List<ChartSeries> getSeries() { return series; }
        public void setSeries(List<ChartSeries> series) { this.series = series == null ? new ArrayList<>() : new ArrayList<>(series); }
    }

    /** 图表系列。 */
    @Note("图表系列")
    public static class ChartSeries {
        @Note("系列 key") private String key;
        @Note("单值；用于分布图") private BigDecimal value;
        @Note("时间点；用于趋势图") private List<TrendPoint> points = new ArrayList<>();

        public ChartSeries() { }

        public ChartSeries(String key, BigDecimal value, List<TrendPoint> points) {
            this.key = key;
            this.value = value;
            this.points = points == null ? new ArrayList<>() : new ArrayList<>(points);
        }

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public BigDecimal getValue() { return value; }
        public void setValue(BigDecimal value) { this.value = value; }
        public List<TrendPoint> getPoints() { return points; }
        public void setPoints(List<TrendPoint> points) { this.points = points == null ? new ArrayList<>() : new ArrayList<>(points); }
    }

    /** 趋势点。 */
    @Note("趋势点")
    public static class TrendPoint {
        @Note("日期") private LocalDate date;
        @Note("数量") private BigDecimal value = BigDecimal.ZERO;

        public TrendPoint() { }

        public TrendPoint(LocalDate date, BigDecimal value) {
            this.date = date;
            this.value = value;
        }

        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        public BigDecimal getValue() { return value; }
        public void setValue(BigDecimal value) { this.value = value; }
    }

    /** 数据警告。 */
    @Note("数据警告")
    public static class Warning {
        @Note("警告 key") private String key;
        @Note("影响范围 key") private String scopeKey;
        @Note("说明") private String message;

        public Warning() { }

        public Warning(String key, String scopeKey, String message) {
            this.key = key;
            this.scopeKey = scopeKey;
            this.message = message;
        }

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getScopeKey() { return scopeKey; }
        public void setScopeKey(String scopeKey) { this.scopeKey = scopeKey; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    /** 设计师本人排名与激励投影。 */
    @Note("设计师本月签约额排名")
    public static class PerformanceRanking {
        private String metricKey = "designerMonthSignedAmount";
        private String metricLabel = "本月签约金额";
        private String periodLabel = "本月";
        private Integer rank = 0;
        private Integer designerCount = 0;
        private Boolean sampleSufficient = Boolean.TRUE;
        private BigDecimal myValue = BigDecimal.ZERO;
        private BigDecimal topValue = BigDecimal.ZERO;
        private BigDecimal peerAverage = BigDecimal.ZERO;
        private BigDecimal gapToTop = BigDecimal.ZERO;
        private BigDecimal gapToAverage = BigDecimal.ZERO;
        private List<RankingItem> topDesigners = new ArrayList<>();

        public String getMetricKey() { return metricKey; }
        public void setMetricKey(String metricKey) { this.metricKey = metricKey; }
        public String getMetricLabel() { return metricLabel; }
        public void setMetricLabel(String metricLabel) { this.metricLabel = metricLabel; }
        public String getPeriodLabel() { return periodLabel; }
        public void setPeriodLabel(String periodLabel) { this.periodLabel = periodLabel; }
        public Integer getRank() { return rank; }
        public void setRank(Integer rank) { this.rank = rank; }
        public Integer getDesignerCount() { return designerCount; }
        public void setDesignerCount(Integer designerCount) { this.designerCount = designerCount; }
        public Boolean getSampleSufficient() { return sampleSufficient; }
        public void setSampleSufficient(Boolean sampleSufficient) { this.sampleSufficient = sampleSufficient; }
        public BigDecimal getMyValue() { return myValue; }
        public void setMyValue(BigDecimal myValue) { this.myValue = myValue; }
        public BigDecimal getTopValue() { return topValue; }
        public void setTopValue(BigDecimal topValue) { this.topValue = topValue; }
        public BigDecimal getPeerAverage() { return peerAverage; }
        public void setPeerAverage(BigDecimal peerAverage) { this.peerAverage = peerAverage; }
        public BigDecimal getGapToTop() { return gapToTop; }
        public void setGapToTop(BigDecimal gapToTop) { this.gapToTop = gapToTop; }
        public BigDecimal getGapToAverage() { return gapToAverage; }
        public void setGapToAverage(BigDecimal gapToAverage) { this.gapToAverage = gapToAverage; }
        public List<RankingItem> getTopDesigners() { return topDesigners; }
        public void setTopDesigners(List<RankingItem> topDesigners) {
            this.topDesigners = topDesigners == null ? new ArrayList<>() : new ArrayList<>(topDesigners);
        }
    }

    /** 排名摘要，仅返回员工姓名和本月签约额。 */
    @Note("设计师签约额排名项")
    public static class RankingItem {
        private Long employeeId;
        private String employeeName;
        private Integer rank = 0;
        private BigDecimal value = BigDecimal.ZERO;
        private Integer signedProjectCount = 0;
        private Boolean currentEmployee = Boolean.FALSE;

        public Long getEmployeeId() { return employeeId; }
        public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
        public String getEmployeeName() { return employeeName; }
        public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
        public Integer getRank() { return rank; }
        public void setRank(Integer rank) { this.rank = rank; }
        public BigDecimal getValue() { return value; }
        public void setValue(BigDecimal value) { this.value = value; }
        public Integer getSignedProjectCount() { return signedProjectCount; }
        public void setSignedProjectCount(Integer signedProjectCount) { this.signedProjectCount = signedProjectCount; }
        public Boolean getCurrentEmployee() { return currentEmployee; }
        public void setCurrentEmployee(Boolean currentEmployee) { this.currentEmployee = currentEmployee; }
    }
}
