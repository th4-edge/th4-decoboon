package com.th4.edge.decoboon.material.service.assistant.ask;

import com.th4.edge.decoboon.material.model.MaterialAssistantAnswer;
import com.th4.edge.decoboon.material.model.MaterialAssistantQuery;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 材料业务助手只读问答。 */
public interface Ask extends Provider<MaterialAssistantQuery, MaterialAssistantAnswer>, Security {
}
