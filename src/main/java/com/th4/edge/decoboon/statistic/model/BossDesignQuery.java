package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

/** 企业负责人设计经营查询。 */
@Note("企业负责人设计经营查询")
public class BossDesignQuery {

    private Long designerId;

    public Long getDesignerId() { return designerId; }
    public void setDesignerId(Long designerId) { this.designerId = designerId; }
}
