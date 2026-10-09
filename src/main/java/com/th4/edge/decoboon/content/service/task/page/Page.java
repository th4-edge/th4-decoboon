package com.th4.edge.decoboon.content.service.task.page;

import com.th4.edge.decoboon.content.model.ContentTask;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 内容任务分页。 */
public interface Page extends Provider<ContentTask, org.springframework.data.domain.Page<ContentTask>>, Security {}
