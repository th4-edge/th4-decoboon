package com.th4.edge.decoboon.content.service.task.claim;

import com.th4.edge.decoboon.content.model.ContentTask;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 员工领取内容任务。 */
public interface Claim extends Provider<ContentTask, ContentTask>, Security {}
