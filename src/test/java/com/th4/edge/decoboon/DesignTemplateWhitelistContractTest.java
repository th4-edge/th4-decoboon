package com.th4.edge.decoboon;

import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.design.model.DesignItem;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplate;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplateItem;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplateRoom;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 设计模版返回字段白名单契约。
 *
 * <p>保障两件事：① 设计师侧响应绝不返回标准成本（`BR-003`/`BR-037`）；
 * ② 模版区域不顺着归属链带出企业/门店/团队结构。</p>
 */
class DesignTemplateWhitelistContractTest {

    private List<String> propertiesOf(Class<?> provider, Class<?> model) {
        Properties properties = provider.getAnnotation(Properties.class);
        assertThat(properties).as(provider.getSimpleName() + " 必须声明 @Properties 白名单").isNotNull();
        return Arrays.stream(properties.allow())
                .filter(item -> item.model().equals(model))
                .map(AllowProperties::properties)
                .flatMap(Arrays::stream)
                .toList();
    }

    @Test
    void templateInfoAndItemSaveMustNotExposeCatalogCost() {
        for (Class<?> provider : List.of(
                com.th4.edge.decoboon.designtemplate.service.templates.info.support.Info.class,
                com.th4.edge.decoboon.designtemplate.service.templates.item.save.support.Save.class,
                com.th4.edge.decoboon.designtemplate.service.templates.item.delete.support.Delete.class)) {
            List<String> catalog = propertiesOf(provider, CatalogItem.class);
            assertThat(catalog).as(provider.getSimpleName() + " 必须白名单 CatalogItem").isNotEmpty();
            assertThat(catalog).doesNotContain("costPrice", "purchasePrice");
            assertThat(catalog).contains("itemId", "name", "unit", "pricingMode");
        }
    }

    @Test
    void templateProvidersNeverExposePriceOrCost() {
        for (Class<?> provider : List.of(
                com.th4.edge.decoboon.designtemplate.service.templates.info.support.Info.class,
                com.th4.edge.decoboon.designtemplate.service.templates.copy.support.Copy.class,
                com.th4.edge.decoboon.designtemplate.service.templates.version.build.support.Build.class,
                com.th4.edge.decoboon.designtemplate.service.templates.items.batchsave.support.BatchSave.class,
                com.th4.edge.decoboon.designtemplate.service.templates.item.save.support.Save.class,
                com.th4.edge.decoboon.designtemplate.service.templates.item.delete.support.Delete.class)) {
            List<String> catalog = propertiesOf(provider, CatalogItem.class);
            assertThat(catalog).as(provider.getSimpleName() + " 的 catalogItem 白名单").isNotEmpty();
            assertThat(catalog).doesNotContain(
                    "materialPrice", "manualPrice", "fixedTotalPrice", "costPrice", "purchasePrice");
            List<String> item = propertiesOf(provider, DesignTemplateItem.class);
            assertThat(item).as(provider.getSimpleName() + " 的模版分项白名单")
                    .doesNotContain("materialPrice", "manualPrice", "fixedTotalPrice", "costPrice");
        }
    }

    @Test
    void instantiateMustNotExposeDesignItemCost() {
        List<String> designItem = propertiesOf(
                com.th4.edge.decoboon.designtemplate.service.templates.instantiate.support.Instantiate.class,
                DesignItem.class);
        assertThat(designItem).isNotEmpty();
        assertThat(designItem).doesNotContain("costPrice", "cost");
        assertThat(designItem).contains("quantity", "amount", "materialPrice", "manualPrice");
    }

    @Test
    void templateItemWhitelistNeverReturnsNameUnitOrPrice() {
        // 分项的名称/单位/计价方式/价格都来自 catalogItem，模版分项自身不得再暴露这些字段
        for (Class<?> provider : List.of(
                com.th4.edge.decoboon.designtemplate.service.templates.info.support.Info.class,
                com.th4.edge.decoboon.designtemplate.service.templates.item.save.support.Save.class,
                com.th4.edge.decoboon.designtemplate.service.templates.item.delete.support.Delete.class)) {
            List<String> item = propertiesOf(provider, DesignTemplateItem.class);
            assertThat(item).doesNotContain("name", "unit", "pricingMode", "materialPrice", "manualPrice");
        }
    }

    @Test
    void templateRoomNeverExposesOwnershipChain() {
        for (Class<?> provider : List.of(
                com.th4.edge.decoboon.designtemplate.service.templates.info.support.Info.class,
                com.th4.edge.decoboon.designtemplate.service.templates.room.save.support.Save.class,
                com.th4.edge.decoboon.designtemplate.service.templates.room.delete.support.Delete.class)) {
            List<String> room = propertiesOf(provider, DesignTemplateRoom.class);
            assertThat(room).doesNotContain("company", "version", "store", "team", "owner");
            assertThat(room).contains("roomKey", "length", "width", "height");
        }
    }

    @Test
    void templatePageDoesNotAssembleRoomsOrItems() {
        List<String> template = propertiesOf(
                com.th4.edge.decoboon.designtemplate.service.templates.page.support.Page.class,
                DesignTemplate.class);
        assertThat(template).doesNotContain("rooms", "items");
        assertThat(template).contains("currentVersion");
    }
}
