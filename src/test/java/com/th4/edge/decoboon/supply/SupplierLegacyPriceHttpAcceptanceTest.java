package com.th4.edge.decoboon.supply;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.configure.PricingMethod;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
import com.th4.edge.decoboon.supply.configure.PartnerStatus;
import com.th4.edge.decoboon.supply.configure.PartnerType;
import com.th4.edge.decoboon.supply.model.MaterialSupplier;
import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.decoboon.supply.model.PartnerPrice;
import com.th4.edge.decoboon.supply.repository.MaterialSupplierRepository;
import com.th4.edge.decoboon.supply.repository.PartnerPriceRepository;
import com.th4.edge.decoboon.supply.repository.PartnerRepository;
import com.th4.edge.tenant.user.configure.CredType;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.configure.Size;
import com.th4.edge.tenant.user.configure.UserStatus;
import com.th4.edge.tenant.user.context.PasswordContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.CompanyRepository;
import com.th4.edge.tenant.user.repository.CredentialsRepository;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import com.th4.edge.tenant.user.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** EOP-D7a HTTP 级：旧供应商价格只读、成本价人工维护、跨企业与角色门禁。 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:supplier-legacy-price;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false",
        "spring.jpa.show-sql=false",
        "th4.edge.components.lock.enabled=false",
        "th4.edge.components.wechat.pay.enabled=false",
        "th4.edge.components.storage.provider=local",
        "th4.edge.framework.upload-dir=${java.io.tmpdir}/supplier-legacy-price-upload",
        "logging.level.root=ERROR",
        "logging.level.org.hibernate.SQL=OFF"
})
@AutoConfigureMockMvc
@Transactional
class SupplierLegacyPriceHttpAcceptanceTest {

    private static final String PASSWORD = "Accept1234";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordContext passwordContext;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private CredentialsRepository credentialsRepository;
    @Autowired private PartnerRepository partnerRepository;
    @Autowired private CatalogItemRepository catalogItemRepository;
    @Autowired private PartnerPriceRepository partnerPriceRepository;
    @Autowired private MaterialSupplierRepository materialSupplierRepository;

    private Fixture fixture;

    @BeforeEach
    void setUp() {
        fixture = fixture();
    }

    @Test
    void legacyPartnerPriceSaveCannotOverwriteCostOrCrossTenant() throws Exception {
        String token = login(fixture.financePhone());

        JsonNode foreign = api("partner-price/save",
                """
                {"partnerId":%d,"itemId":%d,"laborPrice":12}
                """.formatted(fixture.foreignPartner().getPartnerId(), fixture.item().getItemId()), token);
        assertThat(foreign.path("success").asBoolean()).as(foreign.toString()).isFalse();
        assertThat(foreign.path("errorCode").path("code").asText()).isEqualTo("404");

        JsonNode saved = api("partner-price/save",
                """
                {"partnerPriceId":%d,"partnerId":%d,"itemId":%d,"purchasePrice":999,"laborPrice":12}
                """.formatted(fixture.legacyPrice().getPartnerPriceId(),
                fixture.partner().getPartnerId(), fixture.item().getItemId()), token);
        assertThat(saved.path("success").asBoolean()).as(saved.toString()).isTrue();

        PartnerPrice legacy = partnerPriceRepository.findById(fixture.legacyPrice().getPartnerPriceId()).orElseThrow();
        CatalogItem item = catalogItemRepository.findById(fixture.item().getItemId()).orElseThrow();
        assertThat(legacy.getPurchasePrice()).isEqualByComparingTo("68.00");
        assertThat(legacy.getLaborPrice()).isEqualByComparingTo("12.00");
        assertThat(item.getCostPrice()).isEqualByComparingTo("88.00");
    }

    @Test
    void materialSupplySaveIsCompanyScopedAndWriterRoleProtected() throws Exception {
        String finance = login(fixture.financePhone());
        JsonNode foreign = api("supply/material-supplier/save",
                """
                {"partnerId":%d,"catalogItemId":%d,"brand":"测试","unit":"Item"}
                """.formatted(fixture.foreignPartner().getPartnerId(), fixture.item().getItemId()), finance);
        assertThat(foreign.path("success").asBoolean()).as(foreign.toString()).isFalse();
        assertThat(foreign.path("errorCode").path("code").asText()).isEqualTo("404");

        JsonNode page = api("supply/material-supplier/page", "{}", finance);
        assertThat(page.path("success").asBoolean()).as(page.toString()).isTrue();
        assertThat(page.path("data").size()).isEqualTo(1);
        assertThat(page.path("data").get(0).path("partner").path("name").asText()).isEqualTo("本企业供应商");

        String designer = login(fixture.designerPhone());
        JsonNode denied = api("supply/material-supplier/save",
                """
                {"partnerId":%d,"catalogItemId":%d,"brand":"测试","unit":"Item"}
                """.formatted(fixture.partner().getPartnerId(), fixture.item().getItemId()), designer);
        assertThat(denied.path("success").asBoolean()).as(denied.toString()).isFalse();
        assertThat(denied.path("errorCode").path("code").asText()).isEqualTo("403");
    }

