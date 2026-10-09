package com.th4.edge.decoboon.statistic.context;

import com.th4.edge.decoboon.statistic.model.DesignerWorkbenchReport;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 设计师本月签约额排名投影测试。 */
class DesignerWorkbenchRankingTest {

    @Test
    void shouldBuildCurrentRankTopGapAndPeerAverage() {
        DesignerWorkbenchReport.PerformanceRanking ranking = DesignerWorkbenchContext.buildPerformanceRanking(List.of(
                row(1L, "张三", "300000", 3),
                row(2L, "李四", "200000", 2),
                row(3L, "王五", "100000", 1),
                row(4L, "赵六", "50000", 1)
        ), 2L);

        assertThat(ranking.getRank()).isEqualTo(2);
        assertThat(ranking.getDesignerCount()).isEqualTo(4);
        assertThat(ranking.getSampleSufficient()).isTrue();
        assertThat(ranking.getMyValue()).isEqualByComparingTo("200000");
        assertThat(ranking.getTopValue()).isEqualByComparingTo("300000");
        assertThat(ranking.getPeerAverage()).isEqualByComparingTo("162500");
        assertThat(ranking.getGapToTop()).isEqualByComparingTo("100000");
        assertThat(ranking.getGapToAverage()).isEqualByComparingTo("37500");
        assertThat(ranking.getTopDesigners()).hasSize(4);
        assertThat(ranking.getTopDesigners().get(1).getCurrentEmployee()).isTrue();
    }

    @Test
    void shouldMarkSmallSampleAndHandleMissingCurrentEmployee() {
        DesignerWorkbenchReport.PerformanceRanking ranking = DesignerWorkbenchContext.buildPerformanceRanking(List.of(
                row(1L, "张三", "300000", 3),
                row(2L, "李四", "200000", 2)
        ), 99L);

        assertThat(ranking.getRank()).isZero();
        assertThat(ranking.getSampleSufficient()).isFalse();
        assertThat(ranking.getGapToTop()).isEqualByComparingTo("300000");
        assertThat(ranking.getGapToAverage()).isEqualByComparingTo("-250000");
    }

    private static Object[] row(Long employeeId, String name, String value, int projectCount) {
        return new Object[]{employeeId, name, new BigDecimal(value), projectCount};
    }
}
