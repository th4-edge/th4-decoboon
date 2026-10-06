package com.th4.edge.decoboon.statistic.context;

import com.th4.edge.decoboon.statistic.model.MetricDefinition;
import com.th4.edge.decoboon.statistic.model.MetricDictionaryQuery;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/** 对外指标口径字典。所有对外金额/比率/数量必须在此登记后方可展示。 */
@Component
public class MetricDictionaryContext {

    private static final String UPDATED = "2026-10-07";

    private static final List<MetricDefinition> DEFINITIONS = List.of(
            def("signedAmount", "签约额", "本月新签合同的金额", "operation",
                    "按签约日归月的新签合同金额。",
                    "Σ businessOrigin=New 且 signedAt∈所选月份的合同金额（currentAmount 优先，为零回退 baselineAmount）",
                    "ProjectContract", "签约日归月", "企业全部门店；驾驶舱可按门店筛选",
                    "正常新签合同", "历史结转合同、未设置签约日的合同",
                    "无数据时显示 0，并标注「暂无新签」", "麦鸿 2026-10 签约额 53,000"),
            def("receiptAmount", "累计收款", "客户实际交到公司账上的钱", "finance",
                    "累计实际收到的客户款项，按收款单统计。",
                    "Σ Receipt.amount",
                    "Receipt 收款单", "累计；按 receivedAt 可切本月", "BOSS/财务全企业；店长本店只读",
                    "所有项目实际收款，含历史归档项目", "应收计划、未收款、手工登记的其他收入、退款冲销",
                    "无收款时显示 0", "麦鸿 118,900 = 非归档 80,380 + 归档 38,520"),
            def("monthReceiptAmount", "本月回款", "本月客户实际交到公司账上的钱", "finance",
                    "本月实际收到的客户款项，按收款单统计。",
                    "Σ 本月 Receipt.amount",
                    "Receipt 收款单", "按 receivedAt 归月", "BOSS/财务全企业；店长本店只读",
                    "本月所有实际收款，含历史归档项目", "上月及更早收款、应收计划、手工其他收入",
                    "本月无收款时显示 0", "麦鸿 2026-10 本月回款 28,130"),
            def("receivedAmount", "已收金额", "当前在管项目已经核销的应收", "finance",
                    "当前在管项目应收节点已经核销的收款合计，排除历史归档项目应收。",
                    "Σ Receivable.receivedAmount（排除历史归档项目）",
                    "Receivable 应收计划", "累计；按实际核销", "BOSS/财务全企业；店长本店只读",
                    "非归档项目应收核销", "历史归档项目应收、应收计划本身、手工其他收入",
                    "无应收时显示 0", "麦鸿 80,380 = 只算当前在管项目"),
            def("monthIncome", "资金收入", "公司账上实际收到的全部钱", "finance",
                    "本月资金台账收入合计，含客户收款与手工登记的其他收入。",
                    "Σ 本月 Ledger.direction=Income",
                    "Ledger 资金台账", "按 occurredAt 归月", "BOSS/财务全企业；店长本店只读",
                    "客户收款、手工其他收入、调整收入", "应收计划、未收款、应付/成本",
                    "无收入时显示 0", "麦鸿 120,900 = 收款 118,900 + 手工其他收入 2,000"),
            def("receivableAmount", "应收金额", "按合同节点应收回来的钱", "finance",
                    "当前在管项目应收计划节点金额合计。",
                    "Σ Receivable.amount（排除历史归档项目）",
                    "Receivable 应收计划", "按应收节点", "BOSS/财务全企业；店长本店只读",
                    "当前在管项目应收计划", "历史归档项目应收、已经收款的现金",
                    "无应收时显示 0", "麦鸿 应收金额 190,100"),
            def("outstandingAmount", "待收金额", "客户还欠多少", "finance",
                    "应收金额减去已收金额后的未收余额。",
                    "Σ max(Receivable.amount − receivedAmount, 0)",
                    "Receivable 应收计划", "实时", "BOSS/财务全企业；店长本店只读",
                    "未收应收余额", "已收清应收、历史归档项目应收",
                    "无待收时显示 0", "麦鸿 待收 109,720"),
            def("overdueAmount", "逾期应收", "已经过了约定日期还没收回的钱", "finance",
                    "到期未收或状态逾期的应收未收余额。",
                    "Σ 逾期应收节点的未收余额",
                    "Receivable + dueDate", "截至今天", "BOSS/财务全企业；店长本店只读",
                    "逾期的未收应收", "未到期应收、已收清应收、历史归档项目应收",
                    "无逾期时显示 0", "麦鸿 逾期 15,900（谢欣首期款）"),
            def("payableAmount", "应付金额", "按合同或采购应当付出去的钱", "finance",
                    "应付单金额合计。",
                    "Σ Payable.amount（排除历史归档项目）",
                    "Payable 应付单", "累计；按应付到期日", "BOSS/财务全企业；店长本店只读",
                    "材料、工资、项目、费用应付", "已付款、实际成本、历史归档项目应付",
                    "无应付时显示 0", "麦鸿 应付 0"),
            def("paidAmount", "已付金额", "已经实际付出去的钱", "finance",
                    "应付单已核销的付款合计。",
                    "Σ Payable.paidAmount（排除历史归档项目）",
                    "Payment 付款单 / Payable", "累计；按实际付款", "BOSS/财务全企业；店长本店只读",
                    "已核销付款", "未付款、实际成本、历史归档项目付款",
                    "无付款时显示 0", "麦鸿 已付 0"),
            def("unpaidAmount", "待付金额", "还欠供应商/工人/员工多少", "finance",
                    "应付金额减去已付金额后的未付余额。",
                    "Σ max(Payable.amount − paidAmount, 0)",
                    "Payable 应付单", "实时", "BOSS/财务全企业；店长本店只读",
                    "未付应付余额", "已付清应付、历史归档项目应付",
                    "无待付时显示 0", "麦鸿 待付 0"),
            def("monthExpense", "资金支出", "公司账上实际花出去的钱", "finance",
                    "本月资金台账支出合计，含付款与手工登记支出。",
                    "Σ 本月 Ledger.direction=Expense",
                    "Ledger 资金台账", "按 occurredAt 归月", "BOSS/财务全企业；店长本店只读",
                    "付款、手工公司费用、项目零星支出", "应付计划、未付款、成本本身",
                    "无支出时显示 0", "麦鸿 2026-10 支出 540"),
            def("monthNetCashFlowAmount", "现金流", "本月公司账上进出的净额", "finance",
                    "本月资金收入减去本月资金支出。",
                    "monthIncome − monthExpense",
                    "Ledger 资金台账", "本月", "BOSS/财务全企业；店长本店只读",
                    "本月实际资金进出", "利润、毛利、应收应付计划",
                    "无流水时显示 0", "麦鸿 27,590 = 28,130 − 540"),
            def("cashBalance", "账户余额", "公司账上现在有多少钱", "finance",
                    "期初余额加累计资金收入减累计资金支出。",
                    "openingBalanceAmount + Σ Income − Σ Expense",
                    "OpeningBalance + Ledger", "累计；截至今天", "BOSS/财务全企业；店长本店只读",
                    "期初余额、全部非作废资金流水", "利润、应收应付计划、未发生收付",
                    "期初未承接时显示「期初未承接」，余额为过程参考", "麦鸿 26,582.64，但期初未承接"),
            def("openingBalanceAmount", "期初余额", "开始用装得清时账上已有的钱", "finance",
                    "入驻或月结起始时点的资金余额，不计入收支和利润。",
                    "OpeningBalance 最新 as_of_date 合计",
                    "OpeningBalance", "时点", "BOSS/财务全企业；店长本店只读",
                    "入驻承接的资金余额", "本期收入支出、利润",
                    "无记录时显示「期初未承接」", "麦鸿暂无期初记录"),
            def("estimatedCost", "预计成本", "按报价和合同快照预计要花多少", "finance",
                    "按合同行冻结成本或报价快照形成的预计成本。",
                    "Σ ContractLine.cost / QuoteLine.cost",
                    "ContractLine / QuoteLine", "签约或报价时点", "BOSS/店长/财务",
                    "合同行冻结成本、报价成本快照", "实际付款、最终成本",
                    "成本价缺失时标记成本未完整", "麦鸿新签项目预计成本 62,291.52"),
            def("actualCost", "实际成本", "项目实际已经发生的成本", "finance",
                    "材料到货入库、人工、项目费用与工资确认等实际发生的项目成本。",
                    "max(ProjectCostContext.actualCost, 项目应付) + 手工项目零星成本",
                    "ContractLine / QuoteLine / GoodsReceipt / Payable / Ledger", "累计；按实际发生", "BOSS/店长/财务",
                    "实际发生成本、应付成本取高、手工项目零星成本", "付款本身、领料损耗重复累计",
                    "成本价=售价或应计成本缺失时 costComplete=false",
                    "麦鸿成本价=售价，项目毛利不展示"),
            def("grossProfit", "项目毛利", "项目收入减去项目成本后的经营结果", "finance",
                    "项目收入减去项目成本；成本不完整时不返回数字。",
                    "项目收入 − 项目成本；costComplete=false 时为 null",
                    "ProjectCostContext", "项目全周期；缺失时 null", "BOSS/店长/财务",
                    "合同当前金额、关联项目其他收入、实际项目成本", "公司费用、往来、净利润、现金流",
                    "costComplete=false 时显示「成本未完整」，禁止按 0 计算",
                    "麦鸿新签项目因成本价=售价显示成本未完整"),
            def("grossMargin", "项目毛利率", "项目每 100 元收入赚多少", "finance",
                    "项目毛利除以项目收入；成本不完整时为 null。",
                    "项目毛利 ÷ 项目收入 × 100%；costComplete=false 时为 null",
                    "ProjectCostContext", "项目全周期；缺失时 null", "BOSS/店长/财务",
                    "成本完整时的项目毛利与收入", "成本未完整时的任何数值、净利率",
                    "costComplete=false 时显示「成本未完整」", "成本未完整时不展示"),
            def("activeProjectCount", "在建项目", "当前还在施工/准备/验收的项目数", "operation",
                    "签约起始日不晚于统计期末，且项目未结束的在建项目数。",
                    "count(在建项目) = Preparing + Ongoing + Accepting",
                    "Project", "截至统计期末", "企业全部门店；驾驶舱可按门店筛选",
                    "Preparing、Ongoing、Accepting", "Finished、Settled、已归档项目",
                    "无在建项目时显示 0", "麦鸿 2026-10 在建 4"),
            def("achievementRate", "目标达成率", "实际值完成目标的百分比", "period",
                    "当前值除以目标值的百分比；未设目标时不显示达成率。",
                    "当前值 ÷ 目标值 × 100%",
                    "BusinessTarget / DashboardMetric", "所选月份", "BOSS",
                    "已设置目标的指标", "未设目标、目标为 0 的情况",
                    "未设目标时显示「未设目标」", "麦鸿 2026-10 未设目标"),
            def("monthOverMonthRate", "环比", "和上个月比变化多少", "period",
                    "本月值与上月值的环比变化；基期为 0 或缺失时不显示百分比。",
                    "(本月值 − 上月值) ÷ |上月值| × 100%",
                    "DashboardMetric", "本月 vs 上月", "BOSS",
                    "上月有非零基期的指标", "上月为 0、上月成本不完整",
                    "暂无基期时显示「暂无基期」，不显示 0%", "麦鸿签约额 9 月为 0，显示暂无基期"),
            def("yearOverYearRate", "同比", "和去年同月比变化多少", "period",
                    "本月值与去年同月值的同比变化；基期为 0 或缺失时不显示百分比。",
                    "(本月值 − 去年同月值) ÷ |去年同月值| × 100%",
                    "DashboardMetric", "本月 vs 去年同月", "BOSS",
                    "去年同月有非零基期的指标", "去年同月为 0、成本不完整",
                    "暂无基期时显示「暂无基期」，不显示 0%", "麦鸿去年同月无数据")
    );

