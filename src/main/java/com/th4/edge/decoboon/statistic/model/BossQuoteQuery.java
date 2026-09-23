package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

/** 企业负责人报价谈单查询。 */
@Note("企业负责人报价谈单查询")
public class BossQuoteQuery {

    private Long customerId;

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
}
