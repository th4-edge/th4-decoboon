package com.th4.edge.decoboon.customer;

import com.th4.edge.decoboon.bim.configure.SpaceType;
import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.configure.PricingMethod;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
import com.th4.edge.decoboon.customer.context.CustomerJourneyContext;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerJourney;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.design.configure.DesignStatus;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.DesignItem;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.design.repository.DesignItemRepository;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import com.th4.edge.decoboon.design.repository.SpaceRepository;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.property.configure.MeasureStatus;
import com.th4.edge.decoboon.property.configure.PropertyStatus;
import com.th4.edge.decoboon.property.model.Measure;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.quote.configure.QuoteLineRole;
import com.th4.edge.decoboon.quote.configure.QuoteMode;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.Size;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.CompanyRepository;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import com.th4.edge.tenant.user.repository.StoreRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 业主端「事实页」数据装配测试。
 *
 * <p>业主端量房、设计、报价各自是独立页面，但都依赖同一份旅程装配；
 * 本测试保证量房照片（带时间）、设计图纸、区域尺寸、按区域分组的报价清单
 * 都能真实产出，而不是退化为纯文本行。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:customer-journey-enrichment;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "th4.edge.components.lock.enabled=false",
        "th4.edge.components.wechat.pay.enabled=false",
        "logging.level.org.hibernate.SQL=OFF"
})
@Transactional
class CustomerJourneyEnrichmentTest {

    @Autowired private CompanyRepository companyRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private PropertyRepository propertyRepository;
    @Autowired private MeasureRepository measureRepository;
    @Autowired private CatalogItemRepository catalogItemRepository;
    @Autowired private DesignRepository designRepository;
    @Autowired private SpaceRepository spaceRepository;
    @Autowired private DesignItemRepository designItemRepository;
    @Autowired private QuoteRepository quoteRepository;
    @Autowired private QuoteLineRepository quoteLineRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private CustomerJourneyContext journeyContext;

