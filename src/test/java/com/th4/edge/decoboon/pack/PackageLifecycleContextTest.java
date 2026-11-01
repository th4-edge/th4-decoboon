package com.th4.edge.decoboon.pack;

import com.th4.edge.components.lock.DistributedLock;
import com.th4.edge.decoboon.pack.configure.PackageTemplateStatus;
import com.th4.edge.decoboon.pack.context.PackageContext;
import com.th4.edge.decoboon.pack.context.PackageLifecycleContext;
import com.th4.edge.decoboon.pack.context.PackageSnapshotContext;
import com.th4.edge.decoboon.pack.model.PackageLine;
import com.th4.edge.decoboon.pack.model.PackageTemplate;
import com.th4.edge.decoboon.pack.model.PackageTemplateSnapshot;
import com.th4.edge.decoboon.pack.repository.PackageLineRepository;
import com.th4.edge.decoboon.pack.repository.PackageTemplateRepository;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PackageLifecycleContextTest {

    private PackageLifecycleContext context;
    private PackageTemplateRepository templates;
    private PackageLineRepository lines;
    private PackageContext packageContext;
    private PackageSnapshotContext snapshotContext;
    private TransactionTemplate transactionTemplate;
    private Employee boss;
    private Company company;

    @BeforeEach
    void setUp() {
        TenantContext tenant = mock(TenantContext.class);
        DistributedLock lock = mock(DistributedLock.class);
        templates = mock(PackageTemplateRepository.class);
        lines = mock(PackageLineRepository.class);
        packageContext = mock(PackageContext.class);
        snapshotContext = mock(PackageSnapshotContext.class);
        transactionTemplate = mock(TransactionTemplate.class);

        company = new Company();
        company.setComId(100L);
        boss = new Employee();
        boss.setEmpId(1L);
        boss.setRole(EmployeeRole.Boss);
        boss.setCompany(company);

        when(tenant.requireRole(eq(boss), eq(EmployeeRole.Boss), eq(EmployeeRole.StoreManager))).thenReturn(boss);
        when(lock.tryLock(any(), eq(1000L), eq(30000L))).thenReturn(true);
        when(templates.save(any(PackageTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(templates.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionTemplate.execute(org.mockito.ArgumentMatchers.<TransactionCallback<PackageTemplate>>any()))
                .thenAnswer(invocation -> {
                    TransactionCallback<?> callback = invocation.getArgument(0);
                    return callback.doInTransaction(mock(TransactionStatus.class));
                });

        context = new PackageLifecycleContext(
                templates, lines, packageContext, snapshotContext, tenant, lock, transactionTemplate);
    }

    @Test
    void createsDraftV1WithCompanyCodeGeneration() {
        when(packageContext.calculateCost(any())).thenReturn(new BigDecimal("0.00"));

        PackageTemplate created = context.createDraft(new PackageTemplate(), "品质整装",
                new BigDecimal("100"), new BigDecimal("100000"), new BigDecimal("0.95"), boss);

        assertThat(created.getCompany()).isSameAs(company);
        assertThat(created.getCode()).startsWith("PACK-");
        assertThat(created.getVersion()).isEqualTo(1);
        assertThat(created.getStatus()).isEqualTo(PackageTemplateStatus.Draft);
        assertThat(created.getCurrentPublished()).isFalse();
    }

    @Test
    void publishedTemplateCannotBeEditedInPlace() {
        PackageTemplate published = template(1L, 1, PackageTemplateStatus.Published);
        published.setCurrentPublished(true);
        when(templates.findByTemplateIdAndCompany_ComId(1L, 100L)).thenReturn(Optional.of(published));

        assertThatThrownBy(() -> context.requireDraft(1L, boss))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("创建新版本");
    }

    @Test
    void buildsNextDraftAndCopiesLineSnapshots() {
        PackageTemplate published = template(1L, 1, PackageTemplateStatus.Published);
        published.setCurrentPublished(true);
        PackageTemplate draft = template(2L, 2, PackageTemplateStatus.Draft);
        draft.setCode(published.getCode());
        PackageLine sourceLine = line(published);
        PackageLine copiedLine = line(draft);

        when(templates.findByTemplateIdAndCompany_ComId(1L, 100L)).thenReturn(Optional.of(published));
        when(templates.findFirstByCompany_ComIdAndCodeOrderByVersionDesc(100L, published.getCode()))
                .thenReturn(Optional.of(published));
        when(templates.save(any(PackageTemplate.class))).thenAnswer(invocation -> {
            PackageTemplate saved = invocation.getArgument(0);
            saved.setTemplateId(2L);
            return saved;
        });
        when(lines.findByTemplate_TemplateIdAndCompany_ComIdOrderBySortAscItemIdAsc(1L, 100L))
                .thenReturn(List.of(sourceLine));
        when(templates.findByTemplateIdAndCompany_ComId(2L, 100L)).thenReturn(Optional.of(draft));
        when(lines.findByTemplate_TemplateIdAndCompany_ComIdOrderBySortAscItemIdAsc(2L, 100L))
                .thenReturn(List.of(copiedLine));

        PackageTemplate result = context.buildNextVersion(1L, boss);

        assertThat(result.getTemplateId()).isEqualTo(2L);
        assertThat(result.getVersion()).isEqualTo(2);
        assertThat(result.getStatus()).isEqualTo(PackageTemplateStatus.Draft);
        assertThat(result.getCurrentPublished()).isFalse();
    }

    @Test
    void copiesLatestTemplateIntoIndependentDraftWithAllLines() {
        PackageTemplate source = template(1L, 2, PackageTemplateStatus.Published);
        source.setCurrentPublished(true);
        source.setSnapshotJson("{\"published\":true}");
        source.setSnapshotHash("old-hash");
        PackageTemplate copiedTemplate = template(9L, 1, PackageTemplateStatus.Draft);
        PackageLine sourceLine = line(source);
        PackageTemplate[] savedHolder = new PackageTemplate[1];

        when(templates.findByTemplateIdAndCompany_ComId(1L, 100L)).thenReturn(Optional.of(source));
        when(templates.findFirstByCompany_ComIdAndCodeOrderByVersionDesc(100L, source.getCode()))
                .thenReturn(Optional.of(source));
        when(templates.save(any(PackageTemplate.class))).thenAnswer(invocation -> {
            PackageTemplate saved = invocation.getArgument(0);
            saved.setTemplateId(9L);
            savedHolder[0] = saved;
            return saved;
        });
        when(lines.findByTemplate_TemplateIdAndCompany_ComIdOrderBySortAscItemIdAsc(1L, 100L))
                .thenReturn(List.of(sourceLine));
        when(lines.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(templates.findByTemplateIdAndCompany_ComId(9L, 100L))
                .thenAnswer(invocation -> Optional.of(savedHolder[0]));

        PackageTemplate result = context.copyTemplate(1L, boss);

        assertThat(result.getTemplateId()).isEqualTo(9L);
        assertThat(result.getCode()).startsWith("PACK-").isNotEqualTo(source.getCode());
        assertThat(result.getVersion()).isEqualTo(1);
        assertThat(result.getStatus()).isEqualTo(PackageTemplateStatus.Draft);
        assertThat(result.getCurrentPublished()).isFalse();
        assertThat(result.getName()).isEqualTo("品质整装（副本）");
        assertThat(result.getSnapshotJson()).isNull();
        assertThat(result.getSnapshotHash()).isNull();
        org.mockito.Mockito.verify(lines).saveAll(any());
    }

    @Test
    void publishFreezesSnapshotAndSwitchesCurrentPublishedVersion() {
        PackageTemplate draft = template(2L, 2, PackageTemplateStatus.Draft);
        PackageTemplate previous = template(1L, 1, PackageTemplateStatus.Published);
        previous.setCurrentPublished(true);
        PackageLine line = line(draft);
        PackageTemplateSnapshot snapshot = new PackageTemplateSnapshot();
        snapshot.setJson("{\"version\":2}");
        snapshot.setHash("abc123");

        when(templates.findByTemplateIdAndCompany_ComId(2L, 100L)).thenReturn(Optional.of(draft));
        when(templates.findFirstByCompany_ComIdAndCodeOrderByVersionDesc(100L, draft.getCode()))
                .thenReturn(Optional.of(draft));
        when(templates.findByCompany_ComIdAndCodeAndCurrentPublishedTrue(100L, draft.getCode()))
                .thenReturn(new ArrayList<>(List.of(previous)));
        when(lines.findByTemplate_TemplateIdAndCompany_ComIdOrderBySortAscItemIdAsc(2L, 100L))
                .thenReturn(List.of(line));
        when(packageContext.calculateCost(any())).thenReturn(new BigDecimal("88.00"));
        when(snapshotContext.snapshot(any())).thenReturn(snapshot);

        PackageTemplate result = context.publish(2L, boss);

        assertThat(result.getStatus()).isEqualTo(PackageTemplateStatus.Published);
        assertThat(result.getCurrentPublished()).isTrue();
        assertThat(result.getSnapshotJson()).isEqualTo("{\"version\":2}");
        assertThat(result.getSnapshotHash()).isEqualTo("abc123");
        assertThat(previous.getCurrentPublished()).isFalse();
        PackageTemplate repeated = context.publish(2L, boss);
        assertThat(repeated).isSameAs(result);
    }

    @Test
    void legacyCurrentPublishedTemplateGetsSnapshotBeforeQuoteUse() {
        PackageTemplate legacy = template(2L, 1, PackageTemplateStatus.Published);
        legacy.setCurrentPublished(true);
        legacy.setSnapshotJson(null);
        legacy.setSnapshotHash(null);
        PackageLine line = line(legacy);
        PackageTemplateSnapshot snapshot = new PackageTemplateSnapshot();
        snapshot.setJson("{\"legacy\":true}");
        snapshot.setHash("legacy-hash");

        when(templates.findByTemplateIdAndCompany_ComId(2L, 100L)).thenReturn(Optional.of(legacy));
        when(lines.findByTemplate_TemplateIdAndCompany_ComIdOrderBySortAscItemIdAsc(2L, 100L))
                .thenReturn(List.of(line));
        when(packageContext.calculateCost(any())).thenReturn(new BigDecimal("20.00"));
        when(snapshotContext.snapshot(any())).thenReturn(snapshot);

        PackageTemplate result = context.requireQuoteSource(2L, 100L);

        assertThat(result.getSnapshotJson()).isEqualTo("{\"legacy\":true}");
        assertThat(result.getSnapshotHash()).isEqualTo("legacy-hash");
    }

    @Test
    void buildNextVersionRechecksLatestAfterLock() {
        PackageTemplate base = template(1L, 1, PackageTemplateStatus.Published);
        PackageTemplate newer = template(3L, 3, PackageTemplateStatus.Draft);
        newer.setCode(base.getCode());

        when(templates.findByTemplateIdAndCompany_ComId(1L, 100L)).thenReturn(Optional.of(base));
        when(templates.findFirstByCompany_ComIdAndCodeOrderByVersionDesc(100L, base.getCode()))
                .thenReturn(Optional.of(newer));

        assertThatThrownBy(() -> context.buildNextVersion(1L, boss))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("版本已变化");
    }

    @Test
    void duplicateVersionWriteIsMappedToConflict() {
        PackageTemplate base = template(1L, 1, PackageTemplateStatus.Published);
        when(templates.findByTemplateIdAndCompany_ComId(1L, 100L)).thenReturn(Optional.of(base));
        doThrow(new DataIntegrityViolationException("duplicate version"))
                .when(transactionTemplate)
                .execute(org.mockito.ArgumentMatchers.<TransactionCallback<PackageTemplate>>any());

        assertThatThrownBy(() -> context.buildNextVersion(1L, boss))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("套餐版本正在被其他操作更新");
    }

    @Test
    void versionWritesMustAcquireLockBeforeProgrammaticTransaction() throws Exception {
        assertThat(PackageLifecycleContext.class
                .getMethod("buildNextVersion", Long.class, Employee.class)
                .isAnnotationPresent(Transactional.class)).isFalse();
        assertThat(PackageLifecycleContext.class
                .getMethod("publish", Long.class, Employee.class)
                .isAnnotationPresent(Transactional.class)).isFalse();
        assertThat(PackageLifecycleContext.class
                .getMethod("disable", Long.class, Employee.class)
                .isAnnotationPresent(Transactional.class)).isFalse();
        assertThat(com.th4.edge.decoboon.pack.service.templates.build.support.Build.class
                .getMethod("call", PackageTemplate.class)
                .isAnnotationPresent(Transactional.class)).isFalse();
    }

    @Test
    void quoteCanOnlyUseCurrentPublishedVersion() {
        PackageTemplate disabled = template(2L, 2, PackageTemplateStatus.Disabled);
        disabled.setCurrentPublished(false);
        when(templates.findByTemplateIdAndCompany_ComId(2L, 100L)).thenReturn(Optional.of(disabled));

        assertThatThrownBy(() -> context.requireQuoteSource(2L, 100L))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("不是当前发布版本");
    }

    private PackageTemplate template(Long id, int version, PackageTemplateStatus status) {
        PackageTemplate template = new PackageTemplate();
        template.setTemplateId(id);
        template.setCompany(company);
        template.setCode("PACK-TEST");
        template.setVersion(version);
        template.setStatus(status);
        template.setName("品质整装");
        template.setArea(new BigDecimal("100"));
        template.setPrice(new BigDecimal("100000"));
        template.setDiscount(BigDecimal.ONE);
        template.setCost(BigDecimal.ZERO);
        return template;
    }

    private PackageLine line(PackageTemplate template) {
        PackageLine line = new PackageLine();
        line.setCompany(company);
        line.setTemplate(template);
        line.setName("项目");
        line.setQuantityFactor(BigDecimal.ONE);
        line.setSort(1);
        line.setSectionNameSnapshot("客厅");
        line.setCostPriceSnapshot(new BigDecimal("20.00"));
        return line;
    }
}
