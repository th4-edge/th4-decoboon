package com.th4.edge.decoboon.common.access;

import com.th4.edge.decoboon.common.context.StoreAttributionContext;
import com.th4.edge.decoboon.customer.context.CustomerContext;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.design.context.DesignContext;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.project.context.ProjectPermissionContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.property.configure.PropertyStatus;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.configure.Size;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.CompanyRepository;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import com.th4.edge.tenant.user.repository.StoreRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 门店归属**写入端统一**验收（`DEC-070` / `DESIGN-BIZ-20261013-006`）。
 *
 * <p>覆盖规格 §4 断言 3/4/6 与解析顺序：负责人门店 → 单门店企业默认 → 多门店阻断；
 * 设计归属改为客户门店快照；共用入口（合同/项目/采购单/任务/验收轮）取客户门店快照；
 * 换负责人**不回溯**历史对象门店。</p>
 *
 * <p>断言 1/2（应收双路径、应付三路径同源）由「统一解析器 + 同一写入方法」的结构保证：
 * 合同节点应收与手工应收都调用 {@code FinanceAccessContext.applyOwnership}，
 * 采购/到货/手工应付门店都经 {@link StoreAttributionContext#forCustomerSnapshot} 解析。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:store-attribution-write;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "th4.edge.components.lock.enabled=false",
        "th4.edge.components.wechat.pay.enabled=false",
        "th4.edge.components.storage.provider=local",
        "th4.edge.framework.upload-dir=${java.io.tmpdir}/store-attribution-write-upload",
        "logging.level.root=ERROR",
        "logging.level.org.hibernate.SQL=OFF"
})
@Transactional
class StoreAttributionWriteAcceptanceTest {

    @Autowired private StoreAttributionContext storeAttribution;
    @Autowired private CustomerContext customerContext;
    @Autowired private DesignContext designContext;
    @Autowired private FinanceAccessContext financeAccessContext;
    @Autowired private ProjectPermissionContext permissionContext;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private PropertyRepository propertyRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private ProjectContractRepository contractRepository;
    @Autowired private EntityManager entityManager;

    private Company company;
    private Store storeA;
    private Store storeB;
    private Employee bossInStoreA;

    @BeforeEach
    void setUp() {
        long seq = System.nanoTime();
        company = company("归属统一" + Math.abs(seq % 1000), 2);
        storeA = store(company, "归属总店", true);
        storeB = store(company, "归属城北店", false);
        bossInStoreA = employee(company, storeA, "归属负责人", EmployeeRole.Boss, seq, 1);
        entityManager.flush();
    }

    @Test
    void customerTakesOwnerStore() {
        Customer customer = new Customer();
        customerContext.applyOwnership(customer, bossInStoreA);
        assertThat(customer.getStore().getStoreId()).as("客户门店 = 负责人门店").isEqualTo(storeA.getStoreId());
    }

    @Test
    void designAndSharedEntryTakeCustomerStoreSnapshotNotOwnerStore() {
        // 客户门店 = A 店；负责人属于 B 店 → 设计/共用入口必须取客户门店（A），而不是负责人门店（B）
        Customer customer = customer(company, storeA, bossInStoreA, "归属快照客户", 1);
        Employee ownerInStoreB = employee(company, storeB, "归属B店负责人", EmployeeRole.Designer, System.nanoTime(), 3);
        entityManager.flush();

        Design design = new Design();
        designContext.apply(design, customer, ownerInStoreB, propertyOf(customer));
        assertThat(design.getStore().getStoreId()).as("设计门店 = 客户门店快照").isEqualTo(storeA.getStoreId());

        Project project = new Project();
        permissionContext.apply(project, propertyOf(customer), customer);
        assertThat(project.getStore().getStoreId()).as("项目/合同等共用入口 = 客户门店快照").isEqualTo(storeA.getStoreId());
    }

