package com.th4.edge.decoboon.design.service.talks.build;

import com.th4.edge.decoboon.design.model.Talk;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 谈单 PPT 内容生成。 */
public interface Build extends Provider<Talk, Talk>, Security {
}
