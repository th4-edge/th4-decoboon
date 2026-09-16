package com.th4.edge.tenant.billing.context;

import com.th4.edge.components.data.context.ModelIdentityContext;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.date.model.DateTime;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.billing.configure.LedgerBizType;
import com.th4.edge.tenant.billing.configure.OrderItemType;
import com.th4.edge.tenant.billing.configure.OrderStatus;
import com.th4.edge.tenant.billing.configure.PayChannel;
import com.th4.edge.tenant.billing.configure.PlanAddonType;
import com.th4.edge.tenant.billing.configure.PlanModule;
import com.th4.edge.tenant.billing.configure.SubStatus;
import com.th4.edge.tenant.billing.model.BalanceLedger;
import com.th4.edge.tenant.billing.model.Order;
import com.th4.edge.tenant.billing.model.OrderItem;
import com.th4.edge.tenant.billing.model.Plan;
import com.th4.edge.tenant.billing.model.PlanAddon;
import com.th4.edge.tenant.billing.model.PlanVersion;
import com.th4.edge.tenant.billing.model.Subscription;
import com.th4.edge.tenant.billing.repository.BalanceLedgerRepository;
import com.th4.edge.tenant.billing.repository.OrderItemRepository;
import com.th4.edge.tenant.billing.repository.OrderRepository;
import com.th4.edge.tenant.billing.repository.PlanAddonRepository;
import com.th4.edge.tenant.billing.repository.PlanRepository;
import com.th4.edge.tenant.billing.repository.PlanVersionRepository;
import com.th4.edge.tenant.billing.repository.SubscriptionRepository;
import com.th4.edge.tenant.user.model.Company;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 企业订阅上下文。
 *
 * <p>负责正式套餐初始化、试用发放、订单创建、支付到账履约、套餐版本快照、
 * 剩余天数计算和订阅额度维护。</p>
 */
@Note("企业订阅上下文")
@Component
public class SubscriptionContext {

    private static final long DAY_SECONDS = 24L * 60L * 60L;
    private static final int TRIAL_DAYS = 14;
    private static final DateTimeFormatter ORDER_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PlanRepository planRepository;
    private final PlanVersionRepository planVersionRepository;
    private final PlanAddonRepository planAddonRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final BalanceLedgerRepository balanceLedgerRepository;
    private final ModelIdentityContext modelIdentityContext;

    public SubscriptionContext(PlanRepository planRepository,
                               PlanVersionRepository planVersionRepository,
                               PlanAddonRepository planAddonRepository,
                               SubscriptionRepository subscriptionRepository,
                               OrderRepository orderRepository,
                               OrderItemRepository orderItemRepository,
                               BalanceLedgerRepository balanceLedgerRepository,
                               ModelIdentityContext modelIdentityContext) {
        this.planRepository = planRepository;
        this.planVersionRepository = planVersionRepository;
        this.planAddonRepository = planAddonRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.balanceLedgerRepository = balanceLedgerRepository;
        this.modelIdentityContext = modelIdentityContext;
    }

    /**
     * 初始化正式套餐、加购商品和历史套餐快照。
     */
    @Transactional
    public void initializePlans() {
        snapshotLegacyPlans();
        initializePlan(PlanDefinition.free());
        initializePlan(PlanDefinition.basic());
        initializePlan(PlanDefinition.pro());
        initializePlan(PlanDefinition.flagship());
        initializeAddons();
    }

    /**
     * 注册企业发放 14 天基础版试用。试用不创建订单，但使用基础版版本快照。
     */
    @Transactional
    public Subscription grantTrial(Company company) {
        initializePlans();
        PlanVersion version = requireVersion(PlanDefinition.BASIC_CODE);

        Subscription subscription = subscriptionRepository.findByCompany_ComId(company.getComId());
        if (subscription == null) {
            subscription = new Subscription();
            subscription.setCompany(company);
            subscription.setTrialActivated(false);
        }
        if (Boolean.TRUE.equals(subscription.getTrialActivated())) {
            throw new DefinedException(new DefaultErrorCode("400", "该企业已领取过免费试用"));
        }

        subscription.setPlan(version.getPlan());
        subscription.setPlanVersion(version);
        subscription.setStatus(SubStatus.Trial);
        subscription.setServiceEndTime(new DateTime(LocalDateTime.now().plusDays(TRIAL_DAYS)));
        subscription.setTrialActivated(true);
        normalizeExtra(subscription);
        subscriptionRepository.save(modelIdentityContext.prepare(subscription));

        writeLedger(company, null, LedgerBizType.TrialGrant, TRIAL_DAYS,
                remainingDays(subscription), "注册赠送 14 天基础版试用");
        return subscription;
    }