    @Test
    void ownerChangeKeepsHistoricalObjectsStoreUntouched() {
        Customer customer = customer(company, storeA, bossInStoreA, "归属不回溯客户", 2);
        entityManager.flush();
        Property property = propertyOf(customer);
        Project project = new Project();
        project.setCompany(company);
        project.setCustomer(customer);
        project.setProperty(property);
        permissionContext.apply(project, property, customer);
        ProjectContract contract = new ProjectContract();
        contract.setCompany(company);
        contract.setProject(project);
        contract.setCustomer(customer);
        contract.setProperty(property);

        // 换负责人：客户门店随之变化（新归属），历史对象门店保持不变（不回溯）
        customerContext.applyOwnership(customer, employeeOfStore(storeB));
        customerRepository.save(customer);

        assertThat(customer.getStore().getStoreId()).as("客户门店随新负责人变化").isEqualTo(storeB.getStoreId());
        assertThat(project.getStore().getStoreId()).as("历史项目门店不回溯").isEqualTo(storeA.getStoreId());
        assertThat(contract.getStore()).isNull();
    }

    @Test
    void resolverRejectsForeignOrUnknownStore() {
        Company other = company("归属他企" + Math.abs(System.nanoTime() % 1000), 1);
        Store foreign = store(other, "他企门店", true);
        assertThatThrownBy(() -> storeAttribution.requireCompanyStore(foreign.getStoreId(), company.getComId()))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("门店不存在或无权访问");
        assertThatThrownBy(() -> storeAttribution.requireCompanyStore(999999999L, company.getComId()))
                .isInstanceOf(DefinedException.class);
    }

    /**
     * 断言 1 · **应收双路径同源**（`DESIGN-BIZ-20261013-006` §4）。
     *
     * <p>历史缺陷：合同付款节点路径取 `project.property.store`、手工路径取 `project.customer.store`，
     * 同项目下两条路径可能落到不同门店。本用例把夹具构造成**房屋门店 ≠ 客户门店**
     * （客户/负责人 A 店、房屋 B 店），先断言共用写入方法两条调用都落到**客户门店 A**，
     * 再断言两者相互一致且与房屋门店不同——若任一调用方退回「自行复制来源」即失败。</p>
     */
    @Test
    void receivableContractPathAndManualPathShareOneStoreSource() {
        Customer customer = customer(company, storeA, bossInStoreA, "应收同源客户", 7);
        entityManager.flush();
        Property propertyInStoreB = propertyIn(propertyOf(customer), storeB);
        Project project = new Project();
        project.setCompany(company);
        project.setCustomer(customer);
        project.setProperty(propertyInStoreB);
        permissionContext.apply(project, propertyInStoreB, customer);
        assertThat(propertyInStoreB.getStore().getStoreId()).as("夹具：房屋门店故意与客户门店不同（B）")
                .isEqualTo(storeB.getStoreId());
        assertThat(project.getStore().getStoreId()).as("夹具：项目门店已按客户门店快照（A）")
                .isEqualTo(storeA.getStoreId());

        Receivable fromContractNode = new Receivable();
        financeAccessContext.applyOwnership(fromContractNode, bossInStoreA, project);
        Receivable manual = new Receivable();
        financeAccessContext.applyOwnership(manual, bossInStoreA, project);

        assertThat(fromContractNode.getStore().getStoreId()).as("合同节点应收 = 客户门店").isEqualTo(storeA.getStoreId());
        assertThat(manual.getStore().getStoreId()).as("手工应收 = 客户门店").isEqualTo(storeA.getStoreId());
        assertThat(fromContractNode.getStore().getStoreId()).as("应收双路径同源")
                .isEqualTo(manual.getStore().getStoreId());
        assertThat(fromContractNode.getStore().getStoreId()).as("应收门店不得取自房屋门店")
                .isNotEqualTo(propertyInStoreB.getStore().getStoreId());
    }

