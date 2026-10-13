package com.th4.edge.decoboon.project.context;

import com.th4.edge.decoboon.project.configure.ContractChangeStatus;
import com.th4.edge.decoboon.project.model.ContractChange;
import com.th4.edge.decoboon.project.model.ContractChangeItem;
import com.th4.edge.decoboon.project.model.ContractChangeRange;
import com.th4.edge.decoboon.project.model.ContractLine;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ContractChangeItemRepository;
import com.th4.edge.decoboon.project.repository.ContractLineRepository;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.service.contractchange.range.support.Range;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 增减项可变更来源投影契约（DEC-055 / BR-041）。
 *
 * <p>契约：只暴露「原合同行 + 已生效变更目标」两类来源，且每条来源都带服务端投影的
 * 当前有效剩余 / 待审批占用 / 可变更量；未生效变更的目标不得出现。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RangeProviderTest {

    @Mock private ProjectContractRepository contractRepository;
    @Mock private ContractLineRepository lineRepository;
    @Mock private ContractChangeItemRepository itemRepository;
    @Mock private ContractChangeRangeContext rangeContext;
    @Mock private ProjectAccessContext accessContext;

    private Range provider;

    @BeforeEach
    void setUp() {
        provider = new Range(contractRepository, lineRepository, itemRepository, rangeContext, accessContext);
        ReflectionTestUtils.setField(provider, "contractId", 20L);
        ReflectionTestUtils.setField(provider, "employee", new Employee());
        when(accessContext.requireEmployee(any())).thenReturn(new Employee());
    }

    @Test
    @DisplayName("来源清单 = 原合同行 + 已生效变更目标，并带当前有效剩余与可变更量")
    void shouldProjectSourcesWithRangeSnapshot() {
        ProjectContract contract = contract();
        when(contractRepository.findOne(any(Specification.class))).thenReturn(Optional.of(contract));
        ContractLine line = contractLine(101L, "卫生间墙砖铺贴");
        when(lineRepository.findByContractOrderBySortAsc(contract)).thenReturn(List.of(line));
        // 同一实例既用于仓储返回也用于打桩：ContractChangeItem 无 equals，按实例匹配。
        ContractChangeItem effectiveSource = effectiveItem(201L);
        ContractChangeItem pendingSource = pendingItem(202L);
        when(itemRepository.findByChange_Contract_ContractIdOrderByChangeItemIdAsc(20L))
                .thenReturn(List.of(effectiveSource, pendingSource));
        when(rangeContext.snapshot(contract, line)).thenReturn(
                new ContractChangeRangeContext.RangeSnapshot(new BigDecimal("22.5"), new BigDecimal("22.5"),
                        BigDecimal.ZERO, new BigDecimal("22.5"), new BigDecimal("2925"), new BigDecimal("130"),
                        true, null));
        when(rangeContext.snapshot(contract, effectiveSource)).thenReturn(
                new ContractChangeRangeContext.RangeSnapshot(new BigDecimal("12"), new BigDecimal("8"),
                        new BigDecimal("4"), new BigDecimal("4"), new BigDecimal("1040"), new BigDecimal("130"),
                        true, null));

        ContractChangeRange view = provider.call(new ContractChange());

        assertThat(view.getSources()).hasSize(2);
        ContractChangeRange.Source fromContract = view.getSources().get(0);
        assertThat(fromContract.getGroup()).isEqualTo("contract");
        assertThat(fromContract.getSourceLineId()).isEqualTo(101L);
        assertThat(fromContract.getEffectiveRemainingQuantity()).isEqualByComparingTo("22.5");
        assertThat(fromContract.getAvailableQuantity()).isEqualByComparingTo("22.5");
        assertThat(fromContract.getConfirmable()).isTrue();

        ContractChangeRange.Source fromEffective = view.getSources().get(1);
        assertThat(fromEffective.getGroup()).isEqualTo("effective");
        assertThat(fromEffective.getSourceChangeItemId()).isEqualTo(201L);
        assertThat(fromEffective.getPendingReservedQuantity()).isEqualByComparingTo("4");
        assertThat(fromEffective.getAvailableQuantity()).isEqualByComparingTo("4");
        assertThat(view.getConfirmable()).isTrue();
        // 待审批（未生效）变更的目标不得成为可变更来源
        assertThat(view.getSources()).noneSatisfy(item ->
                assertThat(item.getSourceChangeItemId()).isEqualTo(202L));
    }

    @Test
    @DisplayName("指定来源时回带 selected；来源不可变更时 confirmable=false 且带阻断原因")
    void shouldReturnSelectedSourceAndBlockReason() {
        ProjectContract contract = contract();
        when(contractRepository.findOne(any(Specification.class))).thenReturn(Optional.of(contract));
        ContractLine line = contractLine(101L, "卫生间墙砖铺贴");
        when(lineRepository.findByContractOrderBySortAsc(contract)).thenReturn(List.of(line));
        when(itemRepository.findByChange_Contract_ContractIdOrderByChangeItemIdAsc(20L)).thenReturn(List.of());
        when(rangeContext.snapshot(contract, line)).thenReturn(
                new ContractChangeRangeContext.RangeSnapshot(new BigDecimal("10"), BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        false, "当前有效剩余暂不可确认"));
        ReflectionTestUtils.setField(provider, "sourceLineId", 101L);

        ContractChangeRange view = provider.call(new ContractChange());

        assertThat(view.getSelected()).isNotNull();
        assertThat(view.getSelected().getSourceLineId()).isEqualTo(101L);
        assertThat(view.getSelected().getConfirmable()).isFalse();
        assertThat(view.getSelected().getBlockReason()).isEqualTo("当前有效剩余暂不可确认");
        assertThat(view.getConfirmable()).isFalse();
    }

    @Test
    @DisplayName("合同不存在或越权：404，不泄露对象存在性")
    void shouldRejectUnknownContract() {
        when(contractRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());
        assertThatThrownBy(() -> provider.call(new ContractChange()))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("合同基线不存在或无权访问");
    }

    private ProjectContract contract() {
        Project project = new Project();
        project.setProjectId(10L);
        ProjectContract contract = new ProjectContract();
        contract.setContractId(20L);
        contract.setNo("HT2026-0512");
        contract.setCurrentAmount(new BigDecimal("286400"));
        contract.setProject(project);
        return contract;
    }

    private ContractLine contractLine(Long id, String name) {
        ContractLine line = new ContractLine();
        line.setLineId(id);
        line.setName(name);
        line.setQuantity(new BigDecimal("22.5"));
        line.setAmount(new BigDecimal("2925"));
        return line;
    }

    private ContractChangeItem effectiveItem(Long id) {
        ContractChange change = new ContractChange();
        change.setChangeId(300L);
        change.setNo("BG-20261012-001");
        change.setStatus(ContractChangeStatus.CustomerConfirmed);
        ContractChangeItem item = new ContractChangeItem();
        item.setChangeItemId(id);
        item.setChange(change);
        item.setTargetName("阳台防水");
        item.setTargetUnit(com.th4.edge.decoboon.bim.configure.Unit.Area);
        return item;
    }

    private ContractChangeItem pendingItem(Long id) {
        ContractChangeItem item = effectiveItem(id);
        item.getChange().setStatus(ContractChangeStatus.Pending);
        return item;
    }
}
