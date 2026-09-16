package com.th4.edge.decoboon.owner;

import com.th4.edge.decoboon.common.file.configure.FileScope;
import com.th4.edge.decoboon.common.file.model.FileGrant;
import com.th4.edge.decoboon.customer.configure.CustomerAccessStatus;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerAccess;
import com.th4.edge.decoboon.customer.repository.CustomerAccessRepository;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.configure.ReportType;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectReport;
import com.th4.edge.decoboon.project.repository.ProjectReportRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.property.configure.PropertyStatus;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.quote.configure.QuoteMode;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.CredType;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.Size;
import com.th4.edge.tenant.user.configure.UserStatus;
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
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 私有文件授权、签名、路径边界和旧公开入口的独立验收。 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:owner-file-exposure;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        // 本地 config/application-dev.yml 会携带 OSS 配置，本测试断言的是本地签名下载链路，这里显式回到本地存储
        "th4.edge.components.storage.provider=local",
        "th4.edge.components.lock.enabled=false",
        "th4.edge.components.wechat.pay.enabled=false",
        "th4.edge.framework.upload-dir=/private/tmp/th4-owner-file-acceptance/private-root",
        "th4.edge.framework.public-dir=/private/tmp/th4-owner-file-acceptance/public-root",
        "th4.edge.framework.file-signature-secret=acceptance-file-secret",
        "th4.edge.framework.file-signature-ttl-seconds=300",
        "logging.level.org.hibernate.SQL=OFF"
})
@AutoConfigureMockMvc
@Transactional
class OwnerFileExposureAcceptanceTest {

    private static final Path PRIVATE_ROOT = Path.of("/private/tmp/th4-owner-file-acceptance/private-root");
    private static final Path PUBLIC_ROOT = Path.of("/private/tmp/th4-owner-file-acceptance/public-root");
    private static final String PRIVATE_PATH = "upload/reports/safe.txt";
    private static final String MISSING_PATH = "upload/reports/missing.txt";
    private static final String PRIVATE_CONTENT = "private contract content";

    @Autowired private MockMvc mockMvc;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private CredentialsRepository credentialsRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private PropertyRepository propertyRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private ProjectReportRepository reportRepository;
    @Autowired private QuoteRepository quoteRepository;
    @Autowired private CustomerAccessRepository accessRepository;

    @Autowired private com.th4.edge.decoboon.common.file.service.grants.build.Build buildProvider;
    @Autowired private com.th4.edge.decoboon.common.file.service.grants.download.Download downloadProvider;

    @BeforeEach
    void prepareStorageAndUnwrapProviders() throws Exception {
        buildProvider = AopTestUtils.getTargetObject(buildProvider);
        downloadProvider = AopTestUtils.getTargetObject(downloadProvider);
        Files.createDirectories(PRIVATE_ROOT.resolve("reports"));
        Files.createDirectories(PUBLIC_ROOT);
        Files.writeString(PRIVATE_ROOT.resolve("reports/safe.txt"), PRIVATE_CONTENT);
        Files.writeString(PUBLIC_ROOT.resolve("open.txt"), "public asset");
    }