    /**
     * 创建套餐、席位包和门店包订单。
     */
    @Transactional
    public Order createOrder(Company company, String planCode, Integer seatQuantity,
                             Integer storeQuantity, PayChannel payChannel) {
        initializePlans();
        Plan plan = planRepository.findByCode(planCode);
        if (plan == null || !Boolean.TRUE.equals(plan.getEnabled())) {
            throw new DefinedException(new DefaultErrorCode("400", "套餐不存在或已下架"));
        }
        PlanVersion version = requireVersion(planCode);
        if (version.getPrice() == null || version.getPrice() <= 0) {
            throw new DefinedException(new DefaultErrorCode("400", "免费版无需创建支付订单"));
        }

        int seats = seatQuantity == null ? 0 : seatQuantity;
        int stores = storeQuantity == null ? 0 : storeQuantity;
        if (seats < 0 || stores < 0) {
            throw new DefinedException(new DefaultErrorCode("400", "加购数量不能为负数"));
        }

        List<OrderItem> items = new ArrayList<>();
        long total = version.getPrice();
        items.add(orderItem(OrderItemType.Plan, version.getPlanCode(), version.getPlanName(),
                version.getPrice(), 1, version.getPrice(), version.getDays(), version, null, company));

        PlanAddon seatAddon = planAddonRepository.findByCode(PlanDefinition.SEAT_ADDON_CODE);
        PlanAddon storeAddon = planAddonRepository.findByCode(PlanDefinition.STORE_ADDON_CODE);
        total += addAddonItem(items, company, seatAddon, seats, OrderItemType.Seats, PlanAddonType.Seats);
        total += addAddonItem(items, company, storeAddon, stores, OrderItemType.Stores, PlanAddonType.Stores);

        Order order = new Order();
        order.setOrderNo(nextOrderNo());
        order.setCompany(company);
        order.setPlan(plan);
        order.setPlanVersion(version);
        order.setAmount(total);
        order.setDays(version.getDays());
        order.setStatus(OrderStatus.Pending);
        order.setPayChannel(payChannel == null ? PayChannel.Offline : payChannel);
        order.setOutTradeNo(order.getOrderNo());
        order.setExpireTime(LocalDateTime.now().plusMinutes(30));
        order.setExtraSeats(seats);
        order.setExtraStores(stores);
        order = orderRepository.save(modelIdentityContext.prepare(order));

        for (OrderItem item : items) {
            item.setOrder(order);
            orderItemRepository.save(modelIdentityContext.prepare(item));
        }
        order.setItems(items);
        return order;
    }

    /**
     * 支付回调后幂等到账。重复回调直接返回已经履约的订单。
     */
    @Transactional
    public Order fulfillOrder(String orderNo, String outTradeNo, Long amount, PayChannel payChannel) {
        return fulfillOrder(orderNo, outTradeNo, null, amount, payChannel);
    }

    /**
     * 支付回调后幂等到账，并记录微信支付交易号。
     */
    @Transactional
    public Order fulfillOrder(String orderNo, String outTradeNo, String transactionId,
                              Long amount, PayChannel payChannel) {
        Order order = orderRepository.findByOrderNoForUpdate(orderNo);
        if (order == null) {
            throw new DefinedException(new DefaultErrorCode("404", "订单不存在"));
        }
        if (order.getStatus() == OrderStatus.Granted) {
            order.setItems(orderItemRepository.findByOrder_OrderIdOrderByOrderItemIdAsc(order.getOrderId()));
            return order;
        }
        if (order.getStatus() != OrderStatus.Pending && order.getStatus() != OrderStatus.Paid) {
            throw new DefinedException(new DefaultErrorCode("400", "当前订单状态不能履约"));
        }
        if (amount == null || !amount.equals(order.getAmount())) {
            throw new DefinedException(new DefaultErrorCode("400", "支付金额与订单金额不一致"));
        }
        if (payChannel != null && order.getPayChannel() != null && payChannel != order.getPayChannel()) {
            throw new DefinedException(new DefaultErrorCode("400", "支付渠道与订单不一致"));
        }

        order.setStatus(OrderStatus.Paid);
        order.setOutTradeNo(outTradeNo);
        order.setTransactionId(transactionId);
        order.setPaidTime(LocalDateTime.now());
        applyBenefits(order);
        order.setStatus(OrderStatus.Granted);
        orderRepository.save(order);
        order.setItems(orderItemRepository.findByOrder_OrderIdOrderByOrderItemIdAsc(order.getOrderId()));
        return order;
    }

