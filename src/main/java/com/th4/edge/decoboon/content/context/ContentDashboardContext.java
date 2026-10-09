package com.th4.edge.decoboon.content.context;

import com.th4.edge.decoboon.content.configure.ContentAttributionConfidence;
import com.th4.edge.decoboon.content.configure.EmployeeContentStatus;
import com.th4.edge.decoboon.content.model.ContentDashboardReport;
import com.th4.edge.decoboon.content.model.LeadTouch;
import com.th4.edge.decoboon.content.model.PublicationRecord;
import com.th4.edge.decoboon.content.repository.ContentTaskRepository;
import com.th4.edge.decoboon.content.repository.EmployeeContentRepository;
import com.th4.edge.decoboon.content.repository.LeadTouchRepository;
import com.th4.edge.decoboon.content.repository.PublicationRecordRepository;
import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/** 内容经营看板聚合上下文。 */
@Note("内容经营看板聚合")
@Component
public class ContentDashboardContext {

    private final LeadTouchRepository leadTouchRepository;
    private final PublicationRecordRepository publicationRepository;
    private final EmployeeContentRepository employeeContentRepository;
    private final ContentTaskRepository taskRepository;
    private final ContentAccessContext accessContext;

    public ContentDashboardContext(LeadTouchRepository leadTouchRepository,
                                   PublicationRecordRepository publicationRepository,
                                   EmployeeContentRepository employeeContentRepository,
                                   ContentTaskRepository taskRepository,
                                   ContentAccessContext accessContext) {
        this.leadTouchRepository = leadTouchRepository;
        this.publicationRepository = publicationRepository;
        this.employeeContentRepository = employeeContentRepository;
        this.taskRepository = taskRepository;
        this.accessContext = accessContext;
    }

    /** P0 仅允许企业负责人查看企业全量内容经营；店长/团队长按范围能力留待后续。 */
    public ContentDashboardReport report(Employee principal) {
        Employee me = accessContext.requireEmployee(principal);
        if (me.getRole() != EmployeeRole.Boss) {
            throw new DefinedException(new DefaultErrorCode("403", "内容经营看板当前仅对企业负责人开放"));
        }
        Long comId = me.getCompany().getComId();
        List<LeadTouch> touches = leadTouchRepository.findByCompany_ComIdOrderByTouchIdDesc(comId);
        Set<Long> validLeadIds = new HashSet<>();
        Set<Long> measuredIds = new HashSet<>();
        Set<Long> signedIds = new HashSet<>();
        for (LeadTouch touch : touches) {
            if (!Boolean.TRUE.equals(touch.getIsPrimary())) continue;
            if (touch.getConfidence() == null || touch.getConfidence() == ContentAttributionConfidence.Low) continue;
            PublicationRecord publication = touch.getPublication();
            if (publication == null || (!"Backfilled".equals(publication.getStatus()) && !"Published".equals(publication.getStatus()))) {
                continue;
            }
            Customer customer = touch.getCustomer();
            if (customer.getStatus() == CustStatus.Lost || customer.getStatus() == CustStatus.Dormant) continue;
            validLeadIds.add(customer.getCustId());
            if (customer.getMeasureAt() != null) measuredIds.add(customer.getCustId());
            if (customer.getStatus() == CustStatus.Signed || customer.getStatus() == CustStatus.Serving
                    || customer.getStatus() == CustStatus.Finished) {
                signedIds.add(customer.getCustId());
            }
        }
        ContentDashboardReport report = new ContentDashboardReport();
        report.setMetrics(List.of(
                new ContentDashboardReport.Metric("published", "已回填发布",
                        publicationRepository.countByCompany_ComIdAndStatus(comId, "Backfilled"), "已保存发布事实的内容"),
                new ContentDashboardReport.Metric("pendingReview", "待审核成片",
                        employeeContentRepository.countByCompany_ComIdAndStatus(comId, EmployeeContentStatus.PendingReview), "需要企业审核"),
                new ContentDashboardReport.Metric("leads", "有效内容线索", validLeadIds.size(),
                        "主归因、高/中置信、已回填发布，排除流失和休眠"),
                new ContentDashboardReport.Metric("measured", "内容线索已量房", measuredIds.size(),
                        "有效内容线索进入现有客户跟进链路"),
                new ContentDashboardReport.Metric("signed", "内容线索已签约", signedIds.size(),
                        "有效内容线索达到签约、履约或完工")
        ));
        report.setRecentTouches(touches.stream().limit(10).toList());
        report.setRecentTasks(taskRepository.findByCompany_ComIdOrderByTaskIdDesc(comId).stream().limit(5).toList());
        return report;
    }
}
