package com.th4.edge.decoboon.document.context;

import com.th4.edge.components.storage.service.ObjectStorage;
import com.th4.edge.components.storage.service.StoredObject;
import com.th4.edge.decoboon.common.file.configure.FileScope;
import com.th4.edge.decoboon.common.file.context.FileSignatureContext;
import com.th4.edge.decoboon.common.file.context.ObjectKeyContext;
import com.th4.edge.decoboon.document.configure.DocumentAudience;
import com.th4.edge.decoboon.document.configure.DocumentType;
import com.th4.edge.decoboon.document.model.DocumentPrint;
import com.th4.edge.decoboon.document.model.DocumentProfile;
import com.th4.edge.decoboon.document.model.DocumentRecord;
import com.th4.edge.decoboon.document.model.DocumentTemplate;
import com.th4.edge.decoboon.document.repository.DocumentPrintRepository;
import com.th4.edge.decoboon.document.repository.DocumentRecordRepository;
import com.th4.edge.decoboon.project.model.ContractLine;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ContractLineRepository;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.security.User;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.DataScopeSpecification;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

/** 报价 PDF 渲染、私有归档、重打复制和访问签名。 */
@Component
public class DocumentArchiveContext {

    private final DataScopeContext dataScopeContext;
    private final QuoteRepository quoteRepository;
    private final QuoteLineRepository quoteLineRepository;
    private final ProjectRepository projectRepository;
    private final DocumentTemplateContext templateContext;
    private final DocumentProfileContext profileContext;
    private final QuotePdfRenderer quotePdfRenderer;
    private final ContractPdfRenderer contractPdfRenderer;
    private final ContractLineRepository contractLineRepository;
    private final ProjectContractRepository contractRepository;
    private final ObjectStorage storage;
    private final ObjectKeyContext keyContext;
    private final FileSignatureContext signatureContext;
    private final DocumentRecordRepository recordRepository;
    private final DocumentPrintRepository printRepository;

    public DocumentArchiveContext(DataScopeContext dataScopeContext,
                                  QuoteRepository quoteRepository,
                                  QuoteLineRepository quoteLineRepository,
                                  ProjectRepository projectRepository,
                                  DocumentTemplateContext templateContext,
                                  DocumentProfileContext profileContext,
                                  QuotePdfRenderer quotePdfRenderer,
                                  ContractPdfRenderer contractPdfRenderer,
                                  ContractLineRepository contractLineRepository,
                                  ProjectContractRepository contractRepository,
                                  ObjectStorage storage,
                                  ObjectKeyContext keyContext,
                                  FileSignatureContext signatureContext,
                                  DocumentRecordRepository recordRepository,
                                  DocumentPrintRepository printRepository) {
        this.dataScopeContext = dataScopeContext;
        this.quoteRepository = quoteRepository;
        this.quoteLineRepository = quoteLineRepository;
        this.projectRepository = projectRepository;
        this.templateContext = templateContext;
        this.profileContext = profileContext;
        this.quotePdfRenderer = quotePdfRenderer;
        this.contractPdfRenderer = contractPdfRenderer;
        this.contractLineRepository = contractLineRepository;
        this.contractRepository = contractRepository;
        this.storage = storage;
        this.keyContext = keyContext;
        this.signatureContext = signatureContext;
        this.recordRepository = recordRepository;
        this.printRepository = printRepository;
    }

    public DocumentRecord renderQuote(User<?> principal, Long quoteId, DocumentAudience audience) {
        Employee current = requireEmployee(principal);
        Quote quote = requireQuote(current, quoteId);
        DocumentTemplate template = templateContext.template(current.getCompany().getComId(), DocumentType.Quote);
        DocumentProfile profile = profileContext.profile(current);
        List<QuoteLine> lines = quoteLineRepository.findByQuote(quote);
        byte[] content = quotePdfRenderer.render(quote, lines, template, profile, audience);
        Project project = projectRepository.findFirstByCompany_ComIdAndQuote_QuoteIdOrderByProjectIdDesc(
                current.getCompany().getComId(), quote.getQuoteId()).orElse(null);
        Long projectId = project == null ? null : project.getProjectId();
        String fileName = quoteFileName(profile, quote);
        String objectKey = keyContext.generated(current.getCompany().getComId(), projectId, FileScope.Document, fileName);
        store(objectKey, content);

        DocumentRecord record = new DocumentRecord();
        apply(record, current, quote, project, template, audience, objectKey, content, fileName);
        record = recordRepository.save(record);
        print(record, current, 1, "首次生成打印稿");
        return signed(record);
    }

