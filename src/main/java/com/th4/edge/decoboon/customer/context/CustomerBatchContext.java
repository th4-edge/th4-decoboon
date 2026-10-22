package com.th4.edge.decoboon.customer.context;

import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.configure.CustomerSource;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerBatchPreview;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 客户批量手机号解析与录入。
 *
 * <p>线索可能以逗号、顿号、分号、空格、换行、竖线等任意字符分隔；
 * 服务端先返回解析预览，用户确认后再次解析并写库，保证幂等和可核对。</p>
 */
@Note("客户批量手机号解析与录入")
@Component
public class CustomerBatchContext {

    public static final int MAX_BATCH_SIZE = 200;
    private static final Pattern DIGITS = Pattern.compile("\\d+");

    private final CustomerRepository customerRepository;
    private final CustomerContext customerContext;

    public CustomerBatchContext(CustomerRepository customerRepository,
                                CustomerContext customerContext) {
        this.customerRepository = customerRepository;
        this.customerContext = customerContext;
    }

    /** 解析手机号并返回有效、重复、无效三类预览。 */
    public CustomerBatchPreview preview(Employee current, String text) {
        requireManager(current);
        List<String> candidates = parseCandidates(text);
        if (candidates.isEmpty()) {
            throw new DefinedException(new DefaultErrorCode("400", "没有识别到可处理的手机号"));
        }
        if (candidates.size() > MAX_BATCH_SIZE) {
            throw new DefinedException(new DefaultErrorCode(
                    "400", "单次最多录入 " + MAX_BATCH_SIZE + " 个手机号，请拆分后再提交"));
        }

        Long comId = current.getCompany().getComId();
        Set<String> existing = new HashSet<>();
        for (Customer customer : customerRepository.findByCompany_ComId(comId)) {
            String phone = digitsOnly(customer.getTelNo());
            if (!phone.isBlank()) existing.add(phone);
        }

        Set<String> seen = new HashSet<>();
        CustomerBatchPreview preview = new CustomerBatchPreview();
        List<CustomerBatchPreview.Item> items = new ArrayList<>();
        for (String candidate : candidates) {
            CustomerBatchPreview.Item item = new CustomerBatchPreview.Item();
            item.setInput(candidate);
            if (!candidate.matches("^1[3-9]\\d{9}$")) {
                item.setStatus("invalid");
                item.setReason("不是有效的 11 位手机号");
                preview.setInvalidCount(preview.getInvalidCount() + 1);
            } else if (existing.contains(candidate) || !seen.add(candidate)) {
                item.setPhone(candidate);
                item.setStatus("duplicate");
                item.setReason(existing.contains(candidate) ? "企业客户库已存在" : "本次输入中重复");
                preview.setDuplicateCount(preview.getDuplicateCount() + 1);
            } else {
                item.setPhone(candidate);
                item.setStatus("valid");
                preview.setValidCount(preview.getValidCount() + 1);
            }
            items.add(item);
        }
        preview.setTotal(items.size());
        preview.setItems(items);
        return preview;
    }

    /** 再次解析并创建有效、非重复的客户，统一交付给指定负责人。 */
    public CustomerBatchPreview save(Employee current, String text, Long ownerId) {
        CustomerBatchPreview preview = preview(current, text);
        Employee owner = customerContext.resolveAssignableOwner(current, ownerId);
        List<Customer> customers = new ArrayList<>();
        for (CustomerBatchPreview.Item item : preview.getItems()) {
            if (!"valid".equals(item.getStatus())) continue;
            Customer customer = new Customer();
            customer.setName(batchName(item.getPhone()));
            customer.setTelNo(item.getPhone());
            customer.setGender(null);
            customer.setSource(CustomerSource.BatchPhone);
            customer.setStatus(CustStatus.New);
            customer.setRemark("批量手机号导入，待完善姓名与装修需求");
            customer.setWechatAdded(Boolean.FALSE);
            customer.setWechatNo(null);
            customer.setVisitCount(0);
            customerContext.applyOwnership(customer, owner);
            customers.add(customer);
            item.setStatus("created");
            item.setReason(null);
        }
        if (!customers.isEmpty()) {
            customerRepository.saveAll(customers);
        }
        preview.setCreatedCount(customers.size());
        return preview;
    }

    private void requireManager(Employee current) {
        if (current == null || current.getCompany() == null || current.getCompany().getComId() == null) {
            throw new DefinedException(new DefaultErrorCode("403", "当前员工未归属企业"));
        }
        if (current.getRole() != EmployeeRole.Boss
                && current.getRole() != EmployeeRole.StoreManager
                && current.getRole() != EmployeeRole.Finance) {
            throw new DefinedException(new DefaultErrorCode("403", "仅企业负责人、店长或财务可以批量录入客户"));
        }
    }

    private List<String> parseCandidates(String text) {
        String source = text == null ? "" : text;
        Matcher matcher = DIGITS.matcher(source);
        List<String> candidates = new ArrayList<>();
        while (matcher.find()) {
            String value = matcher.group();
            if ("86".equals(value) || "0086".equals(value)) continue;
            if (value.startsWith("0086") && value.length() > 11) value = value.substring(4);
            else if (value.startsWith("86") && value.length() > 11) value = value.substring(2);
            candidates.add(value);
        }
        return candidates;
    }

    private String digitsOnly(String value) {
        if (value == null) return "";
        return value.replaceAll("\\D", "");
    }

    private String batchName(String phone) {
        String masked = phone.length() == 11
                ? phone.substring(0, 3) + "****" + phone.substring(7)
                : phone;
        return "待完善-" + masked;
    }
}
