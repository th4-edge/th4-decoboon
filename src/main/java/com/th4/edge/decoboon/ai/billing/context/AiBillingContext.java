package com.th4.edge.decoboon.ai.billing.context;

import com.th4.edge.decoboon.ai.billing.configure.AiAccountStatus;
import com.th4.edge.decoboon.ai.billing.configure.AiLedgerType;
import com.th4.edge.decoboon.ai.billing.configure.AiRateCardStatus;
import com.th4.edge.decoboon.ai.billing.model.AiRateCardVersion;
import com.th4.edge.decoboon.ai.billing.model.AiResourceAccount;
import com.th4.edge.decoboon.ai.billing.model.AiResourceLedger;
import com.th4.edge.decoboon.ai.billing.repository.AiRateCardVersionRepository;
import com.th4.edge.decoboon.ai.billing.repository.AiResourceAccountRepository;
import com.th4.edge.decoboon.ai.billing.repository.AiResourceLedgerRepository;
import com.th4.edge.decoboon.ai.model.AiUsageCall;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 正式点数账本上下文。
 *
 * <p>A0.2 只提供内部结算机制，默认关闭正式扣点。费率、套餐点数和资源包价格必须在 A0.4 成本验证后另行发布。</p>
 */
@Component
public class AiBillingContext {

    private final AiResourceAccountRepository accountRepository;
    private final AiResourceLedgerRepository ledgerRepository;
    private final AiRateCardVersionRepository rateCardRepository;
    private final boolean formalEnabled;

    public AiBillingContext(AiResourceAccountRepository accountRepository,
                            AiResourceLedgerRepository ledgerRepository,
                            AiRateCardVersionRepository rateCardRepository,
                            @Value("${ai.billing.formal-enabled:false}") boolean formalEnabled) {
        this.accountRepository = accountRepository;
        this.ledgerRepository = ledgerRepository;
        this.rateCardRepository = rateCardRepository;
        this.formalEnabled = formalEnabled;
    }

    /** 读取企业账户，不存在时创建零余额账户。 */
    @Transactional
    public AiResourceAccount account(Company company) {
        requireCompany(company);
        AiResourceAccount account = accountRepository.lockByCompanyId(company.getComId()).orElse(null);
        if (account != null) return account;
        account = new AiResourceAccount();
        account.setCompany(company);
        return accountRepository.save(account);
    }

    /** 内部测试或套餐发放点数。真实套餐额度数值暂缓。 */
    @Transactional
    public AiResourceLedger grant(Employee operator, Company company, long units, String reason) {
        requirePositive(units, "发放点数必须大于0");
        AiResourceAccount account = lockedAccount(company);
        account.setTotalGranted(nvl(account.getTotalGranted()) + units);
        account.setAvailableUnits(nvl(account.getAvailableUnits()) + units);
        return ledger(operator, account, null, AiLedgerType.Grant, units, null, reason);
    }

    /** 预占点数。同一调用重复预占幂等。 */
    @Transactional
    public AiResourceLedger reserve(Employee operator, AiUsageCall call, long units) {
        requireFormal();
        if (call == null || call.getCallId() == null) throw error("AI调用尚未保存，不能预占点数");
        if (units <= 0) return null;
        List<AiResourceLedger> exists = ledgerRepository
                .findByCompany_ComIdAndCall_CallIdAndType(call.getCompany().getComId(), call.getCallId(), AiLedgerType.Reserve);
        if (!exists.isEmpty()) return exists.get(exists.size() - 1);
        AiResourceAccount account = lockedAccount(call.getCompany());
        if (account.getStatus() != AiAccountStatus.Active) throw error("AI资源账户已冻结");
        if (nvl(account.getAvailableUnits()) < units) throw error("AI资源点数不足");
        account.setAvailableUnits(nvl(account.getAvailableUnits()) - units);
        account.setReservedUnits(nvl(account.getReservedUnits()) + units);
        return ledger(operator, account, call, AiLedgerType.Reserve, -units, call.getRateCardVersion(), "调用预占");
    }

