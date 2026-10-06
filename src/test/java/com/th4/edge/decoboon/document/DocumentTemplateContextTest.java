package com.th4.edge.decoboon.document;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.decoboon.document.configure.DocumentStatus;
import com.th4.edge.decoboon.document.configure.DocumentTemplateScope;
import com.th4.edge.decoboon.document.configure.DocumentTemplateVersionStatus;
import com.th4.edge.decoboon.document.configure.DocumentType;
import com.th4.edge.decoboon.document.context.DocumentTemplateBaseline;
import com.th4.edge.decoboon.document.context.DocumentTemplateContext;
import com.th4.edge.decoboon.document.model.DocumentTemplate;
import com.th4.edge.decoboon.document.model.DocumentTemplateVersion;
import com.th4.edge.decoboon.document.repository.DocumentTemplateRepository;
import com.th4.edge.decoboon.document.repository.DocumentTemplateVersionRepository;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentTemplateContextTest {

    @Mock private DocumentTemplateRepository repository;
    @Mock private DocumentTemplateVersionRepository versionRepository;
    @Mock private TenantContext tenantContext;

    private DocumentTemplateContext context;
    private Employee boss;

    @BeforeEach
    void setUp() {
        Company company = new Company();
        company.setComId(100L);
        boss = new Employee();
        boss.setEmpId(1L);
        boss.setCompany(company);
        lenient().when(versionRepository.save(any(DocumentTemplateVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(versionRepository.findByCompany_ComIdAndTemplate_TemplateIdAndVersion(any(), any(), any()))
                .thenReturn(Optional.empty());
        lenient().when(versionRepository.findByCompany_ComIdAndTemplate_TemplateIdOrderByVersionDesc(any(), any()))
                .thenReturn(java.util.List.of());
        context = new DocumentTemplateContext(repository, versionRepository, new DocumentTemplateBaseline(), tenantContext, new ObjectMapper());
    }

    @Test
    void shouldSaveEveryBuiltInPresetWithoutAllowlistViolation() {
        when(tenantContext.requireRole(eq(boss), any(), any(), any())).thenReturn(boss);
        when(repository.findFirstByCompany_ComIdAndTypeAndPresetCodeAndStatusOrderByVersionDesc(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(repository.save(any(DocumentTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        for (DocumentTemplate preset : new DocumentTemplateBaseline().presets()) {
            DocumentTemplate saved = context.save(boss, preset.getType(), preset.getPresetCode(), preset.getName(),
                    preset.getFieldConfig(), preset.getStyleConfig());
            assertThat(saved.getName()).as(preset.getPresetCode()).isEqualTo(preset.getName());
            assertThat(saved.getFieldConfig()).as(preset.getPresetCode()).isNotBlank();
        }
    }

    @Test
    void shouldAcceptVoucherFieldKeysForPaymentTemplate() {
        when(tenantContext.requireRole(eq(boss), any(), any(), any())).thenReturn(boss);
        when(repository.findFirstByCompany_ComIdAndTypeAndStatusOrderByVersionDesc(any(), eq(DocumentType.Payment), any()))
                .thenReturn(Optional.empty());
        when(repository.save(any(DocumentTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentTemplate saved = context.save(boss, DocumentType.Payment, "工资发放凭证",
                """
                {"showCompanyName":true,"showServiceHotline":true,"showVoucherNo":true,"showVoucherDate":true,
                 "showPartyName":true,"showWorkerPhone":true,"showWorkerIdCard":true,"showWorkerTrade":true,
                 "showProject":true,"showAmount":true,"showAmountWords":true,"showSignBlock":true,
                 "showDetailNo":true,"showDetailDate":true,"showDetailAccount":true,"showDetailAmount":true,
                 "showDetailTarget":true,"showDetailCategory":false,"showDetailRemark":false}
                """,
                "{\"primaryColor\":\"#2F7D5A\",\"fontSize\":12,\"rowSpacing\":2}");

        assertThat(saved.getName()).isEqualTo("工资发放凭证");
        assertThat(saved.getFieldConfig()).contains("showWorkerIdCard").contains("showDetailTarget");
        assertThat(saved.getStyleConfig()).contains("#2F7D5A").contains("\"fontSize\":12");
    }

    @Test
    void shouldRejectUnknownVoucherFieldKey() {
        when(tenantContext.requireRole(eq(boss), any(), any(), any())).thenReturn(boss);
        assertThatThrownBy(() -> context.save(boss, DocumentType.Payment, "工资发放凭证",
                "{\"showUnknownVoucherField\":true}", "{\"primaryColor\":\"#2F7D5A\"}"))
                .hasMessageContaining("不支持的字段配置");
    }

    @Test
    void pageUsesPlatformBaselineWhenCompanyHasNoTemplate() {
        when(tenantContext.requireEmployee(boss)).thenReturn(boss);
        when(repository.findByCompany_ComIdAndStatusOrderByTypeAscPresetCodeAscVersionDesc(100L, DocumentStatus.Active))
                .thenReturn(java.util.List.of());

        var templates = context.page(boss);

        assertThat(templates).hasSize(new DocumentTemplateBaseline().presets().size() + 3);
        assertThat(templates).allMatch(item -> item.getScope() == DocumentTemplateScope.Platform);
    }

    @Test
    void saveNamedPresetKeepsSourcePresetCode() {
        when(tenantContext.requireRole(eq(boss), any(), any(), any())).thenReturn(boss);
        when(repository.findFirstByCompany_ComIdAndTypeAndPresetCodeAndStatusOrderByVersionDesc(
                100L, DocumentType.Receipt, "R-CUSTOMER-PROGRESS", DocumentStatus.Active))
                .thenReturn(Optional.empty());
        when(repository.save(any(DocumentTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentTemplate saved = context.save(boss, DocumentType.Receipt, "R-CUSTOMER-PROGRESS",
                "我的进度款收据", "{\"showCompanyName\":true}", "{}");

        assertThat(saved.getCompany()).isEqualTo(boss.getCompany());
        assertThat(saved.getType()).isEqualTo(DocumentType.Receipt);
        assertThat(saved.getPresetCode()).isEqualTo("R-CUSTOMER-PROGRESS");
        assertThat(saved.getScope()).isEqualTo(DocumentTemplateScope.Company);
        assertThat(saved.getName()).isEqualTo("我的进度款收据");
    }

    @Test
    void publishAndDisableKeepImmutableVersionHistory() {
        when(tenantContext.requireRole(eq(boss), any(), any(), any())).thenReturn(boss);

        DocumentTemplate head = new DocumentTemplate();
        head.setTemplateId(501L);
        head.setCompany(boss.getCompany());
        head.setType(DocumentType.Receipt);
        head.setScope(DocumentTemplateScope.Company);
        head.setName("我的收款收据");
        head.setVersion(2);
        head.setBaselineVersion(DocumentTemplateBaseline.VERSION);
        head.setFieldConfig("{\"showCompanyName\":true}");
        head.setStyleConfig("{}");
        head.setStatus(DocumentStatus.Active);

        DocumentTemplateVersion previous = new DocumentTemplateVersion();
        previous.setVersion(1);
        previous.setStatus(DocumentTemplateVersionStatus.Published);

        DocumentTemplateVersion current = new DocumentTemplateVersion();
        current.setTemplate(head);
        current.setVersion(2);
        current.setName(head.getName());
        current.setFieldConfig(head.getFieldConfig());
        current.setStyleConfig(head.getStyleConfig());
        current.setBaselineVersion(head.getBaselineVersion());
        current.setStatus(DocumentTemplateVersionStatus.Draft);

        when(repository.findByCompany_ComIdAndTemplateId(100L, 501L)).thenReturn(Optional.of(head));
        when(repository.save(any(DocumentTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(versionRepository.findByCompany_ComIdAndTemplate_TemplateIdAndVersion(100L, 501L, 2))
                .thenReturn(Optional.of(current));
        when(versionRepository.findByCompany_ComIdAndTemplate_TemplateIdAndStatus(
                100L, 501L, DocumentTemplateVersionStatus.Published)).thenReturn(java.util.List.of(previous));
        when(versionRepository.findByCompany_ComIdAndTemplate_TemplateIdOrderByVersionDesc(100L, 501L))
                .thenReturn(java.util.List.of(current, previous));

        DocumentTemplate published = context.publish(boss, 501L);
        assertThat(published.getPublishedVersion()).isEqualTo(2);
        assertThat(current.getStatus()).isEqualTo(DocumentTemplateVersionStatus.Published);
        assertThat(previous.getStatus()).isEqualTo(DocumentTemplateVersionStatus.Superseded);

        when(versionRepository.findFirstByCompany_ComIdAndTemplate_TemplateIdAndStatusOrderByVersionDesc(
                100L, 501L, DocumentTemplateVersionStatus.Published)).thenReturn(Optional.of(current));

        DocumentTemplate disabled = context.disable(boss, 501L);
        assertThat(disabled.getPublishedVersion()).isZero();
        assertThat(current.getStatus()).isEqualTo(DocumentTemplateVersionStatus.Disabled);
    }

    @Test
    void saveNormalizesDesignerConfig() {
        when(tenantContext.requireRole(eq(boss), any(), any(), any())).thenReturn(boss);
        when(repository.findFirstByCompany_ComIdAndTypeAndPresetCodeAndStatusOrderByVersionDesc(
                100L, DocumentType.Payment, "P-SALARY", DocumentStatus.Active))
                .thenReturn(Optional.empty());
        when(repository.save(any(DocumentTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentTemplate saved = context.save(boss, DocumentType.Payment, "P-SALARY", "工资设计器",
                "{\"showCompanyName\":true}", "{}", null,
                """
                {"schemaVersion":1,"page":{"widthMm":210,"heightMm":297,"orientation":"portrait","marginMm":12,"gridSizeMm":5},
                 "elements":[{"id":"title","type":"text","xMm":10,"yMm":5,"widthMm":120,"heightMm":10,"fixedText":"工资凭证","zIndex":1}],
                 "tables":[{"id":"detail","binding":"voucher.items","xMm":10,"yMm":80,"widthMm":180,"heightMm":90,
                 "headerRepeat":true,"rowsPerPage":10,"rowHeightMm":8,"columns":[{"key":"amount","label":"金额","widthMm":30,"align":"right"}]}],
                 "print":{"scale":100,"offsetXMm":0,"offsetYMm":0}}
                """);

        assertThat(saved.getDesignerConfig()).contains("\"elements\"").contains("\"tables\"")
                .contains("工资凭证").contains("voucher.items");
    }

    @Test
    void saveNormalizesLayoutConfig() {
        when(tenantContext.requireRole(eq(boss), any(), any(), any())).thenReturn(boss);
        when(repository.findFirstByCompany_ComIdAndTypeAndPresetCodeAndStatusOrderByVersionDesc(
                100L, DocumentType.Receipt, "R-CUSTOMER-PROGRESS", DocumentStatus.Active))
                .thenReturn(Optional.empty());
        when(repository.save(any(DocumentTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentTemplate saved = context.save(boss, DocumentType.Receipt, "R-CUSTOMER-PROGRESS", "进度款",
                "{\"showCompanyName\":true}", "{}",
                """
                {"pageSize":"A4","orientation":"landscape","marginMm":14,
                 "blocks":[{"key":"title","visible":true,"sort":10},{"key":"detail","visible":false,"sort":20}],
                 "signatures":["制单","审核"],"footer":"测试页脚"}
                """);

        assertThat(saved.getLayoutConfig()).contains("\"orientation\":\"landscape\"")
                .contains("\"marginMm\":14").contains("\"visible\":false").contains("测试页脚");
    }

    @Test
    void saveCopiesBaselineIntoCompanyTemplateAndNormalizesConfig() {
        when(tenantContext.requireRole(eq(boss), any(), any(), any())).thenReturn(boss);
        when(repository.findFirstByCompany_ComIdAndTypeAndStatusOrderByVersionDesc(100L, DocumentType.Quote, DocumentStatus.Active))
                .thenReturn(Optional.empty());
        when(repository.save(any(DocumentTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentTemplate saved = context.save(boss, DocumentType.Quote, "我的报价单模板",
                "{\"showCompanyName\":true,\"showPropertyAddress\":false}",
                "{\"primaryColor\":\"#123ABC\",\"fontSize\":11,\"rowSpacing\":3}");

        assertThat(saved.getCompany()).isEqualTo(boss.getCompany());
        assertThat(saved.getScope()).isEqualTo(DocumentTemplateScope.Company);
        assertThat(saved.getVersion()).isEqualTo(1);
        assertThat(saved.getBaselineVersion()).isEqualTo(DocumentTemplateBaseline.VERSION);
        assertThat(saved.getFieldConfig()).contains("\"showPropertyAddress\":false")
                .contains("\"showLineSpec\":true");
        assertThat(saved.getStyleConfig()).contains("\"primaryColor\":\"#123ABC\"")
                .contains("\"fontSize\":11");
    }

    @Test
    void saveRejectsUnknownFieldAndUncontrolledStyle() {
        when(tenantContext.requireRole(eq(boss), any(), any(), any())).thenReturn(boss);

        assertThatThrownBy(() -> context.save(boss, DocumentType.Quote, "x",
                "{\"showInternalCost\":true}", "{}"))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("不支持的字段配置");

        assertThatThrownBy(() -> context.save(boss, DocumentType.Quote, "x",
                "{\"showCompanyName\":true}", "{\"html\":\"<script/>\"}"))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("不支持的样式配置");
    }
}
