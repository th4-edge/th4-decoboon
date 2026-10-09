package com.th4.edge.decoboon.content.service.publication.backfill;
import com.th4.edge.decoboon.content.model.PublicationRecord;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
/** 回填员工发布记录。 */
public interface Backfill extends Provider<PublicationRecord, PublicationRecord>, Security {}
