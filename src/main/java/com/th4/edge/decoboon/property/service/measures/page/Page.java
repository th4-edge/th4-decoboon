package com.th4.edge.decoboon.property.service.measures.page;
import com.th4.edge.decoboon.property.model.Measure;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
public interface Page extends Provider<Measure, org.springframework.data.domain.Page<Measure>>, Security {}
