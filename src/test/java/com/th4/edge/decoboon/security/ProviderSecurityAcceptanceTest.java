package com.th4.edge.decoboon.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.decoboon.common.configure.ProviderSecurityValidator;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** PP-07 Provider 安全和公开白名单独立验收。 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:provider-security;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "th4.edge.components.lock.enabled=false",
        "th4.edge.components.wechat.pay.enabled=false",
        "logging.level.org.hibernate.SQL=OFF"
})
@AutoConfigureMockMvc
class ProviderSecurityAcceptanceTest {

    private static final Set<String> EXPECTED_PUBLIC_ROUTES = Set.of(
            "auth/session/login",
            "auth/session/password-login",
            "auth/session/password-setup",
            "auth/session/register",
            // 本地开发业主会话（仅 dev + 显式开关；默认关闭），用于业主侧链路端到端验证。
            "auth/session/dev-owner-login",
            "identity/customer-invite/accept",
            "identity/customer-invite/info",
            "invite/invitation/accept",
            "invite/invitation/info",
            "invite/invitation/reject",
            "order/callback",
            "file/private/download",
            "platform/auth/login",
            "site/lead/submit",
            "site/lead/douyin/submit",
            // 2026-10-15 亲友只读通道 1 条（customer/shareview/info）：
            // 业主分享给家人/好友后对方没有账号，登录墙会让分享功能失效；
            // 安全由令牌承担（唯一 + 带时效 + 可撤销 + 服务端脱敏 + 不区分失效原因）。
            "customer/shareview/info"
    );

