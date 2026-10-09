package com.th4.edge.decoboon.designtemplate.service.templates.publish.support;

import com.th4.edge.decoboon.designtemplate.context.DesignTemplateAccessContext;
import com.th4.edge.decoboon.designtemplate.context.DesignTemplateFormulaContext;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplate;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplateItem;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplateRoom;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplateVersion;
import com.th4.edge.decoboon.designtemplate.repository.DesignTemplateItemRepository;
import com.th4.edge.decoboon.designtemplate.repository.DesignTemplateRepository;
import com.th4.edge.decoboon.designtemplate.repository.DesignTemplateRoomRepository;
import com.th4.edge.decoboon.designtemplate.repository.DesignTemplateVersionRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import com.th4.edge.tenant.user.model.Company;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 设计模版发布。
 *
 * <p>发布**不设审批**，但必须通过发布前校验硬门禁（BR-040 §5 / DEC-050）：
 * 公式使用专业变量且可解析、单位与变量维度一致、分项引用的项目库条目有效、
 * 分项归属区域存在于本版本、至少一个区域。</p>
 */
@Note("设计模版发布")
@Description("发布设计模版当前草稿版本；发布后版本不可变")
@Properties(allow = {
        @AllowProperties(model = DesignTemplate.class,
                properties = {"templateId", "scope", "status", "currentVersion", "remark", "createTime", "owner", "store"}),
        @AllowProperties(model = DesignTemplateVersion.class,
                properties = {"versionId", "version", "status", "name", "houseType", "areaMin", "areaMax",
                        "tags", "publishedAt"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name"}),
        @AllowProperties(model = Team.class, properties = {"teamId", "name", "color"}),
        @AllowProperties(model = Company.class, properties = {"comId", "title", "name", "color"})
})
@Service("design/template/publish")
@Scope(ScopeTypes.PROTOTYPE)
public class Publish implements com.th4.edge.decoboon.designtemplate.service.templates.publish.Publish {

    @AutoInject
    private Long templateId;

    @User
    private Employee employee;

    private final DesignTemplateRepository templateRepository;
    private final DesignTemplateVersionRepository versionRepository;
    private final DesignTemplateRoomRepository roomRepository;
    private final DesignTemplateItemRepository itemRepository;
    private final DesignTemplateFormulaContext formulaContext;
    private final DesignTemplateAccessContext accessContext;

    public Publish(DesignTemplateRepository templateRepository,
                   DesignTemplateVersionRepository versionRepository,
                   DesignTemplateRoomRepository roomRepository,
                   DesignTemplateItemRepository itemRepository,
                   DesignTemplateFormulaContext formulaContext,
                   DesignTemplateAccessContext accessContext) {
        this.templateRepository = templateRepository;
        this.versionRepository = versionRepository;
        this.roomRepository = roomRepository;
        this.itemRepository = itemRepository;
        this.formulaContext = formulaContext;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional
    public DesignTemplate call(DesignTemplate request) {
        Employee me = accessContext.requireEmployee(employee);
        Long comId = me.getCompany().getComId();
        if (templateId == null) {
            throw new DefinedException(new DefaultErrorCode("400", "templateId不能为空"));
        }
        DesignTemplate template = templateRepository.findByTemplateIdAndCompany_ComId(templateId, comId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "模版不存在")));
        accessContext.requireMaintain(me, template);
        if (template.getStatus() == DesignTemplate.Status.Disabled) {
            throw new DefinedException(new DefaultErrorCode("400", "模版已停用，不能发布"));
        }

        DesignTemplateVersion version = template.getCurrentVersion();
        if (version == null) {
            throw new DefinedException(new DefaultErrorCode("400", "模版没有可发布版本"));
        }
        if (version.getStatus() == DesignTemplate.Status.Disabled) {
            throw new DefinedException(new DefaultErrorCode("400", "当前版本已停用，不能发布"));
        }
        if (version.getStatus() == DesignTemplate.Status.Published) {
            throw new DefinedException(new DefaultErrorCode("400", "当前版本已发布；修改模版请新建版本"));
        }

        List<DesignTemplateRoom> rooms = roomRepository
                .findByVersion_VersionIdAndCompany_ComIdOrderBySortNoAsc(version.getVersionId(), comId);
        if (rooms.isEmpty()) {
            throw new DefinedException(new DefaultErrorCode("400", "模版至少需要一个区域"));
        }
        Set<String> roomKeys = new HashSet<>();
        for (DesignTemplateRoom room : rooms) {
            roomKeys.add(room.getRoomKey());
        }

        List<DesignTemplateItem> items = itemRepository
                .findByVersion_VersionIdAndCompany_ComIdOrderBySortNoAsc(version.getVersionId(), comId);
        for (DesignTemplateItem item : items) {
            if (item.getCatalogItem() == null) {
                throw new DefinedException(new DefaultErrorCode("400", "分项必须引用报价项目库条目"));
            }
            if (item.getRoomKey() != null && !roomKeys.contains(item.getRoomKey())) {
                throw new DefinedException(new DefaultErrorCode("400",
                        "分项归属区域不存在：" + item.getRoomKey()));
            }
            if (item.getQuantityFormula() != null && !item.getQuantityFormula().isBlank()) {
                formulaContext.validate(item.getQuantityFormula());
                // 单位与公式维度一致性（BR-040 §2）：不匹配即阻断，禁止自动换算
                formulaContext.requireDimension(item.getCatalogItem().getUnit(), item.getQuantityFormula());
            } else if (item.getDefaultQuantity() == null) {
                throw new DefinedException(new DefaultErrorCode("400", "分项必须填写工程量公式或固定数量"));
            }
        }

        version.setStatus(DesignTemplate.Status.Published);
        version.setPublishedAt(LocalDateTime.now());
        version.setPublishedBy(me);
        versionRepository.save(version);

        template.setStatus(DesignTemplate.Status.Published);
        return templateRepository.save(template);
    }
}
