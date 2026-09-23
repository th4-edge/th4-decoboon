package com.th4.edge.decoboon.document.service.reprint.save;

import com.th4.edge.decoboon.document.model.DocumentRecord;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

public interface Save extends Provider<DocumentRecord, DocumentRecord>, Security {
}