    @Autowired private ApplicationContext applicationContext;
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void shouldInventoryEveryProviderWithExplicitSecurityOrPublicWhitelist() {
        Map<String, Class<?>> routes = new LinkedHashMap<>();
        for (Provider<?, ?> provider : applicationContext.getBeansOfType(Provider.class).values()) {
            Class<?> target = AopUtils.getTargetClass(provider);
            if (!target.getName().startsWith("com.th4.edge.decoboon")
                    && !target.getName().startsWith("com.th4.edge.tenant")
                    && !target.getName().startsWith("com.th4.edge.platform")
                    && !target.getName().startsWith("com.th4.edge.site")) {
                continue;
            }
            Service service = AnnotationUtils.findAnnotation(target, Service.class);
            if (service == null || service.value().isBlank() || service.value().contains("{")) {
                continue;
            }
            assertThat(routes.put(service.value(), target))
                    .as("duplicate Provider route: " + service.value())
                    .isNull();
            assertThat(EXPECTED_PUBLIC_ROUTES.contains(service.value()) || provider instanceof Security)
                    .as("route without Security or whitelist: " + service.value())
                    .isTrue();
        }

        assertThat(ProviderSecurityValidator.PUBLIC_ROUTES).containsExactlyInAnyOrderElementsOf(EXPECTED_PUBLIC_ROUTES);
        assertThat(routes.keySet()).containsAll(EXPECTED_PUBLIC_ROUTES);
        // 2026-09-21 同步：在 263 基线上并入企业负责人项目经营聚合 1 条（project/project/overview）、模板基线库 1 条（document/baseline/list）、
        // 工人/凭证 7 条、常用项目 1 条（document/frequent-project/list）、主材选材 5 条（material/selection page/info/save/confirm/board）
        // 与阶段验收整改闭环 2 条（project/acceptance round/page、rectify/page）
        // => 总数为 277（public 12 + protected 265）
        // 2026-09-21 合同增减项 P0 并入 1 条（project/contract-change/info 变更单详情）
        // 2026-09-22 量房经营报告 P0 并入 1 条（statistic/boss/measure/report）
        // 2026-09-22 工作台读模型批次 2 并入 1 条（statistic/workbench/site/report）
        // => 总数为 282（public 12 + protected 270）
        // 2026-09-22 主材选材逐项操作链新增 2 条（material/selection/item/page、material/selection/item/info）
        // 2026-09-22 企业负责人增减项经营工作台并入 1 条（statistic/boss/contract-change/report）
        // => 总数为 288（public 12 + protected 276）；材料计划 P0 4 条 + 材料退货单 1 条
        // 2026-09-23 企业负责人 Web 端 P1 并入 2 条（finance/aging/report、statistic/organization/report）
        // 2026-09-23 材料计划 P2 并入 3 条（project/purchase/batch、material/category/page、material/category/save）
        // 2026-09-23 企业负责人 Web 端 P2 并入 2 条（statistic/boss/business/report、statistic/risk/report）
        // 2026-09-23 风险处置闭环 P2.1 并入 7 条（risk/item/{assign,claim,batch-assign,record,close,reopen,log/page}）
        // => 总数为 295（public 12 + protected 283）
        // => 总数为 293（public 12 + protected 281）
        // 2026-09-23 业主合同履约档案拆分 2 条（customer/contract/workbench、customer/contract-line/page）
        // 2026-09-23 业主验收轮次时间线拆分 1 条（customer/acceptance/round/page）
        // 2026-09-23 业主项目旅程 1 条、意见投诉 5 条
        // 2026-09-24 业主工地相册聚合 1 条（customer/project/album/page）
        // 2026-09-26 报价项目库批量导入 P0 并入 7 条（catalog/import-batch/{build,info,page,save,update}、
        // catalog/import-row/page、catalog/import-template/build）
        // 2026-09-26 新装企业在跑项目承接 P0 并入 13 条（carryover/batch/{info,save}、
        // carryover/project/{page,info,save,update,build}、carryover/confirmation/save、
        // carryover/adjustment/{save,page}、carryover/attachment/{save,page,delete}）。
        // 接管后剩余施工计划新增 5 条（stage/save、stage/delete、stage/reorder、plan/confirm、plan/change/page）。
        // => 总数为 340（public 12 + protected 328）；待 acceptance 复核。
        // 2026-09-26 报价项目库批量导入 W2 并入 1 条（catalog/item/delete）
        // 2026-09-26 A0.1 AI用量新增 2 条（ai/usage/report、ai/usage/page）
        // 2026-09-26 A1 AI经营参谋新增 1 条（ai/brief/report）
        // 2026-09-26 A0.2 AI账户摘要新增 1 条（ai/account/report）
        // => AI 相关基线共 4 条，干净基线为 349（public 12 + protected 337）。
        // 2026-09-27 企业入驻台账并入 4 条（platform/tenant/onboarding/{page,info,reveal,service/save}）。
        // 2026-09-27 恢复未发布能力新增 2 条（site/lead/submit、statistic/success/report）。
        // 2026-10-04 财务全量汇总新增 3 条（finance/ledger/report、finance/payable/report、document/voucher/report）。
        // 2026-10-05 手工记账分类与更正新增 6 条（finance/ledger-category tree/save/update/delete、finance/ledger/update、finance/ledger/correct）。
        // 2026-10-05 手工记账分类/更正 + Web 报价公式引擎（batchsave/build/import）合并后基线。
        // => 总数为 367（public 13 + protected 354）。
        // 2026-10-05 手工记账分类/更正 + Web 报价公式引擎（batchsave/build/import）合并后基线。
        // => 总数为 367（public 13 + protected 354）。
        // 2026-10-05 设计分项原子批量保存新增 1 条（design/items/save）。
        // 2026-10-06 手工流水作废新增 1 条（finance/ledger/delete）。
        // 2026-10-06 期初资金余额新增 2 条（finance/opening-balance/save、finance/opening-balance/page）。
        // 2026-10-06 剩余收付款计划新增 2 条（carryover/payment-plan/save、carryover/payment-plan/report）。
        // 2026-10-06 首月双轨对账新增 3 条（carryover/reconciliation/save、report、item/save）。
        // 2026-10-06 双轨确认关闭、导出与附件新增 5 条（reconciliation/update、build、attachment/save|page|delete）。
        // 2026-10-06 工资付款补录工人新增 1 条（finance/payment/update）。
        // 2026-10-06 同步当前 dev 基线实际 Provider 数：383（public 13 + protected 370）。
        // 2026-10-07 指标口径字典新增 1 条（statistic/metric/dictionary，protected）。
        // => 总数为 384（public 13 + protected 371）。
        // 2026-10-07 员工工资提成 / 企业单据模板版本 · 版式 · 凭证设计器 / 客户退款 在途功能整批上线，
        // 2026-10-07 薪酬方案与月度考勤删除新增 2 条（payroll/compensation/delete、attendance/summary/delete）。
        // protected 新增 34 条 => 418（public 13 + protected 405）。
        // 2026-10-08 设计师业务工作台新增 2 条（statistic/workbench/designer/queue/list、report）=> 420（public 13 + protected 407）。
        // 2026-10-08 工资单草稿删除新增 1 条（payroll/sheet/delete，protected）=> 421（public 13 + protected 408）。
        // 2026-10-08 客户休眠与恢复新增 3 条（customer/customer/dormant、resume、status-log/page）=> 424（public 13 + protected 411）。
        // 2026-10-08 客户休眠与恢复新增 3 条；2026-10-09 设计师待量房任务新增 1 条；
        // 2026-10-09 计划偏差（DEC-051）新增 3 条（project/plan-deviation/page、update，customer/plan-deviation/page，protected）。
        // 合并后当前 Provider 面板为 430（public 13 + protected 417）。
        // 2026-10-10 内容获客 P0-1 新增 7 条（content/asset page/info/save/status、content/authorization list/save/revoke，protected）。
        // => 当前 Provider 面板为 449（public 13 + protected 436）。
        // 2026-10-10 内容任务 P0-2 新增 7 条（content/template/list、content/task page/info/save/publish/claim/close，protected）。
        // => 当前 Provider 面板为 456（public 13 + protected 443）。
        // 2026-10-10 员工成片与发布回填 P0-3 新增 5 条（content/employee-content page/info/generate/review、content/publication/backfill，protected）。
        // => 当前 Provider 面板为 461（public 13 + protected 448）。
        // 2026-10-10 内容线索归因 P0-4 新增 3 条（content/lead-touch save/list、content/dashboard/report，protected）。
        // 2026-10-10 抖音公开留资新增 1 条（site/lead/douyin/submit，public）。
        // => 当前 Provider 面板为 465（public 14 + protected 451）。
        // 2026-10-11 本地开发业主会话新增 1 条（auth/session/dev-owner-login，public，仅 dev + 显式开关生效）。
        // => 当前 Provider 面板为 466（public 15 + protected 451）。
        // 2026-10-12 量房原始区域（DEC-066）新增 3 条（property/measure/region list/save/delete，protected）。
        // => 当前 Provider 面板为 469（public 15 + protected 454）。
        // 2026-10-13 门店维度补登记 3 条（user/store/status、user/store/update、statistic/store-comparison/report，protected）——
        // 该批路由此前未登记，导致基线自 2026-10-13 起失败（469 vs 实际 472）。
        // => 当前 Provider 面板为 472（public 15 + protected 457）。
        // 2026-10-13 增减项草稿预览（DEC-055）新增 1 条（project/contract-change/preview，protected）。
        // => 当前 Provider 面板为 473（public 15 + protected 458）。
        // 2026-10-13 增减项可变更来源与当前有效剩余（DEC-055/BR-041）新增 1 条（project/contract-change/range，protected）。
        // => 当前 Provider 面板为 474（public 15 + protected 459）。
        // 2026-10-15 亲友免登录只读通道新增 3 条：customer/sharelink/save、customer/sharelink/update（protected，业主本人）
        // 与 customer/shareview/info（**public**，凭带时效令牌免登录）。
        // => 当前 Provider 面板为 477（public 16 + protected 461）。
        // 2026-10-15 业主端开通方式改为「项目码扫码 + 手机号验证」（USER-DECISION-20261015-001）新增 2 条 protected：
        // project/owner-qrcode/build（项目码生成，员工 + 项目可见性）与
        // identity/customer-access/scan-enter（扫码开通业主授权，微信登录态 + 手机号一致）。
        // => 当前 Provider 面板为 479（public 16 + protected 463）。
        // 2026-10-15 主材下单作业单 P0-2 第一批（SOL-014 / OBJ-008 / BR-044）新增 7 条 protected：
        // material/template build、page、info、save、update、publish、delete
        // （模板配置权限：Boss / StoreManager；作业单与采购单为两张单，DEC-073）。
        // => 当前 Provider 面板为 486（public 16 + protected 470）。
        // 2026-10-15 主材下单作业单 P0-2 第二批新增 9 条 protected：
        // material/sheet page、info、save、update、delete、submit、measure、order、report
        // （作业单生命周期与下单编排；作业单与采购单为两张单，DEC-073）。
        // => 当前 Provider 面板为 495（public 16 + protected 479）。
        // 2026-10-15 主材采购结算 P3（BR-045 / DEC-072 / DEC-073）新增 4 条 protected：
        // material/settlement page、info、preview、confirm
        // （逐单结算与月结并存；互斥 + 逐单优先 + 月结兜底；结算价回填与暂估应付修正）。
        // => 当前 Provider 面板为 499（public 16 + protected 483）。
        // 2026-10-15 主材下单作业单 Excel 单据导出（P2）新增 1 条 protected：material/sheet/export
        // => 当前 Provider 面板为 500（public 16 + protected 484）。
        // 2026-10-15 App 建单需要品类清单新增 1 条 protected：material/sheet/enums
        // => 当前 Provider 面板为 501（public 16 + protected 485）。
        // 2026-10-15 结算撤销（用户裁决本轮做）新增 1 条 protected：material/settlement/cancel
        // => 当前 Provider 面板为 502（public 16 + protected 486）。
        // 2026-10-17 作业单独立取消新增 1 条 protected：material/sheet/cancel
        // 2026-10-17 B5 移除旧直采 project/purchase/save、project/purchase/batch；
        // 蓝图运行时/体检 Provider 已纳入安全面板。
        // => 2026-10-17 店长目标/门店利润新增 2 条（statistic/target/info、statistic/store/profit/report）。
        // 2026-10-20 设计方案分项纯排序新增 1 条 protected：design/items/reorder。
        // 2026-10-20 收款冲正新增 2 条 protected：finance/receipt/info、finance/receipt/reverse。
        // 2026-10-21 业主视角只读预览新增 1 条 protected：project/owner-preview/info。
        // 2026-10-21 收款/付款筛选汇总新增 2 条 protected：finance/receipt/report、finance/payment/report。
        // 2026-10-21 应收筛选汇总新增 1 条 protected：finance/receivable/report。
        // 2026-10-21 退款与提成汇总新增 2 条 protected：finance/refund/report、payroll/commission/report。
        // 2026-10-21 供应商往来分页新增 1 条 protected：finance/partner-ledger/page。
        // 2026-10-21 项目账本汇总新增 1 条 protected：finance/project-ledger/report。
        // DQ-X2 新增 1 条 protected：statistic/designer/measure/page。
        // => 当前 Provider 面板为 525（public 16 + protected 509）；套餐模板/复制发布能力新增 6 条。
        assertThat(routes).hasSize(525);
        assertThat(routes.keySet()).filteredOn(route -> !EXPECTED_PUBLIC_ROUTES.contains(route)).hasSize(509);
        assertThat(routes.keySet()).filteredOn(EXPECTED_PUBLIC_ROUTES::contains).hasSize(16);
    }