    public List<MetricDefinition> definitions() {
        return DEFINITIONS;
    }

    public List<MetricDefinition> filter(MetricDictionaryQuery query) {
        if (query == null) {
            return DEFINITIONS;
        }
        String category = query.getCategory() == null ? null : query.getCategory().trim().toLowerCase(Locale.ROOT);
        List<String> keys = query.getKeys();
        return DEFINITIONS.stream()
                .filter(item -> category == null || category.isBlank() || category.equals(item.getCategory()))
                .filter(item -> keys == null || keys.isEmpty() || keys.contains(item.getKey()))
                .toList();
    }

    private static MetricDefinition def(String key, String name, String userLabel, String category,
                                        String definition, String formula, String source, String timeScope,
                                        String dataScope, String includes, String excludes,
                                        String missingValue, String example) {
        MetricDefinition item = new MetricDefinition();
        item.setKey(key);
        item.setName(name);
        item.setUserLabel(userLabel);
        item.setCategory(category);
        item.setDefinition(definition);
        item.setFormula(formula);
        item.setSource(source);
        item.setTimeScope(timeScope);
        item.setDataScope(dataScope);
        item.setIncludes(includes);
        item.setExcludes(excludes);
        item.setMissingValue(missingValue);
        item.setExample(example);
        item.setUpdatedAt(UPDATED);
        item.setStatus("verified");
        return item;
    }
}
