package com.th4.edge.decoboon.customer.context;

import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.configure.TagCategory;
import com.th4.edge.decoboon.customer.configure.TagScope;
import com.th4.edge.decoboon.customer.configure.TagStatus;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerTag;
import com.th4.edge.decoboon.customer.model.CustomerTagLink;
import com.th4.edge.decoboon.customer.repository.CustomerTagLinkRepository;
import com.th4.edge.framework.core.annotation.Note;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 客户标签装配上下文。
 *
 * <p>系统标签由客户事实派生、不落库（决策 B4）；人工标签读取
 * {@code cust_tag_link} 关联。两者合并后按分类稳定输出，供 page/info 复用，
 * 避免各 Provider 各写一套口径。</p>
 */
@Note("客户标签装配上下文")
@Component
public class CustomerTagContext {

    /** 系统标签不落库，使用稳定的负数占位标识，前端以 key 为准。 */
    private static final long SYSTEM_TAG_BASE = -1L;

    private final CustomerTagLinkRepository linkRepository;

    public CustomerTagContext(CustomerTagLinkRepository linkRepository) {
        this.linkRepository = linkRepository;
    }

    /** 组装单个客户的系统标签 + 人工标签。 */
    public List<CustomerTag> assemble(Customer customer, boolean hasMeasure, boolean hasMeasureScheduled) {
        List<CustomerTag> tags = new ArrayList<>(systemTags(customer, hasMeasure, hasMeasureScheduled));
        if (customer != null && customer.getCustId() != null && customer.getCompany() != null) {
            List<CustomerTagLink> links = linkRepository
                    .findByCompany_ComIdAndCustomer_CustId(customer.getCompany().getComId(), customer.getCustId());
            links.stream()
                    .map(CustomerTagLink::getTag)
                    .filter(tag -> tag != null && tag.getStatus() == TagStatus.Enabled)
                    .forEach(tags::add);
        }
        return dedupe(tags);
    }

    /** 批量装配，避免列表页 N+1。 */
    public Map<Long, List<CustomerTag>> assembleBatch(Long comId, Collection<Customer> customers,
                                                      Set<Long> measuredCustomerIds,
                                                      Set<Long> scheduledCustomerIds) {
        Map<Long, List<CustomerTag>> result = new LinkedHashMap<>();
        if (customers == null || customers.isEmpty()) return result;
        List<Long> customerIds = customers.stream()
                .map(Customer::getCustId)
                .filter(java.util.Objects::nonNull)
                .toList();
        Map<Long, List<CustomerTag>> manual = customerIds.isEmpty()
                ? Map.of()
                : linkRepository.findByCompany_ComIdAndCustomer_CustIdIn(comId, customerIds).stream()
                        .filter(link -> link.getTag() != null && link.getTag().getStatus() == TagStatus.Enabled)
                        .collect(Collectors.groupingBy(
                                link -> link.getCustomer().getCustId(),
                                Collectors.mapping(CustomerTagLink::getTag, Collectors.toList())));
        for (Customer customer : customers) {
            if (customer == null || customer.getCustId() == null) continue;
            boolean hasMeasure = measuredCustomerIds != null && measuredCustomerIds.contains(customer.getCustId());
            boolean hasMeasureScheduled = !hasMeasure && scheduledCustomerIds != null
                    && scheduledCustomerIds.contains(customer.getCustId());
            List<CustomerTag> tags = new ArrayList<>(systemTags(customer, hasMeasure, hasMeasureScheduled));
            tags.addAll(manual.getOrDefault(customer.getCustId(), List.of()));
            result.put(customer.getCustId(), dedupe(tags));
        }
        return result;
    }

    /** 由客户事实派生的系统标签（只读，不入库）。 */
    public List<CustomerTag> systemTags(Customer customer, boolean hasMeasure, boolean hasMeasureScheduled) {
        List<CustomerTag> tags = new ArrayList<>();
        if (customer == null) return tags;
        int seq = 0;

        if (customer.getSource() != null) {
            tags.add(systemTag(--seq, customer.getSource().getName(), TagCategory.Source, "source:" + customer.getSource().name()));
        }
        if (Boolean.TRUE.equals(customer.getWechatAdded())) {
            tags.add(systemTag(--seq, "已加微信", TagCategory.Behavior, "wechat"));
        }
        int visits = customer.getVisitCount() == null ? 0 : customer.getVisitCount();
        if (visits >= 2) {
            tags.add(systemTag(--seq, "多次到店", TagCategory.Behavior, "visit:multi"));
        } else if (visits == 1) {
            tags.add(systemTag(--seq, "已到店", TagCategory.Behavior, "visit:once"));
        }
        // 只有当前房屋存在 Completed 量房事实才算"已量房"；预约态单独表达，
        // 禁止用客户级 measureAt（预约时间）推断量房完成。
        if (hasMeasure) {
            tags.add(systemTag(--seq, "已量房", TagCategory.Behavior, "measured"));
        } else if (hasMeasureScheduled) {
            tags.add(systemTag(--seq, "已预约量房", TagCategory.Behavior, "measure:scheduled"));
        }
        if (isFollowOverdue(customer)) {
            tags.add(systemTag(--seq, "跟进已逾期", TagCategory.Risk, "risk:followOverdue"));
        }
        if (isGoldenNoContact(customer)) {
            tags.add(systemTag(--seq, "黄金期未首联", TagCategory.Risk, "risk:goldenNoContact"));
        }
        return tags;
    }

    /** 跟进是否已逾期（自然日口径）。 */
    public boolean isFollowOverdue(Customer customer) {
        if (customer == null || customer.getNextFollowAt() == null) return false;
        if (isClosed(customer.getStatus())) return false;
        return customer.getNextFollowAt().toLocalDate().isBefore(LocalDate.now());
    }

    /** 建档 ≤3 自然日且尚未首联。 */
    public boolean isGoldenNoContact(Customer customer) {
        if (customer == null || customer.getCreateTime() == null || customer.getLastFollowAt() != null) return false;
        if (isClosed(customer.getStatus())) return false;
        Long createdMillis = customer.getCreateTime().getDateTime();
        if (createdMillis == null) return false;
        LocalDate createdDate = java.time.LocalDateTime
                .ofInstant(java.time.Instant.ofEpochMilli(createdMillis), java.time.ZoneId.systemDefault())
                .toLocalDate();
        long days = java.time.temporal.ChronoUnit.DAYS.between(createdDate, LocalDate.now());
        return days >= 0 && days <= 3;
    }

    private boolean isClosed(CustStatus status) {
        return status == CustStatus.Lost || status == CustStatus.Finished;
    }

    private CustomerTag systemTag(long id, String name, TagCategory category, String key) {
        CustomerTag tag = new CustomerTag();
        tag.setTagId(id);
        tag.setName(name);
        tag.setCategory(category);
        tag.setScope(TagScope.System);
        tag.setStatus(TagStatus.Enabled);
        tag.setSort(0);
        tag.setKey(key);
        return tag;
    }

    private List<CustomerTag> dedupe(List<CustomerTag> tags) {
        Map<Long, CustomerTag> unique = new LinkedHashMap<>();
        for (CustomerTag tag : tags) {
            if (tag == null || tag.getTagId() == null) continue;
            unique.putIfAbsent(tag.getTagId(), tag);
        }
        return new ArrayList<>(unique.values());
    }
}