    @Test
    void protectedRepresentativeRoutesMustReturnUnauthorizedWithoutToken() throws Exception {
        assertUnauthorized("design/items/reorder");
        assertUnauthorized("project/contract-change/preview");
        assertUnauthorized("content/template/list");
        assertUnauthorized("content/lead-touch/save");
        assertUnauthorized("content/lead-touch/list");
        assertUnauthorized("content/dashboard/report");
        assertUnauthorized("content/employee-content/page");
        assertUnauthorized("content/employee-content/info");
        assertUnauthorized("content/employee-content/generate");
        assertUnauthorized("content/employee-content/review");
        assertUnauthorized("content/publication/backfill");
        assertUnauthorized("content/task/page");
        assertUnauthorized("content/task/info");
        assertUnauthorized("content/task/save");
        assertUnauthorized("content/task/publish");
        assertUnauthorized("content/task/claim");
        assertUnauthorized("content/task/close");
        assertUnauthorized("content/asset/page");
        assertUnauthorized("content/asset/info");
        assertUnauthorized("content/asset/save");
        assertUnauthorized("content/asset/status");
        assertUnauthorized("content/authorization/list");
        assertUnauthorized("content/authorization/save");
        assertUnauthorized("content/authorization/revoke");
        assertUnauthorized("project/project/page");
        assertUnauthorized("project/project/overview");
        assertUnauthorized("statistic/designer/measure/page");
        assertUnauthorized("finance/aging/report");
        assertUnauthorized("finance/ledger/report");
        assertUnauthorized("finance/payable/report");
        assertUnauthorized("finance/ledger-category/tree");
        assertUnauthorized("finance/ledger-category/save");
        assertUnauthorized("finance/ledger-category/update");
        assertUnauthorized("finance/ledger-category/delete");
        assertUnauthorized("finance/ledger/update");
        assertUnauthorized("finance/ledger/correct");
        assertUnauthorized("finance/ledger/delete");
        assertUnauthorized("finance/receipt/info");
        assertUnauthorized("finance/receipt/reverse");
        assertUnauthorized("project/owner-preview/info");
        assertUnauthorized("finance/receipt/report");
        assertUnauthorized("finance/payment/report");
        assertUnauthorized("finance/opening-balance/save");
        assertUnauthorized("finance/opening-balance/page");
        assertUnauthorized("carryover/payment-plan/save");
        assertUnauthorized("carryover/payment-plan/report");
        assertUnauthorized("carryover/reconciliation/save");
        assertUnauthorized("carryover/reconciliation/report");
        assertUnauthorized("carryover/reconciliation/item/save");
        assertUnauthorized("carryover/reconciliation/update");
        assertUnauthorized("carryover/reconciliation/build");
        assertUnauthorized("carryover/reconciliation/attachment/save");
        assertUnauthorized("carryover/reconciliation/attachment/page");
        assertUnauthorized("carryover/reconciliation/attachment/delete");
        assertUnauthorized("document/voucher/report");
        assertUnauthorized("statistic/organization/report");
        assertUnauthorized("statistic/boss/business/report");
        assertUnauthorized("statistic/risk/report");
        assertUnauthorized("risk/item/assign");
        assertUnauthorized("risk/item/close");
        assertUnauthorized("project/task/update");
        assertUnauthorized("identity/customer-invite/save");
        assertUnauthorized("identity/customer-access/scan-enter");
        assertUnauthorized("project/owner-qrcode/build");
        assertUnauthorized("customer/project/album/page");
        assertUnauthorized("file/private/grant");
        assertUnauthorized("finance/receivable/page");
        assertUnauthorized("finance/receivable/report");
        assertUnauthorized("finance/refund/report");
        assertUnauthorized("finance/partner-ledger/page");
        assertUnauthorized("finance/project-ledger/report");
        assertUnauthorized("payroll/commission/report");
        assertUnauthorized("labor/worker/page");
        assertUnauthorized("labor/worker/info");
        assertUnauthorized("labor/worker/save");
        assertUnauthorized("labor/worker/update");
        assertUnauthorized("document/voucher/build");
        assertUnauthorized("document/voucher/page");
        assertUnauthorized("document/baseline/list");
        assertUnauthorized("document/frequent-project/list");
        assertUnauthorized("material/selection/page");
        assertUnauthorized("material/selection/info");
        assertUnauthorized("material/selection/save");
        assertUnauthorized("material/selection/board");
        assertUnauthorized("material/selection/item/page");
        assertUnauthorized("material/selection/item/info");
        assertUnauthorized("material/sheet/cancel");
        assertUnauthorized("project/acceptance/round/page");
        assertUnauthorized("project/acceptance/rectify/page");
        assertUnauthorized("platform/tenant/page");
        // P0-b 客户标签 / 画像路由必须鉴权（未携带 token 一律 401）
        assertUnauthorized("customer/customer-tag/page");
        assertUnauthorized("customer/customer-tag/list");
        assertUnauthorized("customer/customer-tag/save");
        assertUnauthorized("customer/customer-tag/update");
        assertUnauthorized("customer/customer-tag/delete");
        assertUnauthorized("customer/customer-tag-link/save");
        assertUnauthorized("customer/customer-tag-link/delete");
        assertUnauthorized("customer/customer-portrait/save");
        assertUnauthorized("customer/customer/overview");
        assertUnauthorized("customer/customer-batch/build");
        assertUnauthorized("customer/customer-batch/save");
        assertUnauthorized("customer/customer/delete");
        assertUnauthorized("design/scheme/delete");
        assertUnauthorized("pack/section/save");
        assertUnauthorized("pack/line/save");
        assertUnauthorized("pack/template-item/save");
        assertUnauthorized("quote/line/save");
        assertUnauthorized("quote/line/delete");
        assertUnauthorized("statistic/onboarding/report");
        assertUnauthorized("ai/usage/report");
        assertUnauthorized("ai/brief/report");
        assertUnauthorized("ai/account/report");
        assertUnauthorized("ai/usage/page");
        assertUnauthorized("statistic/boss/workbench");
        assertUnauthorized("statistic/boss/design/report");
        assertUnauthorized("statistic/boss/quote/report");
        assertUnauthorized("statistic/boss/measure/report");
        assertUnauthorized("statistic/workbench/site/report");
        assertUnauthorized("customer/contract/workbench");
        assertUnauthorized("customer/contract-line/page");
        assertUnauthorized("customer/acceptance/round/page");
        assertUnauthorized("customer/project/journey/info");
        assertUnauthorized("customer/feedback/save");
        assertUnauthorized("customer/feedback/page");
        assertUnauthorized("customer/feedback/close");
        assertUnauthorized("project/feedback/page");
        assertUnauthorized("project/feedback/update");
        assertUnauthorized("document/quote/build");
        assertUnauthorized("document/reprint/save");
        assertUnauthorized("plan/template/page");
        assertUnauthorized("plan/payment-template/page");
        assertUnauthorized("plan/payment-template/save");
        assertUnauthorized("plan/payment-template/update");
        assertUnauthorized("plan/payment-template/publish");
        assertUnauthorized("plan/payment-template/disable");
        assertUnauthorized("plan/payment-template/default");
        assertUnauthorized("plan/construction-template/page");
        assertUnauthorized("plan/construction-template/save");
        assertUnauthorized("plan/construction-template/update");
        assertUnauthorized("plan/construction-template/publish");
        assertUnauthorized("plan/construction-template/disable");
        assertUnauthorized("plan/construction-template/default");
    }

