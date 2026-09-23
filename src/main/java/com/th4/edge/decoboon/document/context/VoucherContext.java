package com.th4.edge.decoboon.document.context;

import com.th4.edge.components.storage.service.ObjectStorage;
import com.th4.edge.components.storage.service.StoredObject;
import com.th4.edge.decoboon.common.file.configure.FileScope;
import com.th4.edge.decoboon.common.file.context.FileSignatureContext;
import com.th4.edge.decoboon.common.file.context.ObjectKeyContext;
import com.th4.edge.decoboon.document.configure.DocumentStatus;
import com.th4.edge.decoboon.document.configure.DocumentType;
import com.th4.edge.decoboon.document.model.DocumentProfile;
import com.th4.edge.decoboon.document.model.DocumentTemplate;
import com.th4.edge.decoboon.document.model.VoucherItem;
import com.th4.edge.decoboon.document.model.VoucherRecord;
import com.th4.edge.decoboon.document.repository.VoucherItemRepository;
import com.th4.edge.decoboon.document.repository.VoucherRecordRepository;
import com.th4.edge.decoboon.finance.configure.LedgerDirection;
import com.th4.edge.decoboon.finance.configure.LedgerSource;
import com.th4.edge.decoboon.finance.model.Ledger;
import com.th4.edge.decoboon.finance.model.Payment;
import com.th4.edge.decoboon.finance.repository.LedgerRepository;
import com.th4.edge.decoboon.finance.repository.PaymentRepository;
import com.th4.edge.decoboon.labor.model.Worker;
import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 凭证生成：由来源资金流水构建凭证记录，渲染 PDF 并落库。
 *
 * <p>合并判定为"收款方/付款方 + 项目 + 款项类型"三者相同；
 * 凭证号在首次生成时产生并持久化，重打读取同一条记录。</p>
 */
@Component
public class VoucherContext {

    private static final DateTimeFormatter NUMBER_PREFIX = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final TenantContext tenantContext;
    private final LedgerRepository ledgerRepository;
    private final PaymentRepository paymentRepository;
    private final DocumentTemplateContext templateContext;
    private final DocumentProfileContext profileContext;
    private final VoucherPdfRenderer renderer;
    private final ObjectStorage storage;
    private final ObjectKeyContext keyContext;
    private final FileSignatureContext signatureContext;
    private final VoucherRecordRepository recordRepository;
    private final VoucherItemRepository itemRepository;

    public VoucherContext(TenantContext tenantContext,
                          LedgerRepository ledgerRepository,
                          PaymentRepository paymentRepository,
                          DocumentTemplateContext templateContext,
                          DocumentProfileContext profileContext,
                          VoucherPdfRenderer renderer,
                          ObjectStorage storage,
                          ObjectKeyContext keyContext,
                          FileSignatureContext signatureContext,
                          VoucherRecordRepository recordRepository,
                          VoucherItemRepository itemRepository) {
        this.tenantContext = tenantContext;
        this.ledgerRepository = ledgerRepository;
        this.paymentRepository = paymentRepository;
        this.templateContext = templateContext;
        this.profileContext = profileContext;
        this.renderer = renderer;
        this.storage = storage;
        this.keyContext = keyContext;
        this.signatureContext = signatureContext;
        this.recordRepository = recordRepository;
        this.itemRepository = itemRepository;
    }

