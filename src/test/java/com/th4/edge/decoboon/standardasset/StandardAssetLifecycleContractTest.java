package com.th4.edge.decoboon.standardasset;

import com.th4.edge.decoboon.standardasset.configure.StandardAssetDefaultPolicy;
import com.th4.edge.decoboon.standardasset.configure.StandardAssetSourceMode;
import com.th4.edge.decoboon.standardasset.configure.StandardAssetType;
import com.th4.edge.decoboon.standardasset.configure.StandardAssetVersionMode;
import com.th4.edge.decoboon.standardasset.context.StandardAssetLifecycleContext;
import com.th4.edge.decoboon.standardasset.model.StandardAssetLifecycleContract;
import com.th4.edge.decoboon.standardasset.model.StandardAssetStatusContract;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Service;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class StandardAssetLifecycleContractTest {

    private final List<StandardAssetLifecycleContract> contracts = new StandardAssetLifecycleContext().contracts();

    @Test
    void exposesExactlyFiveStandardAssets() {
        assertThat(contracts).extracting(StandardAssetLifecycleContract::getAssetType)
                .containsExactly(StandardAssetType.CatalogItem, StandardAssetType.PackageTemplate,
                        StandardAssetType.PaymentPlanTemplate, StandardAssetType.ConstructionPlanTemplate,
                        StandardAssetType.DocumentTemplate);
    }

    @Test
    void usesSemanticStatusSetsInsteadOfOneSharedStateMachine() {
        Map<StandardAssetType, List<String>> statuses = contracts.stream().collect(Collectors.toMap(
                StandardAssetLifecycleContract::getAssetType,
                item -> item.getStatuses().stream().map(StandardAssetStatusContract::getKey).toList()));

        assertThat(statuses.get(StandardAssetType.CatalogItem)).containsExactly("Active", "Inactive");
        assertThat(statuses.get(StandardAssetType.PackageTemplate)).containsExactly("Draft", "Published", "Disabled");
        assertThat(statuses.get(StandardAssetType.PaymentPlanTemplate)).containsExactly("Draft", "Enabled", "Disabled");
        assertThat(statuses.get(StandardAssetType.ConstructionPlanTemplate)).containsExactly("Draft", "Enabled", "Disabled");
        assertThat(statuses.get(StandardAssetType.DocumentTemplate)).containsExactly("Draft", "Published", "Disabled");
        assertThat(statuses.values().stream().distinct().count()).isGreaterThan(1);
    }

    @Test
    void everyPublishedOrEnabledStatusAllowsNewBusinessAndHistoryStaysReadable() {
        for (StandardAssetLifecycleContract contract : contracts) {
            assertThat(contract.getStatuses()).allSatisfy(status -> assertThat(status.getHistoryReadable()).isTrue());
            assertThat(contract.getStatuses().stream()
                    .filter(status -> status.getKey().equals("Published") || status.getKey().equals("Enabled")))
                    .allSatisfy(status -> assertThat(status.getNewBusinessEligible()).isTrue());
            assertThat(contract.getStatuses().stream()
                    .filter(status -> status.getKey().equals("Draft")))
                    .allSatisfy(status -> assertThat(status.getNewBusinessEligible()).isFalse());
        }
    }

    @Test
    void catalogRemainsEditableWhenInactiveButCannotBeUsedForNewBusiness() {
        StandardAssetStatusContract inactive = contract(StandardAssetType.CatalogItem).getStatuses().stream()
                .filter(status -> status.getKey().equals("Inactive")).findFirst().orElseThrow();
        assertThat(inactive.getEditable()).isTrue();
        assertThat(inactive.getNewBusinessEligible()).isFalse();
    }

    @Test
    void distinguishesRevisionAssetsAndImmutableVersionAssets() {
        assertThat(contract(StandardAssetType.CatalogItem).getVersionMode()).isEqualTo(StandardAssetVersionMode.Revision);
        assertThat(contracts.stream().filter(item -> item.getAssetType() != StandardAssetType.CatalogItem))
                .allSatisfy(item -> assertThat(item.getVersionMode()).isEqualTo(StandardAssetVersionMode.ImmutableVersion));
    }

    @Test
    void onlyPaymentConstructionAndDocumentTemplatesCanCopyPlatformBaselines() {
        assertThat(contract(StandardAssetType.CatalogItem).getSourceMode()).isEqualTo(StandardAssetSourceMode.CompanyOnly);
        assertThat(contract(StandardAssetType.PackageTemplate).getSourceMode()).isEqualTo(StandardAssetSourceMode.CompanyOnly);
        assertThat(contract(StandardAssetType.PaymentPlanTemplate).getSourceMode()).isEqualTo(StandardAssetSourceMode.BaselineCopy);
        assertThat(contract(StandardAssetType.ConstructionPlanTemplate).getSourceMode()).isEqualTo(StandardAssetSourceMode.BaselineCopy);
        assertThat(contract(StandardAssetType.DocumentTemplate).getSourceMode()).isEqualTo(StandardAssetSourceMode.BaselineCopy);
    }

    @Test
    void leavesPackageDefaultDeferredAndUsesSingleOptionalDefaultForPlans() {
        assertThat(contract(StandardAssetType.CatalogItem).getDefaultPolicy()).isEqualTo(StandardAssetDefaultPolicy.NotApplicable);
        assertThat(contract(StandardAssetType.PackageTemplate).getDefaultPolicy()).isEqualTo(StandardAssetDefaultPolicy.Deferred);
        assertThat(contract(StandardAssetType.PaymentPlanTemplate).getDefaultPolicy()).isEqualTo(StandardAssetDefaultPolicy.OptionalSingle);
        assertThat(contract(StandardAssetType.ConstructionPlanTemplate).getDefaultPolicy()).isEqualTo(StandardAssetDefaultPolicy.OptionalSingle);
        assertThat(contract(StandardAssetType.DocumentTemplate).getDefaultPolicy()).isEqualTo(StandardAssetDefaultPolicy.NotApplicable);
    }

    @Test
    void everyContractCarriesReferenceAndSnapshotFields() {
        assertThat(contracts).allSatisfy(contract -> {
            assertThat(contract.getReferenceFields()).isNotEmpty();
            assertThat(contract.getSnapshotFields()).isNotEmpty();
            assertThat(contract.getHistoryPolicy()).contains("不从当前资产重新计算");
        });
    }

    @Test
    void providerIsAuthenticatedAndUsesFrozenRouteAndWhitelist() throws Exception {
        assertThat(Security.class.isAssignableFrom(
                com.th4.edge.decoboon.standardasset.service.lifecycle.list.List.class)).isTrue();
        Service service = com.th4.edge.decoboon.standardasset.service.lifecycle.list.support.List.class.getAnnotation(Service.class);
        assertThat(service).isNotNull();
        assertThat(service.value()).isEqualTo("standard-asset/lifecycle/list");
        assertThat(com.th4.edge.decoboon.standardasset.service.lifecycle.list.support.List.class.getAnnotation(Properties.class)).isNotNull();

        Method call = com.th4.edge.decoboon.standardasset.service.lifecycle.list.support.List.class.getMethod("call", Employee.class);
        assertThat(call.getParameters()[0].isAnnotationPresent(User.class)).isTrue();
    }

    @Test
    void readContractDoesNotDefineMoneyOrCostCalculationFields() {
        List<String> fieldNames = Arrays.stream(StandardAssetLifecycleContract.class.getDeclaredFields())
                .map(Field::getName).map(String::toLowerCase).toList();
        assertThat(fieldNames).noneMatch(name -> name.contains("amount") || name.contains("price")
                || name.contains("profit") || name.contains("cost"));
    }

    private StandardAssetLifecycleContract contract(StandardAssetType type) {
        return contracts.stream().filter(item -> item.getAssetType() == type).findFirst().orElseThrow();
    }
}
