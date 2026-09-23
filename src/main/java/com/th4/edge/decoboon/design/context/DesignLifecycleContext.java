package com.th4.edge.decoboon.design.context;

import com.th4.edge.decoboon.design.configure.DesignStatus;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import com.th4.edge.framework.core.annotation.Note;
import org.springframework.stereotype.Component;

/**
 * 设计方案生命周期上下文。
 *
 * <p>设计方案状态必须由业务主链推进，不能只靠设计师在方案页手动修改。
 * 客户接受报价、合同签约与生效、项目立项都意味着主推方案已经被客户选中，
 * 因此这里在商务主链上同步推进设计方案状态，避免出现“已经报价、已经施工，
 * 方案仍停留在待谈单”的断链。</p>
 *
 * <p>本上下文只向前推进状态：</p>
 * <ul>
 *   <li>{@code Ready}/{@code Presented} -&gt; {@code Selected}；</li>
 *   <li>{@code Selected}/{@code Archived} 幂等返回，绝不回退；</li>
 *   <li>{@code Draft} 未完成方案和没有关联设计方案的套餐报价不处理。</li>
 * </ul>
 */
@Note("设计方案生命周期上下文")
@Component
public class DesignLifecycleContext {

    private final DesignRepository designRepository;

    public DesignLifecycleContext(DesignRepository designRepository) {
        this.designRepository = designRepository;
    }

    /**
     * 客户选中方案：{@code Ready}/{@code Presented} -&gt; {@code Selected}。
     *
     * <p>幂等操作，已选中或已归档的方案不会被回退；空方案（套餐报价没有关联
     * 设计方案）直接忽略。</p>
     */
    public void markSelected(Design design) {
        if (design == null || design.getDesignId() == null) {
            return;
        }
        DesignStatus status = design.getStatus();
        if (status == DesignStatus.Ready || status == DesignStatus.Presented) {
            design.setStatus(DesignStatus.Selected);
            designRepository.save(design);
        }
    }
}
