package com.th4.edge.decoboon.document;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.decoboon.document.configure.DocumentStatus;
import com.th4.edge.decoboon.document.configure.DocumentTemplateScope;
import com.th4.edge.decoboon.document.configure.DocumentType;
import com.th4.edge.decoboon.document.context.DocumentTemplateBaseline;
import com.th4.edge.decoboon.document.context.DocumentTemplateContext;
import com.th4.edge.decoboon.document.model.DocumentTemplate;
import com.th4.edge.decoboon.document.repository.DocumentTemplateRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentTemplateContextTest {

    @Mock private DocumentTemplateRepository repository;
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
        context = new DocumentTemplateContext(repository, new DocumentTemplateBaseline(), tenantContext, new ObjectMapper());
    }

    @Test
    void shouldSaveEveryBuiltInPresetWithoutAllowlistViolation() {
        when(tenantContext.requireRole(eq(boss), any(), any(), any())).thenReturn(boss);
        when(repository.findFirstByCompany_ComIdAndTypeAndStatusOrderByVersionDesc(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(repository.save(any(DocumentTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        for (DocumentTemplate preset : new DocumentTemplateBaseline().presets()) {
            DocumentTemplate saved = context.save(boss, preset.getType(), preset.getName(),
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
        when(repository.findFirstByCompany_ComIdAndTypeAndStatusOrderByVersionDesc(100L, DocumentType.Quote, DocumentStatus.Active))
                .thenReturn(Optional.empty());
        when(repository.findFirstByCompany_ComIdAndTypeAndStatusOrderByVersionDesc(100L, DocumentType.Material, DocumentStatus.Active))
                .thenReturn(Optional.empty());
        when(repository.findFirstByCompany_ComIdAndTypeAndStatusOrderByVersionDesc(100L, DocumentType.Receipt, DocumentStatus.Active))
                .thenReturn(Optional.empty());
        when(repository.findFirstByCompany_ComIdAndTypeAndStatusOrderByVersionDesc(100L, DocumentType.Payment, DocumentStatus.Active))
                .thenReturn(Optional.empty());
        when(repository.findFirstByCompany_ComIdAndTypeAndStatusOrderByVersionDesc(100L, DocumentType.Contract, DocumentStatus.Active))
                .thenReturn(Optional.empty());

        var templates = context.page(boss);

        assertThat(templates).hasSize(DocumentType.values().length);
        assertThat(templates).allMatch(item -> item.getScope() == DocumentTemplateScope.Platform);
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
