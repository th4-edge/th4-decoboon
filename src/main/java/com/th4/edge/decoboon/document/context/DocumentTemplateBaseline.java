package com.th4.edge.decoboon.document.context;

import com.th4.edge.decoboon.document.configure.DocumentStatus;
import com.th4.edge.decoboon.document.configure.DocumentTemplateScope;
import com.th4.edge.decoboon.document.configure.DocumentType;
import com.th4.edge.decoboon.document.model.DocumentTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 平台行业单据模板基线。
 *
 * <p>报价、材料与合同各一条基线；收款单与付款单提供**常用凭证模板库**，
 * 企业可一键套用后改名保存为自己的模板。基线不落企业表。</p>
 */
@Component
public class DocumentTemplateBaseline {

    public static final int VERSION = 2;

    private static final String DEFAULT_FIELDS = """
            {"showCompanyName":true,"showCompanyAddress":true,"showServiceHotline":true}
            """.trim();

    private static final String DEFAULT_STYLE = """
            {"primaryColor":"#0E8A7D","fontSize":12,"rowSpacing":2}
            """.trim();

    /** 常用凭证模板预设。 */
    public record Preset(String code, DocumentType type, String name, String note, String fieldConfig) {
    }

    /** 付款类预设：统一生成字段 JSON。 */
    private static String paymentFields(boolean workerIdentity, boolean detailCategory, boolean detailRemark) {
        return """
                {"showCompanyName":true,"showServiceHotline":true,"showVoucherNo":true,"showVoucherDate":true,
                 "showPartyName":true,"showWorkerPhone":%s,"showWorkerIdCard":%s,"showWorkerTrade":%s,
                 "showProject":%s,"showAmount":true,"showAmountWords":true,"showSignBlock":true,
                 "showDetailNo":true,"showDetailDate":true,"showDetailAccount":true,"showDetailAmount":true,
                 "showDetailTarget":%s,"showDetailCategory":%s,"showDetailRemark":%s}
                """.formatted(workerIdentity, workerIdentity, workerIdentity,
                workerIdentity, workerIdentity, detailCategory, detailRemark).trim();
    }

    /** 收款类预设：统一生成字段 JSON。 */
    private static String receiptFields(boolean detailRemark) {
        return """
                {"showCompanyName":true,"showServiceHotline":true,"showVoucherNo":true,"showVoucherDate":true,
                 "showPartyName":true,"showWorkerPhone":true,"showWorkerIdCard":false,"showWorkerTrade":false,
                 "showProject":true,"showAmount":true,"showAmountWords":true,"showSignBlock":true,
                 "showDetailNo":true,"showDetailDate":true,"showDetailAccount":true,"showDetailAmount":true,
                 "showDetailTarget":true,"showDetailCategory":false,"showDetailRemark":%s}
                """.formatted(detailRemark).trim();
    }

    /** 常用凭证模板库（企业按业务场景选用）。 */
    private static final List<Preset> VOUCHER_PRESETS = List.of(
            new Preset("P-SALARY", DocumentType.Payment, "工资发放凭证",
                    "发给个人工人：带身份证、工种与到账账户，工资凭证常用", paymentFields(true, false, false)),
            new Preset("P-LABOR", DocumentType.Payment, "班组结算凭证",
                    "发给劳务班组：不体现个人身份证与工种", paymentFields(false, false, false)),
            new Preset("P-SUPPLIER", DocumentType.Payment, "材料付款凭证",
                    "发给材料供应商：带项目、到账账户与款项类型", paymentFields(false, true, false)),
            new Preset("P-EXPENSE", DocumentType.Payment, "费用报销付款单",
                    "内部费用支出：精简字段，突出金额与签字", paymentFields(false, true, true)),
            new Preset("R-STANDARD", DocumentType.Receipt, "客户收款收据",
                    "对客户开具的收据：带项目、金额大写、到账账户与签字栏", receiptFields(false)),
            new Preset("R-DEPOSIT", DocumentType.Receipt, "定金收据",
                    "收取定金与首期款：精简明细，突出金额与大写", receiptFields(true))
    );

    /** 报价、材料、合同仍各一条基线。 */
    private static final Map<DocumentType, TemplateSpec> SPECS = Map.of(
            DocumentType.Quote, new TemplateSpec("装得清行业报价单基线", """
                    {"showCompanyName":true,"showCompanyAddress":true,"showServiceHotline":true,"showPropertyAddress":true,"showPropertyArea":true,"showLineBrand":true,"showLineSpec":true,"showTerms":true}
                    """.trim()),
            DocumentType.Material, new TemplateSpec("装得清行业材料计划单基线", """
                    {"showCompanyName":true,"showCompanyAddress":true,"showServiceHotline":true,"showProject":true,"showMaterial":true,"showQuantity":true,"showRequiredAt":true}
                    """.trim()),
            DocumentType.Contract, new TemplateSpec("装得清住宅施工合同打印稿基线", """
                    {"showLegalName":true,"showCustomer":true,"showSignedAmount":true,"showPaymentPlan":true,"showBlankSealPlaceholder":true}
                    """.trim())
    );

    /** 常用凭证模板库：收款单与付款单的全部预设。 */
    public List<DocumentTemplate> presets() {
        return VOUCHER_PRESETS.stream().map(preset -> copy(preset, null)).toList();
    }

    /** 指定单据类型的默认模板：凭证取该类型第一个预设，其余取行业基线。 */
    public DocumentTemplate template(DocumentType type) {
        return VOUCHER_PRESETS.stream()
                .filter(preset -> preset.type() == type)
                .findFirst()
                .map(preset -> copy(preset, null))
                .orElseGet(() -> {
                    TemplateSpec spec = SPECS.getOrDefault(type, new TemplateSpec("装得清行业单据基线", DEFAULT_FIELDS));
                    DocumentTemplate template = new DocumentTemplate();
                    template.setType(type);
                    template.setScope(DocumentTemplateScope.Platform);
                    template.setName(spec.name());
                    template.setVersion(VERSION);
                    template.setBaselineVersion(VERSION);
                    template.setFieldConfig(spec.fields());
                    template.setStyleConfig(DEFAULT_STYLE);
                    template.setStatus(DocumentStatus.Active);
                    return template;
                });
    }

    private DocumentTemplate copy(Preset preset, String unused) {
        DocumentTemplate template = new DocumentTemplate();
        template.setType(preset.type());
        template.setScope(DocumentTemplateScope.Platform);
        template.setName(preset.name());
        template.setVersion(VERSION);
        template.setBaselineVersion(VERSION);
        template.setFieldConfig(preset.fieldConfig());
        template.setStyleConfig(DEFAULT_STYLE);
        template.setStatus(DocumentStatus.Active);
        template.setPresetCode(preset.code());
        template.setPresetNote(preset.note());
        return template;
    }

    private record TemplateSpec(String name, String fields) {
    }
}