    @Test
    void legacyUploadIs404AndOnlyExplicitPublicDirectoryIsAnonymous() throws Exception {
        mockMvc.perform(get("/upload/reports/safe.txt"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/public/open.txt"))
                .andExpect(status().isOk())
                .andExpect(content().string("public asset"));
        mockMvc.perform(get("/public/reports/safe.txt"))
                .andExpect(status().isNotFound());
    }

    @Test
    void employeeCanGrantAndDownloadOwnedReportFile() throws Exception {
        Fixture fixture = fixture("employee-file");
        FileGrant grant = grant(fixture.marketerA(), FileScope.Report, fixture.reportA1().getReportId(), PRIVATE_PATH);

        assertThat(grant.getProjectId()).isEqualTo(fixture.projectA1().getProjectId());
        assertThat(grant.getUrl()).startsWith("/file/private/download?path=");
        Signed grantParts = parse(grant.getUrl());
        assertThat(download(grantParts)).hasContent(PRIVATE_CONTENT);
        mockMvc.perform(get("/api" + grant.getUrl()).header("User-Agent", "acceptance-test"))
                .andExpect(status().isOk())
                .andExpect(content().string(PRIVATE_CONTENT));
    }

    @Test
    void ownerCanGrantAndDownloadExplicitlyAuthorizedVisibleReportFile() throws Exception {
        Fixture fixture = fixture("owner-file");
        FileGrant grant = grant(fixture.owner(), FileScope.Report, fixture.reportA1().getReportId(), PRIVATE_PATH);

        assertThat(grant.getProjectId()).isEqualTo(fixture.projectA1().getProjectId());
        Signed grantParts = parse(grant.getUrl());
        assertThat(download(grantParts)).hasContent(PRIVATE_CONTENT);
    }

    @Test
    void unauthenticatedCrossCompanyAndCrossProjectGrantAreRejected() {
        Fixture fixture = fixture("deny-scope");

        assertDefined(() -> grant(null, FileScope.Report, fixture.reportA1().getReportId(), PRIVATE_PATH), "404");
        assertDefined(() -> grant(fixture.marketerB(), FileScope.Report, fixture.reportA1().getReportId(), PRIVATE_PATH), "404");
        assertDefined(() -> grant(fixture.owner(), FileScope.Report, fixture.reportA2().getReportId(), PRIVATE_PATH), "404");
    }

    @Test
    void ownerCannotGrantHiddenOrUnownedReportFile() {
        Fixture fixture = fixture("owner-hidden");
        fixture.reportA1().setVisibleToCustomer(false);
        reportRepository.save(fixture.reportA1());

        assertDefined(() -> grant(fixture.owner(), FileScope.Report, fixture.reportA1().getReportId(), PRIVATE_PATH), "404");
    }

    @Test
    void expiredAndTamperedSignaturesAreRejected() {
        Fixture fixture = fixture("signature");
        FileGrant grant = grant(fixture.marketerA(), FileScope.Report, fixture.reportA1().getReportId(), PRIVATE_PATH);
        Signed valid = parse(grant.getUrl());

        Signed expired = new Signed(valid.path(), Instant.now().getEpochSecond() - 1, valid.signature());
        assertDefined(() -> download(expired), "404");

        String tamperedValue = valid.signature().substring(0, valid.signature().length() - 1)
                + (valid.signature().endsWith("A") ? "B" : "A");
        Signed tampered = new Signed(valid.path(), valid.expires(), tamperedValue);
        assertDefined(() -> download(tampered), "404");
    }

    @Test
    void traversalAndMissingOrUnownedPathAreRejected() {
        Fixture fixture = fixture("path-boundary");

        assertDefined(() -> grant(fixture.marketerA(), FileScope.Report,
                fixture.reportA1().getReportId(), "upload/../etc/passwd"), "404");
        assertDefined(() -> grant(fixture.marketerA(), FileScope.Report,
                fixture.reportA1().getReportId(), "upload/reports/other.txt"), "404");

        ProjectReport missing = report(fixture.projectA1(), fixture.marketerA(), "missing", MISSING_PATH);
        FileGrant missingGrant = grant(fixture.marketerA(), FileScope.Report, missing.getReportId(), MISSING_PATH);
        assertDefined(() -> download(parse(missingGrant.getUrl())), "404");
    }

    @Test
    void publicDirectoryCannotAccessPrivateDirectoryFile() throws Exception {
        mockMvc.perform(get("/public/reports/safe.txt"))
                .andExpect(status().isNotFound());
    }

    private Fixture fixture(String prefix) {
        Company companyA = company();
        Company companyB = company();
        Store storeA = store(companyA);
        Store storeB = store(companyB);
        Employee marketerA = employee(companyA, storeA, EmployeeRole.Marketer);
        Employee marketerB = employee(companyB, storeB, EmployeeRole.Marketer);
        Customer customerA1 = customer(companyA, storeA, marketerA);
        Customer customerA2 = customer(companyA, storeA, marketerA);
        Project projectA1 = project(customerA1, marketerA, prefix + "-A1");
        Project projectA2 = project(customerA2, marketerA, prefix + "-A2");
        ProjectReport reportA1 = report(projectA1, marketerA, prefix + "-report-A1", PRIVATE_PATH);
        ProjectReport reportA2 = report(projectA2, marketerA, prefix + "-report-A2", PRIVATE_PATH);
        Credentials owner = credential(prefix + "-owner");
        access(companyA, owner, customerA1, projectA1, prefix + "-owner");
        return new Fixture(marketerA, marketerB, owner, projectA1, projectA2, reportA1, reportA2);
    }

    private FileGrant grant(Object principal, FileScope scope, Long businessId, String path) {
        ReflectionTestUtils.setField(buildProvider, "principal", principal);
        ReflectionTestUtils.setField(buildProvider, "businessType", scope);
        ReflectionTestUtils.setField(buildProvider, "businessId", businessId);
        ReflectionTestUtils.setField(buildProvider, "paths", new ArrayList<>(List.of(path)));
        return buildProvider.call(new FileGrant()).get(0);
    }

    private File download(Signed signed) {
        ReflectionTestUtils.setField(downloadProvider, "path", signed.path());
        ReflectionTestUtils.setField(downloadProvider, "expires", signed.expires());
        ReflectionTestUtils.setField(downloadProvider, "signature", signed.signature());
        return downloadProvider.call(new FileGrant());
    }

    private Signed parse(String url) {
        URI uri = URI.create(url);
        Map<String, String> values = new LinkedHashMap<>();
        Arrays.stream(uri.getRawQuery().split("&")).forEach(item -> {
            String[] pair = item.split("=", 2);
            values.put(pair[0], URLDecoder.decode(pair.length > 1 ? pair[1] : "", StandardCharsets.UTF_8));
        });
        return new Signed(values.get("path"), Long.parseLong(values.get("expires")), values.get("signature"));
    }

    private Company company() {
        Company company = new Company();
        company.setTitle("文件验收企业");
        company.setName("C" + Long.toHexString(System.nanoTime()));
        company.setSize(Size.A);
        return companyRepository.save(company);
    }

    private Store store(Company company) {
        Store store = new Store();
        store.setCompany(company);
        store.setName("S" + Long.toHexString(System.nanoTime()));
        store.setDefaultStore(true);
        return storeRepository.save(store);
    }

    private Employee employee(Company company, Store store, EmployeeRole role) {
        Employee employee = new Employee();
        employee.setCompany(company);
        employee.setStore(store);
        employee.setRole(role);
        employee.setName("E" + Long.toHexString(System.nanoTime()));
        employee.setTelNo("1" + Math.abs(System.nanoTime() % 1_000_000_000L));
        return employeeRepository.save(employee);
    }

    private Customer customer(Company company, Store store, Employee owner) {
        Customer customer = new Customer();
        customer.setCompany(company);
        customer.setStore(store);
        customer.setOwner(owner);
        customer.setName("X" + Long.toHexString(System.nanoTime()));
        customer.setTelNo("2" + Math.abs(System.nanoTime() % 1_000_000_000L));
        customer.setCommunity("小区");
        customer.setAddr("文件验收地址");
        customer.setArea(new BigDecimal("100"));
        return customerRepository.save(customer);
    }

    private Project project(Customer customer, Employee owner, String name) {
        Property property = new Property();
        property.setCompany(customer.getCompany());
        property.setStore(customer.getStore());
        property.setOwner(owner);
        property.setCustomer(customer);
        property.setPropertyNo("P-" + Long.toHexString(System.nanoTime()));
        property.setCommunity(customer.getCommunity());
        property.setAddress(customer.getAddr());
        property.setArea(new BigDecimal("100"));
        property.setStatus(PropertyStatus.Serving);
        property = propertyRepository.save(property);

        Quote quote = new Quote();
        quote.setNo("Q-" + Long.toHexString(System.nanoTime()));
        quote.setTitle("文件验收报价");
        quote.setCompany(customer.getCompany());
        quote.setStore(customer.getStore());
        quote.setCustomer(customer);
        quote.setProperty(property);
        quote.setCreatedBy(owner);
        quote.setVersion(1);
        quote.setMode(QuoteMode.Custom);
        quote.setArea(property.getArea());
        quote.setSubtotal(new BigDecimal("100000"));
        quote.setDiscount(BigDecimal.ONE);
        quote.setTaxAmount(BigDecimal.ZERO);
        quote.setTaxRate(new BigDecimal("0.05"));
        quote.setAmount(new BigDecimal("100000"));
        quote.setCost(new BigDecimal("80000"));
        quote.setDesignFee(BigDecimal.ZERO);
        quote.setManagementFee(BigDecimal.ZERO);
        quote.setStatus(QuoteStatus.Accepted);
        quote = quoteRepository.save(quote);

        Project project = new Project();
        project.setNo("PJ-" + Long.toHexString(System.nanoTime()));
        project.setName(name);
        project.setCompany(customer.getCompany());
        project.setCustomer(customer);
        project.setProperty(property);
        project.setQuote(quote);
        project.setStore(customer.getStore());
        project.setOwner(owner);
        project.setDesigner(owner);
        project.setManager(owner);
        project.setSupport(owner);
        project.setSiteAddr(property.getAddress());
        project.setArea(property.getArea());
        project.setContractAmount(new BigDecimal("100000"));
        project.setCost(new BigDecimal("80000"));
        project.setStatus(ProjectStatus.Ongoing);
        project.setProgress(10);
        project.setStartDate(LocalDate.now());
        project.setPlanEndDate(LocalDate.now().plusDays(90));
        return projectRepository.save(project);
    }

    private ProjectReport report(Project project, Employee author, String title, String path) {
        ProjectReport report = new ProjectReport();
        report.setCompany(project.getCompany());
        report.setStore(project.getStore());
        report.setTeam(project.getTeam());
        report.setOwner(project.getOwner());
        report.setProject(project);
        report.setProperty(project.getProperty());
        report.setType(ReportType.Progress);
        report.setTitle(title);
        report.setContent(title + " 内容");
        report.setImages(new ArrayList<>(List.of(path, MISSING_PATH)));
        report.setAuthor(author);
        report.setAuthorRole(author.getRole());
        report.setVisibleToCustomer(true);
        return reportRepository.save(report);
    }

    private Credentials credential(String idKey) {
        Credentials credential = new Credentials();
        credential.setType(CredType.Wx);
        credential.setIdKey(idKey + "-" + System.nanoTime());
        credential.setIdSecret("union-" + System.nanoTime());
        credential.setPhone("3" + Math.abs(System.nanoTime() % 1_000_000_000L));
        credential.setStatus(UserStatus.Active);
        return credentialsRepository.save(credential);
    }

    private void access(Company company, Credentials credential, Customer customer, Project project, String name) {
        CustomerAccess access = new CustomerAccess();
        access.setCompany(company);
        access.setCredential(credential);
        access.setCustomer(customer);
        access.setProject(project);
        access.setDisplayName(name);
        access.setStatus(CustomerAccessStatus.Active);
        access.setGrantedAt(LocalDateTime.now());
        accessRepository.save(access);
    }

    private void assertDefined(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable, String code) {
        assertThatThrownBy(callable).isInstanceOfSatisfying(DefinedException.class,
                ex -> assertThat(ex.getErrorCode().getCode()).isEqualTo(code));
    }

    private record Fixture(Employee marketerA, Employee marketerB, Credentials owner,
                           Project projectA1, Project projectA2,
                           ProjectReport reportA1, ProjectReport reportA2) {
    }

    private record Signed(String path, long expires, String signature) {
    }
}
