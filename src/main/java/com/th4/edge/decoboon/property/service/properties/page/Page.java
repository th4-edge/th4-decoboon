package com.th4.edge.decoboon.property.service.properties.page;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
public interface Page extends Provider<Property, org.springframework.data.domain.Page<Property>>, Security {}
