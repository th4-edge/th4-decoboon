package com.th4.edge.decoboon.finance.service.receipts.save.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.finance.configure.FundAccount;
import com.th4.edge.decoboon.finance.configure.LedgerDirection;
import com.th4.edge.decoboon.finance.configure.LedgerSource;
import com.th4.edge.decoboon.finance.context.FinanceAccessContext;
import com.th4.edge.decoboon.finance.model.Receipt;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.repository.ReceiptRepository;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Optional;

/** 收款创建实现，收款与应收核销在同一事务内完成，并按幂等键防重。 */
@Note("客户收款创建")
@Properties(allow = {
        @AllowProperties(model = Receipt.class,
                properties = {"receiptId", "amount", "account", "receivedAt", "payerName", "voucherUrl",
                        "remark", "idempotencyKey", "createTime"}),
        @AllowProperties(model = Receivable.class,
                properties = {"receivableId", "name", "type", "amount", "receivedAmount", "dueDate", "status", "property" }),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "contractAmount", "property", "customer", "status" }),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "telNo"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("finance/receipt/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.finance.service.receipts.save.Save {

    @User
    private Employee employee;
    @AutoInject
    @NotNull
    private Long projectId;
    @AutoInject
    @NotNull
    private Long receivableId;
    @AutoInject
    @NotNull
    private BigDecimal amount;
    @AutoInject
    private FundAccount account;
    @AutoInject
    @NotNull
    private LocalDate receivedAt;
    @AutoInject
    private String payerName;
    @AutoInject
    private String voucherUrl;
    @AutoInject
    private String remark;
    /** 客户端幂等键；缺失时由服务端按业务指纹生成，两者共同参与去重。 */
    @AutoInject
    @Length(max = 128)
    private String idempotencyKey;

    private final ReceiptRepository receiptRepository;
    private final ReceivableRepository receivableRepository;
    private final FinanceAccessContext accessContext;

    public Save(ReceiptRepository receiptRepository,
                ReceivableRepository receivableRepository,
                FinanceAccessContext accessContext) {
        this.receiptRepository = receiptRepository;
        this.receivableRepository = receivableRepository;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional
    public Receipt call(Receipt request) {
        Employee current = accessContext.requireEditor(employee);
        Project project = accessContext.resolveProject(current, projectId);
        Receivable receivable = receivableRepository.findById(receivableId)
                .filter(item -> item.getProject() != null
                        && item.getProject().getProjectId().equals(project.getProjectId()))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "应收节点不存在")));
        BigDecimal paid = amount == null ? BigDecimal.ZERO : amount;
        if (paid.signum() <= 0) {
            throw new DefinedException(new DefaultErrorCode("400", "收款金额必须大于 0"));
        }
        FundAccount targetAccount = account == null ? FundAccount.CompanyBank : account;
        String targetPayer = payerName == null || payerName.isBlank()
                ? project.getCustomer().getName() : payerName;
        // 幂等键不替代金额、应收余额、企业/项目归属和权限校验，只用于同企业内业务事实去重。
        // 但重复提交必须先命中幂等分支：余额校验放在其后，否则同一笔收款的网络重试会在
        // 核销完成后被「收款金额不能超过应收余额」拦截，永远拿不到原收款记录（DEF-PROJ37-002）。
        String key = idempotencyKey(project, receivable, paid, targetAccount, receivedAt, targetPayer);
        Optional<Receipt> existing = receiptRepository
                .findByCompany_ComIdAndIdempotencyKey(current.getCompany().getComId(), key);
        if (existing.isPresent()) {
            Receipt found = existing.get();
            if (sameBusiness(found, project, receivable, paid, targetAccount, receivedAt, targetPayer)) {
                // 重复提交返回原收款记录，不再新增收款、核销和资金台账。
                return found;
            }
            throw new DefinedException(new DefaultErrorCode("409", "该幂等键已被其他收款占用，请勿复用"));
        }
        // 非重复提交才校验应收余额，保证超额收款仍被阻断。
        BigDecimal outstanding = accessContext.outstanding(receivable.getAmount(), receivable.getReceivedAmount());
        if (paid.compareTo(outstanding) > 0) {
            throw new DefinedException(new DefaultErrorCode("400", "收款金额不能超过应收余额"));
        }

        request.setCompany(current.getCompany());
        request.setProject(project);
        request.setReceivable(receivable);
        request.setAmount(paid);
        request.setAccount(targetAccount);
        request.setReceivedAt(receivedAt);
        request.setPayerName(targetPayer);
        request.setVoucherUrl(voucherUrl);
        request.setRemark(remark);
        request.setIdempotencyKey(key);
        request.setCreatedBy(current);
        accessContext.applyOwnership(request, current, project);
        Receipt saved = receiptRepository.save(request);

        receivable.setReceivedAmount(accessContext.value(receivable.getReceivedAmount()).add(paid));
        accessContext.refreshReceivableStatus(receivable);
        receivableRepository.save(receivable);
        accessContext.recordLedger(current, project, LedgerDirection.Income,
                receivable.getType().getName(), LedgerSource.Receipt, saved.getReceiptId(), paid,
                saved.getAccount(), receivedAt, saved.getPayerName(),
                "收款 · " + receivable.getName(), remark);
        return saved;
    }

    /** 企业内唯一的幂等键：客户端键 + 项目/应收/金额/账户/业务日期/付款人业务指纹。 */
    private String idempotencyKey(Project project, Receivable receivable, BigDecimal amount,
                                  FundAccount account, LocalDate receivedAt, String payerName) {
        String client = idempotencyKey == null ? "" : idempotencyKey.trim();
        String base = String.join("|",
                String.valueOf(project.getProjectId()),
                String.valueOf(receivable.getReceivableId()),
                amount.stripTrailingZeros().toPlainString(),
                account.name(),
                receivedAt == null ? "" : receivedAt.toString(),
                payerName,
                client);
        return sha256(base);
    }

    private boolean sameBusiness(Receipt found, Project project, Receivable receivable, BigDecimal amount,
                                 FundAccount account, LocalDate receivedAt, String payerName) {
        return found.getProject() != null
                && project.getProjectId().equals(found.getProject().getProjectId())
                && found.getReceivable() != null
                && receivable.getReceivableId().equals(found.getReceivable().getReceivableId())
                && found.getAmount() != null && found.getAmount().compareTo(amount) == 0
                && found.getAccount() == account
                && receivedAt != null && receivedAt.equals(found.getReceivedAt())
                && payerName.equals(found.getPayerName());
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }
}