    @Test
    void journeyMustExposePhotosRegionsAndRegionalQuoteGroups() {
        long seq = System.currentTimeMillis();
        Company company = new Company();
        company.setComId(seq);
        company.setTitle("业主事实页测试企业");
        company.setName("事实页测试");
        company.setSize(Size.A);
        company = companyRepository.save(company);

        Store store = new Store();
        store.setStoreId(seq + 1);
        store.setCompany(company);
        store.setName("测试门店");
        store.setDefaultStore(true);
        store = storeRepository.save(store);

        Employee designer = new Employee();
        designer.setEmpId(seq + 2);
        designer.setName("测试设计师");
        designer.setTelNo("19900000021");
        designer.setCompany(company);
        designer.setStore(store);
        designer.setRole(EmployeeRole.Designer);
        designer = employeeRepository.save(designer);

        Customer customer = new Customer();
        customer.setCompany(company);
        customer.setStore(store);
        customer.setOwner(designer);
        customer.setName("测试客户");
        customer.setTelNo("19900000022");
        customer.setCommunity("测试小区");
        customer.setAddr("测试小区 3 栋 303");
        customer.setArea(new BigDecimal("100"));
        customer = customerRepository.save(customer);

        Property property = new Property();
        property.setCompany(company);
        property.setStore(store);
        property.setOwner(designer);
        property.setCustomer(customer);
        property.setPropertyNo("PE" + seq);
        property.setCommunity(customer.getCommunity());
        property.setAddress(customer.getAddr());
        property.setArea(customer.getArea());
        property.setStatus(PropertyStatus.Serving);
        property = propertyRepository.save(property);

        Measure measure = new Measure();
        measure.setCompany(company);
        measure.setStore(store);
        measure.setCustomer(customer);
        measure.setProperty(property);
        measure.setOwner(designer);
        measure.setDesigner(designer);
        measure.setExecutor(designer);
        measure.setMeasureTime(LocalDateTime.of(2026, 9, 20, 10, 30));
        measure.setStatus(MeasureStatus.Completed);
        measure.setSitePhotos(List.of("upload/measure/site-1.jpg", "upload/measure/site-2.jpg"));
        measure.setStructureImages(List.of("upload/measure/structure-1.jpg"));
        measure.setDimensions("客厅 5.0×4.0");
        measureRepository.save(measure);

        CatalogItem catalogItem = new CatalogItem();
        catalogItem.setCompany(company);
        catalogItem.setMaintainer(designer);
        catalogItem.setType(ItemType.Construction);
        catalogItem.setCategory("墙面施工");
        catalogItem.setName("墙面施工");
        catalogItem.setUnit(Unit.Area);
        catalogItem.setPricingMode(PricingMethod.MaterialAndManual);
        catalogItem.setMaterialPrice(new BigDecimal("80"));
        catalogItem.setManualPrice(BigDecimal.ZERO);
        catalogItem.setCostPrice(new BigDecimal("40"));
        catalogItem.setFormula("固定项");
        catalogItem.setActive(true);
        catalogItem.setSort(0);
        catalogItem = catalogItemRepository.save(catalogItem);

        Design design = new Design();
        design.setNo("DE" + seq);
        design.setName("测试设计方案");
        design.setCompany(company);
        design.setStore(store);
        design.setOwner(designer);
        design.setCustomer(customer);
        design.setProperty(property);
        design.setVersion(1);
        design.setArea(new BigDecimal("20"));
        design.setStyle("现代简约");
        design.setStatus(DesignStatus.Selected);
        design.setCadPreviewUrl("upload/design/cad-preview.jpg");
        design.setCoverUrl("upload/design/cover.jpg");
        design = designRepository.save(design);

        Space space = new Space();
        space.setCompany(company);
        space.setStore(store);
        space.setOwner(designer);
        space.setDesign(design);
        space.setProperty(property);
        space.setType(SpaceType.LivingRoom);
        space.setName("客厅");
        space.setLength(new BigDecimal("5"));
        space.setWidth(new BigDecimal("4"));
        space.setHeight(new BigDecimal("2.8"));
        space.setArea(new BigDecimal("20"));
        space.setPerimeter(new BigDecimal("18"));
        space = spaceRepository.save(space);

        DesignItem designItem = new DesignItem();
        designItem.setCompany(company);
        designItem.setStore(store);
        designItem.setOwner(designer);
        designItem.setDesign(design);
        designItem.setSpace(space);
        designItem.setProperty(property);
        designItem.setType(ItemType.Construction);
        designItem.setCategory("墙面施工");
        designItem.setCatalogItem(catalogItem);
        designItem.setName("墙面施工");
        designItem.setUnit(Unit.Area);
        designItem.setQuantity(new BigDecimal("40"));
        designItem.setMaterialPrice(new BigDecimal("80"));
        designItem.setManualPrice(BigDecimal.ZERO);
        designItem.setCostPrice(new BigDecimal("40"));
        designItem.setPricingMode(PricingMethod.MaterialAndManual);
        designItem.setAmount(new BigDecimal("3200"));
        designItem.setCost(new BigDecimal("1600"));
        designItem.setFormula("固定项");
        designItem.setSort(0);
        designItem = designItemRepository.save(designItem);

        Quote quote = new Quote();
        quote.setNo("QE" + seq);
        quote.setTitle("测试报价");
        quote.setCompany(company);
        quote.setStore(store);
        quote.setCustomer(customer);
        quote.setProperty(property);
        quote.setDesign(design);
        quote.setCreatedBy(designer);
        quote.setVersion(1);
        quote.setMode(QuoteMode.Custom);
        quote.setArea(property.getArea());
        quote.setSubtotal(new BigDecimal("3200"));
        quote.setDiscount(BigDecimal.ONE);
        quote.setTaxRate(new BigDecimal("0.05"));
        quote.setTaxAmount(new BigDecimal("160"));
        quote.setAmount(new BigDecimal("3360"));
        quote.setCost(new BigDecimal("1600"));
        quote.setDesignFee(BigDecimal.ZERO);
        quote.setManagementFee(BigDecimal.ZERO);
        quote.setStatus(QuoteStatus.Accepted);
        quote = quoteRepository.save(quote);

        QuoteLine line = new QuoteLine();
        line.setQuote(quote);
        line.setCompany(company);
        line.setStore(store);
        line.setOwner(designer);
        line.setProperty(property);
        line.setDesignItem(designItem);
        line.setCatalogItem(catalogItem);
        line.setSpace(space);
        line.setRole(QuoteLineRole.Base);
        line.setType(ItemType.Construction);
        line.setCategory("墙面施工");
        line.setName("墙面施工");
        line.setUnit(Unit.Area);
        line.setQuantity(new BigDecimal("40"));
        line.setMaterialPrice(new BigDecimal("80"));
        line.setManualPrice(BigDecimal.ZERO);
        line.setPricingMode(PricingMethod.MaterialAndManual);
        line.setPrice(new BigDecimal("80"));
        line.setAmount(new BigDecimal("3200"));
        line.setCost(new BigDecimal("1600"));
        quoteLineRepository.save(line);

        Project project = new Project();
        project.setNo("PJE" + seq);
        project.setName("事实页测试项目");
        project.setCompany(company);
        project.setCustomer(customer);
        project.setProperty(property);
        project.setQuote(quote);
        project.setStore(store);
        project.setOwner(designer);
        project.setDesigner(designer);
        project.setManager(designer);
        project.setSiteAddr(property.getAddress());
        project.setArea(property.getArea());
        project.setContractAmount(new BigDecimal("3360"));
        project.setCost(new BigDecimal("1600"));
        project.setStatus(ProjectStatus.Ongoing);
        project.setProgress(20);
        project.setStartDate(LocalDate.now());
        project.setPlanEndDate(LocalDate.now().plusDays(90));
        project = projectRepository.save(project);

        CustomerJourney journey = journeyContext.build(project);

        CustomerJourney.Section measureSection = section(journey, "measure");
        CustomerJourney.Item measureItem = measureSection.getItems().get(0);
        assertThat(measureItem.getMedia()).extracting(CustomerJourney.Media::getKind)
                .contains("Photo", "Structure");
        assertThat(measureItem.getMedia()).filteredOn(m -> "Photo".equals(m.getKind()))
                .allSatisfy(m -> assertThat(m.getTime()).isNotBlank());

        CustomerJourney.Section designSection = section(journey, "design");
        CustomerJourney.Item designFact = designSection.getItems().get(0);
        assertThat(designFact.getMedia()).extracting(CustomerJourney.Media::getKind).contains("Cad", "Render");
        assertThat(designFact.getRegions()).hasSize(1);
        CustomerJourney.Region region = designFact.getRegions().get(0);
        assertThat(region.getName()).isEqualTo("客厅");
        assertThat(region.getArea()).isEqualTo("20");
        assertThat(region.getLength()).isEqualTo("5");
        assertThat(region.getItemCount()).isEqualTo(1);
        assertThat(designFact.getPriceGroups()).hasSize(1);
        assertThat(designFact.getPriceGroups().get(0).getRows()).hasSize(1);

        CustomerJourney.Section quoteSection = section(journey, "quote");
        CustomerJourney.Item quoteFact = quoteSection.getItems().get(0);
        assertThat(quoteFact.getHasSourceLines()).isTrue();
        assertThat(quoteFact.getRegionGroups()).hasSize(1);
        assertThat(quoteFact.getTypeGroups()).hasSize(1);
        var group = quoteFact.getRegionGroups().get(0);
        assertThat(group.getName()).isEqualTo("客厅");
        assertThat(group.getAmount()).isEqualByComparingTo("3200.00");
        assertThat(group.getItems()).hasSize(1);
        assertThat(group.getItems().get(0).getName()).isEqualTo("墙面施工");
        assertThat(group.getItems().get(0).getAmount()).isEqualByComparingTo("3200.00");

        // 服务主体知情权（USER-DECISION-20261014-002）：
        // 业主端可以看到服务方与为这一户服务的人，但**不得出现员工个人手机号**。
        assertThat(journey.getVendor()).isNotNull();
        assertThat(journey.getVendor().getName()).isEqualTo("业主事实页测试企业");
        assertThat(journey.getVendor().getCustodianCredit())
                .as("平台署名默认关闭（ANS-BIZ-20261010-004 §8）").isFalse();
        assertThat(journey.getTeam()).isNotEmpty();
        assertThat(journey.getTeam()).allSatisfy(member -> {
            assertThat(member.getName()).isNotBlank();
            assertThat(member.getRoleName()).isNotBlank();
        });
        String ownerFacing = journey.getVendor().getServiceTel() + "|" + journey.getVendor().getAfterSaleTel()
                + "|" + journey.getTeam().stream()
                .map(member -> member.getName() + "/" + member.getRoleName())
                .collect(java.util.stream.Collectors.joining(","));
        assertThat(ownerFacing)
                .as("业主端投影不得出现员工个人手机号")
                .doesNotContain("19900000021")
                .doesNotContain("19900000022");
    }

    private CustomerJourney.Section section(CustomerJourney journey, String key) {
        return journey.getSections().stream()
                .filter(item -> key.equals(item.getKey()))
                .findFirst()
                .orElseThrow();
    }
}
