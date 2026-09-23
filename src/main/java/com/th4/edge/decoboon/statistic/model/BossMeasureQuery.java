package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.decoboon.customer.configure.CustomerSource;
import com.th4.edge.framework.core.annotation.Note;

import java.time.LocalDate;

/** 企业负责人量房经营查询。 */
@Note("企业负责人量房经营查询")
public class BossMeasureQuery {

    private String view;
    private String status;
    private Long marketerId;
    private Long designerId;
    private CustomerSource source;
    private String keyword;
    private LocalDate month;
    private Long storeId;

    public String getView() { return view; }
    public void setView(String view) { this.view = view; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getMarketerId() { return marketerId; }
    public void setMarketerId(Long marketerId) { this.marketerId = marketerId; }
    public Long getDesignerId() { return designerId; }
    public void setDesignerId(Long designerId) { this.designerId = designerId; }
    public CustomerSource getSource() { return source; }
    public void setSource(CustomerSource source) { this.source = source; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public LocalDate getMonth() { return month; }
    public void setMonth(LocalDate month) { this.month = month; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
}
