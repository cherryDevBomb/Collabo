package com.github.cherrydevbomb.collabo.persistence;

import lombok.Getter;

public enum Table {
    INSERT_LOCAL("insert_local"),
    INSERT_REMOTE("insert_remote"),
    DELETE_LOCAL("delete_local"),
    DELETE_REMOTE("delete_remote"),
    GARBAGE_COLLECT("garbage_collect");

    @Getter
    private String tableName;

    Table(String tableName) {
        this.tableName = tableName;
    }
}
