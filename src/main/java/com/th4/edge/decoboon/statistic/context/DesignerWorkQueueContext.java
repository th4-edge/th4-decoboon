package com.th4.edge.decoboon.statistic.context;

import com.th4.edge.decoboon.statistic.model.WorkQueueItem;
import com.th4.edge.tenant.user.model.Employee;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 设计师今日主线队列聚合。
 *
 * <p>默认按“设计 → 报价 → 合同交接”各取一个最高优先级对象，最多 3 项。
 * 每个查询固定 SQL、LIMIT 1、索引路径明确；动作和路由均由服务端给出。</p>
 */
@Component
public class DesignerWorkQueueContext {

    private final EntityManager entityManager;

    public DesignerWorkQueueContext(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public List<WorkQueueItem> build(Employee current) {
        Long comId = current.getCompany().getComId();
        Long empId = current.getEmpId();
        List<WorkQueueItem> items = new ArrayList<>(3);
        WorkQueueItem design = designQueueItem(comId, empId);
        if (design != null) items.add(design);
        WorkQueueItem quote = quoteQueueItem(comId, empId);
        if (quote != null) items.add(quote);
        WorkQueueItem contract = contractQueueItem(comId, empId);
        if (contract != null) items.add(contract);
        return items;
    }

    private WorkQueueItem designQueueItem(Long comId, Long empId) {
        Object[] row = firstRow("""
                SELECT d.design_id,
                       d.customer_id,
                       d.property_id,
                       c.name,
                       p.community,
                       p.address,
                       d.status,
                       COALESCE(d.update_date_time, d.create_date_time),
                       TIMESTAMPDIFF(DAY,
                         STR_TO_DATE(CAST(COALESCE(d.update_date_time, d.create_date_time) AS CHAR), '%Y%m%d%H%i%s'),
                         NOW()) AS stay_days
                FROM cust_customer c
                STRAIGHT_JOIN db_property p
                  ON p.com_id = c.com_id
                 AND p.customer_id = c.cust_id
                STRAIGHT_JOIN db_design d
                  ON d.com_id = p.com_id
                 AND d.property_id = p.property_id
                WHERE c.com_id = :comId
                  AND c.designer_id = :empId
                  AND d.status IN ('Draft','Ready','Presented')
                  AND NOT EXISTS (
                    SELECT 1 FROM db_design closed
                    WHERE closed.com_id = d.com_id
                      AND closed.property_id = d.property_id
                      AND closed.status IN ('Selected','Archived')
                  )
                ORDER BY FIELD(d.status, 'Presented', 'Ready', 'Draft'),
                         stay_days DESC,
                         d.design_id DESC
                LIMIT 1
                """, Map.of("comId", comId, "empId", empId));
        if (row == null) return null;

        Long designId = longValue(row[0]);
        Long customerId = longValue(row[1]);
        Long propertyId = longValue(row[2]);
        String customerName = string(row[3]);
        String status = string(row[6]);
        int stayDays = integer(row[8]);
        WorkQueueItem item = base(
                "designer.queue.design." + designId,
                "Design",
                designId,
                customerId,
                propertyId,
                null,
                title(customerName, string(row[4]), string(row[5])),
                stateLabel(status) + " · 已停留 " + stayDays + " 天",
                status,
                stateLabel(status),
                10,
                stayDays,
                riskLevel(status, stayDays)
        );
        item.setNextActionKey("design/open");
        item.setNextActionLabel("Ready".equals(status) || "Presented".equals(status) ? "去谈单" : "继续设计");
        item.setRoute("/designer/design/" + designId + "?activeCustomerId=" + customerId + "&activePropertyId=" + propertyId);
        return item;
    }

    private WorkQueueItem quoteQueueItem(Long comId, Long empId) {
        Object[] row = firstRow("""
                SELECT q.quote_id,
                       q.customer_id,
                       q.property_id,
                       c.name,
                       p.community,
                       p.address,
                       q.status,
                       q.rejected_at,
                       q.create_date_time,
                       TIMESTAMPDIFF(DAY,
                         CASE WHEN q.status = 'Rejected' AND q.rejected_at IS NOT NULL
                              THEN q.rejected_at
                              ELSE STR_TO_DATE(CAST(q.create_date_time AS CHAR), '%Y%m%d%H%i%s')
                         END,
                         NOW()) AS stay_days
                FROM cust_customer c
                STRAIGHT_JOIN db_property p
                  ON p.com_id = c.com_id
                 AND p.customer_id = c.cust_id
                STRAIGHT_JOIN quote q
                  ON q.com_id = c.com_id
                 AND q.customer_id = c.cust_id
                WHERE c.com_id = :comId
                  AND q.emp_id = :empId
                  AND q.status IN ('Draft','Rejected')
                ORDER BY FIELD(q.status, 'Rejected', 'Draft'),
                         stay_days DESC,
                         q.quote_id DESC
                LIMIT 1
                """, Map.of("comId", comId, "empId", empId));
        if (row == null) return null;

        Long quoteId = longValue(row[0]);
        Long customerId = longValue(row[1]);
        Long propertyId = longValue(row[2]);
        String customerName = string(row[3]);
        String status = string(row[6]);
        int stayDays = integer(row[9]);
        WorkQueueItem item = base(
                "designer.queue.quote." + quoteId,
                "Quote",
                quoteId,
                customerId,
                propertyId,
                null,
                title(customerName, string(row[4]), string(row[5])),
                quoteStateLabel(status) + " · 已停留 " + stayDays + " 天",
                status,
                quoteStateLabel(status),
                20,
                stayDays,
                quoteRiskLevel(status, stayDays)
        );
        item.setNextActionKey("quote/open");
        item.setNextActionLabel("Rejected".equals(status) ? "按反馈调整" : "继续报价");
        item.setRoute("/designer/quotes/" + quoteId + "?activeCustomerId=" + customerId + "&activePropertyId=" + propertyId);
        return item;
    }

    private WorkQueueItem contractQueueItem(Long comId, Long empId) {
        Object[] row = firstRow("""
                SELECT ct.contract_id,
                       ct.customer_id,
                       ct.property_id,
                       ct.quote_id,
                       c.name,
                       p.community,
                       p.address,
                       ct.status,
                       ct.signed_at,
                       ct.create_date_time,
                       TIMESTAMPDIFF(DAY,
                         CASE WHEN ct.status = 'Signed' AND ct.signed_at IS NOT NULL
                              THEN ct.signed_at
                              ELSE STR_TO_DATE(CAST(ct.create_date_time AS CHAR), '%Y%m%d%H%i%s')
                         END,
                         NOW()) AS stay_days
                FROM cust_customer c
                STRAIGHT_JOIN db_property p
                  ON p.com_id = c.com_id
                 AND p.customer_id = c.cust_id
                STRAIGHT_JOIN db_project_contract ct
                  ON ct.com_id = c.com_id
                 AND ct.customer_id = c.cust_id
                WHERE c.com_id = :comId
                  AND c.designer_id = :empId
                  AND ct.status IN ('PendingSign','Signed')
                ORDER BY FIELD(ct.status, 'Signed', 'PendingSign'),
                         stay_days DESC,
                         ct.contract_id DESC
                LIMIT 1
                """, Map.of("comId", comId, "empId", empId));
        if (row == null) return null;

        Long contractId = longValue(row[0]);
        Long customerId = longValue(row[1]);
        Long propertyId = longValue(row[2]);
        Long quoteId = longValue(row[3]);
        String customerName = string(row[4]);
        String status = string(row[7]);
        int stayDays = integer(row[10]);
        WorkQueueItem item = base(
                "designer.queue.contract." + contractId,
                "Contract",
                contractId,
                customerId,
                propertyId,
                null,
                title(customerName, string(row[5]), string(row[6])),
                contractStateLabel(status) + " · 已停留 " + stayDays + " 天",
                status,
                contractStateLabel(status),
                30,
                stayDays,
                contractRiskLevel(stayDays)
        );
        item.setNextActionKey("contract/open");
        item.setNextActionLabel("查看合同交接");
        item.setRoute("/designer/contract?customerId=" + customerId + "&quoteId=" + quoteId + "&contractId=" + contractId);
        return item;
    }

    private WorkQueueItem base(String queueKey,
                               String objectType,
                               Long objectId,
                               Long customerId,
                               Long propertyId,
                               Long projectId,
                               String title,
                               String subtitle,
                               String stateKey,
                               String stateLabel,
                               int priority,
                               int stayDays,
                               String riskLevel) {
        WorkQueueItem item = new WorkQueueItem();
        item.setQueueKey(queueKey);
        item.setObjectType(objectType);
        item.setObjectId(objectId);
        item.setCustomerId(customerId);
        item.setPropertyId(propertyId);
        item.setProjectId(projectId);
        item.setTitle(title);
        item.setSubtitle(subtitle);
        item.setStateKey(stateKey);
        item.setStateLabel(stateLabel);
        item.setOwnerName("我");
        item.setPriority(priority);
        item.setStayDays(stayDays);
        item.setRiskLevel(riskLevel);
        item.setAllowed(Boolean.TRUE);
        return item;
    }

    private String title(String customerName, String community, String address) {
        String place = community != null && !community.isBlank()
                ? community
                : address != null && !address.isBlank() ? address : "房屋待完善";
        return (customerName == null || customerName.isBlank() ? "客户待完善" : customerName) + " · " + place;
    }

    private String stateLabel(String status) {
        return switch (status == null ? "" : status) {
            case "Draft" -> "设计中";
            case "Ready" -> "待谈单";
            case "Presented" -> "已讲解";
            default -> "待推进";
        };
    }

    private String quoteStateLabel(String status) {
        return switch (status == null ? "" : status) {
            case "Draft" -> "报价草稿";
            case "Rejected" -> "需调整";
            default -> "待处理";
        };
    }

    private String contractStateLabel(String status) {
        return switch (status == null ? "" : status) {
            case "PendingSign" -> "待签约";
            case "Signed" -> "已签约未生效";
            default -> "待交接";
        };
    }

    private String riskLevel(String status, int stayDays) {
        if (stayDays >= 7) return "high";
        return "normal";
    }

    private String quoteRiskLevel(String status, int stayDays) {
        if ("Draft".equals(status)) return stayDays >= 7 ? "high" : "normal";
        if ("Rejected".equals(status)) return stayDays >= 1 ? "warning" : "normal";
        return "normal";
    }

    private String contractRiskLevel(int stayDays) {
        if (stayDays >= 7) return "high";
        if (stayDays >= 3) return "warning";
        return "normal";
    }

    private Object[] firstRow(String sql, Map<String, Object> parameters) {
        Query query = entityManager.createNativeQuery(sql);
        parameters.forEach(query::setParameter);
        List<?> rows = query.getResultList();
        if (rows.isEmpty()) return null;
        Object result = rows.get(0);
        return result instanceof Object[] values ? values : new Object[]{result};
    }

    private int integer(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
    }

    private Long longValue(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private LocalDateTime dateTime(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDateTime localDateTime) return localDateTime;
        if (value instanceof java.sql.Timestamp timestamp) return timestamp.toLocalDateTime();
        return null;
    }
}
