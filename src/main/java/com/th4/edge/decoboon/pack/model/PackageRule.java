package com.th4.edge.decoboon.pack.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Lob;


import java.math.BigDecimal;

@Note("差价公式")
@Embeddable
public class PackageRule {

    @Note("单价")
    @Unit("元")
    @Column(precision = 10, scale = 2,nullable = false)
    private java.math.BigDecimal price;

    @Note("标准差值")
    @Column(precision = 10, scale = 2,nullable = false)
    private java.math.BigDecimal standard;


    @Note("备注")
    @Lob
    private String remark;

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getStandard() {
        return standard;
    }

    public void setStandard(BigDecimal standard) {
        this.standard = standard;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