    /**
     * 计算剩余可用天数：服务到期时间 - 当前时间，向上取整，不落库。
     */
    public int remainingDays(Subscription subscription) {
        if (subscription == null || subscription.getServiceEndTime() == null) {
            return 0;
        }
        long seconds = Duration.between(LocalDateTime.now(),
                subscription.getServiceEndTime().toLocalDateTime()).getSeconds();
        if (seconds <= 0) {
            return 0;
        }
        return (int) ((seconds + DAY_SECONDS - 1) / DAY_SECONDS);
    }

    /**
     * 当前企业订阅。历史企业尚未创建订阅时返回 null，由调用方按兼容策略处理。
     */
    /**
     * 按企业归属加载订单，禁止通过订单号读取其他企业订单。
     */
    public Order requireOrder(Company company, String orderNo) {
        if (company == null || company.getComId() == null || orderNo == null || orderNo.isBlank()) {
            throw new DefinedException(new DefaultErrorCode("400", "订单号不能为空"));
        }
        Order order = orderRepository.findByOrderNo(orderNo.trim());
        if (order == null || order.getCompany() == null
                || !Objects.equals(order.getCompany().getComId(), company.getComId())) {
            throw new DefinedException(new DefaultErrorCode("404", "订单不存在"));
        }
        order.setItems(orderItemRepository.findByOrder_OrderIdOrderByOrderItemIdAsc(order.getOrderId()));
        return order;
    }

    /**
     * 将本地已过期的待支付订单标记为过期。
     */
    @Transactional
    public Order expireIfNecessary(Order order) {
        if (order != null && order.getStatus() == OrderStatus.Pending
                && order.getExpireTime() != null
                && !order.getExpireTime().isAfter(LocalDateTime.now())) {
            order.setStatus(OrderStatus.Expired);
            return orderRepository.save(order);
        }
        return order;
    }

    /**
     * 标记支付订单失效。
     */
    @Transactional
    public Order expire(Order order) {
        if (order == null) {
            return null;
        }
        order.setStatus(OrderStatus.Expired);
        return orderRepository.save(order);
    }

    /**
     * 当前企业订阅。
     */
    public Subscription current(Company company) {
        if (company == null || company.getComId() == null) {
            return null;
        }
        return subscriptionRepository.findByCompany_ComId(company.getComId());
    }

    /**
     * 锁定企业订阅行，串行化席位和门店额度校验，避免并发超额。
     */
    public Subscription lockCurrent(Company company) {
        if (company == null || company.getComId() == null) {
            return null;
        }
        return subscriptionRepository.findByCompany_ComIdForUpdate(company.getComId());
    }

    /**
     * 企业当前是否允许写入。无订阅的历史企业继续兼容放行。
     */
    public boolean isWritable(Company company) {
        return !isReadOnly(current(company));
    }

    /**
     * 企业订阅是否已经超过服务到期时间。
     */
    public boolean isExpired(Company company) {
        Subscription subscription = current(company);
        if (subscription == null) {
            return false;
        }
        if (subscription.getStatus() == SubStatus.Expired) {
            return true;
        }
        return subscription.getServiceEndTime() != null
                && !subscription.getServiceEndTime().toLocalDateTime().isAfter(LocalDateTime.now());
    }

    /**
     * 企业订阅过期、冻结或关闭后，只允许查询历史数据。
     */
    public void requireWritable(Company company) {
        Subscription subscription = current(company);
        if (!isReadOnly(subscription)) {
            return;
        }
        throw new DefinedException(new DefaultErrorCode(
                "SUBSCRIPTION_EXPIRED", "企业订阅已到期或不可用，当前仅支持查询。请续费后继续操作。"));
    }

