package com.th4.edge.decoboon.customer.service.follows.save.support;

import com.th4.edge.components.lock.DistributedLock;
import com.th4.edge.decoboon.common.file.context.FileSignatureContext;
import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.configure.FollowWay;
import com.th4.edge.decoboon.customer.context.CustomerContext;
import com.th4.edge.decoboon.customer.context.CustomerDormantContext;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerFollow;
import com.th4.edge.decoboon.customer.repository.CustomerFollowRepository;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** 新增客户跟进实现。 */
@Note("新增客户跟进")
@Properties(allow = {
        @AllowProperties(model = CustomerFollow.class,
                properties = {"followId", "customer", "emp", "way", "content", "nextFollowAt",
                        "images", "imageUrls", "imageCount", "requestId", "createTime"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name"})
})
@Service("customer/customer-follow/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.customer.service.follows.save.Save {

    private static final int MAX_IMAGES = 9;

    @User
    private Employee employee;

    @AutoInject
    @NotNull
    private Long customerId;

    @AutoInject
    private FollowWay way;

    @AutoInject
    @NotEmpty
    @Length(max = 512)
    private String content;

    @AutoInject
    private String nextFollowAt;

    @AutoInject
    private List<String> images;

    @AutoInject
    @Length(max = 64)
    private String requestId;

    private final CustomerFollowRepository followRepository;
    private final CustomerRepository customerRepository;
    private final CustomerContext customerContext;
    private final CustomerDormantContext dormantContext;
    private final DataScopeContext dataScopeContext;
    private final DistributedLock distributedLock;
    private final FileSignatureContext signatureContext;

    public Save(CustomerFollowRepository followRepository,
                CustomerRepository customerRepository,
                CustomerContext customerContext,
                CustomerDormantContext dormantContext,
                DataScopeContext dataScopeContext,
                DistributedLock distributedLock,
                FileSignatureContext signatureContext) {
        this.followRepository = followRepository;
        this.customerRepository = customerRepository;
        this.customerContext = customerContext;
        this.dormantContext = dormantContext;
        this.dataScopeContext = dataScopeContext;
        this.distributedLock = distributedLock;
        this.signatureContext = signatureContext;
    }

    @Override
    @Transactional
    public CustomerFollow call(CustomerFollow request) {
        Employee current = dataScopeContext.requireEmployee(employee);
        String normalizedRequestId = normalizeRequestId(requestId);
        List<String> normalizedImages = normalizeImages(images, current.getCompany().getComId());

        CustomerFollow replayed = replay(current, normalizedRequestId, normalizedImages);
        if (replayed != null) return replayed;

        String lockKey = "customer-status:" + current.getCompany().getComId() + ":" + customerId;
        if (!distributedLock.tryLock(lockKey, 1000, 30000)) {
            throw new DefinedException(new DefaultErrorCode("409", "该客户正在被处理，请稍后重试"));
        }
        try {
            CustomerFollow replayedAfterLock = replay(current, normalizedRequestId, normalizedImages);
            if (replayedAfterLock != null) return replayedAfterLock;
            ensureImagesUnused(normalizedImages);

            Customer customer = customerContext.requireVisible(customerId, current);
            CustomerFollow follow = new CustomerFollow();
            follow.setCompany(current.getCompany());
            follow.setStore(customer.getStore());
            follow.setTeam(customer.getTeam());
            follow.setCustomer(customer);
            follow.setEmp(current);
            follow.setWay(way == null ? FollowWay.Other : way);
            follow.setContent(content);
            follow.setImages(normalizedImages);
            follow.setRequestId(normalizedRequestId);
            LocalDateTime next = parseDate(nextFollowAt);
            follow.setNextFollowAt(next);
            if (customer.getStatus() == CustStatus.Dormant) {
                customer = dormantContext.wakeByFollow(customer, current);
            }
            customer.setLastFollowAt(LocalDateTime.now());
            customer.setNextFollowAt(next);
            if (customer.getStatus() == CustStatus.New) {
                customer.setStatus(CustStatus.Following);
            }
            customerRepository.save(customer);
            CustomerFollow saved = followRepository.save(follow);
            saved.setImageUrls(buildImageUrls(saved.getImages()));
            return saved;
        } finally {
            distributedLock.unlock(lockKey);
        }
    }

    private CustomerFollow replay(Employee current, String requestId, List<String> images) {
        Optional<CustomerFollow> existing = followRepository
                .findByCompany_ComIdAndRequestId(current.getCompany().getComId(), requestId);
        if (existing.isEmpty()) return null;
        CustomerFollow follow = existing.get();
        FollowWay expectedWay = way == null ? FollowWay.Other : way;
        LocalDateTime expectedNext = parseDateForReplay(nextFollowAt);
        if (!Objects.equals(customerId, follow.getCustomer().getCustId())
                || follow.getWay() != expectedWay
                || !Objects.equals(content, follow.getContent())
                || !Objects.equals(expectedNext, follow.getNextFollowAt())
                || !Objects.equals(images, safeImages(follow.getImages()))) {
            throw new DefinedException(new DefaultErrorCode("409", "请求已处理，请刷新后重试"));
        }
        follow.setImageUrls(buildImageUrls(follow.getImages()));
        return follow;
    }

    private String normalizeRequestId(String value) {
        if (value == null || value.isBlank()) {
            // 兼容旧客户端：旧版本不传 requestId 时仍可保存，但不具备重放幂等。
            return "auto-" + UUID.randomUUID();
        }
        return value.trim();
    }

    private List<String> normalizeImages(List<String> values, Long companyId) {
        if (values == null || values.isEmpty()) return new ArrayList<>();
        if (values.size() > MAX_IMAGES) {
            throw new DefinedException(new DefaultErrorCode("400", "单条跟进最多上传 9 张图片"));
        }
        String prefix = "companies/" + companyId + "/files/customerfollow/";
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                throw new DefinedException(new DefaultErrorCode("400", "图片路径不能为空"));
            }
            String path = value.trim();
            if (path.length() > 512) {
                throw new DefinedException(new DefaultErrorCode("400", "图片路径过长"));
            }
            if (!path.startsWith(prefix)) {
                throw new DefinedException(new DefaultErrorCode("403", "图片不属于当前企业或无权使用"));
            }
            if (!unique.add(path)) {
                throw new DefinedException(new DefaultErrorCode("400", "同一跟进不能重复添加同一张图片"));
            }
        }
        return new ArrayList<>(unique);
    }

    private void ensureImagesUnused(List<String> images) {
        for (String image : images) {
            if (followRepository.existsByImagePath(image)) {
                throw new DefinedException(new DefaultErrorCode("400", "图片已被其他跟进使用"));
            }
        }
    }

    private List<String> buildImageUrls(List<String> images) {
        if (images == null || images.isEmpty()) return new ArrayList<>();
        return images.stream().map(signatureContext::sign).toList();
    }

    private List<String> safeImages(List<String> images) {
        return images == null || images.isEmpty() ? new ArrayList<>() : new ArrayList<>(images);
    }

    private LocalDateTime parseDate(String value) {
        LocalDateTime parsed = parseDateForReplay(value);
        if (parsed != null && parsed.toLocalDate().isBefore(LocalDate.now())) {
            throw new DefinedException(new DefaultErrorCode("400", "下次跟进时间不能早于今天"));
        }
        return parsed;
    }

    /** 幂等重放时允许读取历史日期，不再用“不能早于今天”重判有效请求。 */
    private LocalDateTime parseDateForReplay(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return value.length() == 10
                    ? LocalDate.parse(value).atStartOfDay()
                    : LocalDateTime.parse(value.replace(' ', 'T'));
        } catch (java.time.format.DateTimeParseException error) {
            throw new DefinedException(new DefaultErrorCode("400", "下次跟进时间格式不正确"));
        }
    }
}
