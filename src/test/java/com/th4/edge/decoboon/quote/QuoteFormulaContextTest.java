package com.th4.edge.decoboon.quote;

import com.th4.edge.decoboon.quote.context.QuoteFormulaContext;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.framework.exception.DefinedException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuoteFormulaContextTest {

    private final QuoteFormulaContext context = new QuoteFormulaContext();

    @Test
    void shouldEvaluateArithmeticAndAbsoluteReference() {
        QuoteLine first = line(1L, 0, "12.5");
        QuoteLine second = line(2L, 1, "1");

        Map<Long, BigDecimal> result = context.evaluate(
                List.of(first, second),
                Map.of(2L, "=$F$1*2+3")
        );

        assertThat(result.get(2L)).isEqualByComparingTo("28.000");
    }

    @Test
    void shouldEvaluateRangeSumAndRound() {
        QuoteLine first = line(1L, 0, "10");
        QuoteLine second = line(2L, 1, "20");
        QuoteLine third = line(3L, 2, "1");

        Map<Long, BigDecimal> result = context.evaluate(
                List.of(first, second, third),
                Map.of(3L, "=ROUND(SUM(F1:F2)/3,3)")
        );

        assertThat(result.get(3L)).isEqualByComparingTo("10.000");
    }

    @Test
    void shouldRejectCircularReference() {
        QuoteLine first = line(1L, 0, "1");
        QuoteLine second = line(2L, 1, "1");

        assertThatThrownBy(() -> context.evaluate(
                List.of(first, second),
                Map.of(1L, "=F2+1", 2L, "=F1+1")
        )).isInstanceOf(DefinedException.class).hasMessageContaining("循环引用");
    }

    @Test
    void shouldRejectUnsupportedFunction() {
        QuoteLine first = line(1L, 0, "1");

        assertThatThrownBy(() -> context.evaluate(
                List.of(first),
                Map.of(1L, "=IF(F1>0,1,0)")
        )).isInstanceOf(DefinedException.class).hasMessageContaining("不支持的函数");
    }

    private QuoteLine line(Long id, int sort, String quantity) {
        QuoteLine line = new QuoteLine();
        line.setItemId(id);
        line.setSort(sort);
        line.setQuantity(new BigDecimal(quantity));
        line.setMaterialPrice(BigDecimal.ZERO);
        line.setManualPrice(BigDecimal.ZERO);
        line.setPrice(BigDecimal.ZERO);
        line.setAmount(BigDecimal.ZERO);
        return line;
    }
}