    @Test
    void enterpriseSupplierMasterIsSharedAcrossSameCompanyStores() throws Exception {
        String storeAFinance = login(fixture.financePhone());
        String storeBManager = login(fixture.storeManagerBPhone());
        String sharedName = "双店共享供应商";

        JsonNode created = api("partner/save",
                """
                {"type":"Supplier","name":"%s"}
                """.formatted(sharedName), storeBManager);
        assertThat(created.path("success").asBoolean()).as(created.toString()).isTrue();

        JsonNode pageA = api("partner/page", "{\"type\":\"Supplier\"}", storeAFinance);
        JsonNode pageB = api("partner/page", "{\"type\":\"Supplier\"}", storeBManager);
        assertThat(containsPartner(pageA.path("data"), sharedName)).as(pageA.toString()).isTrue();
        assertThat(containsPartner(pageB.path("data"), sharedName)).as(pageB.toString()).isTrue();

        JsonNode relationA = api("supply/material-supplier/page", "{}", storeAFinance);
        JsonNode relationB = api("supply/material-supplier/page", "{}", storeBManager);
        assertThat(relationA.path("data").size()).isEqualTo(1);
        assertThat(relationB.path("data").size()).isEqualTo(1);
        assertThat(relationA.path("data").get(0).path("partner").path("name").asText()).isEqualTo("本企业供应商");
    }

    private Fixture fixture() {
        Company company = company("本企业", 1);
        Company foreignCompany = company("外企业", 2);
        Store store = store(company, "本企业总店", true);
        Store secondStore = store(company, "本企业二店", false);
        Store foreignStore = store(foreignCompany, "外企业总店", true);
        Employee finance = employee(company, store, EmployeeRole.Finance, "本企业财务", 101L);
        Employee designer = employee(company, store, EmployeeRole.Designer, "本企业设计", 102L);
        Employee storeManagerB = employee(company, secondStore, EmployeeRole.StoreManager, "本企业二店店长", 104L);
        Employee foreignFinance = employee(foreignCompany, foreignStore, EmployeeRole.Finance, "外企业财务", 103L);
        Partner partner = partner(company, finance, "本企业供应商");
        Partner foreignPartner = partner(foreignCompany, foreignFinance, "外企业供应商");
        CatalogItem item = catalogItem(company, finance, "本企业材料");
        CatalogItem foreignItem = catalogItem(foreignCompany, finance, "外企业材料");

        PartnerPrice legacy = new PartnerPrice();
        legacy.setCompany(company);
        legacy.setPartner(partner);
        legacy.setItem(item);
        legacy.setPurchasePrice(new BigDecimal("68.00"));
        legacy.setLaborPrice(new BigDecimal("10.00"));
        legacy.setActive(true);
        legacy = partnerPriceRepository.save(legacy);

        MaterialSupplier current = new MaterialSupplier();
        current.setCompany(company);
        current.setPartner(partner);
        current.setCatalogItem(item);
        current.setMaterialKey("ci:" + item.getItemId() + "|brand:测试|unit:Item");
        current.setMaterialName("本企业材料");
        current.setBrand("测试");
        current.setUnit(Unit.Item);
        current.setPurchasePrice(new BigDecimal("68.00"));
        current.setCreatedBy(finance);
        current.setActive(true);
        materialSupplierRepository.save(current);

        MaterialSupplier foreign = new MaterialSupplier();
        foreign.setCompany(foreignCompany);
        foreign.setPartner(foreignPartner);
        foreign.setCatalogItem(foreignItem);
        foreign.setMaterialKey("ci:" + foreignItem.getItemId() + "|brand:测试|unit:Item");
        foreign.setMaterialName("外企业材料");
        foreign.setBrand("测试");
        foreign.setUnit(Unit.Item);
        foreign.setPurchasePrice(new BigDecimal("99.00"));
        foreign.setCreatedBy(foreignFinance);
        foreign.setActive(true);
        materialSupplierRepository.save(foreign);

        return new Fixture(company, foreignCompany, finance, designer, partner, foreignPartner,
                item, foreignItem, legacy, finance.getTelNo(), designer.getTelNo(), storeManagerB.getTelNo(),
                partner.getPartnerId(), foreignPartner.getPartnerId());
    }

