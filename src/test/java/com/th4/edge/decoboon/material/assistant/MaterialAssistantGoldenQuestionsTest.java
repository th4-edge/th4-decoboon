package com.th4.edge.decoboon.material.assistant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.decoboon.material.context.MaterialHealthContext;
import com.th4.edge.decoboon.material.context.MaterialPlanWorkbenchContext;
import com.th4.edge.decoboon.material.context.MaterialSheetContext;
import com.th4.edge.decoboon.material.context.MaterialSheetTemplateContext;
import com.th4.edge.decoboon.material.configure.MaterialSheetStatus;
import com.th4.edge.decoboon.material.configure.SheetPriceState;
import com.th4.edge.decoboon.material.model.MaterialAssistantAnswer;
import com.th4.edge.decoboon.material.model.MaterialAssistantCitation;
import com.th4.edge.decoboon.material.model.MaterialHealthIssue;
import com.th4.edge.decoboon.material.model.MaterialHealthReport;
import com.th4.edge.decoboon.material.model.MaterialOrderSheet;
import com.th4.edge.decoboon.material.model.MaterialPlanWorkbench;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MaterialAssistantGoldenQuestionsTest {

    private final TenantContext tenantContext = mock(TenantContext.class);
    private final ProjectAccessContext projectAccessContext = mock(ProjectAccessContext.class);
    private final MaterialSheetContext sheetContext = mock(MaterialSheetContext.class);
    private final MaterialSheetTemplateContext templateContext = mock(MaterialSheetTemplateContext.class);
    private final MaterialHealthContext healthContext = mock(MaterialHealthContext.class);
    private final MaterialPlanWorkbenchContext workbenchContext = mock(MaterialPlanWorkbenchContext.class);
    private final Employee employee = new Employee();
    private final MaterialAssistantKnowledge knowledge = new MaterialAssistantKnowledge(new ObjectMapper());
    private final MaterialAssistantContext context = new MaterialAssistantContext(
            tenantContext, knowledge, new MaterialAssistantIntentResolver(), projectAccessContext,
            sheetContext, templateContext, healthContext, workbenchContext);

    @BeforeEach
    void setUp() {
        Company company = new Company();
        company.setComId(1L);
        employee.setEmpId(1L);
        employee.setRole(EmployeeRole.Boss);
        employee.setCompany(company);
        when(tenantContext.requireEmployee(employee)).thenReturn(employee);
    }

    @Test
    void allGoldenQuestionsMustProduceGroundingOrExplicitRefusal() {
        for (MaterialAssistantKnowledge.GoldenQuestion question : knowledge.goldenQuestions()) {
            MaterialAssistantAnswer answer = context.ask(employee, question.question(), null, null);
            assertThat(answer.getIntent()).as(question.id()).isEqualTo(question.intent().name());
            assertThat(answer.getAnswer()).as(question.id()).isNotBlank();
            assertThat(answer.isReadOnly()).as(question.id()).isTrue();
            assertThat(answer.getAnswerType()).as(question.id()).isIn(
                    "EXPLANATION", "LIVE_DATA", "PERMISSION_DENIED", "REFUSAL", "NO_EVIDENCE");
            if (!question.expectedRefs().isEmpty()) {
                List<String> ids = answer.getCitations().stream().map(MaterialAssistantCitation::getId).toList();
                assertThat(ids).as(question.id() + " citations").containsAll(question.expectedRefs());
            }
        }
    }

    @Test
    void projectHealthMustUseLiveProviderAndKeepProfitNullWhenCostIncomplete() {
        Project project = new Project();
        project.setProjectId(7L);
        when(projectAccessContext.requireReadableProject(7L, employee)).thenReturn(project);
        MaterialHealthReport health = new MaterialHealthReport();
        health.setProjectId(7L);
        health.setHasMaterialPlan(true);
        health.setTotalItems(3);
        health.setSelectedItems(2);
        health.setUnselectedItems(1);
        health.setReadyToOrder(1);
        health.setPendingArrival(1);
        health.setDelayed(1);
        health.setCostComplete(false);
        health.setCostIncompleteCount(2);
        health.setMaterialCost(new BigDecimal("1200.00"));
        health.setActualCost(new BigDecimal("900.00"));
        health.setProfitAmount(null);
        health.setStageLabel("选材 / 现场确认");
        health.setStageSummary("仍有材料计划行未完成选材。");
        health.setNextAction("设计师完成选材并交付财务");
        MaterialHealthIssue issue = new MaterialHealthIssue();
        issue.setId("CHK-UNSELECTED");
        issue.setLabel("未完成选材");
        issue.setCount(1);
        health.setIssues(List.of(issue));
        when(healthContext.report(employee, 7L)).thenReturn(health);

        MaterialAssistantAnswer answer = context.ask(employee, "这个项目的材料业务现在缺什么？", 7L, null);

        assertThat(answer.getAnswerType()).isEqualTo("LIVE_DATA");
        assertThat(answer.getAnswer()).contains("选材 / 现场确认", "未完成选材", "设计师完成选材并交付财务");
        assertThat(answer.getAnswer()).contains("利润：null");
        assertThat(answer.getToolCalls()).extracting("provider").contains("blueprint/check/report", "project/project/info");
    }

    @Test
    void nonFinanceProjectQuestionMustUseWorkbenchAndNotReadCost() {
        Employee designer = new Employee();
        designer.setEmpId(2L);
        designer.setRole(EmployeeRole.Designer);
        Company company = new Company();
        company.setComId(1L);
        designer.setCompany(company);
        when(tenantContext.requireEmployee(designer)).thenReturn(designer);
        Project project = new Project();
        project.setProjectId(8L);
        when(projectAccessContext.requireReadableProject(8L, designer)).thenReturn(project);
        MaterialPlanWorkbench workbench = new MaterialPlanWorkbench();
        workbench.setTotalItems(2);
        workbench.setReadyToOrder(1);
        workbench.setPendingArrival(1);
        workbench.setDelayed(0);
        when(workbenchContext.summarize(designer, 8L, null)).thenReturn(List.of(workbench));

        MaterialAssistantAnswer answer = context.ask(designer, "这个项目的材料业务现在缺什么？", 8L, null);

        assertThat(answer.getAnswer()).contains("当前角色可查看履约状态");
        verifyNoInteractions(healthContext);
        assertThat(answer.getToolCalls()).extracting("provider").contains("material/plan/workbench");
    }

    @Test
    void sheetQuestionMustUseLiveSheetAndTemplateGate() {
        Project project = new Project();
        project.setProjectId(9L);
        MaterialOrderSheet sheet = new MaterialOrderSheet();
        sheet.setSheetId(99L);
        sheet.setSheetNo("TILE-20261017-001");
        sheet.setSheetType("Tile");
        sheet.setStatus(MaterialSheetStatus.Draft);
        sheet.setPriceState(SheetPriceState.Unpriced);
        sheet.setProject(project);
        sheet.setInstallAt(java.time.LocalDate.now().plusDays(3));
        when(sheetContext.info(employee, 99L)).thenReturn(sheet);
        when(projectAccessContext.requireReadableProject(9L, employee)).thenReturn(project);
        when(templateContext.requiredHeaderFields(any(), eq("Tile"))).thenReturn(List.of(
                new MaterialSheetTemplateContext.FieldRequirement("measureAt", "测量时间", true, true, 1)));

        MaterialAssistantAnswer answer = context.ask(employee, "测量字段没填能提交请购吗？", null, 99L);

        assertThat(answer.getIntent()).isEqualTo("SHEET_STATUS");
        assertThat(answer.getAnswer()).contains("当前模板必填字段缺失：测量时间", "先补齐模板必填字段");
        assertThat(answer.getToolCalls()).extracting("provider").contains("material/sheet/info");
    }

    @Test
    void permissionQuestionMustExplainReadOnlyScope() {
        MaterialAssistantAnswer answer = context.ask(employee, "谁能查看材料成本和供应商价格？", null, null);
        assertThat(answer.getAnswerType()).isEqualTo("PERMISSION_DENIED");
        assertThat(answer.getAnswer()).contains("Boss", "店长", "财务", "跨企业");
        assertThat(answer.isReadOnly()).isTrue();
    }

    @Test
    void unknownQuestionMustNotInventEvidence() {
        MaterialAssistantAnswer answer = context.ask(employee, "明天适合穿什么衣服？", null, null);
        assertThat(answer.getAnswerType()).isEqualTo("NO_EVIDENCE");
        assertThat(answer.getAnswer()).contains("暂不可确认");
        assertThat(answer.getConfidence()).isLessThan(0.5);
    }
}
