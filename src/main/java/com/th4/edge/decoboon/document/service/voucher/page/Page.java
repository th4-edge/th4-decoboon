package com.th4.edge.decoboon.document.service.voucher.page;

import com.th4.edge.decoboon.document.model.VoucherRecord;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 凭证记录分页。 */
public interface Page extends ModelService<VoucherRecord, org.springframework.data.domain.Page<VoucherRecord>>, Security {
}
