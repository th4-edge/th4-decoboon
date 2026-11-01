package com.th4.edge.decoboon.project.context;

import com.th4.edge.decoboon.catalog.configure.ItemType;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 增减项可变更来源投影契约（DEC-055 / BR-041）。
 *
 * <p>契约：只暴露「原合同行 + 已生效变更目标」两类来源；来源按 PICKER_PAGE 分页，
 * 当前有效剩余所需变更明细只加载一次。</p>
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
    @DisplayName("来源 = 原合同行首页 + 已生效变更目标；分页元数据存在且变更明细只加载一次")
    void shouldProjectPaginatedSourcesWithSingleChangeItemLoad() {
        ProjectContract contract = contract();
        when(contractRepository.findOne(any(Specification.class))).thenReturn(Optional.of(contract));
        ContractLine line = contractLine(101L, "卫生间墙砖铺贴", ItemType.Construction);
        ContractChangeItem effectiveSource = effectiveItem(201L, ItemType.Material);
        ContractChangeItem pendingSource = pendingItem(202L);
        List<ContractChangeItem> changeItems = List.of(effectiveSource, pendingSource);
        when(itemRepository.findByChange_Contract_ContractIdOrderByChangeItemIdAsc(20L)).thenReturn(changeItems);
        when(lineRepository.count(any(Specification.class))).thenReturn(1L);
        when(lineRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(line), org.springframework.data.domain.PageRequest.of(0, 20), 1));
        when(lineRepository.summarizePickerFacetsByContract(contract)).thenReturn(List.<Object[]>of(
                new Object[]{ItemType.Construction, "卫生间", 1L}));
        when(rangeContext.snapshot(contract, line, changeItems)).thenReturn(snapshot("22.5", "22.5", "0", "22.5", "2925", "130", true, null));
        when(rangeContext.snapshot(contract, effectiveSource, changeItems)).thenReturn(snapshot("12", "8", "4", "4", "1040", "130", true, null));

        ContractChangeRange view = provider.call(new ContractChange());

        assertThat(view.getCurrent()).isEqualTo(1);
        assertThat(view.getSize()).isEqualTo(20);
        assertThat(view.getSources()).hasSize(2);
        assertThat(view.getTotal()).isEqualTo(2);
        assertThat(view.getContractTotal()).isEqualTo(1);
        assertThat(view.getEffectiveTotal()).isEqualTo(1);
        assertThat(view.getHasMore()).isFalse();
        assertThat(view.getItemTypes()).extracting(ContractChangeRange.Facet::getKey)
                .containsExactlyInAnyOrder("Construction", "Material");
        assertThat(view.getSpaces()).extracting(ContractChangeRange.Facet::getKey)
                .contains("卫生间");
        ContractChangeRange.Source fromContract = view.getSources().get(0);
        assertThat(fromContract.getSourceLineId()).isEqualTo(101L);
        assertThat(fromContract.getAvailableQuantity()).isEqualByComparingTo("22.5");
        ContractChangeRange.Source fromEffective = view.getSources().get(1);
        assertThat(fromEffective.getSourceChangeItemId()).isEqualTo(201L);
        assertThat(fromEffective.getPendingReservedQuantity()).isEqualByComparingTo("4");
        assertThat(view.getSources()).noneSatisfy(item ->
                assertThat(item.getSourceChangeItemId()).isEqualTo(202L));
        verify(itemRepository, times(1)).findByChange_Contract_ContractIdOrderByChangeItemIdAsc(20L);
    }

    @Test
    @DisplayName("指定来源时回带 selected；来源不可变更时 confirmable=false 且带阻断原因")
    void shouldReturnSelectedSourceAndBlockReason() {
        ProjectContract contract = contract();
        when(contractRepository.findOne(any(Specification.class))).thenReturn(Optional.of(contract));
        ContractLine line = contractLine(101L, "卫生间墙砖铺贴", ItemType.Construction);
        when(itemRepository.findByChange_Contract_ContractIdOrderByChangeItemIdAsc(20L)).thenReturn(List.of());
        when(lineRepository.count(any(Specification.class))).thenReturn(1L);
        when(lineRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(line), org.springframework.data.domain.PageRequest.of(0, 20), 1));
        when(lineRepository.findByLineIdAndContract_ContractId(101L, 20L)).thenReturn(Optional.of(line));
        when(lineRepository.summarizePickerFacetsByContract(contract)).thenReturn(List.of());
        when(rangeContext.snapshot(eq(contract), eq(line), any(List.class)))
                .thenReturn(snapshot("10", "0", "0", "0", "0", "0", false, "当前有效剩余暂不可确认"));
        ReflectionTestUtils.setField(provider, "sourceLineId", 101L);

        ContractChangeRange view = provider.call(new ContractChange());

        assertThat(view.getSelected()).isNotNull();
        assertThat(view.getSelected().getSourceLineId()).isEqualTo(101L);
        assertThat(view.getSelected().getConfirmable()).isFalse();
        assertThat(view.getSelected().getBlockReason()).isEqualTo("当前有效剩余暂不可确认");
    }

    @Test
    void effectiveOnlyPageMustExcludeContractRows() {
        ProjectContract contract = contract();
        when(contractRepository.findOne(any(Specification.class))).thenReturn(Optional.of(contract));
        ContractChangeItem effective = effectiveItem(201L, ItemType.Material);
        List<ContractChangeItem> items = List.of(effective);
        when(itemRepository.findByChange_Contract_ContractIdOrderByChangeItemIdAsc(20L)).thenReturn(items);
        when(lineRepository.count(any(Specification.class))).thenReturn(3L);
        when(lineRepository.summarizePickerFacetsByContract(contract)).thenReturn(List.of());
        when(rangeContext.snapshot(contract, effective, items))
                .thenReturn(snapshot("5", "5", "0", "5", "440", "88", true, null));
        ReflectionTestUtils.setField(provider, "group", "effective");

        ContractChangeRange view = provider.call(new ContractChange());

        assertThat(view.getContractTotal()).isEqualTo(3);
        assertThat(view.getEffectiveTotal()).isEqualTo(1);
        assertThat(view.getTotal()).isEqualTo(1);
        assertThat(view.getSources()).singleElement()
                .satisfies(item -> assertThat(item.getGroup()).isEqualTo("effective"));
    }

    @Test
    void consecutivePagesMustCoverContractRowsAndEffectiveSourceWithoutDuplicates() {
        ProjectContract contract = contract();
        when(contractRepository.findOne(any(Specification.class))).thenReturn(Optional.of(contract));
        List<ContractLine> lines = new ArrayList<>();
        for (long index = 0; index < 6; index++) {
            lines.add(contractLine(1000L + index, "行" + index, ItemType.Construction));
        }
        ContractChangeItem effective = effectiveItem(201L, ItemType.Material);
        List<ContractChangeItem> items = List.of(effective);
        when(itemRepository.findByChange_Contract_ContractIdOrderByChangeItemIdAsc(20L)).thenReturn(items);
        when(lineRepository.count(any(Specification.class))).thenReturn(6L);
        when(lineRepository.findAll(any(Specification.class), any(Pageable.class))).thenAnswer(invocation -> {
            Pageable pageable = invocation.getArgument(1);
            int from = pageable.getPageNumber() * pageable.getPageSize();
            int to = Math.min(lines.size(), from + pageable.getPageSize());
            return new PageImpl<>(lines.subList(Math.min(from, lines.size()), to), pageable, lines.size());
        });
        when(lineRepository.summarizePickerFacetsByContract(contract)).thenReturn(List.of());
        when(rangeContext.snapshot(eq(contract), any(ContractLine.class), any(List.class)))
                .thenReturn(snapshot("1", "1", "0", "1", "10", "10", true, null));
        when(rangeContext.snapshot(contract, effective, items))
                .thenReturn(snapshot("5", "5", "0", "5", "440", "88", true, null));
        ReflectionTestUtils.setField(provider, "size", 2);

        ReflectionTestUtils.setField(provider, "current", 1);
        ContractChangeRange page1 = provider.call(new ContractChange());
        ReflectionTestUtils.setField(provider, "current", 2);
        ContractChangeRange page2 = provider.call(new ContractChange());
        ReflectionTestUtils.setField(provider, "current", 3);
        ContractChangeRange page3 = provider.call(new ContractChange());
        ReflectionTestUtils.setField(provider, "current", 4);
        ContractChangeRange page4 = provider.call(new ContractChange());

        List<ContractChangeRange.Source> all = new ArrayList<>();
        all.addAll(page1.getSources());
        all.addAll(page2.getSources());
        all.addAll(page3.getSources());
        all.addAll(page4.getSources());
        assertThat(all).extracting(item -> item.getSourceLineId() != null
                        ? item.getSourceLineId() : item.getSourceChangeItemId())
                .containsExactly(1000L, 1001L, 1002L, 1003L, 1004L, 1005L, 201L);
        assertThat(page1.getSources()).hasSize(2);
        assertThat(page2.getSources()).hasSize(2);
        assertThat(page3.getSources()).hasSize(2);
        assertThat(page4.getSources()).hasSize(1);
    }

    @Test
    void requestedLargePageMustBeCappedAtFifty() {
        ProjectContract contract = contract();
        when(contractRepository.findOne(any(Specification.class))).thenReturn(Optional.of(contract));
        List<ContractLine> lines = new ArrayList<>();
        for (long index = 0; index < 50; index++) {
            lines.add(contractLine(1000L + index, "行" + index, ItemType.Construction));
        }
        List<ContractChangeItem> items = List.of();
        when(itemRepository.findByChange_Contract_ContractIdOrderByChangeItemIdAsc(20L)).thenReturn(items);
        when(lineRepository.count(any(Specification.class))).thenReturn(60L);
        when(lineRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(lines, org.springframework.data.domain.PageRequest.of(0, 50), 60));
        when(lineRepository.summarizePickerFacetsByContract(contract)).thenReturn(List.of());
        when(rangeContext.snapshot(eq(contract), any(ContractLine.class), any(List.class)))
                .thenReturn(snapshot("1", "1", "0", "1", "10", "10", true, null));
        ReflectionTestUtils.setField(provider, "size", 500);

        ContractChangeRange view = provider.call(new ContractChange());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(lineRepository).findAll(any(Specification.class), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(50);
        assertThat(view.getSize()).isEqualTo(50);
        assertThat(view.getSources()).hasSize(50);
        assertThat(view.getHasMore()).isTrue();
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

    private ContractLine contractLine(Long id, String name, ItemType type) {
        ContractLine line = new ContractLine();
        line.setLineId(id);
        line.setName(name);
        line.setType(type);
        line.setSpaceName("卫生间");
        line.setUnit(com.th4.edge.decoboon.bim.configure.Unit.Area);
        line.setQuantity(new BigDecimal("22.5"));
        line.setAmount(new BigDecimal("2925"));
        return line;
    }

    private ContractChangeItem effectiveItem(Long id, ItemType type) {
        ContractChange change = new ContractChange();
        change.setChangeId(300L);
        change.setNo("BG-20261012-001");
        change.setStatus(ContractChangeStatus.CustomerConfirmed);
        ContractChangeItem item = new ContractChangeItem();
        item.setChangeItemId(id);
        item.setChange(change);
        item.setItemType(type);
        item.setTargetType(type);
        item.setSourceLine(contractLine(900L + id, "来源行", type));
        item.setTargetName("阳台防水");
        item.setTargetUnit(com.th4.edge.decoboon.bim.configure.Unit.Area);
        return item;
    }

    private ContractChangeItem pendingItem(Long id) {
        ContractChangeItem item = effectiveItem(id, ItemType.Material);
        item.getChange().setStatus(ContractChangeStatus.Pending);
        return item;
    }

    private ContractChangeRangeContext.RangeSnapshot snapshot(String initialQuantity,
                                                               String effectiveQuantity,
                                                               String pendingQuantity,
                                                               String availableQuantity,
                                                               String remainingAmount,
                                                               String unitPrice,
                                                               boolean confirmable,
                                                               String blockReason) {
        return new ContractChangeRangeContext.RangeSnapshot(
                new BigDecimal(initialQuantity), new BigDecimal(effectiveQuantity),
                new BigDecimal(pendingQuantity), new BigDecimal(availableQuantity),
                new BigDecimal(remainingAmount), new BigDecimal(unitPrice), confirmable, blockReason);
    }
}
