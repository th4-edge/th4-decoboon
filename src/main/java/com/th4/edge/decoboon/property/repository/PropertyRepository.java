package com.th4.edge.decoboon.property.repository;

import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 房屋仓储。 */
public interface PropertyRepository extends JpaRepository<Property, Long>,
        JpaSpecificationExecutor<Property> {

    Optional<Property> findByPropertyIdAndCompany_ComId(Long propertyId, Long comId);

    boolean existsByCompany_ComIdAndCustomer_CustIdAndAddress(Long comId, Long custId, String address);

    boolean existsByCompany_ComIdAndCustomer_CustId(Long comId, Long custId);

    Optional<Property> findFirstByCompany_ComIdAndCustomer_CustIdOrderByPropertyIdAsc(Long comId, Long custId);

    List<Property> findByCompany_ComIdAndCustomer_CustIdOrderByPropertyIdAsc(Long comId, Long custId);

    List<Property> findByCompany_ComIdAndCustomer_CustIdIn(Long comId, List<Long> customerIds);

    /** 已由分页查询限定租户与数据范围后，按主键批量装配当前页。 */
    List<Property> findByCompany_ComIdAndPropertyIdIn(Long comId, Collection<Long> propertyIds);

    /**
     * 设计师待量房队列锚点分页。
     *
     * <p>状态、客户/房屋关键词、责任范围和排序均在 SQL 内完成；当前页只返回稳定锚点，
     * Provider 再按锚点批量读取客户、房屋、量房、诉求和方案，避免全量实体回捞。</p>
     */
    @Query(value = """
            WITH measure_stats AS (
                SELECT com_id, property_id,
                       MAX(CASE WHEN status = 'Scheduled' THEN 1 ELSE 0 END) AS has_scheduled,
                       MAX(CASE WHEN status = 'Completed' THEN 1 ELSE 0 END) AS has_completed,
                       MAX(CASE WHEN status = 'Cancelled' THEN 1 ELSE 0 END) AS has_cancelled,
                       MAX(CASE WHEN status = 'Scheduled' THEN COALESCE(scheduled_at, measure_time) END) AS scheduled_at,
                       MAX(CASE WHEN status = 'Completed' THEN COALESCE(completed_at, measure_time) END) AS completed_at,
                       MAX(CASE WHEN status = 'Cancelled' THEN COALESCE(scheduled_at, measure_time) END) AS cancelled_at
                FROM db_measure
                WHERE com_id = :comId
                GROUP BY com_id, property_id
            ), candidate AS (
                SELECT 'property' AS taskType,
                       c.cust_id AS customerId,
                       p.property_id AS propertyId,
                       CASE
                           WHEN COALESCE(ms.has_completed, 0) = 1 THEN 'completed_pending_handoff'
                           WHEN COALESCE(ms.has_scheduled, 0) = 1 AND ms.scheduled_at < :overdueBefore THEN 'overdue'
                           WHEN COALESCE(ms.has_scheduled, 0) = 1 THEN 'scheduled'
                           WHEN COALESCE(ms.has_cancelled, 0) = 1 THEN 'cancelled'
                           ELSE 'not_scheduled'
                       END AS stateKey,
                       CASE
                           WHEN COALESCE(ms.has_completed, 0) = 1 AND ms.completed_at < :handoffBefore THEN 5
                           WHEN COALESCE(ms.has_completed, 0) = 1 THEN 10
                           WHEN COALESCE(ms.has_scheduled, 0) = 1 AND ms.scheduled_at < :overdueBefore THEN 1
                           WHEN COALESCE(ms.has_scheduled, 0) = 1 THEN 8
                           WHEN COALESCE(ms.has_cancelled, 0) = 1 THEN 12
                           ELSE 15
                       END AS priority,
                       CASE
                           WHEN COALESCE(ms.has_completed, 0) = 1 THEN ms.completed_at
                           WHEN COALESCE(ms.has_scheduled, 0) = 1 THEN ms.scheduled_at
                           WHEN COALESCE(ms.has_cancelled, 0) = 1 THEN ms.cancelled_at
                           ELSE COALESCE(c.pending_measure_at, c.measure_at, c.measure_requested_at)
                       END AS sortAt,
                       ms.scheduled_at AS scheduledAt
                FROM db_property p
                INNER JOIN cust_customer c ON c.cust_id = p.customer_id AND c.com_id = p.com_id
                LEFT JOIN measure_stats ms ON ms.com_id = p.com_id AND ms.property_id = p.property_id
                WHERE p.com_id = :comId
                  AND c.status IN ('New', 'Following', 'PendingMeasure', 'Quoted')
                  AND NOT EXISTS (
                      SELECT 1 FROM cust_customer_loss cl
                      WHERE cl.com_id = p.com_id
                        AND cl.property_id = p.property_id
                        AND cl.restored_at IS NULL
                  )
                  AND (:boss = TRUE OR c.emp_id = :empId OR c.designer_id = :empId)
                  AND (
                      COALESCE(ms.has_scheduled, 0) = 1
                      OR COALESCE(ms.has_completed, 0) = 1
                      OR COALESCE(ms.has_cancelled, 0) = 1
                      OR c.status = 'PendingMeasure'
                      OR p.status IN ('PendingMeasure', 'Measuring')
                      OR c.measure_at IS NOT NULL
                      OR c.measure_requested_at IS NOT NULL
                      OR c.pending_measure_at IS NOT NULL
                  )
                  AND NOT (
                      COALESCE(ms.has_completed, 0) = 1
                      AND EXISTS (
                          SELECT 1 FROM db_design d
                          WHERE d.com_id = p.com_id AND d.property_id = p.property_id
                      )
                  )
                  AND (
                      :keyword IS NULL OR :keyword = ''
                      OR LOWER(CONCAT_WS(' ', c.name, c.tel_no, c.community, p.community,
                                         p.building, p.room_no, p.address, p.property_no))
                         LIKE CONCAT('%', LOWER(:keyword), '%')
                  )
                UNION ALL
                SELECT 'customer' AS taskType,
                       c.cust_id AS customerId,
                       NULL AS propertyId,
                       'missing_property' AS stateKey,
                       20 AS priority,
                       COALESCE(c.pending_measure_at, c.measure_at, c.measure_requested_at) AS sortAt,
                       NULL AS scheduledAt
                FROM cust_customer c
                WHERE c.com_id = :comId
                  AND c.status IN ('New', 'Following', 'PendingMeasure', 'Quoted')
                  AND (:boss = TRUE OR c.emp_id = :empId OR c.designer_id = :empId)
                  AND (
                      c.status = 'PendingMeasure'
                      OR c.measure_at IS NOT NULL
                      OR c.measure_requested_at IS NOT NULL
                      OR c.pending_measure_at IS NOT NULL
                  )
                  AND NOT EXISTS (
                      SELECT 1 FROM db_property p
                      WHERE p.com_id = c.com_id AND p.customer_id = c.cust_id
                  )
                  AND (
                      :keyword IS NULL OR :keyword = ''
                      OR LOWER(CONCAT_WS(' ', c.name, c.tel_no, c.community, c.addr))
                         LIKE CONCAT('%', LOWER(:keyword), '%')
                  )
            )
            SELECT taskType, customerId, propertyId, stateKey, priority, sortAt, scheduledAt
            FROM candidate
            WHERE (
                :state IS NULL OR :state = ''
                OR stateKey = :state
                OR (:state = 'scheduled' AND stateKey IN ('scheduled', 'overdue'))
                OR (:state = 'notScheduled' AND stateKey IN ('missing_property', 'not_scheduled', 'cancelled'))
                OR (:state = 'handoff' AND stateKey = 'completed_pending_handoff')
                OR (:state = 'today' AND DATE(scheduledAt) = CURRENT_DATE())
            )
            ORDER BY priority ASC, sortAt ASC, customerId DESC, propertyId DESC
            """, countQuery = """
            WITH measure_stats AS (
                SELECT com_id, property_id,
                       MAX(CASE WHEN status = 'Scheduled' THEN 1 ELSE 0 END) AS has_scheduled,
                       MAX(CASE WHEN status = 'Completed' THEN 1 ELSE 0 END) AS has_completed,
                       MAX(CASE WHEN status = 'Cancelled' THEN 1 ELSE 0 END) AS has_cancelled,
                       MAX(CASE WHEN status = 'Scheduled' THEN COALESCE(scheduled_at, measure_time) END) AS scheduled_at,
                       MAX(CASE WHEN status = 'Completed' THEN COALESCE(completed_at, measure_time) END) AS completed_at,
                       MAX(CASE WHEN status = 'Cancelled' THEN COALESCE(scheduled_at, measure_time) END) AS cancelled_at
                FROM db_measure
                WHERE com_id = :comId
                GROUP BY com_id, property_id
            ), candidate AS (
                SELECT 'property' AS taskType,
                       CASE
                           WHEN COALESCE(ms.has_completed, 0) = 1 THEN 'completed_pending_handoff'
                           WHEN COALESCE(ms.has_scheduled, 0) = 1 AND ms.scheduled_at < :overdueBefore THEN 'overdue'
                           WHEN COALESCE(ms.has_scheduled, 0) = 1 THEN 'scheduled'
                           WHEN COALESCE(ms.has_cancelled, 0) = 1 THEN 'cancelled'
                           ELSE 'not_scheduled'
                       END AS stateKey,
                       ms.scheduled_at AS scheduledAt
                FROM db_property p
                INNER JOIN cust_customer c ON c.cust_id = p.customer_id AND c.com_id = p.com_id
                LEFT JOIN measure_stats ms ON ms.com_id = p.com_id AND ms.property_id = p.property_id
                WHERE p.com_id = :comId
                  AND c.status IN ('New', 'Following', 'PendingMeasure', 'Quoted')
                  AND NOT EXISTS (
                      SELECT 1 FROM cust_customer_loss cl
                      WHERE cl.com_id = p.com_id
                        AND cl.property_id = p.property_id
                        AND cl.restored_at IS NULL
                  )
                  AND (:boss = TRUE OR c.emp_id = :empId OR c.designer_id = :empId)
                  AND (
                      COALESCE(ms.has_scheduled, 0) = 1
                      OR COALESCE(ms.has_completed, 0) = 1
                      OR COALESCE(ms.has_cancelled, 0) = 1
                      OR c.status = 'PendingMeasure'
                      OR p.status IN ('PendingMeasure', 'Measuring')
                      OR c.measure_at IS NOT NULL
                      OR c.measure_requested_at IS NOT NULL
                      OR c.pending_measure_at IS NOT NULL
                  )
                  AND NOT (
                      COALESCE(ms.has_completed, 0) = 1
                      AND EXISTS (
                          SELECT 1 FROM db_design d
                          WHERE d.com_id = p.com_id AND d.property_id = p.property_id
                      )
                  )
                  AND (
                      :keyword IS NULL OR :keyword = ''
                      OR LOWER(CONCAT_WS(' ', c.name, c.tel_no, c.community, p.community,
                                         p.building, p.room_no, p.address, p.property_no))
                         LIKE CONCAT('%', LOWER(:keyword), '%')
                  )
                UNION ALL
                SELECT 'customer' AS taskType, 'missing_property' AS stateKey, NULL AS scheduledAt
                FROM cust_customer c
                WHERE c.com_id = :comId
                  AND c.status IN ('New', 'Following', 'PendingMeasure', 'Quoted')
                  AND (:boss = TRUE OR c.emp_id = :empId OR c.designer_id = :empId)
                  AND (
                      c.status = 'PendingMeasure'
                      OR c.measure_at IS NOT NULL
                      OR c.measure_requested_at IS NOT NULL
                      OR c.pending_measure_at IS NOT NULL
                  )
                  AND NOT EXISTS (
                      SELECT 1 FROM db_property p
                      WHERE p.com_id = c.com_id AND p.customer_id = c.cust_id
                  )
                  AND (
                      :keyword IS NULL OR :keyword = ''
                      OR LOWER(CONCAT_WS(' ', c.name, c.tel_no, c.community, c.addr))
                         LIKE CONCAT('%', LOWER(:keyword), '%')
                  )
            )
            SELECT COUNT(*)
            FROM candidate
            WHERE (
                :state IS NULL OR :state = ''
                OR stateKey = :state
                OR (:state = 'scheduled' AND stateKey IN ('scheduled', 'overdue'))
                OR (:state = 'notScheduled' AND stateKey IN ('missing_property', 'not_scheduled', 'cancelled'))
                OR (:state = 'handoff' AND stateKey = 'completed_pending_handoff')
                OR (:state = 'today' AND DATE(scheduledAt) = CURRENT_DATE())
            )
            """, nativeQuery = true)
    Page<MeasureTaskAnchor> findMeasureTaskAnchors(@Param("comId") Long comId,
                                                    @Param("empId") Long empId,
                                                    @Param("boss") boolean boss,
                                                    @Param("keyword") String keyword,
                                                    @Param("state") String state,
                                                    @Param("overdueBefore") LocalDateTime overdueBefore,
                                                    @Param("handoffBefore") LocalDateTime handoffBefore,
                                                    Pageable pageable);

    /**
     * 设计师设计工作台锚点分页：资格、责任范围、关键词、设计状态和排序均在 SQL 完成。
     */
    @Query(value = """
            WITH measure_stats AS (
                SELECT com_id, property_id,
                       MAX(CASE WHEN status = 'Completed' THEN COALESCE(completed_at, measure_time) END) AS measure_at
                FROM db_measure
                WHERE com_id = :comId
                GROUP BY com_id, property_id
            ), design_stats AS (
                SELECT com_id, property_id,
                       COUNT(*) AS design_count,
                       MAX(CASE WHEN status <> 'Draft' THEN 1 ELSE 0 END) AS has_non_draft
                FROM db_design
                WHERE com_id = :comId
                GROUP BY com_id, property_id
            )
            SELECT p.property_id AS propertyId,
                   COALESCE(ms.measure_at, p.latest_measure_at) AS measureAt,
                   CASE
                       WHEN COALESCE(ds.design_count, 0) = 0 THEN 'Pending'
                       WHEN COALESCE(ds.has_non_draft, 0) = 1 THEN 'Done'
                       ELSE 'Doing'
                   END AS designStatus
            FROM db_property p
            INNER JOIN cust_customer c ON c.cust_id = p.customer_id AND c.com_id = p.com_id
            LEFT JOIN measure_stats ms ON ms.com_id = p.com_id AND ms.property_id = p.property_id
            LEFT JOIN design_stats ds ON ds.com_id = p.com_id AND ds.property_id = p.property_id
            WHERE p.com_id = :comId
              AND c.status IN ('New', 'Following', 'PendingMeasure', 'Quoted')
              AND NOT EXISTS (
                  SELECT 1 FROM cust_customer_loss cl
                  WHERE cl.com_id = p.com_id
                    AND cl.property_id = p.property_id
                    AND cl.restored_at IS NULL
              )
              AND p.status NOT IN ('Contracted', 'Serving', 'Completed')
              AND COALESCE(ms.measure_at, p.latest_measure_at) IS NOT NULL
              AND NOT EXISTS (
                  SELECT 1 FROM quote q
                  WHERE q.com_id = p.com_id AND q.property_id = p.property_id
                    AND q.status IN ('Accepted', 'Converted')
              )
              AND (
                  :boss = TRUE
                  OR (:storeManager = TRUE AND p.store_id = :storeId)
                  OR (:storeManager = FALSE AND (p.designer_id = :empId OR c.designer_id = :empId))
              )
              AND (
                  :keyword IS NULL OR :keyword = ''
                  OR LOWER(CONCAT_WS(' ', c.name, c.tel_no, c.community, p.community, p.address, p.property_no))
                     LIKE CONCAT('%', LOWER(:keyword), '%')
              )
              AND (
                  :filterValue IS NULL OR :filterValue = '' OR :filterValue = 'all'
                  OR (:filterValue = 'Pending' AND COALESCE(ds.design_count, 0) = 0)
                  OR (:filterValue = 'Doing' AND COALESCE(ds.design_count, 0) > 0 AND COALESCE(ds.has_non_draft, 0) = 0)
                  OR (:filterValue = 'Done' AND COALESCE(ds.has_non_draft, 0) = 1)
              )
            ORDER BY COALESCE(ms.measure_at, p.latest_measure_at) ASC, p.property_id DESC
            """, countQuery = """
            WITH measure_stats AS (
                SELECT com_id, property_id,
                       MAX(CASE WHEN status = 'Completed' THEN COALESCE(completed_at, measure_time) END) AS measure_at
                FROM db_measure
                WHERE com_id = :comId
                GROUP BY com_id, property_id
            ), design_stats AS (
                SELECT com_id, property_id,
                       COUNT(*) AS design_count,
                       MAX(CASE WHEN status <> 'Draft' THEN 1 ELSE 0 END) AS has_non_draft
                FROM db_design
                WHERE com_id = :comId
                GROUP BY com_id, property_id
            )
            SELECT COUNT(*)
            FROM db_property p
            INNER JOIN cust_customer c ON c.cust_id = p.customer_id AND c.com_id = p.com_id
            LEFT JOIN measure_stats ms ON ms.com_id = p.com_id AND ms.property_id = p.property_id
            LEFT JOIN design_stats ds ON ds.com_id = p.com_id AND ds.property_id = p.property_id
            WHERE p.com_id = :comId
              AND c.status IN ('New', 'Following', 'PendingMeasure', 'Quoted')
              AND NOT EXISTS (
                  SELECT 1 FROM cust_customer_loss cl
                  WHERE cl.com_id = p.com_id
                    AND cl.property_id = p.property_id
                    AND cl.restored_at IS NULL
              )
              AND p.status NOT IN ('Contracted', 'Serving', 'Completed')
              AND COALESCE(ms.measure_at, p.latest_measure_at) IS NOT NULL
              AND NOT EXISTS (
                  SELECT 1 FROM quote q
                  WHERE q.com_id = p.com_id AND q.property_id = p.property_id
                    AND q.status IN ('Accepted', 'Converted')
              )
              AND (
                  :boss = TRUE
                  OR (:storeManager = TRUE AND p.store_id = :storeId)
                  OR (:storeManager = FALSE AND (p.designer_id = :empId OR c.designer_id = :empId))
              )
              AND (
                  :keyword IS NULL OR :keyword = ''
                  OR LOWER(CONCAT_WS(' ', c.name, c.tel_no, c.community, p.community, p.address, p.property_no))
                     LIKE CONCAT('%', LOWER(:keyword), '%')
              )
              AND (
                  :filterValue IS NULL OR :filterValue = '' OR :filterValue = 'all'
                  OR (:filterValue = 'Pending' AND COALESCE(ds.design_count, 0) = 0)
                  OR (:filterValue = 'Doing' AND COALESCE(ds.design_count, 0) > 0 AND COALESCE(ds.has_non_draft, 0) = 0)
                  OR (:filterValue = 'Done' AND COALESCE(ds.has_non_draft, 0) = 1)
              )
            """, nativeQuery = true)
    Page<DesignTaskAnchor> findDesignTaskAnchors(@Param("comId") Long comId,
                                                  @Param("empId") Long empId,
                                                  @Param("boss") boolean boss,
                                                  @Param("storeManager") boolean storeManager,
                                                  @Param("storeId") Long storeId,
                                                  @Param("keyword") String keyword,
                                                  @Param("filterValue") String filterValue,
                                                  Pageable pageable);

    interface DesignTaskAnchor {
        Long getPropertyId();
        LocalDateTime getMeasureAt();
        String getDesignStatus();
    }

    /** 待量房队列只需要的稳定锚点，避免返回实体和派生字段。 */
    interface MeasureTaskAnchor {
        String getTaskType();
        Long getCustomerId();
        Long getPropertyId();
        String getStateKey();
        Integer getPriority();
        LocalDateTime getSortAt();
    }

    List<Property> findByCompany_ComIdAndStore_StoreIdOrderByPropertyIdDesc(Long comId, Long storeId);

    List<Property> findByCompany_ComIdAndTeam_TeamIdOrderByPropertyIdDesc(Long comId, Long teamId);

    List<Property> findByCompany_ComIdAndOwner_EmpIdOrderByPropertyIdDesc(Long comId, Long empId);
    List<Property> findByCompany_ComIdAndDesigner_EmpIdOrderByPropertyIdDesc(Long comId, Long empId);
    List<Property> findByCompany_ComIdAndSalesman_EmpIdOrderByPropertyIdDesc(Long comId, Long empId);
}
