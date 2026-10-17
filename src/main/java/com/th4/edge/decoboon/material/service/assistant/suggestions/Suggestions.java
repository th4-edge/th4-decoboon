package com.th4.edge.decoboon.material.service.assistant.suggestions;

import com.th4.edge.decoboon.material.model.MaterialAssistantSuggestionQuery;
import com.th4.edge.decoboon.material.model.MaterialAssistantSuggestions;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 材料业务助手上下文建议问题（只读，复用同一黄金问题知识源）。 */
public interface Suggestions extends Provider<MaterialAssistantSuggestionQuery, MaterialAssistantSuggestions>, Security {
}
