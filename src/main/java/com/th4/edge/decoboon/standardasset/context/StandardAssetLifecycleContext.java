package com.th4.edge.decoboon.standardasset.context;

import com.th4.edge.decoboon.standardasset.configure.StandardAssetDefaultPolicy;
import com.th4.edge.decoboon.standardasset.configure.StandardAssetSourceMode;
import com.th4.edge.decoboon.standardasset.configure.StandardAssetType;
import com.th4.edge.decoboon.standardasset.configure.StandardAssetVersionMode;
import com.th4.edge.decoboon.standardasset.model.StandardAssetLifecycleContract;
import com.th4.edge.decoboon.standardasset.model.StandardAssetStatusContract;
import com.th4.edge.framework.core.annotation.Note;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 企业运营标准资产生命周期契约。
 *
 * <p>只描述跨资产业务语义，不承载金额、成本或企业经营数据。正式口径见
 * DEC-083 / BR-051；各资产写入和历史快照仍由对应领域切片实现。</p>
 */
@Note("企业运营标准资产生命周期契约上下文")
@Component
public class StandardAssetLifecycleContext {

    private static final String SNAPSHOT_HISTORY =
            "历史业务读取来源版本或完整业务快照，不从当前资产重新计算；来源不可确认时保留既有快照并显式标记";

    public List<StandardAssetLifecycleContract> contracts() {
        return List.of(
                catalog(),
                packageTemplate(),
                paymentPlanTemplate(),
                constructionPlanTemplate(),
                documentTemplate()
        );
    }

    private StandardAssetLifecycleContract catalog() {
        return contract(StandardAssetType.CatalogItem, "报价项目库",
                statuses(status("Active", "启用", true, true), status("Inactive", "停用", true, false)),
                StandardAssetVersionMode.Revision,
                StandardAssetSourceMode.CompanyOnly,
                StandardAssetDefaultPolicy.NotApplicable,
                List.of("sourceId", "sourceCode", "sourceRevision"),
                List.of("name", "spec", "unit", "pricingMode", "salePrice", "standardCost", "formula", "craft", "quantity"));
    }

    private StandardAssetLifecycleContract packageTemplate() {
        return contract(StandardAssetType.PackageTemplate, "套餐模板",
                statuses(status("Draft", "草稿", true, false), status("Published", "已发布", false, true),
                        status("Disabled", "已停用", false, false)),
                StandardAssetVersionMode.ImmutableVersion,
                StandardAssetSourceMode.CompanyOnly,
                StandardAssetDefaultPolicy.Deferred,
                List.of("templateId", "code", "version"),
                List.of("name", "standardArea", "listPrice", "discount", "cost", "lines", "quantityFactors", "differences"));
    }

    private StandardAssetLifecycleContract paymentPlanTemplate() {
        return contract(StandardAssetType.PaymentPlanTemplate, "付款计划模板",
                statuses(status("Draft", "草稿", true, false), status("Enabled", "已启用", false, true),
                        status("Disabled", "已停用", false, false)),
                StandardAssetVersionMode.ImmutableVersion,
                StandardAssetSourceMode.BaselineCopy,
                StandardAssetDefaultPolicy.OptionalSingle,
                List.of("sourceCode", "sourceVersion"),
                List.of("nodes", "ratios", "triggerTypes", "triggerStageCodes", "offsetDays", "sort"));
    }

    private StandardAssetLifecycleContract constructionPlanTemplate() {
        return contract(StandardAssetType.ConstructionPlanTemplate, "施工计划模板",
                statuses(status("Draft", "草稿", true, false), status("Enabled", "已启用", false, true),
                        status("Disabled", "已停用", false, false)),
                StandardAssetVersionMode.ImmutableVersion,
                StandardAssetSourceMode.BaselineCopy,
                StandardAssetDefaultPolicy.OptionalSingle,
                List.of("sourceCode", "sourceVersion"),
                List.of("totalDays", "stages", "defaultDays", "acceptanceModes", "sort"));
    }

    private StandardAssetLifecycleContract documentTemplate() {
        return contract(StandardAssetType.DocumentTemplate, "单据/凭证模板",
                statuses(status("Draft", "草稿", true, false), status("Published", "已发布", false, true),
                        status("Disabled", "已停用", false, false)),
                StandardAssetVersionMode.ImmutableVersion,
                StandardAssetSourceMode.BaselineCopy,
                StandardAssetDefaultPolicy.NotApplicable,
                List.of("templateId", "templateVersion"),
                List.of("fieldConfig", "layoutConfig", "styleConfig", "businessSnapshot", "fileHash", "generatedAt"));
    }

    private StandardAssetLifecycleContract contract(StandardAssetType type,
                                                    String productName,
                                                    List<StandardAssetStatusContract> statuses,
                                                    StandardAssetVersionMode versionMode,
                                                    StandardAssetSourceMode sourceMode,
                                                    StandardAssetDefaultPolicy defaultPolicy,
                                                    List<String> referenceFields,
                                                    List<String> snapshotFields) {
        StandardAssetLifecycleContract contract = new StandardAssetLifecycleContract();
        contract.setAssetType(type);
        contract.setProductName(productName);
        contract.setStatuses(statuses);
        contract.setVersionMode(versionMode);
        contract.setSourceMode(sourceMode);
        contract.setDefaultPolicy(defaultPolicy);
        contract.setReferenceFields(referenceFields);
        contract.setSnapshotFields(snapshotFields);
        contract.setHistoryPolicy(SNAPSHOT_HISTORY);
        return contract;
    }

    private List<StandardAssetStatusContract> statuses(StandardAssetStatusContract... statuses) {
        return List.of(statuses);
    }

    private StandardAssetStatusContract status(String key, String label,
                                               boolean editable, boolean newBusinessEligible) {
        StandardAssetStatusContract status = new StandardAssetStatusContract();
        status.setKey(key);
        status.setLabel(label);
        status.setEditable(editable);
        status.setNewBusinessEligible(newBusinessEligible);
        status.setHistoryReadable(Boolean.TRUE);
        return status;
    }
}
