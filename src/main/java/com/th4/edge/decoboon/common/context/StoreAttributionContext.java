package com.th4.edge.decoboon.common.context;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.StoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 门店归属**写入端唯一解析器**（`DEC-070` / `DESIGN-BIZ-20261013-006`）。
 *
 * <p>把「门店归属」从每个对象各自复制来源，改为**单一事实源 = 客户门店 + 创建时快照**：</p>
 * <ol>
 *   <li>客户门店快照：对象创建时取客户门店（客户门店本身 = 负责人门店）；</li>
 *   <li>负责人门店：客户创建时取负责人门店；负责人无门店时——<b>单门店企业</b>默认落该门店，
 *       <b>多门店企业阻断</b>（400 + 可读错误，不静默写空）；</li>
 *   <li>当前门店维度：无项目的手工流水在操作人无门店时，用当前门店维度解析出的门店；</li>
 *   <li>仍无门店 → 阻断。</li>
 * </ol>
 *
 * <p>服务端校验：门店必须属本企业（越权 / 不存在一律 403）。门店启用/停用字段属 `P0D-04` 批次，
 * 本批在「状态」维度做透传（字段落地后在此处补校验，不改变调用方）。</p>
 *
 * <p><b>留痕</b>：归属来源（`owner_store` / `customer_store` / `scope_store` / `operator_store`）以
 * 结构化应用日志记录（本批**不新增数据库字段**）；正式审计表设施待另行立项。</p>
 *
 * <p><b>不回溯</b>：本解析器只在**写入时**调用，任何组织变动（换负责人、换门店、门店停用）都不回写历史对象。</p>
 */
@Component
public class StoreAttributionContext {

    /** 归属来源标签（留痕用，与规格 §3-5 一致）。 */
    public static final String OWNER_STORE = "owner_store";
    public static final String CUSTOMER_STORE = "customer_store";
    public static final String SCOPE_STORE = "scope_store";
    public static final String OPERATOR_STORE = "operator_store";
    /** 历史回退（存量客户缺门店时的原来源；进存量清单，不阻断业务）。 */
    public static final String LEGACY_STORE = "legacy_fallback";

    private static final Logger attributionLog = LoggerFactory.getLogger("store-attribution");

    private final StoreRepository storeRepository;
    private final StoreFilterContext storeFilterContext;

    public StoreAttributionContext(StoreRepository storeRepository, StoreFilterContext storeFilterContext) {
        this.storeRepository = storeRepository;
        this.storeFilterContext = storeFilterContext;
    }

    /** 解析结果：门店 + 归属来源。 */
    public record AttributedStore(Store store, String source) { }

    /**
     * 客户门店：负责人门店 → 单门店企业默认该门店 → 多门店企业阻断（`DEC-070` / `DEC-065` FIN-STORE-02）。
     */
    public AttributedStore forCustomerOwner(Employee owner) {
        if (owner == null || owner.getCompany() == null || owner.getCompany().getComId() == null) {
            throw new DefinedException(new DefaultErrorCode("403", "客户负责人未归属企业"));
        }
        Long comId = owner.getCompany().getComId();
        if (owner.getStore() != null) {
            return trace("customer", new AttributedStore(validateAttached(owner.getStore(), comId), OWNER_STORE));
        }
        List<Store> stores = storeRepository.findByCompany_ComId(comId);
        if (stores.size() == 1) {
            // 单门店企业：负责人无门店时默认落该门店（与上线前行为等价）
            return trace("customer", new AttributedStore(stores.get(0), OWNER_STORE));
        }
        throw new DefinedException(new DefaultErrorCode("400",
                "负责人未归属门店，无法确定客户门店：请先为该负责人在「员工」中设置门店，或改选有门店的负责人"));
    }

