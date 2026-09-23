package com.th4.edge.decoboon.plan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.decoboon.plan.context.PlanTemplateBaselineContext;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PlanTemplateBaselineContextTest {

    @Test
    void loadsPaymentAndConstructionBaselines() {
        PlanTemplateBaselineContext context = new PlanTemplateBaselineContext(new ObjectMapper());
        context.load();

        assertThat(context.catalog().getPaymentPlans()).hasSize(3);
        assertThat(context.catalog().getConstructionPlans()).hasSize(2);
        assertThat(context.paymentPlan("STANDARD_30_40_30").getNodes())
                .hasSize(3);
        assertThat(context.paymentPlan("STANDARD_30_40_30").getNodes().stream()
                .map(item -> item.getRatio())
                .reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo(BigDecimal.ONE);
        assertThat(context.constructionPlan("RESIDENTIAL_STANDARD_8").getStages())
                .extracting("code")
                .containsExactly("PREPARE", "DEMOLITION", "WATER_ELECTRIC", "MASONRY",
                        "CARPENTRY", "PAINTING", "INSTALL", "COMPLETION");
    }
}
