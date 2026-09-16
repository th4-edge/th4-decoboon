package com.th4.edge.decoboon.pack.model;

import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.framework.core.annotation.Note;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.math.BigDecimal;

@Embeddable
public class PackageOption {

    @Note("差价")
    @Column(nullable = false, precision = 10, scale = 2)
    private java.math.BigDecimal price;

    @Note("单位")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false,length = 32)
    private Unit unit;

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }
}
