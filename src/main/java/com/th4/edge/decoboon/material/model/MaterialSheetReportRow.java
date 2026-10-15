package com.th4.edge.decoboon.material.model;

import com.th4.edge.framework.core.annotation.Note;

/** 主材下单作业单品类汇总行（聚合口径，供工作台与报表使用）。 */
@Note("主材下单作业单汇总行")
public class MaterialSheetReportRow {

    @Note("单据类型（品类）") private String sheetType;
    @Note("单据数") private int sheetCount;
    @Note("未定价单据数（成本待结算）") private int unpricedSheetCount;
    @Note("已进入采购的单据数") private int orderedSheetCount;

    public String getSheetType() { return sheetType; }
    public void setSheetType(String sheetType) { this.sheetType = sheetType; }
    public int getSheetCount() { return sheetCount; }
    public void setSheetCount(int sheetCount) { this.sheetCount = sheetCount; }
    public int getUnpricedSheetCount() { return unpricedSheetCount; }
    public void setUnpricedSheetCount(int unpricedSheetCount) { this.unpricedSheetCount = unpricedSheetCount; }
    public int getOrderedSheetCount() { return orderedSheetCount; }
    public void setOrderedSheetCount(int orderedSheetCount) { this.orderedSheetCount = orderedSheetCount; }
}
