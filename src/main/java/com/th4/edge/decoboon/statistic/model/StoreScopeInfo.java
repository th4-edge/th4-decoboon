package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

/**
 * 门店维度范围标注（`DEC-067` BOSS-STORE-03，`DESIGN-BIZ-20261012-001` §1）。
 *
 * <p>随报表返回，供前端展示「全部门店 / 某店 / 含未归属门店 N 条」，让口径自证。</p>
 */
@Note("门店维度范围标注")
public class StoreScopeInfo {

    /** AllStores（全部门店） | Store（指定门店）。 */
    @Note("范围类型")
    private String scopeType = "AllStores";

    @Note("门店 ID；全部门店为空")
    private Long storeId;

    @Note("门店名；全部门店为空")
    private String storeName;

    /** 该企业范围内 `store` 为空的记录数（单店视图不含未归属，恒为 0）。 */
    @Note("未归属门店记录数")
    private Integer unassignedCount = 0;

    public String getScopeType() { return scopeType; }
    public void setScopeType(String scopeType) { this.scopeType = scopeType; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public Integer getUnassignedCount() { return unassignedCount; }
    public void setUnassignedCount(Integer unassignedCount) { this.unassignedCount = unassignedCount; }
}