    /** 生成合同打印稿并私有归档；合同打印稿不输出成本与利润。 */
    @org.springframework.transaction.annotation.Transactional
    public DocumentRecord renderContract(User<?> principal, Long contractId, DocumentAudience audience) {
        Employee current = requireEmployee(principal);
        ProjectContract contract = requireContract(current, contractId);
        DocumentTemplate template = templateContext.template(current.getCompany().getComId(), DocumentType.Contract);
        DocumentProfile profile = profileContext.profile(current);
        List<ContractLine> lines = contractLineRepository.findByContractOrderBySortAsc(contract);
        byte[] content = contractPdfRenderer.render(contract, lines, template, profile, audience);
        Project project = contract.getProject();
        Long projectId = project == null ? null : project.getProjectId();
        String fileName = contractFileName(profile, contract);
        String objectKey = keyContext.generated(current.getCompany().getComId(), projectId, FileScope.Document, fileName);
        store(objectKey, content);

        DocumentRecord record = new DocumentRecord();
        record.setCompany(current.getCompany());
        record.setType(DocumentType.Contract);
        record.setAudience(audience);
        record.setBusinessNo(contract.getNo());
        record.setProject(project);
        record.setTemplateVersion(template.getVersion());
        record.setPath(objectKey);
        record.setSha256(sha256(content));
        record.setFileName(fileName);
        record.setContentType("application/pdf");
        record.setSize((long) content.length);
        record.setGeneratedBy(current);
        record.setGeneratedAt(LocalDateTime.now());
        record = recordRepository.save(record);
        print(record, current, 1, "首次生成合同打印稿");
        return signed(record);
    }

    /** 合同打印稿归档记录。 */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<DocumentRecord> contractRecords(User<?> principal, Long contractId) {
        Employee current = requireEmployee(principal);
        ProjectContract contract = requireContract(current, contractId);
        return recordRepository
                .findByCompany_ComIdAndTypeAndBusinessNoOrderByDocumentIdDesc(
                        current.getCompany().getComId(), DocumentType.Contract, contract.getNo()).stream()
                .map(this::signed)
                .toList();
    }

    private ProjectContract requireContract(Employee current, Long contractId) {
        if (contractId == null) throw invalid("请选择合同");
        return contractRepository.findByContractIdAndCompany_ComId(contractId, current.getCompany().getComId())
                .orElseThrow(() -> notFound("合同不存在或无权访问"));
    }

    /** 合同打印稿文件名：装企_客户_小区_金额_编号。 */
    private String contractFileName(DocumentProfile profile, ProjectContract contract) {
        String community = contract.getProperty() == null ? null : contract.getProperty().getCommunity();
        String fileName = joinFileName(profile == null ? null : profile.getLegalName(),
                contract.getCustomer() == null ? null : contract.getCustomer().getName(),
                community,
                decimal(contract.getSignedAmount() == null ? contract.getBaselineAmount() : contract.getSignedAmount()) + "元",
                contract.getNo());
        return fileName + ".pdf";
    }

    public List<DocumentRecord> records(User<?> principal, Long quoteId) {
        Employee current = requireEmployee(principal);
        Quote quote = requireQuote(current, quoteId);
        return recordRepository.findByCompany_ComIdAndQuote_QuoteIdOrderByDocumentIdDesc(
                        current.getCompany().getComId(), quote.getQuoteId()).stream()
                .map(this::signed)
                .toList();
    }

    public DocumentRecord reprint(User<?> principal, Long documentId, String reason) {
        Employee current = requireEmployee(principal);
        if (reason == null || reason.isBlank()) throw invalid("请填写重打原因");
        DocumentRecord original = recordRepository.findByDocumentIdAndCompany_ComId(
                        documentId, current.getCompany().getComId())
                .orElseThrow(() -> notFound("归档单据不存在或无权访问"));
        if (original.getQuote() != null) {
            requireQuote(current, original.getQuote().getQuoteId());
        } else if (original.getType() == DocumentType.Contract) {
            if (original.getProject() == null) throw invalid("合同打印稿缺少项目上下文，暂不支持重打");
            projectRepository.findById(original.getProject().getProjectId())
                    .filter(project -> project.getCompany() != null
                            && current.getCompany().getComId().equals(project.getCompany().getComId()))
                    .orElseThrow(() -> notFound("合同打印稿不存在或无权访问"));
        } else {
            throw invalid("当前归档单据不支持重打");
        }

        byte[] content;
        try (InputStream input = storage.get(original.getPath())) {
            content = input.readAllBytes();
        } catch (Exception ex) {
            throw new DefinedException(new DefaultErrorCode("500", "原打印稿读取失败：" + ex.getMessage()));
        }
        Project project = original.getProject();
        Long projectId = project == null ? null : project.getProjectId();
        String objectKey = keyContext.generated(current.getCompany().getComId(), projectId, FileScope.Document, original.getFileName());
        store(objectKey, content);

        DocumentRecord record = new DocumentRecord();
        record.setCompany(current.getCompany());
        record.setType(original.getType());
        record.setAudience(original.getAudience());
        record.setBusinessNo(original.getBusinessNo());
        record.setQuote(original.getQuote());
        record.setProject(project);
        record.setReprintOf(original);
        record.setTemplateVersion(original.getTemplateVersion());
        record.setPath(objectKey);
        record.setSha256(sha256(content));
        record.setFileName(original.getFileName());
        record.setContentType(original.getContentType());
        record.setSize((long) content.length);
        record.setGeneratedBy(current);
        record.setGeneratedAt(LocalDateTime.now());
        record = recordRepository.save(record);
        int sequence = (int) printRepository.countByDocument_DocumentId(original.getDocumentId()) + 1;
        print(record, current, sequence, reason.trim());
        return signed(record);
    }

