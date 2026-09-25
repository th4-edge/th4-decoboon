package com.th4.edge.decoboon.design.context;

/** 图像生成模型客户端。 */
public interface DesignRenderClient {

    RenderResult render(String modelName, String prompt, String referenceImage, String parametersJson);

    /** 模型生成结果。 */
    final class RenderResult {
        private final String imageUrl;
        private final String requestId;
        private final int inputImageCount;
        private final int outputImageCount;
        private final String usageJson;

        public RenderResult(String imageUrl, String requestId, int inputImageCount,
                            int outputImageCount, String usageJson) {
            this.imageUrl = imageUrl;
            this.requestId = requestId;
            this.inputImageCount = inputImageCount;
            this.outputImageCount = outputImageCount;
            this.usageJson = usageJson;
        }

        public String getImageUrl() { return imageUrl; }
        public String getRequestId() { return requestId; }
        public int getInputImageCount() { return inputImageCount; }
        public int getOutputImageCount() { return outputImageCount; }
        public String getUsageJson() { return usageJson; }
    }
}