    /**
     * 校验席位容量。occupiedSeats 由调用方按“已入企员工 + 待接受邀请”统计。
     */
    public void requireSeatCapacity(Company company, long occupiedSeats) {
        Subscription subscription = current(company);
        if (subscription == null) {
            return;
        }
        Integer maxSeats = effectiveMaxSeats(subscription);
        if (maxSeats == null || maxSeats <= 0 || occupiedSeats < maxSeats) {
            return;
        }
        throw new DefinedException(new DefaultErrorCode(
                "UPGRADE_REQUIRED",
                "当前套餐最多支持 " + maxSeats + " 个席位，已使用 " + occupiedSeats
                        + " 个。请购买席位包或升级订阅后继续邀请成员。"));
    }

    /**
     * 校验门店容量。occupiedStores 为当前已经创建的门店数。
     */
    public void requireStoreCapacity(Company company, long occupiedStores) {
        Subscription subscription = current(company);
        if (subscription == null) {
            return;
        }
        Integer maxStores = effectiveMaxStores(subscription);
        if (maxStores == null || maxStores <= 0 || occupiedStores < maxStores) {
            return;
        }
        throw new DefinedException(new DefaultErrorCode(
                "UPGRADE_REQUIRED",
                "当前套餐最多支持 " + maxStores + " 家门店，已使用 " + occupiedStores
                        + " 家。请购买门店包或升级订阅后继续新增门店。"));
    }

    /**
     * 校验套餐是否包含指定功能模块。
     */
    public void requireModule(Company company, PlanModule module) {
        if (module == null) {
            return;
        }
        Subscription subscription = current(company);
        if (subscription == null) {
            return;
        }
        String configured = moduleConfig(subscription);
        if (configured == null || configured.isBlank()) {
            return;
        }
        boolean enabled = Arrays.stream(configured.split(","))
                .map(String::trim)
                .anyMatch(value -> value.equalsIgnoreCase(module.name()));
        if (enabled) {
            return;
        }
        throw new DefinedException(new DefaultErrorCode(
                "UPGRADE_REQUIRED",
                "当前套餐未开通“" + module.getName() + "”模块，请升级订阅后使用。"));
    }

    /**
     * 当前可购买的付费套餐。
     */
    public List<Plan> availablePlans() {
        initializePlans();
        return planRepository.findByEnabledTrueAndOneTimeOnlyFalseAndPriceGreaterThanOrderByPriorityAsc(0L);
    }

    /**
     * 当前可购买的席位包和门店包。
     */
    public List<PlanAddon> availableAddons() {
        initializePlans();
        return planAddonRepository.findByEnabledTrueOrderByPriorityAsc();
    }

    /**
     * 企业当前生效的门店、席位和模块快照。
     */
    public PlanVersion effectiveVersion(Subscription subscription) {
        if (subscription == null) {
            return null;
        }
        if (subscription.getPlanVersion() != null) {
            return subscription.getPlanVersion();
        }
        if (subscription.getPlan() == null) {
            return null;
        }
        return planVersionRepository.findTopByPlan_PlanIdOrderByVersionNoDesc(
                subscription.getPlan().getPlanId());
    }

    public Integer effectiveMaxSeats(Subscription subscription) {
        PlanVersion version = effectiveVersion(subscription);
        if (version == null) {
            return null;
        }
        return version.getMaxSeats() + extra(subscription.getExtraSeats());
    }

    public Integer effectiveMaxStores(Subscription subscription) {
        PlanVersion version = effectiveVersion(subscription);
        if (version == null) {
            return null;
        }
        return version.getMaxStores() + extra(subscription.getExtraStores());
    }

    /**
     * 当前企业是否具备多门店权益。
     */
    public boolean supportsMultiStore(Company company) {
        Subscription subscription = current(company);
        if (subscription == null || isReadOnly(subscription)) {
            return false;
        }
        Integer maxStores = effectiveMaxStores(subscription);
        return maxStores != null && maxStores > 1;
    }

