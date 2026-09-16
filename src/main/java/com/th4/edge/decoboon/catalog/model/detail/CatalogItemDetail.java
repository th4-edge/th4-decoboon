package com.th4.edge.decoboon.catalog.model.detail;

import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.model.AbstractModel;
import com.th4.edge.tenant.user.model.Company;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToOne;

/** 六类可售项目类型明细的共同基类。 */
@Note("可售项目类型明细")
@MappedSuperclass
public abstract class CatalogItemDetail extends AbstractModel {

    @Note("所属企业")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @Note("所属可售项目")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false, unique = true)
    private CatalogItem item;

    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public CatalogItem getItem() { return item; }
    public void setItem(CatalogItem item) { this.item = item; }
}
