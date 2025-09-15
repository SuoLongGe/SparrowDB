import com.database.engine.DatabaseEngine;
import com.database.engine.ExecutionResult;
import java.io.File;

/**
 * SQL文件管理功能测试类
 */
public class TestSQLFileManager {
    
    public static void main(String[] args) {
        System.out.println("=== SparrowDB SQL文件管理功能测试 ===\n");
        
        // 初始化数据库引擎
        DatabaseEngine engine = new DatabaseEngine("test_db", "data");
        if (!engine.initialize()) {
            System.err.println("数据库引擎初始化失败！");
            return;
        }
        
        try {
            // 测试1: 导入SQL文件
            testImportSQLFile(engine);
            
            // 测试2: 导出数据库
            testExportDatabase(engine);
            
            // 测试3: 导出单个表
            testExportTable(engine);
            
            // 测试4: 重新导入导出的文件验证一致性
            testImportExportConsistency(engine);
            
        } finally {
            engine.shutdown();
        }
        
        System.out.println("\n=== 测试完成 ===");
    }
    
    /**
     * 测试导入SQL文件
     */
    private static void testImportSQLFile(DatabaseEngine engine) {
        System.out.println("--- 测试1: 导入SQL文件 ---");
        
        String testFile = "test_import.sql";
        File file = new File(testFile);
        
        if (!file.exists()) {
            System.out.println("测试文件不存在，跳过导入测试: " + testFile);
            return;
        }
        
        System.out.println("导入文件: " + testFile);
        
        long startTime = System.currentTimeMillis();
        ExecutionResult result = engine.importSQLFile(testFile, true);
        long endTime = System.currentTimeMillis();
        
        System.out.println("导入结果: " + (result.isSuccess() ? "✓ 成功" : "✗ 失败"));
        System.out.println("消息: " + result.getMessage());
        System.out.println("执行时间: " + (endTime - startTime) + "ms");
        
        // 验证导入的数据
        if (result.isSuccess()) {
            System.out.println("\n验证导入的数据:");
            ExecutionResult queryResult = engine.executeSQL("SELECT COUNT(*) FROM test_sql_import");
            if (queryResult.isSuccess()) {
                System.out.println("test_sql_import 表记录数验证成功");
            }
            
            queryResult = engine.executeSQL("SELECT COUNT(*) FROM test_categories");
            if (queryResult.isSuccess()) {
                System.out.println("test_categories 表记录数验证成功");
            }
        }
        
        System.out.println();
    }
    
    /**
     * 测试导出数据库
     */
    private static void testExportDatabase(DatabaseEngine engine) {
        System.out.println("--- 测试2: 导出数据库 ---");
        
        String outputFile = "exported_database.sql";
        
        System.out.println("导出到文件: " + outputFile);
        
        long startTime = System.currentTimeMillis();
        ExecutionResult result = engine.exportDatabaseToSQL(outputFile);
        long endTime = System.currentTimeMillis();
        
        System.out.println("导出结果: " + (result.isSuccess() ? "✓ 成功" : "✗ 失败"));
        System.out.println("消息: " + result.getMessage());
        System.out.println("执行时间: " + (endTime - startTime) + "ms");
        
        // 检查文件是否创建
        File file = new File(outputFile);
        if (file.exists()) {
            System.out.println("导出文件大小: " + file.length() + " 字节");
        } else {
            System.out.println("导出文件未创建");
        }
        
        System.out.println();
    }
    
    /**
     * 测试导出单个表
     */
    private static void testExportTable(DatabaseEngine engine) {
        System.out.println("--- 测试3: 导出单个表 ---");
        
        String tableName = "test_sql_import";
        String outputFile = "exported_" + tableName + ".sql";
        
        System.out.println("导出表: " + tableName + " 到文件: " + outputFile);
        
        long startTime = System.currentTimeMillis();
        ExecutionResult result = engine.exportTableToSQL(tableName, outputFile);
        long endTime = System.currentTimeMillis();
        
        System.out.println("导出结果: " + (result.isSuccess() ? "✓ 成功" : "✗ 失败"));
        System.out.println("消息: " + result.getMessage());
        System.out.println("执行时间: " + (endTime - startTime) + "ms");
        
        // 检查文件是否创建
        File file = new File(outputFile);
        if (file.exists()) {
            System.out.println("导出文件大小: " + file.length() + " 字节");
        } else {
            System.out.println("导出文件未创建");
        }
        
        System.out.println();
    }
    
    /**
     * 测试导入导出一致性
     */
    private static void testImportExportConsistency(DatabaseEngine engine) {
        System.out.println("--- 测试4: 导入导出一致性验证 ---");
        
        String exportedFile = "exported_database.sql";
        File file = new File(exportedFile);
        
        if (!file.exists()) {
            System.out.println("导出文件不存在，跳过一致性测试");
            return;
        }
        
        // 删除原有测试表
        System.out.println("删除原有测试表...");
        engine.executeSQL("DROP TABLE IF EXISTS test_sql_import");
        engine.executeSQL("DROP TABLE IF EXISTS test_categories");
        
        // 重新导入导出的文件
        System.out.println("重新导入导出的文件: " + exportedFile);
        
        long startTime = System.currentTimeMillis();
        ExecutionResult result = engine.importSQLFile(exportedFile, true);
        long endTime = System.currentTimeMillis();
        
        System.out.println("重新导入结果: " + (result.isSuccess() ? "✓ 成功" : "✗ 失败"));
        System.out.println("消息: " + result.getMessage());
        System.out.println("执行时间: " + (endTime - startTime) + "ms");
        
        // 验证数据一致性
        if (result.isSuccess()) {
            System.out.println("\n验证数据一致性:");
            
            ExecutionResult queryResult = engine.executeSQL("SELECT COUNT(*) FROM test_sql_import");
            if (queryResult.isSuccess()) {
                System.out.println("✓ test_sql_import 表数据一致");
            } else {
                System.out.println("✗ test_sql_import 表数据不一致");
            }
            
            queryResult = engine.executeSQL("SELECT COUNT(*) FROM test_categories");
            if (queryResult.isSuccess()) {
                System.out.println("✓ test_categories 表数据一致");
            } else {
                System.out.println("✗ test_categories 表数据不一致");
            }
        }
        
        System.out.println();
    }
}

