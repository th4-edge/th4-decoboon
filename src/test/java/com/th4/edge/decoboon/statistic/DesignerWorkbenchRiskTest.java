package com.th4.edge.decoboon.statistic;

import com.th4.edge.decoboon.statistic.context.DesignerWorkbenchRisk;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DesignerWorkbenchRiskTest {

    @Test
    void shouldApplyQuoteThresholdsAndNextActions() {
        DesignerWorkbenchRisk.QuoteDecision draft = DesignerWorkbenchRisk.quote("Draft", 7);
        assertThat(draft.riskLevel()).isEqualTo("Danger");
        assertThat(draft.nextActionKey()).isEqualTo("continueEdit");
        assertThat(draft.riskReason()).contains("7 天");

        DesignerWorkbenchRisk.QuoteDecision submitted = DesignerWorkbenchRisk.quote("Submitted", 3);
        assertThat(submitted.riskLevel()).isEqualTo("Danger");
        assertThat(submitted.nextActionKey()).isEqualTo("urgeCustomer");

        DesignerWorkbenchRisk.QuoteDecision rejected = DesignerWorkbenchRisk.quote("Rejected", 1);
        assertThat(rejected.riskLevel()).isEqualTo("Danger");
        assertThat(rejected.nextActionKey()).isEqualTo("reviseAndResubmit");

        assertThat(DesignerWorkbenchRisk.quote("Submitted", 2).riskLevel()).isEqualTo("Normal");
    }

    @Test
    void shouldApplySignThresholdsAndNextOwnerActions() {
        DesignerWorkbenchRisk.SignDecision waiting = DesignerWorkbenchRisk.sign(false, null, false, 3);
        assertThat(waiting.riskLevel()).isEqualTo("Warning");
        assertThat(waiting.nextActionKey()).isEqualTo("createContract");

        DesignerWorkbenchRisk.SignDecision danger = DesignerWorkbenchRisk.sign(false, "PendingSign", false, 7);
        assertThat(danger.riskLevel()).isEqualTo("Danger");
        assertThat(danger.nextActionKey()).isEqualTo("waitEnterpriseSign");

        DesignerWorkbenchRisk.SignDecision signed = DesignerWorkbenchRisk.sign(false, "Signed", false, 2);
        assertThat(signed.nextActionKey()).isEqualTo("waitEnterpriseEffective");

        DesignerWorkbenchRisk.SignDecision signedRisk = DesignerWorkbenchRisk.sign(true, "Signed", false, 9);
        assertThat(signedRisk.riskLevel()).isEqualTo("Normal");
        assertThat(signedRisk.nextActionKey()).isEqualTo("waitEnterpriseEffective");

        DesignerWorkbenchRisk.SignDecision converted = DesignerWorkbenchRisk.sign(true, "Effective", true, 9);
        assertThat(converted.riskLevel()).isEqualTo("Normal");
        assertThat(converted.nextActionKey()).isEqualTo("viewProject");

        DesignerWorkbenchRisk.SignDecision effectiveWithoutProject =
                DesignerWorkbenchRisk.sign(true, "Effective", false, 9);
        assertThat(effectiveWithoutProject.riskLevel()).isEqualTo("Warning");
        assertThat(effectiveWithoutProject.nextActionKey()).isEqualTo("waitProjectCreated");
        assertThat(effectiveWithoutProject.nextActionLabel()).isEqualTo("合同已生效待项目建立");
    }
}