    private void apply(DocumentRecord record, Employee current, Quote quote, Project project,
                       DocumentTemplate template, DocumentAudience audience,
                       String objectKey, byte[] content, String fileName) {
        record.setCompany(current.getCompany());
        record.setType(DocumentType.Quote);
        record.setAudience(audience);
        record.setBusinessNo(quote.getNo());
        record.setQuote(quote);
        record.setProject(project);
        record.setTemplateVersion(template.getVersion());
        record.setPath(objectKey);
        record.setSha256(sha256(content));
        record.setFileName(fileName);
        record.setContentType("application/pdf");
        record.setSize((long) content.length);
        record.setGeneratedBy(current);
        record.setGeneratedAt(LocalDateTime.now());
    }

    private void print(DocumentRecord record, Employee current, int sequence, String reason) {
        DocumentPrint print = new DocumentPrint();
        print.setCompany(current.getCompany());
        print.setDocument(record);
        print.setPrintedBy(current);
        print.setSequenceNo(sequence);
        print.setReason(reason);
        print.setPrintedAt(LocalDateTime.now());
        printRepository.save(print);
    }

    private Quote requireQuote(Employee current, Long quoteId) {
        if (quoteId == null) throw invalid("请选择报价");
        DataScope scope = dataScopeContext.resolve(current);
        Specification<Quote> byId = (root, query, builder) -> builder.equal(root.get("quoteId"), quoteId);
        return quoteRepository.findOne(byId.and(DataScopeSpecification.visible(Quote.class, scope)))
                .orElseThrow(() -> notFound("报价不存在或无权访问"));
    }

    private Employee requireEmployee(User<?> principal) {
        if (!(principal instanceof Employee employee)) {
            throw new DefinedException(new DefaultErrorCode("403", "仅企业员工可以生成业务单据"));
        }
        return dataScopeContext.requireEmployee(employee);
    }

    private void store(String objectKey, byte[] content) {
        try (InputStream input = new ByteArrayInputStream(content)) {
            StoredObject stored = storage.put(objectKey, input, content.length, "application/pdf");
            if (stored.size() != content.length) throw new IllegalStateException("文件大小校验失败");
        } catch (Exception ex) {
            throw new DefinedException(new DefaultErrorCode("500", "打印稿归档失败：" + ex.getMessage()));
        }
    }

    /**
     * 打印稿文件名：装企企业_设计师_客户_小区_面积_价格_版本。
     * 打印稿是线下交付物，文件名必须能脱离系统辨认客户、房屋和版本。
     */
    private String quoteFileName(DocumentProfile profile, Quote quote) {
        String community = quote.getProperty() == null ? null : quote.getProperty().getCommunity();
        String fileName = joinFileName(profile == null ? null : profile.getLegalName(),
                designerName(quote),
                quote.getCustomer() == null ? null : quote.getCustomer().getName(),
                community,
                decimal(quote.getArea()) + "平方米",
                decimal(quote.getAmount()) + "元",
                "V" + value(quote.getVersion(), 1));
        return fileName + ".pdf";
    }

    /** 负责人优先取方案负责设计师，其次取房屋设计师，最后退回报价负责人。 */
    private String designerName(Quote quote) {
        if (quote.getDesign() != null && quote.getDesign().getOwner() != null) {
            return quote.getDesign().getOwner().getName();
        }
        if (quote.getProperty() != null && quote.getProperty().getDesigner() != null) {
            return quote.getProperty().getDesigner().getName();
        }
        return quote.getCreatedBy() == null ? null : quote.getCreatedBy().getName();
    }

    private String joinFileName(String... parts) {
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            String safe = safeFileNamePart(part);
            if (safe.isBlank()) continue;
            if (!result.isEmpty()) result.append('_');
            result.append(safe);
        }
        return result.isEmpty() ? "报价单" : result.toString();
    }

    /** 文件名片段去掉分隔符与控制字符，下划线保留为字段分隔符，因此片段内改用短横线。 */
    private String safeFileNamePart(String value) {
        if (value == null) return "";
        String safe = value.trim()
                .replaceAll("[\\\\/:*?\"<>|\\r\\n\\t]", "")
                .replace('_', '-')
                .replaceAll("\\s+", "");
        return safe.length() > 40 ? safe.substring(0, 40) : safe;
    }

    private String decimal(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value)
                .setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    private DocumentRecord signed(DocumentRecord record) {
        record.setUrl(signatureContext.signGateway(record.getPath(), record.getFileName()));
        record.setUrlExpiresAt(signatureContext.expiresAt());
        return record;
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (Exception ex) {
            throw new DefinedException(new DefaultErrorCode("500", "文件摘要计算失败"));
        }
    }

    private int value(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private DefinedException invalid(String message) {
        return new DefinedException(new DefaultErrorCode("400", message));
    }

    private DefinedException notFound(String message) {
        return new DefinedException(new DefaultErrorCode("404", message));
    }
}
