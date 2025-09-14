package com.database.logging;

import java.io.IOException;
import java.util.Map;

/**
 * 回滚回调接口
 * 用于在事务回滚时执行实际的数据操作
 */
public interface RollbackCallback {
    /**
     * 回滚插入记录
     */
    void rollbackInsert(String tableName, Map<String, Object> record) throws IOException;
    
    /**
     * 回滚更新记录
     */
    void rollbackUpdate(String tableName, Map<String, Object> record) throws IOException;
    
    /**
     * 回滚删除记录
     */
    void rollbackDelete(String tableName, Map<String, Object> record) throws IOException;
    
    /**
     * 回滚创建表
     */
    void rollbackCreateTable(String tableName, String metadata) throws IOException;
    
    /**
     * 回滚删除表
     */
    void rollbackDropTable(String tableName) throws IOException;
}
