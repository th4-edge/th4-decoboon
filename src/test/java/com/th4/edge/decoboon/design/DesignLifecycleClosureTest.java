package com.th4.edge.decoboon.design;

import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.configure.PricingMethod;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.design.configure.DesignStatus;
import com.th4.edge.decoboon.bim.configure.SpaceType;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.DesignItem;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.design.repository.DesignItemRepository;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import com.th4.edge.decoboon.design.repository.SpaceRepository;
import com.th4.edge.decoboon.project.configure.ContractStatus;
import com.th4.edge.decoboon.project.context.ProjectContractContext;
import com.th4.edge.decoboon.project.context.ProjectContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.property.configure.PropertyStatus;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.quote.context.QuoteContext;
import com.th4.edge.decoboon.quote.model.Quote;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 设计方案状态随报价、合同、项目主链闭环推进的集成测试。
 *
 * <p>覆盖用户反馈的真实断链场景：客户已经接受报价、完成签约并进入施工，
 * 业主端设计方案却一直停留在“待谈单”。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:decoboon-design-lifecycle;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
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
class DesignLifecycleClosureTest {

    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private StoreRepository storeRepository;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private PropertyRepository propertyRepository;
    @Autowired
    private CatalogItemRepository catalogItemRepository;
    @Autowired
    private DesignRepository designRepository;
    @Autowired
    private SpaceRepository spaceRepository;
    @Autowired
    private DesignItemRepository designItemRepository;
    @Autowired
    private QuoteContext quoteContext;
    @Autowired
    private ProjectContractContext projectContractContext;
    @Autowired
    private ProjectContext projectContext;

    @Test
    void designMustLeavePendingTalkOnceQuoteAcceptedContractSignedAndProjectStarted() {
        long seq = System.currentTimeMillis();
        Company company = new Company();
        company.setComId(seq);
        company.setTitle("设计状态闭环测试企业");
        company.setName("设计状态测试");
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
        designer.setTelNo("19900000011");
        designer.setCompany(company);
        designer.setStore(store);
        designer.setRole(EmployeeRole.Designer);
        designer = employeeRepository.save(designer);

        CatalogItem item = new CatalogItem();
        item.setCompany(company);
        item.setMaintainer(designer);
        item.setType(ItemType.Construction);
        item.setCategory("墙面施工");
        item.setName("墙面施工");
        item.setUnit(Unit.Area);
        item.setPricingMode(PricingMethod.MaterialAndManual);
        item.setMaterialPrice(new BigDecimal("80"));
        item.setManualPrice(BigDecimal.ZERO);
        item.setCostPrice(new BigDecimal("40"));
        item.setFormula("固定项");
        item.setActive(true);
        item.setSort(0);
        item = catalogItemRepository.save(item);

        Customer customer = new Customer();
        customer.setCompany(company);
        customer.setName("测试客户");
        customer.setTelNo("19900000012");
        customer.setArea(new BigDecimal("120"));
        customer.setAddr("测试小区 2 栋 202");
        customer.setOwner(designer);
        customer = customerRepository.save(customer);

        Property property = new Property();
        property.setCompany(company);
        property.setStore(store);
        property.setOwner(designer);
        property.setCustomer(customer);
        property.setPropertyNo("DP" + seq);
        property.setAddress(customer.getAddr());
        property.setCommunity(customer.getCommunity());
        property.setArea(customer.getArea());
        property.setStatus(PropertyStatus.PendingMeasure);
        property = propertyRepository.save(property);

        Design design = new Design();
        design.setNo("D" + seq);
        design.setName("测试设计方案");
        design.setCompany(company);
        design.setStore(store);
        design.setOwner(designer);
        design.setCustomer(customer);
        design.setProperty(property);
        design.setVersion(1);
        design.setArea(new BigDecimal("100"));
        design.setStatus(DesignStatus.Ready);
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
        spaceRepository.save(space);

        DesignItem designItem = new DesignItem();
        designItem.setCompany(company);
        designItem.setStore(store);
        designItem.setOwner(designer);
        designItem.setDesign(design);
        designItem.setSpace(space);
        designItem.setProperty(property);
        designItem.setType(ItemType.Construction);
        designItem.setCategory("墙面施工");
        designItem.setCatalogItem(item);
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
        designItemRepository.save(designItem);

        Quote quote = quoteContext.create(design, BigDecimal.ONE);
        assertThat(reload(design).getStatus()).isEqualTo(DesignStatus.Ready);

        quoteContext.submit(quote.getQuoteId());
        assertThat(reload(design).getStatus()).isEqualTo(DesignStatus.Ready);

        quoteContext.accept(quote.getQuoteId());
        assertThat(reload(design).getStatus())
                .as("客户接受报价后设计方案必须进入客户选中")
                .isEqualTo(DesignStatus.Selected);
        assertThat(customer.getStatus()).isEqualTo(CustStatus.Quoted);

        BigDecimal signedAmount = quote.getAmount();
        ProjectContract contract = projectContractContext.createFromQuote(
                quote, designer, null, signedAmount, LocalDate.now(), LocalDate.now().plusDays(90), "设计状态闭环合同");
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.PendingSign);
        assertThat(reload(design).getStatus()).isEqualTo(DesignStatus.Selected);

        projectContractContext.markSigned(contract, LocalDate.now(), signedAmount);
        assertThat(reload(design).getStatus())
                .as("合同签约后设计方案不得回退")
                .isEqualTo(DesignStatus.Selected);

        projectContractContext.markEffective(contract);
        assertThat(reload(design).getStatus()).isEqualTo(DesignStatus.Selected);

        Project draft = new Project();
        draft.setCompany(company);
        draft.setName("设计状态闭环项目");
        draft.setSiteAddr(property.getAddress());
        draft.setStartDate(LocalDate.now());
        draft.setPlanEndDate(LocalDate.now().plusDays(90));
        draft.setManager(designer);
        Project project = projectContext.createFromEffectiveContract(contract, draft);

        assertThat(project.getProjectId()).isNotNull();
        assertThat(reload(design).getStatus())
                .as("项目立项进入施工后业主端不得再显示待谈单")
                .isEqualTo(DesignStatus.Selected);
    }

    private Design reload(Design design) {
        return designRepository.findById(design.getDesignId()).orElseThrow();
    }
}