    @Test
    void publicRepresentativeContractsMustNotBeBlockedByLoginSecurity() throws Exception {
        assertNotUnauthorized("auth/session/login", "{}");
        assertNotUnauthorized("auth/session/password-login", "{}");
        assertNotUnauthorized("auth/session/password-setup", "{}");
        assertNotUnauthorized("identity/customer-invite/info", "{}");
        assertNotUnauthorized("invite/invitation/info", "{}");
        assertNotUnauthorized("order/callback", "{}");
        assertNotUnauthorized("file/private/download", "{}");
        assertNotUnauthorized("site/lead/submit", "{}");
    }

    private void assertUnauthorized(String route) throws Exception {
        JsonNode body = post(route, "{}");
        int status = lastStatus;
        String code = body.path("errorCode").path("code").asText();
        assertThat(status == 401 || Set.of("401", "tokenIsUndefined", "tokenInvalid", "tokenExpired").contains(code))
                .as("route should require authentication: " + route + ", status=" + status + ", code=" + code)
                .isTrue();
    }

    private void assertNotUnauthorized(String route, String body) throws Exception {
        JsonNode response = post(route, body);
        assertThat(lastStatus).as("public route must not return HTTP 401: " + route).isNotEqualTo(401);
        assertThat(response.path("errorCode").path("code").asText())
                .as("public route must not return business 401: " + route)
                .isNotEqualTo("401");
    }

    private int lastStatus;

    private JsonNode post(String route, String body) throws Exception {
        MvcResult result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/" + route)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "acceptance-test")
                        .content(body))
                .andReturn();
        lastStatus = result.getResponse().getStatus();
        String content = result.getResponse().getContentAsString();
        return content == null || content.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(content);
    }
}
