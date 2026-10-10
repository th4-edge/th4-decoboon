package com.th4.edge.decoboon.customer.service.follows.page.support;

import com.th4.edge.decoboon.common.file.context.FileSignatureContext;
import com.th4.edge.decoboon.customer.context.CustomerContext;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerFollow;
import com.th4.edge.decoboon.customer.repository.CustomerFollowRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 客户跟进分页实现。 */
@Note("客户跟进分页")
@Properties(allow = {
        @AllowProperties(model = CustomerFollow.class,
                properties = {"followId", "customer", "emp", "way", "content", "nextFollowAt",
                        "images", "imageUrls", "imageCount", "requestId", "createTime"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name"})
})
@Service("customer/customer-follow/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.customer.service.follows.page.Page {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    @AutoInject
    private Long customerId;

    @AutoInject
    private Integer current;

    @AutoInject
    private Integer size;

    @User
    private Employee employee;

    private final CustomerFollowRepository followRepository;
    private final CustomerContext customerContext;
    private final FileSignatureContext signatureContext;

    public Page(CustomerFollowRepository followRepository,
                CustomerContext customerContext,
                FileSignatureContext signatureContext) {
        this.followRepository = followRepository;
        this.customerContext = customerContext;
        this.signatureContext = signatureContext;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<CustomerFollow> call(CustomerFollow request) {
        if (customerId == null) {
            throw new DefinedException(new DefaultErrorCode("400", "客户索引不能为空"));
        }
        customerContext.requireVisible(customerId, employee);
        int pageNumber = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        org.springframework.data.domain.Page<CustomerFollow> page = followRepository.findPageForDisplay(
                customerId, PageRequest.of(pageNumber, pageSize));
        if (page.isEmpty()) return page;

        List<Long> followIds = page.getContent().stream()
                .map(CustomerFollow::getFollowId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, List<String>> imagesByFollow = new LinkedHashMap<>();
        if (!followIds.isEmpty()) {
            for (CustomerFollow follow : followRepository.findWithImagesByFollowIds(followIds)) {
                imagesByFollow.put(follow.getFollowId(), safeImages(follow.getImages()));
            }
        }
        List<CustomerFollow> content = page.getContent().stream()
                .map(follow -> {
                    follow.setImages(imagesByFollow.getOrDefault(follow.getFollowId(), new ArrayList<>()));
                    follow.setImageUrls(follow.getImages().stream().map(signatureContext::sign).toList());
                    return follow;
                })
                .toList();
        return new PageImpl<>(content, page.getPageable(), page.getTotalElements());
    }

    private List<String> safeImages(List<String> images) {
        return images == null || images.isEmpty() ? new ArrayList<>() : new ArrayList<>(images);
    }
}