    /** 预占转实际消耗。实际消耗不得超过预占，超出进入人工处理。 */
    @Transactional
    public AiResourceLedger settle(Employee operator, AiUsageCall call, long actualUnits) {
        requireFormal();
        if (call == null || call.getCallId() == null) throw error("AI调用尚未保存，不能结算点数");
        if (actualUnits < 0) throw error("实际点数不能小于0");
        List<AiResourceLedger> consumed = ledgerRepository
                .findByCompany_ComIdAndCall_CallIdAndType(call.getCompany().getComId(), call.getCallId(), AiLedgerType.Consume);
        if (!consumed.isEmpty()) return consumed.get(consumed.size() - 1);
        long reserved = reservedForCall(call);
        if (actualUnits > reserved) throw error("实际消耗超过预占点数，需人工处理");

        AiResourceAccount account = lockedAccount(call.getCompany());
        if (reserved > 0) {
            account.setReservedUnits(nvl(account.getReservedUnits()) - reserved);
            account.setAvailableUnits(nvl(account.getAvailableUnits()) + reserved);
            ledger(operator, account, call, AiLedgerType.Release, reserved, call.getRateCardVersion(), "结算释放预占");
        }
        long billable = Math.max(1L, actualUnits);
        account.setAvailableUnits(nvl(account.getAvailableUnits()) - billable);
        account.setConsumedUnits(nvl(account.getConsumedUnits()) + billable);
        return ledger(operator, account, call, AiLedgerType.Consume, -billable, call.getRateCardVersion(), "成功调用结算");
    }

    /** 失败、超时、取消或重复拦截时释放预占。 */
    @Transactional
    public AiResourceLedger release(Employee operator, AiUsageCall call, String reason) {
        requireFormal();
        if (call == null || call.getCallId() == null) throw error("AI调用尚未保存，不能释放点数");
        if (!ledgerRepository.findByCompany_ComIdAndCall_CallIdAndType(
                call.getCompany().getComId(), call.getCallId(), AiLedgerType.Consume).isEmpty()) {
            return null;
        }
        long reserved = reservedForCall(call);
        long released = releasedForCall(call);
        long remaining = reserved - released;
        if (remaining <= 0) return null;
        AiResourceAccount account = lockedAccount(call.getCompany());
        account.setReservedUnits(nvl(account.getReservedUnits()) - remaining);
        account.setAvailableUnits(nvl(account.getAvailableUnits()) + remaining);
        return ledger(operator, account, call, AiLedgerType.Release, remaining, call.getRateCardVersion(), reason);
    }

    /** 人工返还，原 Consume 保留，返还单独入账。 */
    @Transactional
    public AiResourceLedger refund(Employee operator, AiUsageCall call, long units, String reason) {
        requireFormal();
        requirePositive(units, "返还点数必须大于0");
        if (call == null || call.getCallId() == null) throw error("AI调用尚未保存，不能返还点数");
        long consumed = consumedForCall(call);
        long refunded = refundedForCall(call);
        if (consumed <= 0) throw error("调用尚未产生正式消耗，不能返还");
        if (refunded + units > consumed) throw error("返还点数不能超过已消耗点数");
        AiResourceAccount account = lockedAccount(call.getCompany());
        account.setAvailableUnits(nvl(account.getAvailableUnits()) + units);
        account.setRefundedUnits(nvl(account.getRefundedUnits()) + units);
        return ledger(operator, account, call, AiLedgerType.Refund, units, call.getRateCardVersion(), reason);
    }

    /** 人工调整，正数增加、负数扣减，不允许负余额。 */
    @Transactional
    public AiResourceLedger adjust(Employee operator, Company company, long units, String reason) {
        requireFormal();
        if (units == 0) throw error("调整点数不能为0");
        AiResourceAccount account = lockedAccount(company);
        long after = nvl(account.getAvailableUnits()) + units;
        if (after < 0) throw error("调整后余额不能为负数");
        account.setAvailableUnits(after);
        account.setAdjustedUnits(nvl(account.getAdjustedUnits()) + units);
        return ledger(operator, account, null, AiLedgerType.Adjust, units, null, reason);
    }

