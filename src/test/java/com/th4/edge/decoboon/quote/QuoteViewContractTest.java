package com.th4.edge.decoboon.quote;

import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.configure.PricingMethod;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.quote.configure.QuoteDifferenceStatus;
import com.th4.edge.decoboon.quote.configure.QuoteDifferenceType;
import com.th4.edge.decoboon.quote.configure.QuoteLineRole;
import com.th4.edge.decoboon.quote.configure.QuoteViewAction;
import com.th4.edge.decoboon.quote.configure.QuoteViewMode;
import com.th4.edge.decoboon.quote.configure.QuoteViewScene;
import com.th4.edge.decoboon.quote.model.QuoteComponent;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.quote.model.QuotePrintContract;
import com.th4.edge.decoboon.quote.model.QuoteViewContract;
import jakarta.persistence.Transient;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuoteViewContractTest {

    @Test
    void sceneMatrixShouldFreezeItemVisibilityModesAndActions() {
        assertThat(QuoteViewScene.EDIT.exposesRawItems()).isTrue();
        assertThat(QuoteViewScene.EDIT.exposesCostToAuthorizedRole()).isTrue();
        assertThat(QuoteViewScene.EDIT.exposesInternalFormulaToAuthorizedRole()).isTrue();
        assertThat(QuoteViewScene.EDIT.exposesAcceptedBy()).isTrue();
        assertThat(QuoteViewScene.EDIT.getDefaultGroupMode()).isEqualTo(QuoteViewMode.REGION);
        assertThat(QuoteViewScene.EDIT.getActions()).containsExactlyInAnyOrder(
                QuoteViewAction.SAVE, QuoteViewAction.SUBMIT, QuoteViewAction.REVISE, QuoteViewAction.ADJUST);

        assertThat(QuoteViewScene.READ.exposesRawItems()).isTrue();
        assertThat(QuoteViewScene.READ.exposesVersionTrack()).isTrue();
        assertThat(QuoteViewScene.READ.getGroupModes()).containsExactlyInAnyOrder(QuoteViewMode.REGION, QuoteViewMode.TYPE);
        assertThat(QuoteViewScene.READ.getActions()).contains(
                QuoteViewAction.ACCEPT, QuoteViewAction.REJECT, QuoteViewAction.CREATE_CONTRACT, QuoteViewAction.PRINT);

        assertThat(QuoteViewScene.OWNER.exposesRawItems()).isFalse();
        assertThat(QuoteViewScene.OWNER.exposesCostToAuthorizedRole()).isFalse();
        assertThat(QuoteViewScene.OWNER.exposesInternalFormulaToAuthorizedRole()).isFalse();
        assertThat(QuoteViewScene.OWNER.exposesAcceptedAt()).isTrue();
        assertThat(QuoteViewScene.OWNER.exposesAcceptedBy()).isFalse();
        assertThat(QuoteViewScene.OWNER.getActions()).containsExactlyInAnyOrder(
                QuoteViewAction.VIEW, QuoteViewAction.EXPAND_SOURCE, QuoteViewAction.PRINT);

        assertThat(QuoteViewScene.PROJECT.getDefaultGroupMode()).isEqualTo(QuoteViewMode.REGION);
        assertThat(QuoteViewScene.PROJECT.exposesVersionTrack()).isTrue();
        assertThat(QuoteViewScene.CONTRACT.exposesCostSummaryToAuthorizedRole()).isTrue();
        assertThat(QuoteViewScene.CONTRACT.exposesCostToAuthorizedRole()).isFalse();
        assertThat(QuoteViewScene.PRINT.exposesAcceptedAt()).isTrue();
        assertThat(QuoteViewScene.PRINT.exposesCostToAuthorizedRole()).isFalse();
        assertThat(QuoteViewScene.PROJECT.getActions()).containsExactlyInAnyOrder(
                QuoteViewAction.VIEW, QuoteViewAction.NAVIGATE);

        assertThat(QuoteViewScene.CONTRACT.getGroupModes()).isEmpty();
        assertThat(QuoteViewScene.CONTRACT.getActions()).containsExactly(QuoteViewAction.NAVIGATE);
        assertThat(QuoteViewScene.SUMMARY.getGroupModes()).isEmpty();
        assertThat(QuoteViewScene.PRINT.getDefaultGroupMode()).isEqualTo(QuoteViewMode.REGION);
        assertThat(QuoteViewScene.PRINT.getActions()).containsExactlyInAnyOrder(
                QuoteViewAction.GENERATE, QuoteViewAction.REGENERATE, QuoteViewAction.OPEN_ARCHIVE);
    }

    @Test
    void typeMergeKeyShouldIgnoreRegionQuantityAmountCostSortAndFormatting() {
        QuoteLine living = line(" Livi  NG ROOM ", " 木地板 ", "A");
        living.setQuantity(new BigDecimal("12.000"));
        living.setAmount(new BigDecimal("1200.00"));
        living.setCost(new BigDecimal("800.00"));
        living.setSort(10);

        QuoteLine bedroom = line("LIVING ROOM", "木地板", "A");
        bedroom.setQuantity(new BigDecimal("8.000"));
        bedroom.setAmount(new BigDecimal("800.00"));
        bedroom.setCost(new BigDecimal("500.00"));
        bedroom.setSort(20);

        assertThat(QuoteViewContract.canonicalMergeKey(living))
                .isEqualTo(QuoteViewContract.canonicalMergeKey(bedroom));
        assertThat(QuoteViewContract.typeRowId(100L, 2, living))
                .isEqualTo(QuoteViewContract.typeRowId(100L, 2, bedroom))
                .isNotEqualTo(QuoteViewContract.typeRowId(100L, 3, bedroom));
    }

    @Test
    void everyCommercialDifferenceShouldBreakTypeMergeIdentity() {
        String expected = QuoteViewContract.canonicalMergeKey(line("客厅", "木地板", "A"));

        QuoteLine changedUnit = line("客厅", "木地板", "A");
        changedUnit.setUnit(Unit.Metre);
        assertThat(QuoteViewContract.canonicalMergeKey(changedUnit)).isNotEqualTo(expected);

        QuoteLine changedPrice = line("客厅", "木地板", "A");
        changedPrice.setPrice(new BigDecimal("129.00"));
        assertThat(QuoteViewContract.canonicalMergeKey(changedPrice)).isNotEqualTo(expected);

        QuoteLine changedStatus = line("客厅", "木地板", "A");
        changedStatus.setDifferenceStatus(QuoteDifferenceStatus.Confirmed);
        assertThat(QuoteViewContract.canonicalMergeKey(changedStatus)).isNotEqualTo(expected);

        QuoteLine changedDiscount = line("客厅", "木地板", "A");
        changedDiscount.setDiscountAmount(new BigDecimal("10.00"));
        assertThat(QuoteViewContract.canonicalMergeKey(changedDiscount)).isNotEqualTo(expected);

        QuoteLine changedCraft = line("客厅", "木地板", "A");
        changedCraft.setCraft("E0 级环保工艺");
        assertThat(QuoteViewContract.canonicalMergeKey(changedCraft)).isNotEqualTo(expected);

        QuoteLine changedMode = line("客厅", "木地板", "A");
        changedMode.setPricingMode(PricingMethod.FixedTotal);
        assertThat(QuoteViewContract.canonicalMergeKey(changedMode)).isNotEqualTo(expected);

        QuoteLine changedComponent = line("客厅", "木地板", "A");
        QuoteComponent component = new QuoteComponent();
        component.setName("辅料");
        component.setSaleAmount(new BigDecimal("3.00"));
        changedComponent.setComponents(List.of(component));
        assertThat(QuoteViewContract.canonicalMergeKey(changedComponent)).isNotEqualTo(expected);

        QuoteLine changedIncluded = line("客厅", "木地板", "A");
        changedIncluded.setIncluded(false);
        assertThat(QuoteViewContract.canonicalMergeKey(changedIncluded)).isNotEqualTo(expected);
    }

    @Test
    void delimiterCharactersShouldNeverCollapseDifferentMergeKeys() {
        QuoteLine left = line("客厅", "A|B", "C");
        QuoteLine right = line("主卧", "A", "B|C");

        assertThat(QuoteViewContract.canonicalMergeKey(left))
                .isNotEqualTo(QuoteViewContract.canonicalMergeKey(right));

        QuoteComponent componentA = component("辅料|A", "3.00", "2.00");
        QuoteComponent componentB = component("辅料|B", "3.00", "1.00");
        QuoteLine lineA = line("客厅", "木地板", "A");
        lineA.setComponents(List.of(componentA));
        QuoteLine lineB = line("主卧", "木地板", "A");
        lineB.setComponents(List.of(componentB));
        assertThat(QuoteViewContract.canonicalMergeKey(lineA))
                .isNotEqualTo(QuoteViewContract.canonicalMergeKey(lineB));
    }

    @Test
    void blankCategoryShouldFallbackToItemTypeDisplayName() {
        QuoteLine blank = line("客厅", "木地板", "A");
        blank.setCategory(null);
        QuoteLine explicit = line("主卧", "木地板", "A");
        explicit.setCategory(ItemType.Material.getName());

        assertThat(QuoteViewContract.canonicalMergeKey(blank))
                .isEqualTo(QuoteViewContract.canonicalMergeKey(explicit));
    }

    @Test
    void fixedTotalAndComponentSaleAmountShouldParticipateInMergeKey() {
        QuoteLine fixedA = line("客厅", "定制柜", "A");
        fixedA.setPricingMode(PricingMethod.FixedTotal);
        fixedA.setFixedTotalPrice(new BigDecimal("5000.00"));
        QuoteLine fixedB = line("主卧", "定制柜", "A");
        fixedB.setPricingMode(PricingMethod.FixedTotal);
        fixedB.setFixedTotalPrice(new BigDecimal("5200.00"));

        assertThat(QuoteViewContract.canonicalMergeKey(fixedA))
                .isNotEqualTo(QuoteViewContract.canonicalMergeKey(fixedB));

        QuoteComponent saleA = component("板材", "100.00", "60.00");
        QuoteComponent saleB = component("板材", "120.00", "60.00");
        QuoteLine componentA = line("客厅", "木地板", "A");
        componentA.setComponents(List.of(saleA));
        QuoteLine componentB = line("主卧", "木地板", "A");
        componentB.setComponents(List.of(saleB));

        assertThat(QuoteViewContract.canonicalMergeKey(componentA))
                .isNotEqualTo(QuoteViewContract.canonicalMergeKey(componentB));
    }

    @Test
    void sourceFieldsShouldRemainProjectionOnly() throws Exception {
        for (String fieldName : QuoteViewContract.TYPE_SOURCE_FIELDS) {
            Field field = QuoteLine.class.getDeclaredField(fieldName);
            assertThat(field.getAnnotation(Transient.class)).as(fieldName + " must be @Transient").isNotNull();
        }
        assertThat(QuoteViewContract.TYPE_SOURCE_FIELDS)
                .containsExactly("typeRowId", "sourceLineIds", "sourceRegionIds", "sourceRegionNames", "sourceCount");
        assertThat(QuoteViewContract.TYPE_MERGE_KEY_FIELDS)
                .containsExactly("catalogItem.itemId", "type", "category", "name", "brand", "spec", "unit",
                        "pricingMode", "price", "fixedTotalPrice", "role", "differenceType", "differenceStatus",
                        "included", "discountAmount", "craft", "components.saleSignature");
    }

    @Test
    void printContractShouldBindVersionGroupAndCustomerFieldBoundary() {
        assertThat(QuotePrintContract.DEFAULT_GROUP_MODE).isEqualTo(QuoteViewMode.REGION);
        assertThat(QuotePrintContract.REQUIRED_PARAMETERS).containsExactly("quoteId", "version", "groupMode");
        assertThat(QuotePrintContract.CUSTOMER_VISIBLE_FIELDS).contains("version", "groupMode", "amount", "acceptedAt");
        assertThat(QuotePrintContract.FORBIDDEN_FIELDS).contains("cost", "profit", "formula", "supplierPrice", "acceptedBy");
        assertThat(QuotePrintContract.fileSuffix(2, QuoteViewMode.REGION)).isEqualTo("_V2_按区域");
        assertThatThrownBy(() -> QuotePrintContract.requireParameters(1L, null, QuoteViewMode.REGION))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void designerInfoMustExposeAcceptanceRegistrarAndTime() {
        com.th4.edge.framework.data.jackson.annotation.Properties properties =
                com.th4.edge.decoboon.quote.service.quotes.designerinfo.support.Info.class
                        .getAnnotation(com.th4.edge.framework.data.jackson.annotation.Properties.class);
        assertThat(properties).isNotNull();
        assertThat(properties.allow()).anySatisfy(item -> {
            assertThat(item.model()).isEqualTo(com.th4.edge.decoboon.quote.model.Quote.class);
            assertThat(java.util.Arrays.asList(item.properties())).contains("acceptedAt", "acceptedBy");
        });
    }

    @Test
    void stableIdShouldRejectIncompleteIdentity() {
        QuoteLine line = line("客厅", "木地板", "A");
        assertThatThrownBy(() -> QuoteViewContract.typeRowId(null, 1, line))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> QuoteViewContract.typeRowId(1L, null, line))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private QuoteLine line(String spaceName, String name, String brand) {
        QuoteLine line = new QuoteLine();
        line.setType(ItemType.Material);
        line.setCategory("地面");
        line.setName(name);
        line.setBrand(brand);
        line.setSpec("1200×150×18mm");
        line.setUnit(Unit.Area);
        line.setPricingMode(PricingMethod.MaterialAndManual);
        line.setPrice(new BigDecimal("100.00"));
        line.setRole(QuoteLineRole.Base);
        line.setDifferenceType(QuoteDifferenceType.None);
        line.setDifferenceStatus(QuoteDifferenceStatus.None);
        line.setIncluded(true);
        line.setDiscountAmount(BigDecimal.ZERO);
        line.setCraft("常规工艺");
        Space space = new Space();
        space.setSpaceId((long) Math.abs(spaceName.hashCode()));
        space.setName(spaceName.trim());
        space.setSort((int) (space.getSpaceId() % 10));
        line.setSpace(space);
        CatalogItem catalog = new CatalogItem();
        catalog.setItemId(88L);
        line.setCatalogItem(catalog);
        return line;
    }


    private QuoteComponent component(String name, String saleAmount, String costAmount) {
        QuoteComponent component = new QuoteComponent();
        component.setName(name);
        component.setSaleAmount(new BigDecimal(saleAmount));
        component.setCostAmount(new BigDecimal(costAmount));
        component.setSort(1);
        return component;
    }
}