    /**
     * 断言 2 · **应付三路径同源**（采购下单 / 到货补建 / 手工）。
     *
     * <p>历史缺陷：采购路径取 `purchase.store`、到货路径取 `purchase.property.store`、手工取 `project.customer.store`。
     * 本用例让三条路径各自带上**不同的历史来源门店**（采购单 B 店、房屋 B 店、手工无来源），
     * 断言三条路径最终都落到客户门店 A 且相互一致。</p>
     */
    @Test
    void payablePurchaseReceiptAndManualPathsShareOneStoreSource() {
        Customer customer = customer(company, storeA, bossInStoreA, "应付同源客户", 8);
        entityManager.flush();
        Property propertyInStoreB = propertyIn(propertyOf(customer), storeB);
        Project project = new Project();
        project.setCompany(company);
        project.setCustomer(customer);
        project.setProperty(propertyInStoreB);
        permissionContext.apply(project, propertyInStoreB, customer);

        // ① 采购下单路径：原来源 = purchase.store（B 店）
        Payable fromPurchase = new Payable();
        financeAccessContext.applyPayableStore(fromPurchase, project.getCustomer(), bossInStoreA, storeB);
        // ② 到货补建路径：原来源 = purchase.property.store（B 店）
        Payable fromGoodsReceipt = new Payable();
        financeAccessContext.applyPayableStore(fromGoodsReceipt, project.getCustomer(), bossInStoreA,
                propertyInStoreB.getStore());
        // ③ 手工新增路径：走财务域统一写入
        Payable manual = new Payable();
        financeAccessContext.applyOwnership(manual, bossInStoreA, project);

        for (Payable payable : java.util.List.of(fromPurchase, fromGoodsReceipt, manual)) {
            assertThat(payable.getStore().getStoreId()).as("应付（采购/到货/手工）= 客户门店").isEqualTo(storeA.getStoreId());
        }
        assertThat(fromPurchase.getStore().getStoreId()).as("应付三路径同源：采购 = 到货")
                .isEqualTo(fromGoodsReceipt.getStore().getStoreId());
        assertThat(fromPurchase.getStore().getStoreId()).as("应付三路径同源：采购 = 手工")
                .isEqualTo(manual.getStore().getStoreId());
        assertThat(fromPurchase.getStore().getStoreId()).as("应付门店不得取自采购单/房屋门店")
                .isNotEqualTo(storeB.getStoreId());
    }

    // ---------- 夹具 ----------

    private Company company(String title, int storeCount) {
        Company created = new Company();
        created.setTitle(title);
        created.setName("归属统一企业");
        created.setSize(Size.A);
        return companyRepository.save(created);
    }

    private Store store(Company owner, String name, boolean defaultStore) {
        Store created = new Store();
        created.setCompany(owner);
        created.setName(name);
        created.setDefaultStore(defaultStore);
        return storeRepository.save(created);
    }

    private Employee employee(Company owner, Store store, String name, EmployeeRole role, long seq, int offset) {
        Employee created = new Employee();
        created.setCompany(owner);
        created.setStore(store);
        created.setRole(role);
        created.setStatus(EmployeeStatus.Active);
        created.setName(name);
        created.setTelNo("5" + (offset % 10) + String.format("%09d", Math.abs((seq + offset) % 1_000_000_000L)));
        created.setJoinedAt(LocalDateTime.now());
        return employeeRepository.save(created);
    }

    private Employee employeeOfStore(Store store) {
        Employee created = employee(company, store, "归属B店新负责人", EmployeeRole.Boss, System.nanoTime(), 5);
        return employeeRepository.save(created);
    }

    private Customer customer(Company owner, Store store, Employee ownerEmployee, String name, int offset) {
        Customer created = new Customer();
        created.setCompany(owner);
        created.setStore(store);
        created.setOwner(ownerEmployee);
        created.setName(name);
        created.setTelNo("6" + (offset % 10) + String.format("%09d", Math.abs(System.nanoTime() % 1_000_000_000L)));
        created.setArea(new BigDecimal("120"));
        return customerRepository.save(created);
    }

    private Property propertyOf(Customer customer) {
        Property property = new Property();
        property.setCompany(customer.getCompany());
        property.setStore(customer.getStore());
        property.setOwner(customer.getOwner());
        property.setCustomer(customer);
        property.setPropertyNo("PROP-ATT-" + Math.abs(System.nanoTime() % 100000));
        property.setAddress("归属统一小区 1 栋 101");
        property.setArea(new BigDecimal("120"));
        property.setStatus(PropertyStatus.Serving);
        return propertyRepository.save(property);
    }

    /** 构造「房屋门店 ≠ 客户门店」的存量差异样本（G4 前可能出现的真实数据形态）。 */
    private Property propertyIn(Property property, Store store) {
        property.setStore(store);
        return propertyRepository.save(property);
    }

    private Project applyProject(Project project, Customer customer) {
        permissionContext.apply(project, propertyOf(customer), customer);
        return project;
    }
}