    /** 按当前生效费率计算点数。成功调用最低1点。 */
    public long calculate(String capabilityCode, long resourceUnits, BigDecimal qualityMultiplier) {
        AiRateCardVersion rateCard = rateCardRepository
                .findFirstByCapabilityCodeAndStatusOrderByVersionNoDesc(capabilityCode, AiRateCardStatus.Active)
                .orElseThrow(() -> error("未配置生效AI费率版本"));
        if (resourceUnits <= 0) return 0L;
        BigDecimal unitsPerPoint = rateCard.getUnitsPerPoint();
        if (unitsPerPoint == null || unitsPerPoint.signum() <= 0) throw error("每点资源单位配置无效");
        BigDecimal quality = qualityMultiplier == null ? BigDecimal.ONE : qualityMultiplier;
        BigDecimal points = BigDecimal.valueOf(resourceUnits)
                .divide(unitsPerPoint, 8, RoundingMode.HALF_UP)
                .multiply(nvl(rateCard.getModelMultiplier(), BigDecimal.ONE))
                .multiply(nvl(rateCard.getQualityMultiplier(), BigDecimal.ONE))
                .multiply(quality);
        return Math.max(1L, points.setScale(0, RoundingMode.CEILING).longValue());
    }

    private AiResourceAccount lockedAccount(Company company) {
        requireCompany(company);
        AiResourceAccount account = accountRepository.lockByCompanyId(company.getComId()).orElse(null);
        if (account == null) {
            account = new AiResourceAccount();
            account.setCompany(company);
            account = accountRepository.save(account);
        }
        return account;
    }

    private AiResourceLedger ledger(Employee operator, AiResourceAccount account, AiUsageCall call,
                                    AiLedgerType type, long changeUnits, String rateCardVersion, String reason) {
        long after = nvl(account.getAvailableUnits());
        long before = after - changeUnits;
        account.setBalanceVersion(nvl(account.getBalanceVersion()) + 1);
        AiResourceLedger ledger = new AiResourceLedger();
        ledger.setCompany(account.getCompany());
        ledger.setAllocationStore(call == null ? null : call.getAllocationStore());
        ledger.setCall(call);
        ledger.setType(type);
        ledger.setChangeUnits(changeUnits);
        ledger.setBalanceBefore(before);
        ledger.setBalanceAfter(after);
        ledger.setRateCardVersion(rateCardVersion);
        ledger.setReason(reason);
        ledger.setOperator(operator);
        ledger.setOccurredAt(LocalDateTime.now());
        accountRepository.save(account);
        return ledgerRepository.save(ledger);
    }

    private long reservedForCall(AiUsageCall call) {
        return ledgerRepository.findByCompany_ComIdAndCall_CallIdAndType(
                        call.getCompany().getComId(), call.getCallId(), AiLedgerType.Reserve).stream()
                .mapToLong(item -> Math.abs(nvl(item.getChangeUnits()))).sum();
    }

    private long releasedForCall(AiUsageCall call) {
        return ledgerRepository.findByCompany_ComIdAndCall_CallIdAndType(
                        call.getCompany().getComId(), call.getCallId(), AiLedgerType.Release).stream()
                .mapToLong(item -> nvl(item.getChangeUnits())).sum();
    }

    private long consumedForCall(AiUsageCall call) {
        return ledgerRepository.findByCompany_ComIdAndCall_CallIdAndType(
                        call.getCompany().getComId(), call.getCallId(), AiLedgerType.Consume).stream()
                .mapToLong(item -> Math.abs(nvl(item.getChangeUnits()))).sum();
    }

    private long refundedForCall(AiUsageCall call) {
        return ledgerRepository.findByCompany_ComIdAndCall_CallIdAndType(
                        call.getCompany().getComId(), call.getCallId(), AiLedgerType.Refund).stream()
                .mapToLong(item -> nvl(item.getChangeUnits())).sum();
    }

    private void requireFormal() {
        if (!formalEnabled) throw error("正式AI点数扣减未启用");
    }

    private void requireCompany(Company company) {
        if (company == null || company.getComId() == null) throw error("企业不能为空");
    }

    private void requirePositive(long units, String message) {
        if (units <= 0) throw error(message);
    }

    private long nvl(Long value) { return value == null ? 0L : value; }
    private BigDecimal nvl(BigDecimal value, BigDecimal fallback) { return value == null ? fallback : value; }
    private DefinedException error(String message) { return new DefinedException(new DefaultErrorCode("400", message)); }
}
