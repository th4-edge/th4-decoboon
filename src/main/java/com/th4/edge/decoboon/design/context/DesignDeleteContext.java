package com.th4.edge.decoboon.design.context;

import com.th4.edge.decoboon.design.configure.DesignStatus;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.DesignItem;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.design.repository.DesignItemRepository;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import com.th4.edge.decoboon.design.repository.SpaceRepository;
import com.th4.edge.decoboon.design.repository.TalkRepository;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 删除尚未完成且没有下游业务事实的设计方案。 */
@Note("设计方案删除上下文")
@Component
public class DesignDeleteContext {

    private final DesignContext designContext;
    private final DesignRepository designRepository;
    private final SpaceRepository spaceRepository;
    private final DesignItemRepository itemRepository;
    private final QuoteRepository quoteRepository;
    private final TalkRepository talkRepository;

    public DesignDeleteContext(DesignContext designContext,
                               DesignRepository designRepository,
                               SpaceRepository spaceRepository,
                               DesignItemRepository itemRepository,
                               QuoteRepository quoteRepository,
                               TalkRepository talkRepository) {
        this.designContext = designContext;
        this.designRepository = designRepository;
        this.spaceRepository = spaceRepository;
        this.itemRepository = itemRepository;
        this.quoteRepository = quoteRepository;
        this.talkRepository = talkRepository;
    }

    @Transactional
    public Design delete(Long designId, Employee principal) {
        Design design = designContext.requireDesign(designId, principal);
        designContext.requireEditable(design);
        if (design.getStatus() != DesignStatus.Draft) {
            throw new DefinedException(new DefaultErrorCode("400", "只有未完成的设计方案可以删除"));
        }
        if (quoteRepository.countByDesign_DesignId(designId) > 0) {
            throw new DefinedException(new DefaultErrorCode("400", "方案已生成报价，不能删除"));
        }
        if (talkRepository.existsByDesign_DesignId(designId)) {
            throw new DefinedException(new DefaultErrorCode("400", "方案已生成谈单记录，不能删除"));
        }

        List<DesignItem> items = itemRepository.findByDesign_DesignIdOrderBySortAsc(designId);
        if (!items.isEmpty()) {
            itemRepository.deleteAll(items);
        }
        List<Space> spaces = spaceRepository.findByDesign_DesignIdOrderBySortAsc(designId);
        if (!spaces.isEmpty()) {
            spaceRepository.deleteAll(spaces);
        }
        designRepository.delete(design);
        return design;
    }
}