    private Company company(String name, int seq) {
        Company company = new Company();
        company.setTitle(name + seq);
        company.setName(name);
        company.setSize(Size.A);
        return companyRepository.save(company);
    }

    private Store store(Company company, String name, boolean defaultStore) {
        Store store = new Store();
        store.setCompany(company);
        store.setName(name);
        store.setDefaultStore(defaultStore);
        return storeRepository.save(store);
    }

    private Employee employee(Company company, Store store, EmployeeRole role, String name, long seq) {
        String phone = "6" + String.format("%09d", Math.abs(seq));
        Employee employee = new Employee();
        employee.setCompany(company);
        employee.setStore(store);
        employee.setRole(role);
        employee.setStatus(EmployeeStatus.Active);
        employee.setName(name);
        employee.setTelNo(phone);
        employee.setJoinedAt(LocalDateTime.now());
        employee = employeeRepository.save(employee);

        Credentials credentials = new Credentials();
        credentials.setType(CredType.Password);
        credentials.setIdKey(phone);
        credentials.setIdSecret("password");
        credentials.setPhone(phone);
        credentials.setStatus(UserStatus.Active);
        credentials.setEmployee(employee);
        credentials.setPasswordHash(passwordContext.encode(PASSWORD));
        credentials.setPasswordUpdatedAt(LocalDateTime.now());
        credentials.setFailedAttempts(0);
        credentials.setMustChangePassword(false);
        credentialsRepository.save(credentials);
        return employee;
    }

    private Partner partner(Company company, Employee creator, String name) {
        Partner partner = new Partner();
        partner.setCompany(company);
        partner.setType(PartnerType.Supplier);
        partner.setName(name);
        partner.setStatus(PartnerStatus.Active);
        partner.setCreatedBy(creator);
        return partnerRepository.save(partner);
    }

    private CatalogItem catalogItem(Company company, Employee maintainer, String name) {
        CatalogItem item = new CatalogItem();
        item.setCompany(company);
        item.setMaintainer(maintainer);
        item.setType(ItemType.Material);
        item.setCode("CI-" + Math.abs(System.nanoTime() % 1000000));
        item.setCategory("测试");
        item.setName(name);
        item.setUnit(Unit.Item);
        item.setPricingMode(PricingMethod.MaterialOnly);
        item.setMaterialPrice(new BigDecimal("120.00"));
        item.setManualPrice(BigDecimal.ZERO);
        item.setCostPrice(new BigDecimal("88.00"));
        item.setFormula("材料单价 × 工程量");
        item.setActive(true);
        item.setSort(0);
        return catalogItemRepository.save(item);
    }

    private boolean containsPartner(JsonNode data, String name) {
        if (data == null || data.isMissingNode() || data.isNull()) {
            return false;
        }
        if (data.isArray()) {
            for (JsonNode row : data) {
                if (containsPartner(row, name)) {
                    return true;
                }
            }
            return false;
        }
        if (name.equals(data.path("name").asText())) {
            return true;
        }
        for (JsonNode child : data) {
            if (containsPartner(child, name)) {
                return true;
            }
        }
        return false;
    }

    private JsonNode api(String route, String body, String token) throws Exception {
        var request = post("/api/" + route)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
        if (token != null) {
            request.header("token", token);
        }
        MvcResult result = mockMvc.perform(request).andReturn();
        String content = result.getResponse().getContentAsString();
        return content == null || content.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(content);
    }

    private String login(String phone) throws Exception {
        JsonNode response = api("auth/session/password-login",
                "{\"phone\":\"%s\",\"password\":\"%s\"}".formatted(phone, PASSWORD), null);
        assertThat(response.path("success").asBoolean()).as(response.toString()).isTrue();
        String token = response.path("data").path("token").asText();
        assertThat(token).isNotBlank();
        return token;
    }

    private record Fixture(Company company, Company foreignCompany, Employee finance, Employee designer,
                           Partner partner, Partner foreignPartner, CatalogItem item, CatalogItem foreignItem,
                           PartnerPrice legacyPrice, String financePhone, String designerPhone,
                           String storeManagerBPhone, Long partnerId, Long foreignPartnerId) { }
}
