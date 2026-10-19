package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

/** 门店经营利润请求：店长/门店财务由服务端锁定本店；BOSS 需指定门店。 */
@Note("门店经营利润请求")
public class StoreProfitRequest {
    private Long storeId;

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
}
