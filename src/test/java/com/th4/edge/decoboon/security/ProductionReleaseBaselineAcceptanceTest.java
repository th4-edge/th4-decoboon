package com.th4.edge.decoboon.security;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** PP-05 生产 profile 与发布 Runbook 的本地静态验收。 */
class ProductionReleaseBaselineAcceptanceTest {

    private static final Path PROD = Path.of("src/main/resources/application-prod.yml");
    private static final Path RUNBOOK = Path.of("docs/design/migrations/19-生产发布基线.md");

    @Test
    void productionProfileMustUseExternalConfigurationAndValidateSchema() throws Exception {
        String config = Files.readString(PROD);

        assertThat(config).contains("url: ${DB_URL}");
        assertThat(config).contains("username: ${DB_USERNAME}");
        assertThat(config).contains("password: ${DB_PASSWORD}");
        assertThat(config).contains("password: ${REDIS_PASSWORD}");
        assertThat(config).contains("ddl-auto: validate");
        assertThat(config).contains("file-signature-secret: ${TH4_FILE_SIGNATURE_SECRET}");
        assertThat(config).contains("upload-dir: ${TH4_FILE_UPLOAD_DIR}");
        assertThat(config).contains("public-dir: ${TH4_FILE_PUBLIC_DIR}");

        assertThat(config).doesNotContain("ddl-auto: update");
        // server.address 使用 ${SERVER_ADDRESS:127.0.0.1} 作为安全回环默认值（生产由环境变量覆盖），
        // 因此这里禁止的是数据库/Redis 等外部依赖硬编码回环地址，而不是绑定地址本身。
        assertThat(config).doesNotContain("jdbc:mysql://127.0.0.1");
        assertThat(config).doesNotContain("127.0.0.1:3306");
        assertThat(config).doesNotContain("127.0.0.1:6379");
        assertThat(config).doesNotContain("localhost");
        assertThat(config).doesNotContain("mysqladmin");
        assertThat(config).doesNotContain("password: 123456");
    }

    @Test
    void runbookMustContainReleaseGateSectionsAndExplicitProductionBlockers() throws Exception {
        String runbook = Files.readString(RUNBOOK);

        assertThat(runbook).contains("## 2. 发布前检查");
        assertThat(runbook).contains("## 3. 数据库迁移");
        assertThat(runbook).contains("## 4. 备份");
        assertThat(runbook).contains("## 5. 发布与验证");
        assertThat(runbook).contains("## 6. 回滚");
        assertThat(runbook).contains("## 7. 已知部署阻断项");
        assertThat(runbook).contains("备份恢复和回滚演练尚未完成");
        assertThat(runbook).contains("存量库差异迁移、`adopt` 和恢复演练尚未执行");
        assertThat(runbook).contains("尚未启用带版本记录表的迁移执行器");
    }
}
