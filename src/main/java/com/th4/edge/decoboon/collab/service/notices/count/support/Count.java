package com.th4.edge.decoboon.collab.service.notices.count.support;
import com.th4.edge.framework.data.jackson.annotation.Properties;

import com.th4.edge.decoboon.collab.context.NoticeContext;
import com.th4.edge.decoboon.collab.model.Notice;
import com.th4.edge.decoboon.collab.repository.NoticeRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Note("通知未读数")
@Properties
@Service("collab/notice/count")
@Scope(ScopeTypes.PROTOTYPE)
public class Count implements com.th4.edge.decoboon.collab.service.notices.count.Count {

    @User
    private Employee employee;

    private final NoticeRepository noticeRepository;
    private final NoticeContext noticeContext;
    private final DataScopeContext dataScopeContext;

    public Count(NoticeRepository noticeRepository,
                 NoticeContext noticeContext,
                 DataScopeContext dataScopeContext) {
        this.noticeRepository = noticeRepository;
        this.noticeContext = noticeContext;
        this.dataScopeContext = dataScopeContext;
    }

    @Override
    public Long call(Notice request) {
        Employee current = dataScopeContext.requireEmployee(employee);
        noticeContext.refresh(current);
        return noticeRepository.countByReceiver_EmpIdAndReadFalse(current.getEmpId());
    }
}
