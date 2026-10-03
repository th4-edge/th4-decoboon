package com.th4.edge.decoboon.ai.model;

import com.th4.edge.framework.core.annotation.Note;

import java.time.LocalDate;

/** AI 用量统计与调用明细查询条件。 */
@Note("AI用量查询")
public class AiUsageQuery {

    @Note("统计开始日期，默认本月一日")
    private LocalDate from;

    @Note("统计结束日期，默认今天")
    private LocalDate to;

    @Note("分摊门店；空表示企业全部门店加企业公共")
    private Long storeId;

    @Note("AI能力编码")
    private String capabilityCode;

    @Note("业务模块")
    private String businessModule;

    @Note("页码")
    private Integer current = 1;

    @Note("每页数量")
    private Integer size = 20;

    public LocalDate getFrom() { return from; }
    public void setFrom(LocalDate from) { this.from = from; }
    public LocalDate getTo() { return to; }
    public void setTo(LocalDate to) { this.to = to; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public String getCapabilityCode() { return capabilityCode; }
    public void setCapabilityCode(String capabilityCode) { this.capabilityCode = capabilityCode; }
    public String getBusinessModule() { return businessModule; }
    public void setBusinessModule(String businessModule) { this.businessModule = businessModule; }
    public Integer getCurrent() { return current; }
    public void setCurrent(Integer current) { this.current = current; }
    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
}
