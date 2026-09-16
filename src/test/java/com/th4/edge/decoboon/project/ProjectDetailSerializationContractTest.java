package com.th4.edge.decoboon.project;

import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.service.stages.info.support.Info;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/** 阶段/任务详情响应白名单回归：禁止带出 property 的深层门店、团队、负责人循环关系。 */
class ProjectDetailSerializationContractTest {

    @Test
    void stageAndTaskInfoMustNotSerializeNestedPropertyGraph() {
        assertProjectWhitelistDoesNotContainProperty(Info.class);
        assertProjectWhitelistDoesNotContainProperty(
                com.th4.edge.decoboon.project.service.tasks.info.support.Info.class);
    }

    private void assertProjectWhitelistDoesNotContainProperty(Class<?> provider) {
        Properties properties = provider.getAnnotation(Properties.class);
        assertThat(properties).as(provider.getSimpleName() + " 必须声明 @Properties").isNotNull();
        AllowProperties projectAllow = Arrays.stream(properties.allow())
                .filter(item -> item.model().equals(Project.class))
                .findFirst()
                .orElseThrow();
        assertThat(Arrays.asList(projectAllow.properties()))
                .as(provider.getSimpleName() + " 的项目摘要不得包含 property")
                .doesNotContain("property");
    }
}
