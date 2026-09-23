package com.th4.edge.decoboon.document;

import com.th4.edge.components.storage.service.ObjectStorage;
import com.th4.edge.components.storage.service.StoredObject;
import com.th4.edge.decoboon.common.file.context.FileSignatureContext;
import com.th4.edge.decoboon.common.file.context.ObjectKeyContext;
import com.th4.edge.decoboon.document.configure.DocumentAudience;
import com.th4.edge.decoboon.document.configure.DocumentStatus;
import com.th4.edge.decoboon.document.configure.DocumentType;
import com.th4.edge.decoboon.document.context.DocumentArchiveContext;
import com.th4.edge.decoboon.document.context.DocumentProfileContext;
import com.th4.edge.decoboon.document.context.DocumentTemplateBaseline;
import com.th4.edge.decoboon.document.context.DocumentTemplateContext;
import com.th4.edge.decoboon.document.context.QuotePdfRenderer;
import com.th4.edge.decoboon.document.model.DocumentPrint;
import com.th4.edge.decoboon.document.model.DocumentProfile;
import com.th4.edge.decoboon.document.model.DocumentRecord;
import com.th4.edge.decoboon.document.repository.DocumentPrintRepository;
import com.th4.edge.decoboon.document.repository.DocumentRecordRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.DataScopeType;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.jpa.domain.Specification;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentArchiveContextTest {

    @Mock private DataScopeContext dataScopeContext;
    @Mock private QuoteRepository quoteRepository;
    @Mock private QuoteLineRepository quoteLineRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private DocumentTemplateContext templateContext;
    @Mock private DocumentProfileContext profileContext;
    @Mock private ObjectStorage storage;
    @Mock private ObjectKeyContext keyContext;
    @Mock private FileSignatureContext signatureContext;
    @Mock private DocumentRecordRepository recordRepository;
    @Mock private DocumentPrintRepository printRepository;
    @Mock private com.th4.edge.decoboon.project.repository.ContractLineRepository contractLineRepository;
    @Mock private com.th4.edge.decoboon.project.repository.ProjectContractRepository contractRepository;

    private DocumentArchiveContext context;
    private Employee employee;
    private Quote quote;

    @BeforeEach
    void setUp() {
        Company company = new Company();
        company.setComId(100L);
        employee = new Employee();
        employee.setEmpId(2L);
        employee.setCompany(company);
        quote = new Quote();
        quote.setQuoteId(9L);
        quote.setNo("BJ-9");
        quote.setVersion(1);
        quote.setCompany(company);
        quote.setAmount(java.math.BigDecimal.TEN);

        QuotePdfRenderer renderer = new QuotePdfRenderer(new com.fasterxml.jackson.databind.ObjectMapper());
        com.th4.edge.decoboon.document.context.ContractPdfRenderer contractRenderer =
                new com.th4.edge.decoboon.document.context.ContractPdfRenderer(new com.fasterxml.jackson.databind.ObjectMapper());
        context = new DocumentArchiveContext(dataScopeContext, quoteRepository, quoteLineRepository,
                projectRepository, templateContext, profileContext, renderer, contractRenderer,
                contractLineRepository, contractRepository, storage, keyContext,
                signatureContext, recordRepository, printRepository);

        when(dataScopeContext.requireEmployee(employee)).thenReturn(employee);
        when(dataScopeContext.resolve(employee)).thenReturn(new DataScope(
                DataScopeType.Company, 100L, null, null, employee.getEmpId()));
        when(quoteRepository.findOne(any(Specification.class))).thenReturn(Optional.of(quote));
        when(profileContext.profile(employee)).thenReturn(profile(company));
        when(templateContext.template(100L, DocumentType.Quote))
                .thenReturn(new DocumentTemplateBaseline().template(DocumentType.Quote));
        when(quoteLineRepository.findByQuote(quote)).thenReturn(List.of());
        when(projectRepository.findFirstByCompany_ComIdAndQuote_QuoteIdOrderByProjectIdDesc(100L, 9L))
                .thenReturn(Optional.empty());
        when(keyContext.generated(eq(100L), eq(null), any(), anyString()))
                .thenReturn("companies/100/files/document/2026/09/generated-test.pdf");
        when(storage.put(anyString(), any(), anyLong(), eq("application/pdf")))
                .thenAnswer(invocation -> new StoredObject(invocation.getArgument(0), invocation.getArgument(2), "application/pdf", "etag"));
        when(templateContext.template(100L, DocumentType.Quote))
                .thenReturn(new DocumentTemplateBaseline().template(DocumentType.Quote));
        when(recordRepository.save(any(DocumentRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(signatureContext.signGateway(anyString(), any())).thenReturn("/file/private/download?path=test");
        when(signatureContext.expiresAt()).thenReturn(LocalDateTime.now().plusMinutes(5));
    }

    @Test
    void renderQuoteStoresPrivatePdfAndCreatesFirstPrintAudit() {
        DocumentRecord record = context.renderQuote(employee, 9L, DocumentAudience.Customer);

        assertThat(record.getType()).isEqualTo(DocumentType.Quote);
        assertThat(record.getBusinessNo()).isEqualTo("BJ-9");
        assertThat(record.getPath()).startsWith("companies/100/");
        assertThat(record.getSha256()).hasSize(64);
        assertThat(record.getSize()).isGreaterThan(1000);
        assertThat(record.getUrl()).isEqualTo("/file/private/download?path=test");
        assertThat(record.getFileName()).isEqualTo("合肥示例装饰工程有限公司_0平方米_10元_V1.pdf");
        verify(printRepository).save(any(DocumentPrint.class));
    }

    @Test
    void reprintCopiesOriginalObjectWithoutOverwritingArchive() {
        DocumentRecord original = new DocumentRecord();
        original.setDocumentId(8L);
        original.setCompany(employee.getCompany());
        original.setType(DocumentType.Quote);
        original.setAudience(DocumentAudience.Customer);
        original.setBusinessNo("BJ-9");
        original.setQuote(quote);
        original.setTemplateVersion(1);
        original.setPath("companies/100/files/document/2026/09/original.pdf");
        original.setFileName("报价单-BJ-9-V1.pdf");
        original.setContentType("application/pdf");
        original.setSize(2048L);
        original.setSha256("original-sha");
        original.setGeneratedBy(employee);
        original.setStatus(DocumentStatus.Active);
        byte[] originalBytes = "%PDF-original".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        when(recordRepository.findByDocumentIdAndCompany_ComId(8L, 100L)).thenReturn(Optional.of(original));
        when(storage.get(original.getPath())).thenReturn(new ByteArrayInputStream(originalBytes));
        when(keyContext.generated(100L, null, com.th4.edge.decoboon.common.file.configure.FileScope.Document,
                original.getFileName())).thenReturn("companies/100/files/document/2026/09/generated-reprint.pdf");
        when(printRepository.countByDocument_DocumentId(8L)).thenReturn(1L);

        DocumentRecord reprint = context.reprint(employee, 8L, "客户原件丢失");

        assertThat(reprint.getReprintOf()).isSameAs(original);
        assertThat(reprint.getPath()).isNotEqualTo(original.getPath());
        assertThat(reprint.getSha256()).isEqualTo("e2dc7dcc254d1b9d2b42ebf1fb0a7630e49255fd1f7fe3c5e220fb7ff837982b");
        verify(printRepository).save(any(DocumentPrint.class));
    }

    private DocumentProfile profile(Company company) {
        DocumentProfile profile = new DocumentProfile();
        profile.setCompany(company);
        profile.setLegalName("合肥示例装饰工程有限公司");
        profile.setAddress("合肥市蜀山区");
        profile.setServiceHotline("400-000-0000");
        return profile;
    }
}
