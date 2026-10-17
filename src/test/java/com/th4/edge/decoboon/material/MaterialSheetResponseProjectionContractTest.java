package com.th4.edge.decoboon.material;

import com.th4.edge.decoboon.material.model.MaterialOrderSheet;
import com.th4.edge.decoboon.material.model.MaterialOrderSheetItem;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 作业单响应必须带项目、房屋与来源计划，否则 App/Web 无法追溯或编辑。 */
class MaterialSheetResponseProjectionContractTest {

    private static final List<Class<?>> PROVIDERS = List.of(
            com.th4.edge.decoboon.material.service.sheet.page.support.Page.class,
            com.th4.edge.decoboon.material.service.sheet.info.support.Info.class,
            com.th4.edge.decoboon.material.service.sheet.save.support.Save.class,
            com.th4.edge.decoboon.material.service.sheet.update.support.Update.class,
            com.th4.edge.decoboon.material.service.sheet.measure.support.Measure.class,
            com.th4.edge.decoboon.material.service.sheet.submit.support.Submit.class,
            com.th4.edge.decoboon.material.service.sheet.order.support.Order.class
    );

    @Test
    void everySheetProviderProjectsTraceabilityRelations() {
        for (Class<?> provider : PROVIDERS) {
            Properties properties = provider.getAnnotation(Properties.class);
            assertThat(properties).as(provider.getSimpleName() + " 缺少 @Properties").isNotNull();

            assertThat(propertiesOf(properties, MaterialOrderSheet.class))
                    .as(provider.getSimpleName() + " 作业单响应字段")
                    .contains("project", "property", "supplier", "items");

            assertThat(propertiesOf(properties, MaterialOrderSheetItem.class))
                    .as(provider.getSimpleName() + " 作业单行来源计划")
                    .contains("materialPlan");

            assertThat(Arrays.stream(properties.allow()).map(AllowProperties::model))
                    .as(provider.getSimpleName() + " 关联白名单")
                    .contains(Project.class, Property.class, MaterialPlan.class);
        }
    }

    private static List<String> propertiesOf(Properties properties, Class<?> model) {
        return Arrays.stream(properties.allow())
                .filter(item -> item.model().equals(model))
                .findFirst()
                .map(item -> Arrays.asList(item.properties()))
                .orElseGet(List::of);
    }
}
