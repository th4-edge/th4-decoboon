package com.th4.edge.decoboon.finance.context;

import com.th4.edge.decoboon.finance.model.Expense;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import org.springframework.stereotype.Component;

import java.util.Locale;

/** 费用证据对象键校验：只接受当前企业、当前项目范围的 Expense 上传路径。 */
@Component
public class ExpenseEvidenceContext {

    public String requireForExpense(Expense expense, String rawPath) {
        String validated = validateOrNull(expense, rawPath);
        if (validated == null) {
            throw new DefinedException(new DefaultErrorCode("400", "费用证据文件不属于当前企业或项目，请重新上传"));
        }
        return validated;
    }

    public String validateOrNull(Expense expense, String rawPath) {
        if (expense == null || expense.getCompany() == null || expense.getCompany().getComId() == null
                || rawPath == null || rawPath.isBlank()) {
            return null;
        }
        String path = rawPath.trim().replace('\\', '/');
        if (path.length() > 512 || path.startsWith("/") || path.contains("..") || path.contains("//")
                || path.toLowerCase(Locale.ROOT).startsWith("http")
                || path.contains("/file/private/download")) {
            return null;
        }
        Long comId = expense.getCompany().getComId();
        Long projectId = expense.getProject() == null ? null : expense.getProject().getProjectId();
        String companyPrefix = "companies/" + comId + "/";
        if (!path.startsWith(companyPrefix)) return null;
        if (projectId != null) {
            if (!path.startsWith(companyPrefix + "projects/" + projectId + "/files/expense/")) return null;
        } else {
            if (path.startsWith(companyPrefix + "projects/")) return null;
            if (!path.startsWith(companyPrefix + "files/expense/")) return null;
        }
        return path;
    }
}
