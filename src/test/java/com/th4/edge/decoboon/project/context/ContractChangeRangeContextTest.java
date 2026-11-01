package com.th4.edge.decoboon.project.context;

import com.th4.edge.decoboon.project.configure.ContractChangeStatus;
import com.th4.edge.decoboon.project.configure.ContractChangeType;
import com.th4.edge.decoboon.project.model.ContractChange;
import com.th4.edge.decoboon.project.model.ContractChangeItem;
import com.th4.edge.decoboon.project.model.ContractLine;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ContractChangeItemRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 当前有效剩余与待审批占用的权威投影测试。 */
class ContractChangeRangeContextTest {

    @Test
    void shouldSubtractEffectiveAndReservePendingQuantity() {
        ProjectContract contract = contract(7L);
        ContractLine line = line(11L, contract, "10", "1000");
        ContractChangeItem effectiveReduction = changeItem(21L, contract, ContractChangeStatus.CustomerConfirmed,
                ContractChangeType.Quantity, line, "10", "7", new BigDecimal("-300"));
        ContractChangeItem pendingReduction = changeItem(22L, contract, ContractChangeStatus.Pending,
                ContractChangeType.Quantity, line, "7", "6", new BigDecimal("-100"));
        ContractChangeItemRepository repository = mock(ContractChangeItemRepository.class);
        when(repository.findByChange_Contract_ContractIdOrderByChangeItemIdAsc(7L))
                .thenReturn(List.of(effectiveReduction, pendingReduction));

        var snapshot = new ContractChangeRangeContext(repository).snapshot(contract, line);

        assertThat(snapshot.confirmable()).isTrue();
        assertThat(snapshot.effectiveRemainingQuantity()).isEqualByComparingTo("7");
        assertThat(snapshot.pendingReservedQuantity()).isEqualByComparingTo("1");
        assertThat(snapshot.availableQuantity()).isEqualByComparingTo("6");
        assertThat(snapshot.effectiveRemainingAmount()).isEqualByComparingTo("700.00");
        assertThat(snapshot.currentUnitPrice()).isEqualByComparingTo("100.00");
    }

    @Test
    void batchProjectionMustReuseProvidedItemsWithoutRepositoryQuery() {
        ProjectContract contract = contract(7L);
        ContractLine line = line(11L, contract, "10", "1000");
        ContractChangeItem reduction = changeItem(21L, contract, ContractChangeStatus.CustomerConfirmed,
                ContractChangeType.Quantity, line, "10", "7", new BigDecimal("-300"));
        ContractChangeItemRepository repository = mock(ContractChangeItemRepository.class);

        var snapshot = new ContractChangeRangeContext(repository)
                .snapshot(contract, line, List.of(reduction));

        assertThat(snapshot.effectiveRemainingQuantity()).isEqualByComparingTo("7");
        assertThat(snapshot.availableQuantity()).isEqualByComparingTo("7");
        verifyNoInteractions(repository);
    }

    @Test
    void shouldCloseWholeEffectiveRemainingForRemove() {
        ProjectContract contract = contract(7L);
        ContractLine line = line(11L, contract, "10", "1000");
        ContractChangeItem remove = changeItem(21L, contract, ContractChangeStatus.CustomerConfirmed,
                ContractChangeType.Remove, line, "10", "0", new BigDecimal("-1000"));
        ContractChangeItemRepository repository = mock(ContractChangeItemRepository.class);
        when(repository.findByChange_Contract_ContractIdOrderByChangeItemIdAsc(7L)).thenReturn(List.of(remove));

        var snapshot = new ContractChangeRangeContext(repository).snapshot(contract, line);

        assertThat(snapshot.effectiveRemainingQuantity()).isEqualByComparingTo("0");
        assertThat(snapshot.effectiveRemainingAmount()).isEqualByComparingTo("0.00");
        assertThat(snapshot.availableQuantity()).isEqualByComparingTo("0");
    }

