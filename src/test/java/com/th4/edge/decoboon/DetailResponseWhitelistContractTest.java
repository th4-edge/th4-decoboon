package com.th4.edge.decoboon;

import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.project.model.Acceptance;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/** 高风险详情响应必须为目标关联模型声明字段白名单，防止序列化循环归属图。 */
class DetailResponseWhitelistContractTest {

    @Test
    void quoteInfoMustWhitelistEmployeeSummary() {
        Properties properties = com.th4.edge.decoboon.quote.service.quotes.info.support.Info.class
                .getAnnotation(Properties.class);
        assertThat(properties.allow()).anySatisfy(item -> {
            assertThat(item.model()).isEqualTo(Quote.class);
            assertThat(Arrays.asList(item.properties())).contains("createdBy", "property");
        });
        assertThat(properties.allow()).anySatisfy(item -> {
            assertThat(item.model()).isEqualTo(Employee.class);
            assertThat(Arrays.asList(item.properties())).containsExactly("empId", "name", "role");
        });
    }

    @Test
    void materialPlanPageMustNotExposeRecursivePropertyGraph() {
        Properties properties = com.th4.edge.decoboon.project.service.materialplans.page.support.Page.class
                .getAnnotation(Properties.class);
        assertThat(properties.allow()).anySatisfy(item -> {
            assertThat(item.model()).isEqualTo(MaterialPlan.class);
            assertThat(Arrays.asList(item.properties())).doesNotContain("property");
        });
        assertThat(properties.allow()).anySatisfy(item -> {
            assertThat(item.model()).isEqualTo(Project.class);
            assertThat(Arrays.asList(item.properties())).doesNotContain("property");
        });
        assertThat(properties.allow()).anySatisfy(item -> {
            assertThat(item.model()).isEqualTo(CatalogItem.class);
            assertThat(Arrays.asList(item.properties())).contains("itemId", "name", "unit");
        });
    }

    @Test
    void designSchemeUpdateMustWhitelistCustomerAndEmployeeSummary() {
        Properties properties = com.th4.edge.decoboon.design.service.schemes.update.support.Update.class
                .getAnnotation(Properties.class);
        assertThat(properties.allow()).anySatisfy(item -> {
            assertThat(item.model()).isEqualTo(Customer.class);
            assertThat(Arrays.asList(item.properties())).contains("custId", "name", "community", "addr");
        });
        assertThat(properties.allow()).anySatisfy(item -> {
            assertThat(item.model()).isEqualTo(Employee.class);
            assertThat(Arrays.asList(item.properties())).containsExactly("empId", "name", "role");
        });
    }

    @Test
    void acceptanceInfoMustWhitelistPropertySummary() {
        Properties properties = com.th4.edge.decoboon.project.service.acceptances.info.support.Info.class
                .getAnnotation(Properties.class);
        assertThat(properties.allow()).anySatisfy(item -> {
            assertThat(item.model()).isEqualTo(Project.class);
            assertThat(Arrays.asList(item.properties())).contains("property");
        });
        assertThat(properties.allow()).anySatisfy(item -> {
            assertThat(item.model()).isEqualTo(Property.class);
            assertThat(Arrays.asList(item.properties())).doesNotContain("store", "team", "salesman", "designer");
        });
        assertThat(properties.allow()).anySatisfy(item -> assertThat(item.model()).isEqualTo(Acceptance.class));
    }
}
