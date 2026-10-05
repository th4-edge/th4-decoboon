package com.th4.edge.decoboon.finance.context;

import com.th4.edge.decoboon.finance.configure.FundAccount;
import com.th4.edge.decoboon.finance.model.OpeningBalance;
import com.th4.edge.decoboon.finance.repository.OpeningBalanceRepository;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 期初资金余额：接管日各账户起始余额，不计入收入/支出/利润。 */
@Component
public class OpeningBalanceContext {

    private final OpeningBalanceRepository repository;

    public OpeningBalanceContext(OpeningBalanceRepository repository) {
        this.repository = repository;
    }

    /** 按企业 + 账户 + 接管日幂等保存；重复保存视为更正余额。 */
    @Transactional
    public OpeningBalance save(Employee current, FundAccount account, BigDecimal amount,
                               LocalDate asOfDate, String remark) {
        if (account == null) {
            throw new DefinedException(new DefaultErrorCode("400", "请选择资金账户"));
        }
        if (asOfDate == null) {
            throw new DefinedException(new DefaultErrorCode("400", "请填写接管日"));
        }
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount;
        OpeningBalance target = repository
                .findByCompany_ComIdAndAccountAndAsOfDate(current.getCompany().getComId(), account, asOfDate)
                .orElseGet(OpeningBalance::new);
        if (target.getOpeningId() == null) {
            target.setCompany(current.getCompany());
            target.setAccount(account);
            target.setAsOfDate(asOfDate);
            target.setCreatedBy(current);
        }
        target.setAmount(value);
        target.setRemark(remark == null ? null : remark.trim());
        return repository.save(target);
    }

    @Transactional(readOnly = true)
    public List<OpeningBalance> list(Employee current) {
        return repository.findByCompany_ComIdOrderByAsOfDateDescAccountAsc(current.getCompany().getComId());
    }

    @Transactional(readOnly = true)
    public LocalDate latestAsOfDate(Long comId) {
        return repository.findByCompany_ComIdOrderByAsOfDateDescAccountAsc(comId).stream()
                .map(OpeningBalance::getAsOfDate)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /** 以最新接管日为准汇总期初余额；多批次时只取最新基线。 */
    @Transactional(readOnly = true)
    public BigDecimal totalAtLatest(Long comId) {
        LocalDate latest = latestAsOfDate(comId);
        if (latest == null) return BigDecimal.ZERO;
        return repository.findByCompany_ComIdAndAsOfDateOrderByAccountAsc(comId, latest).stream()
                .map(item -> item.getAmount() == null ? BigDecimal.ZERO : item.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public OpeningBalance require(Long comId, Long openingId) {
        return repository.findByCompany_ComIdAndOpeningId(comId, openingId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "期初余额不存在或无权访问")));
    }
}
