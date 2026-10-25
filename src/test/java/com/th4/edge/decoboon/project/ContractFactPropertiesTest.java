package com.th4.edge.decoboon.project;

import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ContractFactPropertiesTest {

    @Test
    void contractRootResponsesMustNotExposeCostFieldsToDesigners() {
        for (Class<?> provider : List.of(
                com.th4.edge.decoboon.project.service.contract.workbench.support.Workbench.class,
                com.th4.edge.decoboon.project.service.contract.info.support.Info.class,
                com.th4.edge.decoboon.project.service.contract.page.support.Page.class)) {
            assertThat(allowed(provider, ProjectContract.class))
                    .doesNotContain("signedCost", "costComplete");
        }
    }

    private List<String> allowed(Class<?> provider, Class<?> model) {
        Properties properties = provider.getAnnotation(Properties.class);
        assertThat(properties).as(provider.getSimpleName()).isNotNull();
        return Arrays.stream(properties.allow())
                .filter(allow -> allow.model().equals(model))
                .flatMap(allow -> Arrays.stream(allow.properties()))
                .toList();
    }
}
