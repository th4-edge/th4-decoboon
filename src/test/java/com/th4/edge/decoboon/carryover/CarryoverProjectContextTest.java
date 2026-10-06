package com.th4.edge.decoboon.carryover;

import com.th4.edge.decoboon.carryover.configure.CarryoverCostStatus;
import com.th4.edge.decoboon.carryover.context.CarryoverProjectContext;
import com.th4.edge.decoboon.carryover.model.CarryoverProject;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.components.lock.DistributedLock;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CarryoverProjectContextTest {

    private final CarryoverProjectContext context =
            new CarryoverProjectContext(null, null, null, null, noopLock());

    @Test
    void completeDraftHasNoBlockers() {
        CarryoverProject item = validItem();
        assertEquals(List.of(), context.validate(item));
    }

    @Test
    void unknownFinanceAndCostRemainBlocking() {
        CarryoverProject item = validItem();
        item.setReceivableConfirmed(false);
        item.setPayableOutstanding(null);
        item.setCostStatus(CarryoverCostStatus.TotalOnly);
        item.setCostTotal(null);

        List<String> blockers = context.validate(item);
        assertTrue(blockers.contains("期初应收确认"));
        assertTrue(blockers.contains("期初应付确认"));
        assertTrue(blockers.contains("接管前成本总额"));
    }

    @Test
    void incompleteCostMayBeCarriedButIsNotReadyForProfit() {
        CarryoverProject item = validItem();
        item.setCostStatus(CarryoverCostStatus.TotalOnly);
        item.setCostTotal(new BigDecimal("10000.00"));
        assertEquals(List.of(), context.validate(item));
        assertTrue(item.getCostStatus() != CarryoverCostStatus.Complete);
    }

    @Test
    void legacyAccountNoteIsStoredOnEntity() {
        CarryoverProject item = validItem();
        item.setLegacyAccountNote("原账科目：应收账款-工程款");
        assertEquals("原账科目：应收账款-工程款", item.getLegacyAccountNote());
    }

    private CarryoverProject validItem() {
        CarryoverProject item = new CarryoverProject();
        item.setCustomerName("张三");
        item.setCustomerPhone("13800000000");
        item.setAddress("合肥市某小区1栋101");
        Employee owner = new Employee();
        owner.setEmpId(1L);
        item.setOwner(owner);
        Employee manager = new Employee();
        manager.setEmpId(2L);
        item.setManager(manager);
        item.setContractNo("HT-001");
        item.setCurrentAmount(new BigDecimal("300000.00"));
        item.setSignedAt(LocalDate.now().minusMonths(2));
        item.setProjectStatus(ProjectStatus.Ongoing);
        item.setCurrentStageName("泥木阶段");
        item.setProgress(40);
        item.setRemainingWork("继续完成墙地砖和木作");
        item.setReceivableOutstanding(new BigDecimal("120000.00"));
        item.setReceivableConfirmed(true);
        item.setPayableOutstanding(BigDecimal.ZERO);
        item.setPayableConfirmed(true);
        item.setCostStatus(CarryoverCostStatus.Complete);
        item.setCostTotal(new BigDecimal("180000.00"));
        return item;
    }

    private static DistributedLock noopLock() {
        return new DistributedLock() {
            @Override public boolean tryLock(String name, long waitMillis, long leaseMillis) { return true; }
            @Override public void unlock(String name) { }
        };
    }
}
