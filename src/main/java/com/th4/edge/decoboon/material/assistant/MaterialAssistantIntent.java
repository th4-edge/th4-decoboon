package com.th4.edge.decoboon.material.assistant;

/** 材料业务助手可解释的意图。只用于检索与回答，不驱动任何写操作。 */
public enum MaterialAssistantIntent {
    SELECTION_GATE,
    TIME_SEMANTICS,
    SHEET_VS_PURCHASE,
    SHEET_STATUS,
    PRICE_COST,
    RECEIPT,
    CHANGE,
    SETTLEMENT,
    SUPPLIER,
    PROJECT_HEALTH,
    PERMISSION,
    FIELD_DICTIONARY,
    OUT_OF_SCOPE,
    UNKNOWN
}
