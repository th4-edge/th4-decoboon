# 2026-10-08 套餐行保存 orphanRemoval 修复

## 现象

本地导入套餐模板后调用 `pack/line/save` 返回未定义错误，日志显示：

```text
A collection with orphan deletion was no longer referenced by the owning entity instance:
com.th4.edge.decoboon.pack.model.PackageTemplate.items
```

## 根因

`PackageConfigContext.recalculate` 在重算套餐成本时执行：

```java
template.setItems(lines);
```

`PackageTemplate.items` 使用 `cascade = ALL, orphanRemoval = true`。替换 Hibernate 托管集合会导致原集合失去引用，从而触发 orphan deletion 异常。

## 修复

保留原持久化集合，改为：

```java
if (template.getItems() == null) {
    template.setItems(lines);
} else {
    template.getItems().clear();
    template.getItems().addAll(lines);
}
```

涉及文件：

- `src/main/java/com/th4/edge/decoboon/pack/context/PackageConfigContext.java`

## 验证

- 备份本地 dev `pack_*` 相关表：`docs/acceptance/runs/20261008-昭青装饰-20261008-quote-selectors/raw/sql/pack-before-20261008.sql`
- 通过真实 Boss Provider 创建“本地验收套餐模板”；
- `pack/line/save` 成功，`pack_line` 落库 1 行；
- `pack/template/info` 返回 1 个分项，模板成本重算为 `27.40`；
- App 套餐选择器真实点击显示该模板并可回填。

## 边界

- 本次只在本地 dev `decoboon` 库补充 1 个测试套餐模板；
- 回滚 SQL：`raw/sql/pack-rollback-20261008.sql`；
- 未执行生产部署。
