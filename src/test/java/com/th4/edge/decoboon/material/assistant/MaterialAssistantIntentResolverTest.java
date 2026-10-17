package com.th4.edge.decoboon.material.assistant;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MaterialAssistantIntentResolverTest {

    private final MaterialAssistantIntentResolver resolver = new MaterialAssistantIntentResolver();
    private final MaterialAssistantKnowledge knowledge = new MaterialAssistantKnowledge(new ObjectMapper());

    @Test
    void fiftyGoldenQuestionsMustResolveToExpectedIntent() {
        assertThat(knowledge.goldenQuestions()).hasSize(50);
        assertThat(knowledge.goldenQuestions()).extracting(MaterialAssistantKnowledge.GoldenQuestion::id)
                .doesNotHaveDuplicates();
        for (MaterialAssistantKnowledge.GoldenQuestion question : knowledge.goldenQuestions()) {
            assertThat(resolver.resolve(question.question()))
                    .as(question.id() + " " + question.question())
                    .isEqualTo(question.intent());
        }
    }

    @Test
    void outOfScopeActionsMustBeRefusedNotExecuted() {
        assertThat(resolver.resolve("帮我审批这张采购单")).isEqualTo(MaterialAssistantIntent.OUT_OF_SCOPE);
        assertThat(resolver.resolve("帮我付款给供应商")).isEqualTo(MaterialAssistantIntent.OUT_OF_SCOPE);
        assertThat(resolver.resolve("把设计师权限改成财务")).isEqualTo(MaterialAssistantIntent.OUT_OF_SCOPE);
    }

    @Test
    void sheetAndPurchaseQuestionsMustNotBeConfusedWithGenericStatus() {
        assertThat(resolver.resolve("作业单和采购单有什么区别？"))
                .isEqualTo(MaterialAssistantIntent.SHEET_VS_PURCHASE);
        assertThat(resolver.resolve("作业单上为什么不显示价格和账期？"))
                .isEqualTo(MaterialAssistantIntent.SHEET_VS_PURCHASE);
        assertThat(resolver.resolve("这张作业单为什么不能提交请购？"))
                .isEqualTo(MaterialAssistantIntent.SHEET_STATUS);
    }
}
