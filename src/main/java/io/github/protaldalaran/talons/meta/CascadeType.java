package io.github.protaldalaran.talons.meta;

public enum CascadeType {

    /**
     * Cascade all operations
     */
    ALL,

    /**
     * 级联保存 与关联表、中间表同时保存
     */
    PERSIST,

    /**
     * 级联合并操作 建立中间表关联
     */
    MERGE,

    /**
     * 级联删除操作 与关联表、中间表同时删除
     */
    REMOVE,

    /** 级联刷新操作 */
    //REFRESH,

    /**
     * 级联脱管/游离操作
     *  撤销所有相关的外键关联
     * @since Java Persistence 2.0
     *
     */
//    DETACH
}
