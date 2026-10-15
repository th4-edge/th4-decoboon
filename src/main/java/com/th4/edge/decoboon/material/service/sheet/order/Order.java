package com.th4.edge.decoboon.material.service.sheet.order;

import com.th4.edge.decoboon.material.model.MaterialOrderSheet;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 由作业单下单（允许无价：未定价行形成暂估应付） */
public interface Order extends Provider<MaterialOrderSheet, MaterialOrderSheet>, Security {
}
