package com.th4.edge.decoboon.statistic;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.project.model.Acceptance;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.statistic.service.pm.report.support.Report;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 履约驾驶舱响应白名单回归（DEC-036 / rules §5）。
 *
 * <p>pm/report 曾因嵌套复用 Project 全量白名单导致响应体 136KB（materialRisks 占 78.6%）。
 * 收窄后只保留 App 端 normalize 与实际渲染依赖的轻量字段；本测试防止重嵌套回归。</p>
 */
class PmReportSerializationContractTest {

    private static List<String> allowOf(Class<?> model) {
        Properties properties = Report.class.getAnnotation(Properties.class);
        assertThat(properties).isNotNull();
        return Arrays.stream(properties.allow())
                .filter(item -> item.model().equals(model))
                .findFirst()
                .map(item -> Arrays.asList(item.properties()))
                .orElseThrow(() -> new AssertionError("缺少白名单: " + model.getSimpleName()));
    }

    @Test
    void projectSummaryMustNotCarryHeavyNesting() {
        List<String> allow = allowOf(Project.class);
        assertThat(allow).doesNotContain("property", "quote", "designer", "manager");
        assertThat(allow).contains("projectId", "name", "customer", "siteAddr", "contractAmount",
                "status", "progress", "planEndDate");
    }

    @Test
    void materialRiskMustNotCarryPropertyOrTaskTree() {
        List<String> allow = allowOf(MaterialPlan.class);
        assertThat(allow).doesNotContain("property", "task");
        assertThat(allow).contains("planId", "project", "quantity", "unit", "status", "requiredAt");
    }

    @Test
    void customerAndQuoteMustNotCarryEmployeeCirculars() {
        assertThat(allowOf(Customer.class)).doesNotContain("owner", "designer");
        assertThat(allowOf(Quote.class)).doesNotContain("createdBy");
    }

    @Test
    void acceptanceListMustNotCarryEvidencePayloads() {
        assertThat(allowOf(Acceptance.class)).doesNotContain("issues", "evidenceUrls");
    }
}