    @Test
    void shouldUseFrozenSourceRemainingAmountForReplace() {
        ProjectContract contract = contract(7L);
        ContractLine line = line(11L, contract, "10", "1000");
        ContractChangeItem reduction = changeItem(20L, contract, ContractChangeStatus.CustomerConfirmed,
                ContractChangeType.Quantity, line, "10", "7", new BigDecimal("-300"));
        ContractChangeItem replace = changeItem(21L, contract, ContractChangeStatus.CustomerConfirmed,
                ContractChangeType.Replace, line, "7", "6", new BigDecimal("380"));
        replace.setSourceRemainingQuantity(new BigDecimal("7"));
        replace.setSourceRemainingAmount(new BigDecimal("700"));
        ContractChangeItemRepository repository = mock(ContractChangeItemRepository.class);
        when(repository.findByChange_Contract_ContractIdOrderByChangeItemIdAsc(7L))
                .thenReturn(List.of(reduction, replace));

        var snapshot = new ContractChangeRangeContext(repository).snapshot(contract, line);

        assertThat(snapshot.effectiveRemainingQuantity()).isEqualByComparingTo("0");
        assertThat(snapshot.effectiveRemainingAmount()).isEqualByComparingTo("0.00");
        assertThat(snapshot.confirmable()).isTrue();
    }

    @Test
    void shouldBlockWhenHistoricalReplaceHasNoSourceRemainingSnapshot() {
        ProjectContract contract = contract(7L);
        ContractLine line = line(11L, contract, "10", "1000");
        ContractChangeItem replace = changeItem(21L, contract, ContractChangeStatus.CustomerConfirmed,
                ContractChangeType.Replace, line, "10", "6", new BigDecimal("80"));
        ContractChangeItemRepository repository = mock(ContractChangeItemRepository.class);
        when(repository.findByChange_Contract_ContractIdOrderByChangeItemIdAsc(7L)).thenReturn(List.of(replace));

        var snapshot = new ContractChangeRangeContext(repository).snapshot(contract, line);

        assertThat(snapshot.confirmable()).isFalse();
        assertThat(snapshot.blockReason()).contains("历史替换缺少来源剩余快照");
    }

    @Test
    void shouldTreatEffectiveReplacementTargetAsSourceRange() {
        ProjectContract contract = contract(7L);
        ContractChangeItem target = target(30L, contract, "5", "1000");
        ContractChangeItem reduction = changeItem(31L, contract, ContractChangeStatus.CustomerConfirmed,
                ContractChangeType.Quantity, null, "5", "3", new BigDecimal("-400"));
        reduction.setSourceChangeItem(target);
        ContractChangeItemRepository repository = mock(ContractChangeItemRepository.class);
        when(repository.findByChange_Contract_ContractIdOrderByChangeItemIdAsc(7L))
                .thenReturn(List.of(target, reduction));

        var snapshot = new ContractChangeRangeContext(repository).snapshot(contract, target);

        assertThat(snapshot.effectiveRemainingQuantity()).isEqualByComparingTo("3");
        assertThat(snapshot.effectiveRemainingAmount()).isEqualByComparingTo("600.00");
        assertThat(snapshot.availableQuantity()).isEqualByComparingTo("3");
    }

    private ProjectContract contract(Long id) {
        ProjectContract contract = new ProjectContract();
        contract.setContractId(id);
        return contract;
    }

    private ContractLine line(Long id, ProjectContract contract, String quantity, String amount) {
        ContractLine line = new ContractLine();
        line.setLineId(id);
        line.setContract(contract);
        line.setQuantity(new BigDecimal(quantity));
        line.setAmount(new BigDecimal(amount));
        return line;
    }

    private ContractChangeItem changeItem(Long id, ProjectContract contract, ContractChangeStatus status,
                                          ContractChangeType type, ContractLine source, String oldQuantity,
                                          String newQuantity, BigDecimal amountDelta) {
        ContractChange change = new ContractChange();
        change.setChangeId(id + 100);
        change.setContract(contract);
        change.setStatus(status);
        ContractChangeItem item = new ContractChangeItem();
        item.setChangeItemId(id);
        item.setChange(change);
        item.setChangeType(type);
        item.setSourceLine(source);
        item.setOldQuantity(new BigDecimal(oldQuantity));
        item.setNewQuantity(new BigDecimal(newQuantity));
        item.setAmountDelta(amountDelta);
        return item;
    }

    private ContractChangeItem target(Long id, ProjectContract contract, String quantity, String amount) {
        ContractChange change = new ContractChange();
        change.setChangeId(id + 100);
        change.setContract(contract);
        change.setStatus(ContractChangeStatus.CustomerConfirmed);
        ContractChangeItem item = new ContractChangeItem();
        item.setChangeItemId(id);
        item.setChange(change);
        item.setChangeType(ContractChangeType.Add);
        item.setTargetQuantity(new BigDecimal(quantity));
        item.setTargetAmount(new BigDecimal(amount));
        return item;
    }
}
