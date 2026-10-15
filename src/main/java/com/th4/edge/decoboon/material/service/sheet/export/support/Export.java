package com.th4.edge.decoboon.material.service.sheet.export.support;

import com.th4.edge.decoboon.material.context.MaterialSheetBaseline;
import com.th4.edge.decoboon.material.context.MaterialSheetContext;
import com.th4.edge.decoboon.material.context.MaterialSheetExportContext;
import com.th4.edge.decoboon.material.model.MaterialOrderSheet;
import com.th4.edge.decoboon.material.model.MaterialSheetExportFile;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * material/sheet/export 实现。
 *
 * <p>导出为**只读**操作：按当前单据快照生成 Excel（抬头 + 明细 + 签字行），
 * 供打印签字留档与发给厂家；不含价格、账期与成本（DEC-073）。</p>
 */
@Note("导出主材下单作业单")
@Properties(allow = @AllowProperties(model = MaterialSheetExportFile.class,
        properties = {"fileName", "contentType", "contentBase64", "sha256"}))
@Service("material/sheet/export")
@Scope(ScopeTypes.PROTOTYPE)
public class Export implements com.th4.edge.decoboon.material.service.sheet.export.Export {

    @User private Employee employee;
    @AutoInject @NotNull private Long sheetId;

    private final MaterialSheetContext sheetContext;
    private final MaterialSheetExportContext exportContext;
    private final MaterialSheetBaseline baseline;

    public Export(MaterialSheetContext sheetContext, MaterialSheetExportContext exportContext,
                  MaterialSheetBaseline baseline) {
        this.sheetContext = sheetContext;
        this.exportContext = exportContext;
        this.baseline = baseline;
    }

    @Override
    @Transactional(readOnly = true)
    public MaterialSheetExportFile call(MaterialOrderSheet request) {
        MaterialOrderSheet sheet = sheetContext.info(employee, sheetId);
        return exportContext.export(sheet, sheetContext.items(sheetId), baseline);
    }
}
