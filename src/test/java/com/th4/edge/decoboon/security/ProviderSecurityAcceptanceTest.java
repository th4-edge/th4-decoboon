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
            "identity/customer-invite/accept",
            "identity/customer-invite/info",
            "invite/invitation/accept",
            "invite/invitation/info",
            "invite/invitation/reject",
            "order/callback",
            "file/private/download",
            "platform/auth/login",
            "site/lead/submit"
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
        // => 总数为 355（public 13 + protected 342）。
        assertThat(routes).hasSize(355);
        assertThat(routes.keySet()).filteredOn(route -> !EXPECTED_PUBLIC_ROUTES.contains(route)).hasSize(342);
        assertThat(routes.keySet()).filteredOn(EXPECTED_PUBLIC_ROUTES::contains).hasSize(13);
    }

    @Test
    void protectedRepresentativeRoutesMustReturnUnauthorizedWithoutToken() throws Exception {
        assertUnauthorized("project/project/page");
        assertUnauthorized("project/project/overview");
        assertUnauthorized("finance/aging/report");
        assertUnauthorized("statistic/organization/report");
        assertUnauthorized("statistic/boss/business/report");
        assertUnauthorized("statistic/risk/report");
        assertUnauthorized("risk/item/assign");
        assertUnauthorized("risk/item/close");
        assertUnauthorized("project/task/update");
        assertUnauthorized("identity/customer-invite/save");
        assertUnauthorized("customer/project/album/page");
        assertUnauthorized("file/private/grant");
        assertUnauthorized("finance/receivable/page");
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
        assertUnauthorized("material/selection/confirm");
        assertUnauthorized("material/selection/board");
        assertUnauthorized("material/selection/item/page");
        assertUnauthorized("material/selection/item/info");
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
