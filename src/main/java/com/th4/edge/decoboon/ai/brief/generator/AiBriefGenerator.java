package com.th4.edge.decoboon.ai.brief.generator;

/** AI 经营简报生成器。首版使用规则基线，后续可替换为大模型实现。 */
public interface AiBriefGenerator {

    String generate(AiBriefInput input);

    String version();
}
