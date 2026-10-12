package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** 门店对比矩阵报表（`P0D-05`）。全部门店合计 = Σ门店 + 未归属门店。 */
@Note("门店对比矩阵")
public class StoreComparisonReport {

    @Note("期间（月）")
    private LocalDate month;
    @Note("期间文案（yyyy-MM）")
    private String period;
    @Note("排名口径说明（分母与期间）")
    private String rankingNote;
    @Note("各门店行（含未归属行与停用门店行）")
    private List<StoreComparisonRow> rows = new ArrayList<>();
    @Note("全部门店合计（Σ门店 + 未归属）")
    private StoreComparisonRow total;

    public LocalDate getMonth() { return month; }
    public void setMonth(LocalDate month) { this.month = month; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public String getRankingNote() { return rankingNote; }
    public void setRankingNote(String rankingNote) { this.rankingNote = rankingNote; }
    public List<StoreComparisonRow> getRows() { return rows; }
    public void setRows(List<StoreComparisonRow> rows) { this.rows = rows; }
    public StoreComparisonRow getTotal() { return total; }
    public void setTotal(StoreComparisonRow total) { this.total = total; }
}
