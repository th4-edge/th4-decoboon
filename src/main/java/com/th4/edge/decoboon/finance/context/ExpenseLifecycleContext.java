package com.th4.edge.decoboon.finance.context;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.decoboon.finance.configure.ExpenseDocumentType;
import com.th4.edge.decoboon.finance.configure.ExpensePaymentMode;
import com.th4.edge.decoboon.finance.configure.ExpenseStatus;
import com.th4.edge.decoboon.finance.model.Expense;
import com.th4.edge.decoboon.finance.model.ExpenseAttachment;
import com.th4.edge.decoboon.finance.model.ExpenseRevision;
import com.th4.edge.decoboon.finance.repository.ExpenseRepository;
import com.th4.edge.decoboon.finance.repository.ExpenseRevisionRepository;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.components.lock.DistributedLock;
import jakarta.persistence.EntityManager;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 费用状态转换、证据附件装配和提交版本快照。 */
@Component
public class ExpenseLifecycleContext {

    private final ExpenseRepository expenseRepository;
    private final ExpenseRevisionRepository revisionRepository;
    private final ExpenseAccessContext accessContext;
    private final ExpenseEvidenceContext evidenceContext;
    private final ExpensePaymentContext paymentContext;
    private final DistributedLock distributedLock;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;

    public ExpenseLifecycleContext(ExpenseRepository expenseRepository,
                                   ExpenseRevisionRepository revisionRepository,
                                   ExpenseAccessContext accessContext,
                                   ExpenseEvidenceContext evidenceContext,
                                   ExpensePaymentContext paymentContext,
                                   DistributedLock distributedLock,
                                   EntityManager entityManager,
                                   ObjectMapper objectMapper) {
        this.expenseRepository = expenseRepository;
        this.revisionRepository = revisionRepository;
        this.accessContext = accessContext;
        this.evidenceContext = evidenceContext;
        this.paymentContext = paymentContext;
        this.distributedLock = distributedLock;
        this.entityManager = entityManager;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Expense save(Expense expense, List<ExpenseAttachment> attachments, Employee current) {
        if (attachments != null) replaceAttachments(expense, attachments, current);
        return expenseRepository.save(expense);
    }

    @Transactional
    public Expense submit(Expense expense, Employee current) {
        if (expense.getStatus() != ExpenseStatus.Draft && expense.getStatus() != ExpenseStatus.Returned) {
            throw invalidState("当前费用状态不能提交");
        }
        accessContext.requireOwnerOrManager(expense, current);
        validateForSubmit(expense);
        String returnReason = expense.getApprovalNote();
        Employee returnedBy = expense.getApprovedBy();
        LocalDateTime returnedAt = expense.getApprovedAt();
        expense.setVersion(expense.getVersion() == null ? 1 : expense.getVersion() + 1);
        expense.setStatus(ExpenseStatus.Submitted);
        expense.setSubmittedAt(LocalDateTime.now());
        expense.setApprovedBy(null);
        expense.setApprovedAt(null);
        expense.setApprovalNote(null);
        expense.setSelfApproved(Boolean.FALSE);
        Expense saved = expenseRepository.save(expense);
        saveRevision(saved, current, returnReason, returnedBy, returnedAt);
        return saved;
    }

    @Transactional
    public Expense cancel(Expense expense, Employee current) {
        if (expense.getStatus() != ExpenseStatus.Submitted) {
            throw invalidState("只有已提交费用可以撤回");
        }
        if (!Objects.equals(expense.getRequester().getEmpId(), current.getEmpId())) {
            throw new DefinedException(new DefaultErrorCode("403", "只有提交人可以撤回费用单"));
        }
        expense.setStatus(ExpenseStatus.Cancelled);
        return expenseRepository.save(expense);
    }

    @Transactional
    public Expense approve(Expense expense, Employee current, String note) {
        Employee approver = accessContext.requireApprover(current);
        String lockKey = "expense-approve:" + expense.getCompany().getComId() + ":" + expense.getExpenseId();
        if (!distributedLock.tryLock(lockKey, 1000, 30000)) {
            throw new DefinedException(new DefaultErrorCode("409", "该费用正在审批，请稍后重试"));
        }
        boolean unlockAfterTransaction = registerUnlockAfterCommit(lockKey);
        try {
            // 同一费用并发审批必须在锁内重新读取，不能信任进入事务前加载的状态。
            entityManager.refresh(expense);
            if (expense.getStatus() == ExpenseStatus.Approved && expense.getPayable() != null) {
                return expense;
            }
            if (expense.getStatus() != ExpenseStatus.Submitted) {
                throw invalidState("只有已提交费用可以审批");
            }
            boolean self = isOwner(expense, approver);
            if (self && approver.getRole() != EmployeeRole.Boss) {
                throw new DefinedException(new DefaultErrorCode("403", "店长不能审批本人报销"));
            }
            expense.setStatus(ExpenseStatus.Approved);
            expense.setApprovedBy(approver);
            expense.setApprovedAt(LocalDateTime.now());
            expense.setApprovalNote(normalizeNote(note));
            expense.setSelfApproved(self);
            Expense saved = expenseRepository.save(expense);
            paymentContext.ensurePayable(saved, approver);
            return saved;
        } finally {
            if (!unlockAfterTransaction) distributedLock.unlock(lockKey);
        }
    }

    private boolean registerUnlockAfterCommit(String lockKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return false;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                distributedLock.unlock(lockKey);
            }
        });
        return true;
    }

    @Transactional
    public Expense returnForCorrection(Expense expense, Employee current, String note) {
        if (expense.getStatus() != ExpenseStatus.Submitted) {
            throw invalidState("只有已提交费用可以退回");
        }
        Employee actor = accessContext.requireSubmitter(current);
        boolean approver = actor.getRole() == EmployeeRole.Boss || actor.getRole() == EmployeeRole.StoreManager;
        boolean finance = actor.getRole() == EmployeeRole.Finance;
        if (!approver && !finance) {
            throw new DefinedException(new DefaultErrorCode("403", "当前账号无权退回费用单"));
        }
        if (isOwner(expense, actor)) {
            throw new DefinedException(new DefaultErrorCode("403", "本人不能退回自己的费用单"));
        }
        requireNote(note, "请填写退回原因");
        expense.setStatus(ExpenseStatus.Returned);
        expense.setApprovedBy(actor);
        expense.setApprovedAt(LocalDateTime.now());
        expense.setApprovalNote(note.trim());
        return expenseRepository.save(expense);
    }

    @Transactional
    public Expense reject(Expense expense, Employee current, String note) {
        if (expense.getStatus() != ExpenseStatus.Submitted) {
            throw invalidState("只有已提交费用可以拒绝");
        }
        Employee approver = accessContext.requireApprover(current);
        if (isOwner(expense, approver)) {
            throw new DefinedException(new DefaultErrorCode("403", "本人不能拒绝自己的费用单"));
        }
        requireNote(note, "请填写拒绝原因");
        expense.setStatus(ExpenseStatus.Rejected);
        expense.setApprovedBy(approver);
        expense.setApprovedAt(LocalDateTime.now());
        expense.setApprovalNote(note.trim());
        return expenseRepository.save(expense);
    }

    public void replaceAttachments(Expense expense, List<ExpenseAttachment> attachments, Employee current) {
        expense.getAttachments().clear();
        for (ExpenseAttachment source : attachments) {
            if (source == null || source.getFileUrl() == null || source.getFileUrl().isBlank()) {
                throw new DefinedException(new DefaultErrorCode("400", "证据附件路径不能为空"));
            }
            ExpenseAttachment attachment = new ExpenseAttachment();
            attachment.setCompany(expense.getCompany());
            attachment.setExpense(expense);
            attachment.setDocumentType(source.getDocumentType() == null
                    ? ExpenseDocumentType.Other : source.getDocumentType());
            attachment.setFileUrl(evidenceContext.requireForExpense(expense, source.getFileUrl()));
            attachment.setFileName(trim(source.getFileName(), 255));
            attachment.setSha256(trim(source.getSha256(), 64));
            attachment.setSize(source.getSize());
            attachment.setUploadedBy(current);
            attachment.setUploadedAt(LocalDateTime.now());
            expense.getAttachments().add(attachment);
        }
    }

    public void validateForSubmit(Expense expense) {
        if (expense.getCategory() == null) throw invalid("请选择费用分类");
        if (!Boolean.TRUE.equals(expense.getCategory().getActive())) throw invalid("费用分类已停用，请重新选择");
        if (expense.getCompany() == null || expense.getCategory().getCompany() == null
                || !Objects.equals(expense.getCategory().getCompany().getComId(), expense.getCompany().getComId())) {
            throw invalid("费用分类不属于当前企业");
        }
        if (expense.getOwner() == null) throw invalid("请选择费用归属员工");
        if (expense.getAmount() == null || expense.getAmount().signum() <= 0) throw invalid("费用金额必须大于 0");
        if (expense.getExpenseDate() == null) throw invalid("请选择费用发生日期");
        if (expense.getDescription() == null || expense.getDescription().isBlank()) throw invalid("请填写费用说明");
        if (expense.getAttachments() == null || expense.getAttachments().isEmpty()) throw invalid("请至少上传一份证据附件");
        if (expense.getPaymentMode() == ExpensePaymentMode.Reimbursement) {
            if (expense.getPayeeEmployee() == null
                    || !Objects.equals(expense.getPayeeEmployee().getEmpId(), expense.getOwner().getEmpId())) {
                throw invalid("报销收款员工必须等于费用归属员工");
            }
        }
        if (expense.getAmount() != null) {
            expense.setAmount(expense.getAmount().setScale(2, java.math.RoundingMode.HALF_UP));
            if (expense.getAmount().signum() <= 0) throw invalid("费用金额必须大于 0");
        }
    }

    public ExpenseStatus parseTarget(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return ExpenseStatus.valueOf(raw.trim());
        } catch (IllegalArgumentException ex) {
            throw new DefinedException(new DefaultErrorCode("400", "费用状态不合法"));
        }
    }

    private void saveRevision(Expense expense, Employee current, String returnReason,
                              Employee returnedBy, LocalDateTime returnedAt) {
        ExpenseRevision revision = new ExpenseRevision();
        revision.setCompany(expense.getCompany());
        revision.setExpense(expense);
        revision.setVersion(expense.getVersion());
        revision.setSnapshotJson(snapshot(expense, returnReason, returnedBy, returnedAt));
        revision.setSubmittedBy(current);
        revision.setSubmittedAt(expense.getSubmittedAt());
        revision.setReturnedBy(returnedBy);
        revision.setReturnedAt(returnedAt);
        revision.setReturnReason(returnReason);
        revisionRepository.save(revision);
    }

    private String snapshot(Expense expense, String returnReason, Employee returnedBy, LocalDateTime returnedAt) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("expenseId", expense.getExpenseId());
        root.put("expenseNo", expense.getExpenseNo());
        root.put("companyId", expense.getCompany() == null ? null : expense.getCompany().getComId());
        root.put("storeId", expense.getStore() == null ? null : expense.getStore().getStoreId());
        root.put("projectId", expense.getProject() == null ? null : expense.getProject().getProjectId());
        root.put("ownerId", expense.getOwner() == null ? null : expense.getOwner().getEmpId());
        root.put("categoryId", expense.getCategory() == null ? null : expense.getCategory().getCategoryId());
        root.put("categoryName", expense.getCategory() == null ? null : expense.getCategory().getName());
        root.put("paymentMode", expense.getPaymentMode());
        root.put("amount", expense.getAmount());
        root.put("expenseDate", expense.getExpenseDate());
        root.put("description", expense.getDescription());
        root.put("version", expense.getVersion());
        List<Map<String, Object>> attachments = new ArrayList<>();
        for (ExpenseAttachment item : expense.getAttachments()) {
            Map<String, Object> attachment = new LinkedHashMap<>();
            attachment.put("documentType", item.getDocumentType());
            attachment.put("fileUrl", item.getFileUrl());
            attachment.put("fileName", item.getFileName());
            attachment.put("size", item.getSize());
            attachment.put("sha256", item.getSha256());
            attachments.add(attachment);
        }
        root.put("attachments", attachments);
        Map<String, Object> previousReturn = new LinkedHashMap<>();
        previousReturn.put("reason", returnReason);
        previousReturn.put("returnedBy", returnedBy == null ? null : returnedBy.getEmpId());
        previousReturn.put("returnedAt", returnedAt);
        root.put("previousReturn", previousReturn);
        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException ex) {
            throw new DefinedException(new DefaultErrorCode("500", "费用提交快照生成失败"));
        }
    }

    private boolean isOwner(Expense expense, Employee current) {
        return expense.getOwner() != null && Objects.equals(expense.getOwner().getEmpId(), current.getEmpId());
    }

    private void requireNote(String note, String message) {
        if (note == null || note.isBlank()) throw invalid(message);
    }

    private String normalizeNote(String note) {
        return note == null ? null : trim(note, 5000);
    }

    private String trim(String value, int max) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.length() > max ? normalized.substring(0, max) : normalized;
    }

    private DefinedException invalidState(String message) {
        return new DefinedException(new DefaultErrorCode("409", message));
    }

    private DefinedException invalid(String message) {
        return new DefinedException(new DefaultErrorCode("400", message));
    }
}
