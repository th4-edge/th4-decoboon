package com.th4.edge.decoboon.statistic;

import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 角色经营报告经项目->房屋关联时必须开放房屋摘要，避免 JSON 截断后页面伪 0。 */
class StatisticReportPropertyWhitelistContractTest {

    private static final List<String> PROPERTY_SUMMARY_FIELDS = List.of(
            "propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout", "status");

    @Test
    void roleReportsMustWhitelistNestedPropertySummary() {
        List<Class<?>> providers = List.of(
                com.th4.edge.decoboon.statistic.service.store.report.support.Report.class,
                com.th4.edge.decoboon.statistic.service.teamleader.report.support.Report.class,
                com.th4.edge.decoboon.statistic.service.designer.report.support.Report.class,
                com.th4.edge.decoboon.statistic.service.marketer.report.support.Report.class,
                com.th4.edge.decoboon.statistic.service.alert.report.support.Report.class,
                com.th4.edge.decoboon.statistic.service.boss.workbench.support.Workbench.class);

        assertThat(providers).allSatisfy(provider -> {
            Properties properties = provider.getAnnotation(Properties.class);
            assertThat(properties).as(provider.getName()).isNotNull();
            assertThat(properties.allow()).anySatisfy(item -> {
                assertThat(item.model()).isEqualTo(Property.class);
                assertThat(Arrays.asList(item.properties())).containsAll(PROPERTY_SUMMARY_FIELDS);
            });
        });
    }
}
