package com.th4.edge.decoboon.pack;

import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.catalog.configure.CatalogSourceType;
import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.configure.PricingMethod;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
import com.th4.edge.decoboon.pack.configure.PackageTemplateStatus;
import com.th4.edge.decoboon.pack.context.PackageTypeSummaryContext;
import com.th4.edge.decoboon.pack.model.PackageLine;
import com.th4.edge.decoboon.pack.model.PackageSection;
import com.th4.edge.decoboon.pack.model.PackageTemplate;
import com.th4.edge.decoboon.pack.repository.PackageLineRepository;
import com.th4.edge.decoboon.pack.repository.PackageSectionRepository;
import com.th4.edge.decoboon.pack.repository.PackageTemplateRepository;
import com.th4.edge.tenant.user.configure.Size;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.repository.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** 套餐详情类型统计必须可在真实持久化查询上执行。 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:package-type-summary;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.jpa.open-in-view=false",
        "th4.edge.components.lock.enabled=false",
        "th4.edge.components.wechat.pay.enabled=false",
        "logging.level.root=ERROR",
        "logging.level.org.hibernate.SQL=OFF"
})
@Transactional
class PackageTypeSummaryIntegrationTest {

    @Autowired private CompanyRepository companies;
    @Autowired private PackageTemplateRepository templates;
    @Autowired private PackageSectionRepository sections;
    @Autowired private PackageLineRepository lines;
    @Autowired private CatalogItemRepository catalogItems;
    @Autowired private PackageTypeSummaryContext summary;

    @Test
    void summarizesSectionTypeCountsThroughRepositoryQuery() {
        Company company = company();
        PackageTemplate template = templates.saveAndFlush(template(company));
        PackageSection living = sections.saveAndFlush(section(company, "客厅"));
        PackageSection kitchen = sections.saveAndFlush(section(company, "厨房"));
        CatalogItem wall = catalogItems.saveAndFlush(item(company, ItemType.Construction, "墙面施工"));
        CatalogItem tile = catalogItems.saveAndFlush(item(company, ItemType.Material, "客厅瓷砖"));
        CatalogItem cabinet = catalogItems.saveAndFlush(item(company, ItemType.Custom, "厨房柜体"));

        saveLine(company, template, living, wall);
        saveLine(company, template, living, tile);
        saveLine(company, template, kitchen, cabinet);

        summary.apply(template);

        assertThat(template.getSectionLineCounts()).containsEntry(living.getGroupId(), 2).containsEntry(kitchen.getGroupId(), 1);
        assertThat(template.getSectionTypeCounts().get(living.getGroupId()))
                .containsEntry(ItemType.Construction, 1)
                .containsEntry(ItemType.Material, 1);
        assertThat(template.getTypeCounts()).containsEntry(ItemType.Custom, 1);
        assertThat(template.getLineCount()).isEqualTo(3);
        assertThat(template.getSectionReferenceAmounts())
                .containsEntry(living.getGroupId(), new BigDecimal("60.00"))
                .containsEntry(kitchen.getGroupId(), new BigDecimal("30.00"));
        assertThat(template.getReferenceAmount()).isEqualByComparingTo("90.00");
        assertThat(template.getReferenceCost()).isEqualByComparingTo("15.00");
    }

    private Company company() {
        Company company = new Company();
        company.setTitle("套餐类型统计测试企业");
        company.setName("套餐类型统计测试");
        company.setSize(Size.A);
        return companies.saveAndFlush(company);
    }

    private PackageTemplate template(Company company) {
        PackageTemplate template = new PackageTemplate();
        template.setCompany(company);
        template.setCode("PACK-TYPE-TEST");
        template.setVersion(1);
        template.setStatus(PackageTemplateStatus.Draft);
        template.setCurrentPublished(false);
        template.setName("类型统计套餐");
        template.setArea(new BigDecimal("100"));
        template.setPrice(new BigDecimal("100000"));
        template.setDiscount(BigDecimal.ONE);
        template.setCost(BigDecimal.ZERO);
        return templates.saveAndFlush(template);
    }

    private PackageSection section(Company company, String name) {
        PackageSection section = new PackageSection();
        section.setCompany(company);
        section.setName(name);
        return section;
    }

    private CatalogItem item(Company company, ItemType type, String name) {
        CatalogItem item = new CatalogItem();
        item.setCompany(company);
        item.setType(type);
        item.setCategory("测试分类");
        item.setName(name);
        item.setUnit(Unit.Item);
        item.setPricingMode(PricingMethod.MaterialAndManual);
        item.setMaterialPrice(new BigDecimal("10.00"));
        item.setManualPrice(new BigDecimal("20.00"));
        item.setCostPrice(new BigDecimal("5.00"));
        item.setFormula("A");
        item.setSourceType(CatalogSourceType.Manual);
        item.setRevision(1);
        return item;
    }

    private void saveLine(Company company, PackageTemplate template, PackageSection section, CatalogItem item) {
        PackageLine line = new PackageLine();
        line.setCompany(company);
        line.setTemplate(template);
        line.setSection(section);
        line.setItem(item);
        line.setName(item.getName());
        line.setUnit(Unit.Item);
        line.setQuantityFactor(BigDecimal.ONE);
        line.setPricingMethod(PricingMethod.MaterialAndManual);
        line.setCatalogTypeSnapshot(item.getType());
        line.setSectionNameSnapshot(section.getName());
        line.setCostPriceSnapshot(new BigDecimal("5.00"));
        lines.saveAndFlush(line);
    }
}
