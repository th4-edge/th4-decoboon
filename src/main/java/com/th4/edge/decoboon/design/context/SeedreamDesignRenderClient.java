package com.th4.edge.decoboon.design.context;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.framework.core.annotation.Note;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/** 火山方舟Seedream图像生成客户端。 */
@Note("Seedream图像生成客户端")
@Component
public class SeedreamDesignRenderClient implements DesignRenderClient {

    private static final Logger log = LoggerFactory.getLogger(SeedreamDesignRenderClient.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String apiKey;
    private final String baseUrl;
    private final String defaultModel;

    public SeedreamDesignRenderClient(ObjectMapper objectMapper,
                                      @Value("${decoboon.ai.ark.api-key:${ARK_API_KEY:}}") String apiKey,
                                      @Value("${decoboon.ai.ark.base-url:https://ark.cn-beijing.volces.com/api/v3}") String baseUrl,
                                      @Value("${decoboon.ai.ark.model:doubao-seedream-5-0-pro-260628}") String defaultModel) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.baseUrl = baseUrl == null || baseUrl.isBlank()
                ? "https://ark.cn-beijing.volces.com/api/v3" : baseUrl.replaceAll("/+$", "");
        this.defaultModel = defaultModel == null || defaultModel.isBlank()
                ? "doubao-seedream-5-0-pro-260628" : defaultModel;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    @Override
    public RenderResult render(String modelName, String prompt, String referenceImage, String parametersJson) {
        if (apiKey.isBlank()) {
            throw new DesignRenderException("AI_MODEL_NOT_CONFIGURED", "AI模型未配置");
        }
        Map<String, Object> payload = payload(modelName, prompt, referenceImage, parametersJson);
        DesignRenderException last = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/images/generations"))
                        .timeout(Duration.ofSeconds(240))
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return parse(response);
                }
                last = error(response.statusCode(), response.body());
                if (!retryable(response.statusCode()) || attempt == 2) break;
            } catch (DesignRenderException ex) {
                last = ex;
                if (attempt == 2) break;
            } catch (Exception ex) {
                last = new DesignRenderException("AI_MODEL_UNAVAILABLE", "AI模型调用失败", ex);
                if (attempt == 2) break;
            }
            try {
                Thread.sleep(1200L * attempt);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new DesignRenderException("AI_MODEL_INTERRUPTED", "AI生成任务被中断", ex);
            }
        }
        throw last == null ? new DesignRenderException("AI_MODEL_UNAVAILABLE", "AI模型调用失败") : last;
    }

    private Map<String, Object> payload(String modelName, String prompt, String referenceImage, String parametersJson) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", modelName == null || modelName.isBlank() ? defaultModel : modelName);
        payload.put("prompt", prompt);
        if (referenceImage != null && !referenceImage.isBlank()) {
            payload.put("image", referenceImage);
        }
        payload.put("size", "2048x1152");
        payload.put("response_format", "url");
        payload.put("watermark", false);
        if (parametersJson != null && !parametersJson.isBlank()) {
            try {
                JsonNode parameters = objectMapper.readTree(parametersJson);
                parameters.fields().forEachRemaining(entry -> payload.put(entry.getKey(), objectMapper.convertValue(entry.getValue(), Object.class)));
            } catch (Exception ex) {
                log.warn("忽略非法AI模型参数JSON");
            }
        }
        return payload;
    }

    private RenderResult parse(HttpResponse<String> response) throws Exception {
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode data = root.path("data");
        if (!data.isArray() || data.isEmpty()) {
            throw new DesignRenderException("AI_MODEL_BAD_RESPONSE", "AI模型未返回图片");
        }
        String image = data.get(0).path("url").asText(null);
        if (image == null || image.isBlank()) {
            throw new DesignRenderException("AI_MODEL_BAD_RESPONSE", "AI模型未返回有效图片地址");
        }
        String requestId = response.headers().firstValue("x-request-id")
                .orElse(root.path("id").asText(null));
        int input = payloadInputCount();
        int output = data.size();
        String usage = root.has("usage") ? objectMapper.writeValueAsString(root.get("usage")) : null;
        return new RenderResult(image, requestId, input, output, usage);
    }

    private int payloadInputCount() {
        return 1;
    }

    private DesignRenderException error(int status, String body) {
        String code = switch (status) {
            case 401, 403 -> "AI_MODEL_CREDENTIAL_INVALID";
            case 429 -> "AI_MODEL_RATE_LIMITED";
            default -> "AI_MODEL_REQUEST_FAILED";
        };
        String message = switch (status) {
            case 401, 403 -> "AI模型凭证无效或未开通";
            case 429 -> "AI模型请求过于频繁";
            default -> "AI模型请求失败";
        };
        log.warn("Seedream调用失败 status={} body={}", status, abbreviate(body));
        return new DesignRenderException(code, message);
    }

    private boolean retryable(int status) {
        return status == 429 || status == 500 || status == 502 || status == 503 || status == 504;
    }

    private String abbreviate(String text) {
        if (text == null) return "";
        return text.length() <= 500 ? text : text.substring(0, 500);
    }
}
