package com.th4.edge.decoboon.content.service.task.publish;

import com.th4.edge.decoboon.content.model.ContentTask;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 内容任务发布。 */
public interface Publish extends Provider<ContentTask, ContentTask>, Security {}