    private void applyBenefits(Order order) {
        Subscription subscription = subscriptionRepository.findByCompany_ComIdForUpdate(
                order.getCompany().getComId());
        if (subscription == null) {
            subscription = new Subscription();
            subscription.setCompany(order.getCompany());
            subscription.setTrialActivated(false);
        }
        PlanVersion version = order.getPlanVersion() == null
                ? planVersionRepository.findTopByPlan_PlanIdOrderByVersionNoDesc(order.getPlan().getPlanId())
                : order.getPlanVersion();
        if (version == null) {
            throw new DefinedException(new DefaultErrorCode("400", "订单未关联有效的套餐版本"));
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentEnd = subscription.getServiceEndTime() == null
                ? now
                : subscription.getServiceEndTime().toLocalDateTime();
        LocalDateTime nextEnd = currentEnd.isBefore(now)
                ? now.plusDays(version.getDays())
                : currentEnd.plusDays(version.getDays());

        subscription.setPlan(order.getPlan());
        subscription.setPlanVersion(version);
        subscription.setStatus(SubStatus.Active);
        subscription.setServiceEndTime(new DateTime(nextEnd));
        subscription.setLastRenewTime(now);
        // 席位包和门店包按订阅周期购买，续费订单未再次购买时默认不自动延续。
        subscription.setExtraSeats(extra(order.getExtraSeats()));
        subscription.setExtraStores(extra(order.getExtraStores()));
        subscriptionRepository.save(modelIdentityContext.prepare(subscription));

        writeLedger(order.getCompany(), order, LedgerBizType.RechargeGrant, version.getDays(),
                remainingDays(subscription), "订单支付到账并完成套餐履约");
    }

    private long addAddonItem(List<OrderItem> items, Company company, PlanAddon addon,
                              int quantity, OrderItemType itemType, PlanAddonType addonType) {
        if (quantity == 0) {
            return 0L;
        }
        if (addon == null || !Boolean.TRUE.equals(addon.getEnabled()) || addon.getType() != addonType) {
            throw new DefinedException(new DefaultErrorCode("400", "加购商品未配置或已下架"));
        }
        if (quantity > addon.getMaxQuantity()) {
            throw new DefinedException(new DefaultErrorCode("400",
                    addon.getName() + "单次最多购买 " + addon.getMaxQuantity() + " 个"));
        }
        long amount = addon.getUnitPrice() * quantity;
        items.add(orderItem(itemType, addon.getCode(), addon.getName(), addon.getUnitPrice(),
                quantity, amount, addon.getDays(), null, addon, company));
        return amount;
    }

    private OrderItem orderItem(OrderItemType itemType, String code, String name,
                                Long unitPrice, int quantity, Long amount, Integer days,
                                PlanVersion version, PlanAddon addon, Company company) {
        OrderItem item = new OrderItem();
        item.setCompany(company);
        item.setItemType(itemType);
        item.setPlanVersion(version);
        item.setAddon(addon);
        item.setProductCode(code);
        item.setProductName(name);
        item.setUnitPrice(unitPrice);
        item.setQuantity(quantity);
        item.setAmount(amount);
        item.setDays(days == null ? 0 : days);
        return item;
    }

    private void initializePlan(PlanDefinition definition) {
        Plan plan = planRepository.findByCode(definition.code());
        if (plan == null) {
            plan = new Plan();
            plan.setCode(definition.code());
        }
        plan.setName(definition.name());
        plan.setDays(definition.days());
        plan.setMaxStores(definition.maxStores());
        plan.setMaxSeats(definition.maxSeats());
        plan.setModules(definition.modules());
        plan.setPrice(definition.price());
        plan.setOriginalPrice(definition.price());
        plan.setOneTimeOnly(false);
        plan.setEnabled(true);
        plan.setPriority(definition.priority());
        plan.setDescription(definition.description());
        plan.setTagline(definition.tagline());
        plan.setHighlights(definition.highlights());
        plan.setBadge(definition.badge());
        plan.setRecommended(definition.recommended());
        plan = planRepository.save(modelIdentityContext.prepare(plan));
        ensurePlanVersion(plan, definition);
    }

    private void ensurePlanVersion(Plan plan, PlanDefinition definition) {
        PlanVersion latest = planVersionRepository.findTopByPlan_PlanIdOrderByVersionNoDesc(plan.getPlanId());
        if (latest != null && matches(latest, definition)) {
            return;
        }
        int versionNo = latest == null ? 1 : latest.getVersionNo() + 1;
        PlanVersion version = new PlanVersion();
        version.setPlan(plan);
        version.setVersionNo(versionNo);
        version.setPlanCode(definition.code());
        version.setPlanName(definition.name());
        version.setDays(definition.days());
        version.setMaxStores(definition.maxStores());
        version.setMaxSeats(definition.maxSeats());
        version.setModules(definition.modules());
        version.setPrice(definition.price());
        version.setOriginalPrice(definition.price());
        version.setDescription(definition.description());
        planVersionRepository.save(modelIdentityContext.prepare(version));
    }

    private PlanVersion requireVersion(String code) {
        Plan plan = planRepository.findByCode(code);
        if (plan == null) {
            throw new DefinedException(new DefaultErrorCode("400", "套餐不存在"));
        }
        PlanVersion version = planVersionRepository.findTopByPlan_PlanIdOrderByVersionNoDesc(plan.getPlanId());
        if (version == null) {
            throw new DefinedException(new DefaultErrorCode("400", "套餐未配置版本"));
        }
        return version;
    }

    /**
     * 在首次引入版本快照时，先保存现有套餐权益，并绑定历史订阅，防止升级初始化改变老客户权益。
     */
    private void snapshotLegacyPlans() {
        for (Plan plan : planRepository.findAll()) {
            if (planVersionRepository.countByPlan_PlanId(plan.getPlanId()) > 0) {
                PlanVersion existing = planVersionRepository.findTopByPlan_PlanIdOrderByVersionNoDesc(plan.getPlanId());
                bindLegacySubscriptions(plan, existing);
                continue;
            }
            PlanVersion version = new PlanVersion();
            version.setPlan(plan);
            version.setVersionNo(1);
            version.setPlanCode(plan.getCode());
            version.setPlanName(plan.getName());
            version.setDays(plan.getDays());
            version.setMaxStores(plan.getMaxStores() == null ? 1 : plan.getMaxStores());
            version.setMaxSeats(plan.getMaxSeats() == null ? 2 : plan.getMaxSeats());
            version.setModules(plan.getModules() == null ? PlanModule.Quote.name() : plan.getModules());
            version.setPrice(plan.getPrice() == null ? 0L : plan.getPrice());
            version.setOriginalPrice(plan.getOriginalPrice());
            version.setDescription(plan.getDescription());
            version = planVersionRepository.save(modelIdentityContext.prepare(version));
            bindLegacySubscriptions(plan, version);
        }
    }

    private void bindLegacySubscriptions(Plan plan, PlanVersion version) {
        if (version == null) {
            return;
        }
        for (Subscription subscription : subscriptionRepository.findByPlan_PlanIdAndPlanVersionIsNull(plan.getPlanId())) {
            subscription.setPlanVersion(version);
            subscriptionRepository.save(subscription);
        }
    }

    private void initializeAddons() {
        initializeAddon(PlanDefinition.SEAT_ADDON_CODE, "席位包", PlanAddonType.Seats,
                40000L, 365, 200, 10, "按账号加购，每账号每年 400 元");
        initializeAddon(PlanDefinition.STORE_ADDON_CODE, "门店包", PlanAddonType.Stores,
                360000L, 365, 50, 20, "按门店加购，每门店每年 3,600 元");
    }

    private void initializeAddon(String code, String name, PlanAddonType type, Long unitPrice,
                                 Integer days, Integer maxQuantity, Integer priority, String description) {
        PlanAddon addon = planAddonRepository.findByCode(code);
        if (addon == null) {
            addon = new PlanAddon();
            addon.setCode(code);
        }
        addon.setName(name);
        addon.setType(type);
        addon.setUnitPrice(unitPrice);
        addon.setDays(days);
        addon.setMaxQuantity(maxQuantity);
        addon.setEnabled(true);
        addon.setPriority(priority);
        addon.setDescription(description);
        planAddonRepository.save(modelIdentityContext.prepare(addon));
    }

    private boolean matches(PlanVersion version, PlanDefinition definition) {
        return Objects.equals(version.getPlanCode(), definition.code())
                && Objects.equals(version.getPlanName(), definition.name())
                && Objects.equals(version.getDays(), definition.days())
                && Objects.equals(version.getMaxStores(), definition.maxStores())
                && Objects.equals(version.getMaxSeats(), definition.maxSeats())
                && Objects.equals(version.getModules(), definition.modules())
                && Objects.equals(version.getPrice(), definition.price());
    }

    private boolean isReadOnly(Subscription subscription) {
        if (subscription == null) {
            return false;
        }
        if (subscription.getStatus() == SubStatus.Expired
                || subscription.getStatus() == SubStatus.Frozen
                || subscription.getStatus() == SubStatus.Closed) {
            return true;
        }
        return subscription.getServiceEndTime() != null
                && !subscription.getServiceEndTime().toLocalDateTime().isAfter(LocalDateTime.now());
    }

    private String moduleConfig(Subscription subscription) {
        PlanVersion version = effectiveVersion(subscription);
        return version == null ? null : version.getModules();
    }

    private int extra(Integer value) {
        return value == null ? 0 : value;
    }

    private void normalizeExtra(Subscription subscription) {
        subscription.setExtraSeats(extra(subscription.getExtraSeats()));
        subscription.setExtraStores(extra(subscription.getExtraStores()));
    }

    private String nextOrderNo() {
        return "JZB" + LocalDateTime.now().format(ORDER_TIME)
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private void writeLedger(Company company, Order order, LedgerBizType bizType,
                             Integer changeDays, Integer balanceAfter, String remark) {
        BalanceLedger ledger = new BalanceLedger();
        ledger.setCompany(company);
        ledger.setOrder(order);
        ledger.setBizType(bizType);
        ledger.setChangeDays(changeDays);
        ledger.setBalanceAfter(balanceAfter);
        ledger.setRemark(remark);
        ledger.setCreateTime(LocalDateTime.now());
        balanceLedgerRepository.save(modelIdentityContext.prepare(ledger));
    }

    private record PlanDefinition(
            String code,
            String name,
            int days,
            long price,
            int maxStores,
            int maxSeats,
            String modules,
            int priority,
            String description,
            String tagline,
            String highlights,
            String badge,
            boolean recommended
    ) {
        static final String FREE_CODE = "free-quote";
        static final String BASIC_CODE = "annual-basic";
        static final String PRO_CODE = "annual-pro";
        static final String FLAGSHIP_CODE = "annual-flagship";
        static final String SEAT_ADDON_CODE = "seat-pack";
        static final String STORE_ADDON_CODE = "store-pack";

        static PlanDefinition free() {
            return new PlanDefinition(FREE_CODE, "免费报价版", 0, 0L,
                    1, 2, modulesOf(PlanModule.values()), 0,
                    "1 家门店、2 个席位，全功能能力",
                    "低门槛启用装企数字化",
                    "基础客户与报价能力;适合个人工作室首次上线;无需支付年费",
                    "体验版", false);
        }

        static PlanDefinition basic() {
            return new PlanDefinition(BASIC_CODE, "基础版年费", 365, 598000L,
                    1, 5, modulesOf(PlanModule.values()),
                    10, "单店起步，全功能经营，限制为 1 家门店和 5 个席位",
                    "单店经营全流程，一次开通全年可用",
                    "注册赠送 14 天免费试用;客户到签约经营闭环;设计、报价、项目管理;5 个员工席位",
                    "单店首选", false);
        }

        static PlanDefinition pro() {
            return new PlanDefinition(PRO_CODE, "专业版年费", 365, 1198000L,
                    3, 15, modulesOf(PlanModule.values()),
                    20, "成长型多店装企，包含完整经营、财务和供应链闭环",
                    "从增长到盈利，一套系统管全店",
                    "3 家门店、15 个席位;供应链与财务结算;经营分析看板;适合成长型多店装企",
                    "最受欢迎", true);
        }

        static PlanDefinition flagship() {
            return new PlanDefinition(FLAGSHIP_CODE, "旗舰版年费", 365, 2180000L,
                    9999, 30, modulesOf(PlanModule.values()), 30,
                    "连锁和多品牌装企，包含多门店、开放接口、实施陪跑和高级经营能力",
                    "连锁扩张与品牌管控的完整底座",
                    "不限门店、30 个席位;AI 与开放接口;全模块经营体系;专属上线实施支持",
                    "连锁进阶", false);
        }

        private static String modulesOf(PlanModule... modules) {
            return Arrays.stream(modules).map(Enum::name).collect(Collectors.joining(","));
        }
    }
}
