package com.th4.edge.decoboon.customer.model;

import com.th4.edge.framework.core.annotation.Note;

/**
 * 客户批量手机号录入请求。
 *
 * <p>只接收原始文本和负责人；手机号由服务端重新解析并校验，避免前端预览结果被篡改。</p>
 */
@Note("客户批量手机号录入")
public class CustomerBatch {

    private String text;

    private Long ownerId;

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
}
