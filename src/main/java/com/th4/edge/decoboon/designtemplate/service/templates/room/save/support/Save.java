package com.th4.edge.decoboon.designtemplate.service.templates.room.save.support;

import com.th4.edge.decoboon.bim.configure.SpaceType;
import com.th4.edge.decoboon.designtemplate.context.DesignTemplateAccessContext;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplate;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplateRoom;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplateVersion;
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
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 设计模版区域保存（按 roomKey 新增或更新）。
 *
 * <p>只有草稿版本可写：已发布版本不可变（BR-040 §5）。区域尺寸允许留空表示待量房。</p>
 */
@Note("设计模版区域保存")
@Description("在草稿版本上新增/更新区域骨架")
@Properties(allow = {
        @AllowProperties(model = DesignTemplateRoom.class,
                properties = {"roomId", "roomKey", "name", "roomType", "sortNo", "length", "width", "height",
                        "doorWidthTotal", "doorAreaTotal", "windowAreaTotal", "wallRise", "count", "remark"})
})
@Service("design/template/room/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.designtemplate.service.templates.room.save.Save {

    @AutoInject
    private Long versionId;

    @AutoInject
    @NotEmpty
    private String roomKey;

    @AutoInject
    @NotEmpty
    private String name;

    @AutoInject
    private SpaceType roomType;

    @AutoInject
    private Integer sortNo;

    @AutoInject
    private BigDecimal length;

    @AutoInject
    private BigDecimal width;

    @AutoInject
    private BigDecimal height;

    @AutoInject
    private BigDecimal doorWidthTotal;

    @AutoInject
    private BigDecimal doorAreaTotal;

    @AutoInject
    private BigDecimal windowAreaTotal;

    @AutoInject
    private BigDecimal wallRise;

    @AutoInject
    private Integer count;

    @AutoInject
    private String remark;

    @User
    private Employee employee;

    private final DesignTemplateVersionRepository versionRepository;
    private final DesignTemplateRoomRepository roomRepository;
    private final DesignTemplateAccessContext accessContext;

    public Save(DesignTemplateVersionRepository versionRepository,
                DesignTemplateRoomRepository roomRepository,
                DesignTemplateAccessContext accessContext) {
        this.versionRepository = versionRepository;
        this.roomRepository = roomRepository;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional
    public DesignTemplateRoom call(DesignTemplateRoom request) {
        Employee me = accessContext.requireEmployee(employee);
        Long comId = me.getCompany().getComId();
        if (versionId == null) {
            throw new DefinedException(new DefaultErrorCode("400", "versionId不能为空"));
        }
        DesignTemplateVersion version = versionRepository.findByVersionIdAndCompany_ComId(versionId, comId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "模版版本不存在")));
        DesignTemplate template = version.getTemplate();
        accessContext.requireMaintain(me, template);
        if (version.getStatus() != DesignTemplate.Status.Draft) {
            throw new DefinedException(new DefaultErrorCode("400", "已发布版本不可修改，请新建版本"));
        }

        DesignTemplateRoom room = roomRepository
                .findByVersion_VersionIdAndCompany_ComIdAndRoomKey(versionId, comId, roomKey)
                .orElseGet(DesignTemplateRoom::new);
        room.setCompany(me.getCompany());
        room.setVersion(version);
        room.setRoomKey(roomKey);
        room.setName(name);
        room.setRoomType(roomType == null ? SpaceType.Bedroom : roomType);
        room.setSortNo(sortNo == null ? 0 : sortNo);
        room.setLength(length);
        room.setWidth(width);
        room.setHeight(height);
        room.setDoorWidthTotal(doorWidthTotal);
        room.setDoorAreaTotal(doorAreaTotal);
        room.setWindowAreaTotal(windowAreaTotal);
        room.setWallRise(wallRise);
        room.setCount(count == null || count < 1 ? 1 : count);
        room.setRemark(remark);
        return roomRepository.save(room);
    }
}
