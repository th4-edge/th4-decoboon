package com.th4.edge.decoboon.design.context;

import com.th4.edge.framework.core.annotation.Note;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/** AI设计生成异步执行器。 */
@Note("AI设计生成异步执行器")
@Component
public class DesignGenerationRunner {

    private static final Logger log = LoggerFactory.getLogger(DesignGenerationRunner.class);

    private final DesignGenerationStateContext stateContext;
    private final DesignReferenceImageContext referenceImageContext;
    private final DesignRenderClient renderClient;

    public DesignGenerationRunner(DesignGenerationStateContext stateContext,
                                  DesignReferenceImageContext referenceImageContext,
                                  DesignRenderClient renderClient) {
        this.stateContext = stateContext;
        this.referenceImageContext = referenceImageContext;
        this.renderClient = renderClient;
    }

    @Async("designGenerationExecutor")
    public void submit(Long generationId) {
        DesignGenerationStateContext.RunInput input;
        try {
            input = stateContext.start(generationId);
        } catch (DesignRenderException ex) {
            stateContext.fail(generationId, ex.getCode(), ex.getMessage());
            return;
        } catch (Exception ex) {
            stateContext.fail(generationId, "AI_GENERATION_START_FAILED", ex.getMessage());
            return;
        }
        if (input == null) {
            return;
        }
        stateContext.markGenerating(generationId);
        try {
            String referenceImage = referenceImageContext.resolveDataUrl(input.getReferenceImagePath());
            DesignRenderClient.RenderResult result = renderClient.render(input.getModelName(),
                    input.getVisualPrompt(), referenceImage, input.getModelParametersJson());
            stateContext.complete(generationId, result);
        } catch (DesignRenderException ex) {
            log.warn("AI设计生成失败 generationId={} code={}", generationId, ex.getCode());
            stateContext.fail(generationId, ex.getCode(), ex.getMessage());
        } catch (Exception ex) {
            log.error("AI设计生成异常 generationId={}", generationId, ex);
            stateContext.fail(generationId, "AI_GENERATION_FAILED", ex.getMessage());
        }
    }
}