    /**
     * 对象门店快照（设计/报价/合同/项目/采购/单据等）：客户门店 → 负责人门店 → 单门店企业默认
     * → **历史回退**（调用方原来源，保证存量客户不被阻断）→ 仍无则落空并告警（进存量清单）。
     *
     * <p>与客户创建不同，本方法**不阻断**：对象创建不能因存量客户缺门店而失败（`DEC-067` BOSS-STORE-04
     * 要求存量清单人工确认，而不是阻断业务）。</p>
     *
     * @param legacyFallbackStore 调用方整改前的门店来源（仅当客户/负责人均无门店时使用）
     */
    public AttributedStore forCustomerSnapshot(Customer customer, Employee fallbackOwner, Store legacyFallbackStore) {
        if (customer == null) {
            return trace("snapshot", new AttributedStore(legacyFallbackStore, LEGACY_STORE));
        }
        Long comId = customer.getCompany() == null ? null : customer.getCompany().getComId();
        if (customer.getStore() != null && comId != null) {
            return trace("snapshot", new AttributedStore(validateAttached(customer.getStore(), comId), CUSTOMER_STORE));
        }
        Employee owner = customer.getOwner() != null ? customer.getOwner() : fallbackOwner;
        if (owner != null && owner.getStore() != null && comId != null) {
            attributionLog.warn("store-attribution 客户无门店，按负责人门店回落 object=snapshot customerId={} ownerId={}",
                    customer.getCustId(), owner.getEmpId());
            return trace("snapshot", new AttributedStore(validateAttached(owner.getStore(), comId), OWNER_STORE));
        }
        if (comId != null) {
            List<Store> stores = storeRepository.findByCompany_ComId(comId);
            if (stores.size() == 1) {
                return trace("snapshot", new AttributedStore(stores.get(0), OWNER_STORE));
            }
        }
        if (legacyFallbackStore != null) {
            attributionLog.warn("store-attribution 门店归属无法解析，按历史来源回落 object=snapshot customerId={} storeId={}",
                    customer.getCustId(), legacyFallbackStore.getStoreId());
            return trace("snapshot", new AttributedStore(legacyFallbackStore, LEGACY_STORE));
        }
        attributionLog.warn("store-attribution 门店归属无法解析（存量待回填）object=snapshot customerId={}", customer.getCustId());
        return trace("snapshot", new AttributedStore(null, LEGACY_STORE));
    }

    /** 无项目手工流水：操作人门店 → 当前门店维度的门店 → 阻断。 */
    public AttributedStore forOperator(Employee operator, Long requestedStoreId) {
        if (operator == null || operator.getCompany() == null || operator.getCompany().getComId() == null) {
            throw new DefinedException(new DefaultErrorCode("403", "当前员工未归属企业"));
        }
        Long comId = operator.getCompany().getComId();
        if (operator.getStore() != null) {
            return trace("operator", new AttributedStore(validateAttached(operator.getStore(), comId), OPERATOR_STORE));
        }
        // 当前门店维度（越权校验复用唯一实现）；全部门店 / 无门店范围 → 仍无门店 → 阻断
        Long effective = storeFilterContext.resolve(operator, requestedStoreId);
        if (StoreScope.isAllStores(effective)) {
            throw new DefinedException(new DefaultErrorCode("400",
                    "当前账号未归属门店，且未指定门店维度：请先在「员工」中为该账号设置门店，或在门店维度中选择一家门店"));
        }
        return trace("operator", new AttributedStore(requireCompanyStore(effective, comId), SCOPE_STORE));
    }

    /**
     * 校验「已挂在持久化聚合上的门店」（客户/负责人/操作人自身门店）：能拿到企业就比对；
     * 企业未加载时回查仓储；仍查不到则信任聚合（该门店由本系统写入、非前端可控字段）。
     */
    private Store validateAttached(Store store, Long comId) {
        if (store.getCompany() != null && store.getCompany().getComId() != null) {
            if (!store.getCompany().getComId().equals(comId)) {
                throw new DefinedException(new DefaultErrorCode("403", "门店不存在或无权访问"));
            }
            return requireAttributable(store);
        }
        Store persisted = store.getStoreId() == null ? null : storeRepository.findByStoreId(store.getStoreId());
        if (persisted == null) {
            return requireAttributable(store);
        }
        return requireCompanyStore(persisted.getStoreId(), comId);
    }

    /** 服务端校验：门店存在且属本企业（越权 / 不存在 → 403）。 */
    public Store requireCompanyStore(Long storeId, Long comId) {
        Store store = storeId == null ? null : storeRepository.findByStoreId(storeId);
        if (store == null || store.getCompany() == null || !store.getCompany().getComId().equals(comId)) {
            throw new DefinedException(new DefaultErrorCode("403", "门店不存在或无权访问"));
        }
        return requireAttributable(store);
    }

    /**
     * P0D-04 门禁：**停用门店不可承接新的归属**（`DESIGN-BIZ-20261013-002` §2 规则 3）。
     *
     * <p>只拦**写入**（客户/项目/报价/合同/财务单据/采购的新增归属）；读取（门店维度筛选、历史查看）
     * 不受影响——停用门店的历史数据仍可见、仍计入「全部门店」合计。</p>
     */
    public Store requireAttributable(Store store) {
        if (store != null && !store.attributable()) {
            throw new DefinedException(new DefaultErrorCode("400",
                    "门店「" + store.getName() + "」已停用，不能作为新的归属门店：请先在门店主数据中启用该门店，"
                            + "或改选在用门店；停用门店的历史数据仍可查看"));
        }
        return store;
    }

    /** 归属留痕（结构化日志；不新增字段）。 */
    public AttributedStore trace(String object, AttributedStore attributed) {
        attributionLog.info("store-attribution object={} source={} storeId={} storeName={}",
                object, attributed.source(),
                attributed.store() == null ? null : attributed.store().getStoreId(),
                attributed.store() == null ? null : attributed.store().getName());
        return attributed;
    }
}
