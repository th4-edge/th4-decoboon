package com.th4.edge.decoboon.common.context;

import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.SecurityAction;
import com.th4.edge.tenant.user.configure.SecurityResult;
import com.th4.edge.tenant.user.configure.StoreStatus;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.context.UserIdentityContext;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.StoreRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 门店启用/停用规则（`P0D-04` / `DESIGN-BIZ-20261013-002`，`DEC-065`/`DEC-067`）。
 *
 * <p>服务端强制规则（唯一实现，`user/store/status` 与 `user/store/update` 共用）：</p>
 * <ol>
 *   <li><b>仅 BOSS</b>：`TenantContext.requireBoss`；</li>
 *   <li><b>默认总店不可停用</b>（`DEC-065` 总部财务判定依据）；</li>
 *   <li><b>最后一家启用门店不可停用</b>（否则「全部门店」与「本店」等价性被破坏）；</li>
 *   <li><b>`GAP-051` 阻断</b>：该店仍有**在职员工**或**在手业务**（未完工项目 / 在手客户 / 未结应收 / 未结应付）时
 *       拒绝停用并提示先迁移——不允许出现「员工或业务挂在已失效门店」的中间态；</li>
 *   <li><b>审计留痕</b>：写企业安全日志（`SecurityAction.StoreStatusChange`，BOSS 可在「安全日志」查看），
 *       记录操作人、门店、前后状态与时间。</li>
 * </ol>
 *
 * <p>停用**不删除任何数据**：历史单据门店归属保留、仍计入「全部门店」合计、门店维度控件中标注「已停用」可查看；
 * 仅**写入归属**被拦截（见 {@link StoreAttributionContext#requireAttributable}）。</p>
 *
 * <p>套餐门店额度计数**不变**（`SubscriptionContext.requireStoreCapacity` 用 `countByCompany_ComId` 计全部门店，
 * 已含停用门店，天然防「停用—新建」规避额度）。</p>
 */
@Component
public class StoreStatusContext {

    private static final Logger log = LoggerFactory.getLogger(StoreStatusContext.class);

    /**
     * 在手客户状态（停用阻断判据）：已流失 / 已完工视为已结束，其余视为在手。
     * 说明：休眠客户按「已结束」处理，避免历史线索永久阻止门店停用。
     */
    private static final String ACTIVE_CUSTOMER_STATUSES = "'New','Following','PendingMeasure','Quoted','Signed','Serving'";

    private static final String PENDING_BUSINESS_SQL = ("""
            select
              (select count(*) from db_tenant_employee
                 where com_id = :comId and store_id = :storeId and status = 'Active') as employees,
              (select count(*) from cust_customer
                 where com_id = :comId and store_id = :storeId
                   and status in (%s)) as customers,
              (select count(*) from db_project
                 where com_id = :comId and store_id = :storeId
                   and status not in ('Finished','Settled')) as projects,
              (select count(*) from db_finance_receivable
                 where com_id = :comId and store_id = :storeId
                   and status not in ('Received','Closed')) as receivables,
              (select count(*) from db_finance_payable
                 where com_id = :comId and store_id = :storeId
                   and status not in ('Paid','Closed')) as payables
            """).formatted(ACTIVE_CUSTOMER_STATUSES);

    private final TenantContext tenantContext;
    private final StoreRepository storeRepository;
    private final UserIdentityContext userIdentityContext;
    private final EntityManager entityManager;

    public StoreStatusContext(TenantContext tenantContext,
                              StoreRepository storeRepository,
                              UserIdentityContext userIdentityContext,
                              EntityManager entityManager) {
        this.tenantContext = tenantContext;
        this.storeRepository = storeRepository;
        this.userIdentityContext = userIdentityContext;
        this.entityManager = entityManager;
    }

    /** 门店在手业务/在职员工计数（停用阻断与前端提示共用）。 */
    public record PendingBusiness(int employees, int customers, int projects, int receivables, int payables) {
        public boolean blocking() {
            return employees > 0 || customers > 0 || projects > 0 || receivables > 0 || payables > 0;
        }

        public String describe() {
            List<String> parts = new ArrayList<>();
            if (employees > 0) parts.add("在职员工 " + employees + " 人");
            if (customers > 0) parts.add("在手客户 " + customers + " 条");
            if (projects > 0) parts.add("未完工项目 " + projects + " 个");
            if (receivables > 0) parts.add("未结应收 " + receivables + " 条");
            if (payables > 0) parts.add("未结应付 " + payables + " 条");
            return String.join("、", parts);
        }
    }

    /** 停用/启用门店（幂等：状态未变化直接返回且不写审计）。 */
    @Transactional
    public Store change(Employee principal, Long storeId, StoreStatus target) {
        Employee current = tenantContext.requireBoss(principal);
        if (target == null) {
            throw new DefinedException(new DefaultErrorCode("400", "请选择门店状态（启用/已停用）"));
        }
        Long comId = current.getCompany() == null ? null : current.getCompany().getComId();
        Store store = requireCompanyStore(storeId, comId);
        StoreStatus from = store.getStatus() == null ? StoreStatus.Active : store.getStatus();
        if (from == target) {
            return store;
        }
        if (target == StoreStatus.Disabled) {
            requireDisableAllowed(comId, store);
        }
        store.setStatus(target);
        Store saved = storeRepository.save(store);
        String detail = "%s：%s → %s".formatted(store.getName(), from.getName(), target.getName());
        // 审计留痕：带上操作人凭证（userId/phone），保证企业「安全日志」页（按 phone/empId 查询）可见该记录
        userIdentityContext.log(userIdentityContext.requireWx(current), SecurityAction.StoreStatusChange,
                SecurityResult.Success, current, "门店状态变更", detail);
        log.info("store-status storeId={} name={} {} -> {} operator={}",
                saved.getStoreId(), saved.getName(), from, target, current.getEmpId());
        return saved;
    }

    /** 停用前置校验（默认总店 / 最后一家启用门店 / GAP-051 在手业务）。 */
    public void requireDisableAllowed(Long comId, Store store) {
        if (Boolean.TRUE.equals(store.getDefaultStore())) {
            throw new DefinedException(new DefaultErrorCode("400",
                    "默认总店不可停用（总部财务判定依据），如需关店请先另设门店并调整默认总店"));
        }
        long activeStores = storeRepository.countByCompany_ComIdAndStatus(comId, StoreStatus.Active);
        if (activeStores <= 1) {
            throw new DefinedException(new DefaultErrorCode("400",
                    "企业最后一家启用门店不可停用（否则「全部门店」与「本店」等价性被破坏）"));
        }
        PendingBusiness pending = pendingBusiness(comId, store.getStoreId());
        if (pending.blocking()) {
            throw new DefinedException(new DefaultErrorCode("400",
                    "门店「" + store.getName() + "」仍有" + pending.describe() + "，请先迁移员工与业务后再停用"));
        }
    }

    /** 该门店的在职员工与在手业务计数（单条 SQL 聚合）。 */
    @Transactional(readOnly = true)
    public PendingBusiness pendingBusiness(Long comId, Long storeId) {
        Object[] row = (Object[]) entityManager.createNativeQuery(PENDING_BUSINESS_SQL)
                .setParameter("comId", comId)
                .setParameter("storeId", storeId)
                .getSingleResult();
        return new PendingBusiness(number(row[0]), number(row[1]), number(row[2]), number(row[3]), number(row[4]));
    }

    /** 服务端校验：门店存在且属本企业（越权 / 不存在 → 403）。 */
    public Store requireCompanyStore(Long storeId, Long comId) {
        Store store = storeId == null ? null : storeRepository.findByStoreId(storeId);
        if (store == null || comId == null || store.getCompany() == null
                || !comId.equals(store.getCompany().getComId())) {
            throw new DefinedException(new DefaultErrorCode("403", "门店不存在或无权访问"));
        }
        return store;
    }

    private int number(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
    }
}
