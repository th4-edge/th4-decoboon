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
 * <p>报价、材料与合同各一条基线；收付款提供装企可复制的行业基础凭证模板库。
 * 基线不落企业表，企业复制后保存为自己的模板版本。</p>
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

    /** 付款类预设：个人身份、款项类型、备注、到账账户分别控制。 */
    private static String paymentFields(boolean workerIdentity, boolean detailCategory,
                                        boolean detailRemark, boolean detailTarget) {
        return """
                {"showCompanyName":true,"showServiceHotline":true,"showVoucherNo":true,"showVoucherDate":true,
                 "showPartyName":true,"showWorkerPhone":%s,"showWorkerIdCard":%s,"showWorkerTrade":%s,
                 "showProject":true,"showAmount":true,"showAmountWords":true,"showSignBlock":true,
                 "showDetailNo":true,"showDetailDate":true,"showDetailAccount":true,"showDetailAmount":true,
                 "showDetailTarget":%s,"showDetailCategory":%s,"showDetailRemark":%s}
                """.formatted(workerIdentity, workerIdentity, workerIdentity,
                detailTarget, detailCategory, detailRemark).trim();
    }

    /** 收款类预设：客户收据默认不体现个人身份证和工种。 */
    private static String receiptFields(boolean detailRemark) {
        return """
                {"showCompanyName":true,"showServiceHotline":true,"showVoucherNo":true,"showVoucherDate":true,
                 "showPartyName":true,"showWorkerPhone":true,"showWorkerIdCard":false,"showWorkerTrade":false,
                 "showProject":true,"showAmount":true,"showAmountWords":true,"showSignBlock":true,
                 "showDetailNo":true,"showDetailDate":true,"showDetailAccount":true,"showDetailAmount":true,
                 "showDetailTarget":true,"showDetailCategory":false,"showDetailRemark":%s}
                """.formatted(detailRemark).trim();
    }

    /** 行业基础凭证模板库。 */
    private static final List<Preset> VOUCHER_PRESETS = List.of(
            new Preset("P-SALARY", DocumentType.Payment, "工资发放凭证",
                    "发给个人工人：带身份证、工种与到账账户，工资凭证常用", paymentFields(true, false, false, true)),
            new Preset("P-LABOR", DocumentType.Payment, "班组结算凭证",
                    "发给劳务班组：不体现个人身份证与工种", paymentFields(false, false, false, true)),
            new Preset("P-SUPPLIER", DocumentType.Payment, "材料付款凭证",
                    "发给材料供应商：带项目、到账账户与款项类型", paymentFields(false, true, false, true)),
            new Preset("P-EXPENSE", DocumentType.Payment, "费用报销付款单",
                    "内部费用支出：字段精简，突出金额与签字", paymentFields(false, true, true, false)),
            new Preset("P-SUPPLIER-PREPAY", DocumentType.Payment, "材料预付款单",
                    "供应商预付款：显示来源、款项类型与到账账户", paymentFields(false, true, false, true)),
            new Preset("P-SUPPLIER-ARRIVAL", DocumentType.Payment, "到货款付款单",
                    "材料到场或入库后付款：显示来源与到账账户", paymentFields(false, true, false, true)),
            new Preset("P-SUPPLIER-ACCEPTANCE", DocumentType.Payment, "验收款付款单",
                    "材料验收合格后付款：显示来源、款项类型与到账账户", paymentFields(false, true, false, true)),
            new Preset("P-SUPPLIER-REFUND", DocumentType.Payment, "供应商退货/退款冲减单",
                    "退货、退款或冲减：保留来源、款项类型与备注", paymentFields(false, true, true, true)),
            new Preset("P-SUPPLIER-SETTLEMENT", DocumentType.Payment, "供应商结算/对账单",
                    "供应商结算与对账：显示来源、款项类型、备注和到账账户", paymentFields(false, true, true, true)),
            new Preset("P-LABOR-PROGRESS", DocumentType.Payment, "班组进度款单",
                    "按施工节点支付班组：显示款项类型与到账账户", paymentFields(false, true, false, true)),
            new Preset("P-LABOR-SETTLEMENT", DocumentType.Payment, "班组结算单",
                    "班组工程量结算：显示款项类型、备注和到账账户", paymentFields(false, true, true, true)),
            new Preset("P-WORKER-ADJUSTMENT", DocumentType.Payment, "工资补发/扣款单",
                    "工资补发或扣款：保留工人身份、备注与到账账户", paymentFields(true, false, true, true)),
            new Preset("P-LABOR-WARRANTY", DocumentType.Payment, "劳务质保金/返还单",
                    "劳务质保金预留或返还：显示来源、备注和到账账户", paymentFields(false, true, true, true)),
            new Preset("E-REIMBURSEMENT", DocumentType.Payment, "费用报销单",
                    "员工日常费用报销：显示款项类型和备注", paymentFields(false, true, true, false)),
            new Preset("E-TRAVEL", DocumentType.Payment, "差旅费报销单",
                    "差旅交通、住宿和补贴报销：显示款项类型和备注", paymentFields(false, true, true, false)),
            new Preset("E-LOAN", DocumentType.Payment, "借款/备用金单",
                    "员工借款或备用金：显示款项类型和备注", paymentFields(false, true, true, false)),
            new Preset("E-REPAYMENT", DocumentType.Payment, "还款/冲销单",
                    "借款归还或冲销：显示款项类型和备注", paymentFields(false, true, true, false)),
            new Preset("I-PAYMENT-REQUEST", DocumentType.Payment, "付款申请单",
                    "付款前申请：显示来源、款项类型、备注和到账账户", paymentFields(false, true, true, true)),
            new Preset("I-PAYMENT-APPROVAL", DocumentType.Payment, "付款审批单",
                    "内部付款审批：显示来源、款项类型、备注和到账账户", paymentFields(false, true, true, true)),
            new Preset("R-CUSTOMER-REFUND", DocumentType.Payment, "客户退款单",
                    "客户退款：显示来源、款项类型、备注和到账账户", paymentFields(false, true, true, true)),
            new Preset("F-MANUAL-LEDGER", DocumentType.Payment, "手工记账凭证",
                    "手工资金流水：显示来源、款项类型、备注和到账账户", paymentFields(false, true, true, true)),
            new Preset("F-CORRECTION", DocumentType.Payment, "更正/冲正单",
                    "资金流水更正或冲正：保留来源、款项类型和备注", paymentFields(false, true, true, true)),
            new Preset("R-STANDARD", DocumentType.Receipt, "客户收款收据",
                    "对客户开具的收据：带项目、金额大写、到账账户与签字栏", receiptFields(false)),
            new Preset("R-DEPOSIT", DocumentType.Receipt, "定金/首期款收据",
                    "收取定金与首期款：突出金额与大写", receiptFields(false)),
            new Preset("R-CUSTOMER-PROGRESS", DocumentType.Receipt, "客户进度款收款单",
                    "水电、泥木、油漆等节点收款：显示项目、金额和到账账户", receiptFields(false)),
            new Preset("R-CUSTOMER-FINAL", DocumentType.Receipt, "客户竣工/尾款收款单",
                    "竣工验收后收取尾款：显示项目、金额和到账账户", receiptFields(false)),
            new Preset("R-CUSTOMER-CHANGE", DocumentType.Receipt, "客户增项/变更款收款单",
                    "合同增项或变更收款：增加备注字段", receiptFields(true)),
            new Preset("R-CUSTOMER-WARRANTY", DocumentType.Receipt, "质保金收据",
                    "向客户收取质保金：显示项目、金额大写和到账账户", receiptFields(false))
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

    /** 常用凭证模板库：收付款全部预设。 */
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
                    template.setLayoutConfig(DocumentLayoutDefaults.JSON);
                    template.setStatus(DocumentStatus.Active);
                    return template;
                });
    }

    /** 按预设编码取只读基础模板。 */
    public DocumentTemplate preset(String code) {
        return VOUCHER_PRESETS.stream()
                .filter(preset -> preset.code().equals(code))
                .findFirst()
                .map(preset -> copy(preset, null))
                .orElse(null);
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
        template.setLayoutConfig(DocumentLayoutDefaults.JSON);
        template.setStatus(DocumentStatus.Active);
        template.setPresetCode(preset.code());
        template.setPresetNote(preset.note());
        return template;
    }

    private record TemplateSpec(String name, String fields) {
    }
}