    /** 生成凭证：单笔直接生成，多笔必须满足合并判定。 */
    @Transactional
    public VoucherRecord build(Employee principal, List<Long> ledgerIds) {
        Employee current = tenantContext.requireRole(principal, EmployeeRole.Boss, EmployeeRole.Finance);
        Long comId = current.getCompany().getComId();
        List<Long> ids = normalizeIds(ledgerIds);
        if (ids.isEmpty()) throw invalid("请选择要打印的款项记录");

        List<Ledger> ledgers = ledgerRepository.findByCompany_ComIdAndLedgerIdIn(comId, ids);
        if (ledgers.size() != ids.size()) throw notFound("存在无权访问或已不存在的款项记录");
        ledgers.sort(Comparator.comparing(Ledger::getOccurredAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Ledger::getLedgerId, Comparator.nullsLast(Comparator.naturalOrder())));
        requireMergeable(ledgers);

        List<Long> sourceKeys = ledgers.stream().map(this::sourceKey).toList();
        VoucherRecord reprinted = existing(comId, sourceKeys);
        if (reprinted != null) return reprinted;

        BigDecimal total = ledgers.stream()
                .map(Ledger::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.signum() == 0) throw invalid("凭证金额不能为 0");

        Ledger first = ledgers.get(0);
        DocumentType type = first.getDirection() == LedgerDirection.Income
                ? DocumentType.Receipt : DocumentType.Payment;
        DocumentTemplate template = templateContext.template(comId, type);
        DocumentProfile profile = profileContext.profile(current);

        Payment payment = resolvePayment(comId, first);
        Worker worker = payment == null ? null : payment.getWorker();
        Partner partner = payment == null ? null : payment.getPartner();

        VoucherRecord record = new VoucherRecord();
        record.setCompany(current.getCompany());
        record.setStore(first.getStore());
        record.setTeam(first.getTeam());
        record.setOwner(first.getOwner() == null ? current : first.getOwner());
        record.setProject(first.getProject());
        record.setType(type);
        record.setVoucherNo(nextVoucherNo(comId));
        record.setTitle(template.getName());
        // 平台行业基线是瞬态副本，未落库时不建立实体关联，只保留模板版本
        record.setTemplate(template.getTemplateId() == null ? null : template);
        record.setTemplateVersion(template.getVersion() == null ? 1 : template.getVersion());
        record.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP));
        record.setAmountWords(VoucherAmountWords.of(total));
        record.setPartyName(first.getCounterparty());
        record.setWorker(worker);
        record.setPartner(partner);
        record.setGeneratedBy(current);
        record.setGeneratedAt(LocalDateTime.now());
        record.setStatus(DocumentStatus.Active);

        List<VoucherItem> items = new ArrayList<>();
        for (Ledger ledger : ledgers) items.add(item(record, ledger));
        record.setItems(items);

        byte[] content = renderer.render(record, items, template, profile, worker, partner);
        String fileName = fileName(profile, record);
        Long projectId = record.getProject() == null ? null : record.getProject().getProjectId();
        String objectKey = keyContext.generated(comId, projectId, FileScope.Document, fileName);
        store(objectKey, content);
        record.setPath(objectKey);
        record.setSha256(sha256(content));
        record.setFileName(fileName);
        record.setContentType("application/pdf");
        record.setSize((long) content.length);

        VoucherRecord saved = recordRepository.save(record);
        return signed(saved);
    }

    /** 凭证记录签名访问地址。 */
    public VoucherRecord signed(VoucherRecord record) {
        if (record == null) return null;
        record.setUrl(signatureContext.signGateway(record.getPath(), record.getFileName()));
        record.setUrlExpiresAt(signatureContext.expiresAt());
        return record;
    }

    private VoucherItem item(VoucherRecord record, Ledger ledger) {
        VoucherItem item = new VoucherItem();
        item.setCompany(record.getCompany());
        item.setVoucher(record);
        item.setSourceType(DocumentType.Payment);
        item.setSourceId(sourceKey(ledger));
        item.setSourceNo(sourceNo(ledger));
        item.setBusinessDate(ledger.getOccurredAt());
        item.setAccount(ledger.getAccount());
        item.setAmount(ledger.getAmount() == null ? BigDecimal.ZERO : ledger.getAmount());
        item.setCategory(ledger.getCategory());
        item.setRemark(ledger.getRemark() == null ? ledger.getSummary() : ledger.getRemark());
        return item;
    }


    /** 来源记录键：付款用 paymentId，收款用 receiptId，其余退回流水 ID。 */
    private Long sourceKey(Ledger ledger) {
        return ledger.getSourceId() == null ? ledger.getLedgerId() : ledger.getSourceId();
    }

    /** 重打幂等：同一来源组合已生成过凭证时，直接返回原记录，编号与文件不变。 */
    private VoucherRecord existing(Long comId, List<Long> sourceKeys) {
        if (sourceKeys.isEmpty()) return null;
        Map<Long, List<VoucherItem>> grouped = itemRepository
                .findByCompany_ComIdAndSourceIdIn(comId, sourceKeys)
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(item -> item.getVoucher().getVoucherId()));
        Set<Long> expected = new HashSet<>(sourceKeys);
        for (List<VoucherItem> items : grouped.values()) {
            Set<Long> actual = items.stream().map(VoucherItem::getSourceId).collect(java.util.stream.Collectors.toSet());
            if (actual.equals(expected) && items.get(0).getVoucher().getStatus() == DocumentStatus.Active) {
                return signed(items.get(0).getVoucher());
            }
        }
        return null;
    }

    private String sourceNo(Ledger ledger) {
        if (ledger.getSource() == LedgerSource.Payment && ledger.getSourceId() != null) {
            return "PZ" + ledger.getSourceId();
        }
        if (ledger.getSource() == LedgerSource.Receipt && ledger.getSourceId() != null) {
            return "SK" + ledger.getSourceId();
        }
        return String.valueOf(ledger.getLedgerId());
    }

    /** 合并判定：收款方/付款方 + 项目 + 款项类型三者相同。 */
    private void requireMergeable(List<Ledger> ledgers) {
        if (ledgers.size() < 2) return;
        Set<String> keys = new HashSet<>();
        for (Ledger ledger : ledgers) keys.add(key(ledger));
        if (keys.size() > 1) {
            throw invalid("仅可合并收款方/付款方、项目、款项类型三者相同的记录");
        }
    }

    private String key(Ledger ledger) {
        Long projectId = ledger.getProject() == null ? null : ledger.getProject().getProjectId();
        return String.join("|", safe(ledger.getCounterparty()), String.valueOf(projectId), safe(ledger.getCategory()));
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private Payment resolvePayment(Long comId, Ledger ledger) {
        if (ledger.getSource() != LedgerSource.Payment || ledger.getSourceId() == null) return null;
        return paymentRepository.findById(ledger.getSourceId())
                .filter(item -> item.getCompany() != null && comId.equals(item.getCompany().getComId()))
                .orElse(null);
    }

    private List<Long> normalizeIds(List<Long> ledgerIds) {
        if (ledgerIds == null) return List.of();
        return new ArrayList<>(ledgerIds.stream()
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new)));
    }

    private String nextVoucherNo(Long comId) {
        String prefix = "PZ" + LocalDateTime.now().format(NUMBER_PREFIX);
        long issued = recordRepository.countByCompany_ComIdAndVoucherNoStartingWith(comId, prefix);
        for (int attempt = 0; attempt < 100; attempt++) {
            String candidate = prefix + String.format("%04d", issued + 1 + attempt);
            if (!recordRepository.existsByCompany_ComIdAndVoucherNo(comId, candidate)) return candidate;
        }
        throw new DefinedException(new DefaultErrorCode("500", "凭证号生成失败，请重试"));
    }

    private String fileName(DocumentProfile profile, VoucherRecord record) {
        String company = profile == null ? null : profile.getLegalName();
        return joinFileName(company, record.getTitle(), record.getVoucherNo()) + ".pdf";
    }

    private String joinFileName(String... parts) {
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            String safe = safeFileNamePart(part);
            if (safe.isBlank()) continue;
            if (!result.isEmpty()) result.append('_');
            result.append(safe);
        }
        return result.isEmpty() ? "凭证" : result.toString();
    }

    private String safeFileNamePart(String value) {
        if (value == null) return "";
        String safe = value.trim()
                .replaceAll("[\\\\/:*?\"<>|\\r\\n\\t]", "")
                .replace('_', '-')
                .replaceAll("\\s+", "");
        return safe.length() > 40 ? safe.substring(0, 40) : safe;
    }

    private void store(String objectKey, byte[] content) {
        try (InputStream input = new ByteArrayInputStream(content)) {
            StoredObject stored = storage.put(objectKey, input, content.length, "application/pdf");
            if (stored.size() != content.length) throw new IllegalStateException("文件大小校验失败");
        } catch (Exception ex) {
            throw new DefinedException(new DefaultErrorCode("500", "凭证文件保存失败：" + ex.getMessage()));
        }
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (Exception ex) {
            throw new DefinedException(new DefaultErrorCode("500", "文件摘要计算失败"));
        }
    }

    private DefinedException invalid(String message) {
        return new DefinedException(new DefaultErrorCode("400", message));
    }

    private DefinedException notFound(String message) {
        return new DefinedException(new DefaultErrorCode("404", message));
    }
}
