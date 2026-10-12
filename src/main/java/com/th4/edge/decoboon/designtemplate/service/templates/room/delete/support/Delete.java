package com.th4.edge.decoboon.designtemplate.service.templates.room.delete.support;

import com.th4.edge.decoboon.designtemplate.context.DesignTemplateAccessContext;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplate;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplateRoom;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplateVersion;
import com.th4.edge.decoboon.designtemplate.repository.DesignTemplateItemRepository;
import com.th4.edge.decoboon.designtemplate.repository.DesignTemplateRoomRepository;
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
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 设计模版区域删除。
 *
 * <p>只允许删除草稿版本上的区域；已发布版本不可修改（BR-040 §5）。
 * 区域被分项公式引用时必须先解引用：删除被引用的区域会阻断并提示引用数量，
 * 避免出现“区域没了、公式锚点悬空”的静默失效（T16 / DESIGN-BIZ §8.3）。</p>
 */
@Note("设计模版区域删除")
@Description("删除草稿版本上的区域；被分项引用时阻断")
@Properties(allow = {
        @AllowProperties(model = DesignTemplateRoom.class,
                properties = {"roomId", "roomKey", "name", "roomType", "sortNo", "length", "width", "height",
                        "doorWidthTotal", "doorAreaTotal", "windowAreaTotal", "wallRise",
                        "referenceArea", "referencePerimeter", "count", "remark"})
})
@Service("design/template/room/delete")
@Scope(ScopeTypes.PROTOTYPE)
public class Delete implements com.th4.edge.decoboon.designtemplate.service.templates.room.delete.Delete {

    @AutoInject
    private Long roomId;

    @User
    private Employee employee;

    private final DesignTemplateRoomRepository roomRepository;
    private final DesignTemplateItemRepository itemRepository;
    private final DesignTemplateAccessContext accessContext;

    public Delete(DesignTemplateRoomRepository roomRepository,
                  DesignTemplateItemRepository itemRepository,
                  DesignTemplateAccessContext accessContext) {
        this.roomRepository = roomRepository;
        this.itemRepository = itemRepository;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional
    public DesignTemplateRoom call(DesignTemplateRoom request) {
        Employee me = accessContext.requireEmployee(employee);
        Long comId = me.getCompany().getComId();
        if (roomId == null) {
            throw new DefinedException(new DefaultErrorCode("400", "roomId不能为空"));
        }
        DesignTemplateRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "模版区域不存在")));
        if (!room.getCompany().getComId().equals(comId)) {
            throw new DefinedException(new DefaultErrorCode("404", "模版区域不存在"));
        }
        DesignTemplateVersion version = room.getVersion();
        DesignTemplate template = version.getTemplate();
        accessContext.requireMaintain(me, template);
        if (version.getStatus() != DesignTemplate.Status.Draft) {
            throw new DefinedException(new DefaultErrorCode("400", "已发布版本不可修改，请新建版本"));
        }
        long references = itemRepository
                .countByVersion_VersionIdAndCompany_ComIdAndRoomKey(version.getVersionId(), comId, room.getRoomKey());
        if (references > 0) {
            throw new DefinedException(new DefaultErrorCode("400",
                    "区域「" + room.getName() + "」被 " + references + " 个分项引用，请先删除或调整这些分项"));
        }
        roomRepository.delete(room);
        return room;
    }
}
