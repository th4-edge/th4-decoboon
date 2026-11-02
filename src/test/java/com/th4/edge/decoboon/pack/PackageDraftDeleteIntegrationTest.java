package com.th4.edge.decoboon.pack;

import com.th4.edge.components.lock.DistributedLock;
import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.catalog.configure.CatalogSourceType;
import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.configure.PricingMethod;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
import com.th4.edge.decoboon.pack.configure.PackageTemplateStatus;
import com.th4.edge.decoboon.pack.context.PackageContext;
import com.th4.edge.decoboon.pack.context.PackageLifecycleContext;
import com.th4.edge.decoboon.pack.context.PackageSnapshotContext;
import com.th4.edge.decoboon.pack.model.PackageSection;
import com.th4.edge.decoboon.pack.model.PackageTemplate;
import com.th4.edge.decoboon.pack.repository.PackageLineRepository;
import com.th4.edge.decoboon.pack.repository.PackageSectionRepository;
import com.th4.edge.decoboon.pack.repository.PackageTemplateRepository;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.Size;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.CompanyRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 套餐草稿删除必须级联清理项目行与说明，不遗留孤儿数据。 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:package-draft-delete;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
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
class PackageDraftDeleteIntegrationTest {

    @Autowired private CompanyRepository companies;
    @Autowired private PackageTemplateRepository templates;
    @Autowired private PackageLineRepository lines;
    @Autowired private PackageSectionRepository sections;
    @Autowired private CatalogItemRepository catalogItems;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void deletesDraftAndCascadesLinesAndCriterions() {
        Company company = company();
        Employee boss = employee(company);
        PackageTemplate savedTemplate = templates.saveAndFlush(template(company));
        PackageSection savedSection = sections.saveAndFlush(section(company));
        CatalogItem savedItem = catalogItems.saveAndFlush(catalogItem(company));
        PackageTemplate template = entityManager.find(PackageTemplate.class, savedTemplate.getTemplateId());
        PackageSection section = entityManager.find(PackageSection.class, savedSection.getGroupId());
        CatalogItem item = entityManager.find(CatalogItem.class, savedItem.getItemId());
        long lineId = 900000000000000001L;
        jdbc.update("""
                insert into pack_line (item_id, com_id, template_id, section_id, catalog_item_id, name, unit,
                                       quantity_factor, pricing_method, included, deductible, sort)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, lineId, company.getComId(), template.getTemplateId(), section.getGroupId(), item.getItemId(),
                "墙面粉刷", "Area", BigDecimal.ONE, "MaterialAndManual", true, true, 0);
        jdbc.update("insert into pack_line_criterions (item_id, criterions) values (?, ?)",
                lineId, "按实测面积计算");
        entityManager.flush();
        entityManager.clear();

        TenantContext tenant = mock(TenantContext.class);
        DistributedLock lock = mock(DistributedLock.class);
        when(tenant.requireRole(eq(boss), eq(EmployeeRole.Boss), eq(EmployeeRole.StoreManager))).thenReturn(boss);
        when(lock.tryLock(any(), eq(1000L), eq(30000L))).thenReturn(true);
        PackageLifecycleContext context = new PackageLifecycleContext(
                templates, lines, mock(PackageContext.class), mock(PackageSnapshotContext.class),
                tenant, lock, new TransactionTemplate(transactionManager));

        context.deleteDraft(template.getTemplateId(), boss);
        entityManager.flush();
        entityManager.clear();

        assertThat(templates.findById(template.getTemplateId())).isEmpty();
        assertThat(jdbc.queryForObject("select count(*) from pack_line where item_id = ?", Long.class, lineId))
                .isZero();
        assertThat(jdbc.queryForObject("select count(*) from pack_line_criterions where item_id = ?", Long.class, lineId))
                .isZero();
        assertThat(jdbc.queryForObject(
                        "select count(*) from pack_template_criterions where package_template_id = ?",
                        Long.class, template.getTemplateId()))
                .isZero();
    }

    private Company company() {
        Company company = new Company();
        company.setTitle("套餐删除测试企业");
        company.setName("套餐删除测试");
        company.setSize(Size.A);
        return companies.saveAndFlush(company);
    }

    private Employee employee(Company company) {
        Employee employee = new Employee();
        employee.setEmpId(1L);
        employee.setName("测试负责人");
        employee.setRole(EmployeeRole.Boss);
        employee.setCompany(company);
        return employee;
    }

    private PackageTemplate template(Company company) {
        PackageTemplate template = new PackageTemplate();
        template.setCompany(company);
        template.setCode("PACK-DELETE-TEST");
        template.setVersion(1);
        template.setStatus(PackageTemplateStatus.Draft);
        template.setCurrentPublished(false);
        template.setName("待删除套餐");
        template.setArea(new BigDecimal("100"));
        template.setPrice(new BigDecimal("100000"));
        template.setDiscount(BigDecimal.ONE);
        template.setCost(BigDecimal.ZERO);
        template.setCriterions(List.of("测试套餐"));
        return template;
    }

    private PackageSection section(Company company) {
        PackageSection section = new PackageSection();
        section.setCompany(company);
        section.setName("基础施工");
        return section;
    }

    private CatalogItem catalogItem(Company company) {
        CatalogItem item = new CatalogItem();
        item.setCompany(company);
        item.setType(ItemType.Construction);
        item.setCategory("基础施工");
        item.setName("墙面粉刷");
        item.setUnit(Unit.Area);
        item.setPricingMode(PricingMethod.MaterialAndManual);
        item.setMaterialPrice(BigDecimal.ZERO);
        item.setManualPrice(BigDecimal.ZERO);
        item.setFormula("A");
        item.setSourceType(CatalogSourceType.Manual);
        item.setRevision(1);
        return item;
    }

}
