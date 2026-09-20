package com.th4.edge.decoboon.catalog.service.items.importer;

import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

import java.util.List;

/**
 * 报价项目库批量导入。
 *
 * <p>支持两种来源：① 请求体 items 列表；② classpath 中的 JSON 文件（filename 指定）。
 * 导入在单事务中执行，逐条校验并落库；返回成功/失败统计及失败明细。</p>
 */
public interface Import extends Provider<List<CatalogItem>, ImportResult>, Security {
}
