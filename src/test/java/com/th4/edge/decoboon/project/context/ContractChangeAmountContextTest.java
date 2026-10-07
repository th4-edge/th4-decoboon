package com.th4.edge.decoboon.project.context;

import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.project.configure.ContractChangeType;
import com.th4.edge.decoboon.project.model.ContractLine;
import com.th4.edge.decoboon.project.repository.ContractLineRepository;
import com.th4.edge.framework.exception.DefinedException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/** 增减项金额服务端重算唯一口径测试：单条与批量共用 ContractChangeAmountContext。 */
class ContractChangeAmountContextTest {

    private final ContractChangeAmountContext context =
            new ContractChangeAmountContext(mock(ContractLineRepository.class));

    @Test
    void quantityChangeMustRecomputeAmountAndCostOnServer() {
        ContractLine line = line("10", "1000", "600");

        var computed = context.compute(ContractChangeType.Quantity, line, null, null,
                new BigDecimal("12"), null, null);

        assertThat(computed.amountDelta()).isEqualByComparingTo("200.00"); // (12-10)*100
        assertThat(computed.costDelta()).isEqualByComparingTo("120.00");    // (12-10)*60
        assertThat(computed.oldQuantity()).isEqualByComparingTo("10");
        assertThat(computed.newQuantity()).isEqualByComparingTo("12");
    }

    @Test
    void priceChangeMustRecomputeAmountAndCostOnServer() {
        ContractLine line = line("10", "1000", "600");

        var computed = context.compute(ContractChangeType.Price, line, null, null,
                null, new BigDecimal("120"), new BigDecimal("70"));

        assertThat(computed.amountDelta()).isEqualByComparingTo("200.00"); // (120-100)*10
        assertThat(computed.costDelta()).isEqualByComparingTo("100.00");    // (70-60)*10
    }

    @Test
    void addWithoutSourceLineMayUseRequestedAmount() {
        var computed = context.compute(ContractChangeType.Add, null, new BigDecimal("500"),
                new BigDecimal("300"), null, null, null);

        assertThat(computed.amountDelta()).isEqualByComparingTo("500");
        assertThat(computed.costDelta()).isEqualByComparingTo("300");
    }

    @Test
    void addWithoutSourceLineAndAmountMustFail() {
        assertThatThrownBy(() -> context.compute(ContractChangeType.Add, null, null, null, null, null, null))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("缺少原合同行");
    }

    private ContractLine line(String quantity, String amount, String cost) {
        ContractLine line = new ContractLine();
        line.setLineId(1L);
        line.setType(ItemType.Construction);
        line.setQuantity(new BigDecimal(quantity));
        line.setAmount(new BigDecimal(amount));
        line.setCost(new BigDecimal(cost));
        return line;
    }
}
