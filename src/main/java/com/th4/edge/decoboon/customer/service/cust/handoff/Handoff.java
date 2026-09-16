package com.th4.edge.decoboon.customer.service.cust.handoff;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

public interface Handoff extends Provider<Customer, Customer>, Security {
}
