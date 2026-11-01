# 本地视觉评审能力配置

## 1. 当前事实

当前 Codex 主模型配置：

```toml
model = "deepseek-v4-flash"
model_provider = "custom"
```

模型目录中 `deepseek-v4-flash` 与 `deepseek-v4-pro` 均声明：

```json
"input_modalities": ["text"],
"supports_image_detail_original": false
```

因此 DeepSeek 主模型不能直接读取图片。Codex CLI 虽然提供 `--image`，但图片是否真正被模型接收，取决于模型/provider 是否声明 image modality。

本机已经安装 Ollama 视觉模型：

```text
qwen2.5vl:7b  # 推荐，默认视觉评审模型
qwen2.5vl:3b  # 快速预检
```

但 `qwen2.5vl:3b` 不支持 Codex 主 Agent 所需的工具调用，因此不能直接作为 Codex 主模型使用。

## 2. 推荐架构

```text
DeepSeek 主模型
  -> 负责产品、业务、交互、代码和设计判断
  -> 需要看截图时调用本地视觉评审脚本
  -> qwen2.5vl 返回结构化文字描述
  -> DeepSeek 将视觉描述与 DOM/Token/状态证据合并
```

这不是让 DeepSeek 直接“看见”，而是给它接入一双可调用的眼睛。

## 3. 已配置脚本

```text
.agents/skills/zhuangdeqing-ui-craft/scripts/vision_review.py
```

检查环境：

```bash
python3 .agents/skills/zhuangdeqing-ui-craft/scripts/vision_review.py --check
```

## 4. 使用方式

### 当前页面视觉评审

```bash
python3 .agents/skills/zhuangdeqing-ui-craft/scripts/vision_review.py \
  --preset ui-review \
  --image output/playwright/page-1440.png \
  --output output/playwright/page-1440-vision-review.md
```

### 站酷参考作品拆解

```bash
python3 .agents/skills/zhuangdeqing-ui-craft/scripts/vision_review.py \
  --preset reference-study \
  --image output/playwright/reference.jpg \
  --output output/playwright/reference-study.md
```

### 参考图与当前页面对比

顺序必须是：图 1 参考，图 2 当前页面。

```bash
python3 .agents/skills/zhuangdeqing-ui-craft/scripts/vision_review.py \
  --preset reference-compare \
  --image output/playwright/reference.jpg \
  --image output/playwright/current-1440.png \
  --output output/playwright/reference-vs-current.md
```

## 5. 质量分级

### `qwen2.5vl:7b`（默认）

适合：

- 页面结构、信息层级、密度、对齐、配色比例的常规视觉评审；
- 与参考图做初步对比；
- 发现明显的 AI 味、卡片墙和视觉噪音。

限制：

- 仍不是人类审美裁决；
- 可能给出“增加辅助色/说明文案”等泛化建议，主 Agent 必须按装得清规范过滤。

### `qwen2.5vl:3b`（快速预检）

适合：

- 验证图片是否被正确读取；
- 识别明显布局问题；
- 发现明显卡片墙、信息密度、对比度问题；
- 作为快速第一轮视觉预检。

不适合：

- 专业级审美裁决；
- 精细字阶、留白和材质判断；
- 复杂业务界面的最终视觉验收；
- 代替人类设计师或更强视觉模型。

### 更强视觉模型

当前已安装 `qwen2.5vl:7b` 并作为脚本默认模型。

```bash
python3 .agents/skills/zhuangdeqing-ui-craft/scripts/vision_review.py \
  --preset ui-review \
  --image output/playwright/page-1440.png
```

16GB 内存设备不建议本地运行 32B；如需更高视觉判断，优先使用云端视觉模型或人工评审。

更大模型的视觉判断能力更好，但仍不是最终业务验收。

多图对比时可用 `--num-ctx 16384 --max-image-side 1280`；单图快速评审默认使用 8192 context。

## 6. 证据规则

- 视觉模型输出必须与截图、DOM 几何、Token 检查一起保存。
- 必须记录模型名、图片路径、运行时间和原始输出。
- 不得把 3B 模型的“看起来不错”当作视觉通过。
- 最终视觉验收仍需更强视觉模型或人工评审。
- 如果 Ollama 不可用，标记 `VISUAL_REVIEW_UNAVAILABLE`，不声称视觉评审通过。

## 7. 不建议的方案

### 直接把 qwen2.5vl 当 Codex 主模型

当前会失败，因为：

```text
registry.ollama.ai/library/qwen2.5vl:3b does not support tools
```

Codex 主 Agent 依赖工具调用，视觉模型必须同时支持工具调用才能作为主模型。

### 只给 DeepSeek 配置 `--image`

模型目录仍声明 text-only，图片不会可靠进入模型上下文；这会造成“命令看似成功、模型实际没看图”的假象。

### 用 OCR 或色板冒充视觉评审

OCR、色板、DOM、截图只能作为辅助证据，不能替代构图、排版、材质和风格判断。
