package com.database.engine;

import com.sqlcompiler.execution.*;
import com.sqlcompiler.catalog.*;
import org.junit.jupiter.api.*;
import java.util.*;
import java.io.*;

/**
 * 存储系统集成测试 - 验证存储系统和数据库引擎的连接
 */
public class StorageIntegrationTest {
    private DatabaseEngine engine;
    private String testDataDir = "./test_data_integration";
    
    @BeforeEach
    void setUp() {
        // 清理测试目录
        cleanupTestDirectory();
        
        // 创建数据库引擎
        engine = new DatabaseEngine("integration_test_db", testDataDir);
        engine.initialize();
        
        System.out.println("=== 存储系统集成测试开始 ===");
    }
    
    @AfterEach
    void tearDown() {
        if (engine != null) {
            engine.shutdown();
        }
        cleanupTestDirectory();
        System.out.println("=== 存储系统集成测试结束 ===");
    }
    
    @Test
    void testStorageSystemConnection() {
        System.out.println("\n测试1: 存储系统连接验证");
        
        // 创建表
        List<ColumnPlan> columns = new ArrayList<>();
        columns.add(new ColumnPlan("id", "INT", 4, true, true, false, null, true));
        columns.add(new ColumnPlan("name", "VARCHAR", 50, true, false, false, null, false));
        columns.add(new ColumnPlan("email", "VARCHAR", 100, true, false, false, null, false));
        
        ExecutionResult createResult = engine.createTable("users", columns, new ArrayList<>());
        Assertions.assertTrue(createResult.isSuccess(), "表创建应该成功");
        System.out.println("  ✓ 表创建成功");
        
        // 验证文件是否创建
        File tableFile = new File(testDataDir + File.separator + "users.tbl");
        Assertions.assertTrue(tableFile.exists(), "表文件应该存在");
        System.out.println("  ✓ 表文件已创建: " + tableFile.getAbsolutePath());
        
        // 验证系统表是否创建
        File systemTablesFile = new File(testDataDir + File.separator + "__system_tables__.tbl");
        Assertions.assertTrue(systemTablesFile.exists(), "系统表文件应该存在");
        System.out.println("  ✓ 系统表文件已创建");
        
        File systemColumnsFile = new File(testDataDir + File.separator + "__system_columns__.tbl");
        Assertions.assertTrue(systemColumnsFile.exists(), "系统列文件应该存在");
        System.out.println("  ✓ 系统列文件已创建");
    }
    
    @Test
    void testDataPersistence() {
        System.out.println("\n测试2: 数据持久化验证");
        
        // 创建表
        List<ColumnPlan> columns = new ArrayList<>();
        columns.add(new ColumnPlan("id", "INT", 4, true, true, false, null, true));
        columns.add(new ColumnPlan("name", "VARCHAR", 50, true, false, false, null, false));
        
        ExecutionResult createResult = engine.createTable("products", columns, new ArrayList<>());
        Assertions.assertTrue(createResult.isSuccess(), "表创建应该成功");
        
        // 插入数据
        List<List<ExpressionPlan>> values = new ArrayList<>();
        List<ExpressionPlan> row1 = new ArrayList<>();
        row1.add(new LiteralExpressionPlan("1", "NUMBER"));
        row1.add(new LiteralExpressionPlan("笔记本电脑", "STRING"));
        values.add(row1);
        
        List<ExpressionPlan> row2 = new ArrayList<>();
        row2.add(new LiteralExpressionPlan("2", "NUMBER"));
        row2.add(new LiteralExpressionPlan("智能手机", "STRING"));
        values.add(row2);
        
        ExecutionResult insertResult = engine.insertData("products", new ArrayList<>(), values);
        Assertions.assertTrue(insertResult.isSuccess(), "数据插入应该成功");
        System.out.println("  ✓ 数据插入成功");
        
        // 验证数据是否写入文件
        File tableFile = new File(testDataDir + File.separator + "products.tbl");
        Assertions.assertTrue(tableFile.exists(), "表文件应该存在");
        
        // 读取文件内容验证
        try (BufferedReader reader = new BufferedReader(new FileReader(tableFile))) {
            String content = reader.lines().reduce("", (a, b) -> a + b + "\n");
            Assertions.assertTrue(content.contains("笔记本电脑"), "文件应该包含插入的数据");
            Assertions.assertTrue(content.contains("智能手机"), "文件应该包含插入的数据");
            System.out.println("  ✓ 数据已持久化到文件");
        } catch (IOException e) {
            Assertions.fail("读取表文件失败: " + e.getMessage());
        }
    }
    
    @Test
    void testDataRetrieval() {
        System.out.println("\n测试3: 数据检索验证");
        
        // 创建表并插入数据
        List<ColumnPlan> columns = new ArrayList<>();
        columns.add(new ColumnPlan("id", "INT", 4, true, true, false, null, true));
        columns.add(new ColumnPlan("name", "VARCHAR", 50, true, false, false, null, false));
        
        engine.createTable("orders", columns, new ArrayList<>());
        
        // 插入测试数据
        List<List<ExpressionPlan>> values = new ArrayList<>();
        List<ExpressionPlan> row = new ArrayList<>();
        row.add(new LiteralExpressionPlan("100", "NUMBER"));
        row.add(new LiteralExpressionPlan("测试订单", "STRING"));
        values.add(row);
        
        engine.insertData("orders", new ArrayList<>(), values);
        
        // 查询数据
        List<ExpressionPlan> selectList = new ArrayList<>();
        selectList.add(new IdentifierExpressionPlan("*"));
        
        ExecutionResult selectResult = engine.selectData("orders", selectList, null, null, null);
        Assertions.assertTrue(selectResult.isSuccess(), "数据查询应该成功");
        Assertions.assertNotNull(selectResult.getData(), "查询结果不应该为空");
        Assertions.assertTrue(selectResult.getData().size() > 0, "查询结果应该包含数据");
        
        System.out.println("  ✓ 数据查询成功，返回 " + selectResult.getData().size() + " 行数据");
        
        // 验证查询结果
        Map<String, Object> firstRow = selectResult.getData().get(0);
        Assertions.assertEquals("100", firstRow.get("id").toString(), "ID应该匹配");
        Assertions.assertEquals("测试订单", firstRow.get("name").toString(), "名称应该匹配");
        System.out.println("  ✓ 查询结果正确: " + firstRow);
    }
    
    @Test
    void testStorageAdapterIntegration() {
        System.out.println("\n测试4: 存储适配器集成验证");
        
        // 获取存储适配器信息
        StorageAdapter adapter = new StorageAdapter(testDataDir);
        
        // 创建表
        List<ColumnPlan> columns = new ArrayList<>();
        columns.add(new ColumnPlan("id", "INT", 4, true, true, false, null, true));
        columns.add(new ColumnPlan("value", "VARCHAR", 100, true, false, false, null, false));
        
        engine.createTable("test_table", columns, new ArrayList<>());
        
        // 直接使用存储适配器插入数据
        Map<String, Object> record = new HashMap<>();
        record.put("id", "999");
        record.put("value", "存储适配器测试");
        
        boolean insertSuccess = adapter.insertRecord("test_table", record);
        Assertions.assertTrue(insertSuccess, "存储适配器插入应该成功");
        System.out.println("  ✓ 存储适配器插入成功");
        
        // 使用存储适配器扫描数据
        List<Map<String, Object>> records = adapter.scanTable("test_table");
        Assertions.assertTrue(records.size() > 0, "应该能扫描到数据");
        
        // 验证扫描结果
        boolean found = false;
        for (Map<String, Object> scannedRecord : records) {
            if ("999".equals(scannedRecord.get("id")) && "存储适配器测试".equals(scannedRecord.get("value"))) {
                found = true;
                break;
            }
        }
        Assertions.assertTrue(found, "应该能找到插入的记录");
        System.out.println("  ✓ 存储适配器扫描成功，找到 " + records.size() + " 条记录");
        
        // 获取表统计信息
        StorageAdapter.TableStats stats = adapter.getTableStats("test_table");
        Assertions.assertNotNull(stats, "统计信息不应该为空");
        System.out.println("  ✓ 表统计信息: " + stats);
    }
    
    @Test
    void testSystemCatalogIntegration() {
        System.out.println("\n测试5: 系统目录集成验证");
        
        // 创建表
        List<ColumnPlan> columns = new ArrayList<>();
        columns.add(new ColumnPlan("id", "INT", 4, true, true, false, null, true));
        columns.add(new ColumnPlan("title", "VARCHAR", 200, true, false, false, null, false));
        
        engine.createTable("articles", columns, new ArrayList<>());
        
        // 验证系统目录是否正确维护
        CatalogManager catalogManager = engine.getCatalogManager();
        Assertions.assertTrue(catalogManager.tableExists("articles"), "系统目录应该包含新表");
        System.out.println("  ✓ 系统目录包含新表");
        
        // 获取表信息
        TableInfo tableInfo = catalogManager.getTable("articles");
        Assertions.assertNotNull(tableInfo, "表信息不应该为空");
        Assertions.assertEquals("articles", tableInfo.getName(), "表名应该正确");
        Assertions.assertEquals(2, tableInfo.getColumns().size(), "应该有2列");
        System.out.println("  ✓ 表信息正确: " + tableInfo.getName() + ", " + tableInfo.getColumns().size() + " 列");
        
        // 验证列信息
        List<ColumnInfo> columnList = new ArrayList<>(tableInfo.getColumns());
        ColumnInfo idColumn = columnList.get(0);
        Assertions.assertEquals("id", idColumn.getName(), "ID列名应该正确");
        Assertions.assertEquals("INT", idColumn.getDataType(), "ID列类型应该正确");
        Assertions.assertTrue(idColumn.isPrimaryKey(), "ID列应该是主键");
        
        ColumnInfo titleColumn = columnList.get(1);
        Assertions.assertEquals("title", titleColumn.getName(), "标题列名应该正确");
        Assertions.assertEquals("VARCHAR", titleColumn.getDataType(), "标题列类型应该正确");
        System.out.println("  ✓ 列信息正确");
        
        // 获取数据库统计信息
        Map<String, Object> dbInfo = engine.getDatabaseInfo();
        Assertions.assertNotNull(dbInfo, "数据库信息不应该为空");
        System.out.println("  ✓ 数据库信息: " + dbInfo);
    }
    
    @Test
    void testEndToEndWorkflow() {
        System.out.println("\n测试6: 端到端工作流验证");
        
        // 1. 创建表
        System.out.println("  步骤1: 创建表");
        List<ColumnPlan> columns = new ArrayList<>();
        columns.add(new ColumnPlan("id", "INT", 4, true, true, false, null, true));
        columns.add(new ColumnPlan("name", "VARCHAR", 50, true, false, false, null, false));
        columns.add(new ColumnPlan("price", "DECIMAL", 10, true, false, false, null, false));
        
        ExecutionResult createResult = engine.createTable("products", columns, new ArrayList<>());
        Assertions.assertTrue(createResult.isSuccess(), "表创建应该成功");
        System.out.println("    ✓ 表创建成功");
        
        // 2. 插入多条数据
        System.out.println("  步骤2: 插入数据");
        List<List<ExpressionPlan>> values = new ArrayList<>();
        
        String[][] testData = {
            {"1", "苹果", "5999.99"},
            {"2", "香蕉", "5.50"},
            {"3", "橙子", "8.80"}
        };
        
        for (String[] row : testData) {
            List<ExpressionPlan> rowValues = new ArrayList<>();
            rowValues.add(new LiteralExpressionPlan(row[0], "NUMBER"));
            rowValues.add(new LiteralExpressionPlan(row[1], "STRING"));
            rowValues.add(new LiteralExpressionPlan(row[2], "NUMBER"));
            values.add(rowValues);
        }
        
        ExecutionResult insertResult = engine.insertData("products", new ArrayList<>(), values);
        Assertions.assertTrue(insertResult.isSuccess(), "数据插入应该成功");
        System.out.println("    ✓ 插入了 " + testData.length + " 条数据");
        
        // 3. 查询所有数据
        System.out.println("  步骤3: 查询所有数据");
        List<ExpressionPlan> selectAll = new ArrayList<>();
        selectAll.add(new IdentifierExpressionPlan("*"));
        
        ExecutionResult selectAllResult = engine.selectData("products", selectAll, null, null, null);
        Assertions.assertTrue(selectAllResult.isSuccess(), "查询应该成功");
        Assertions.assertEquals(3, selectAllResult.getData().size(), "应该返回3条记录");
        System.out.println("    ✓ 查询到 " + selectAllResult.getData().size() + " 条记录");
        
        // 4. 条件查询
        System.out.println("  步骤4: 条件查询");
        ExpressionPlan whereClause = new BinaryExpressionPlan(
            new IdentifierExpressionPlan("price"),
            ">",
            new LiteralExpressionPlan("10", "NUMBER")
        );
        
        ExecutionResult selectWhereResult = engine.selectData("products", selectAll, whereClause, null, null);
        Assertions.assertTrue(selectWhereResult.isSuccess(), "条件查询应该成功");
        Assertions.assertEquals(1, selectWhereResult.getData().size(), "应该返回1条记录");
        System.out.println("    ✓ 条件查询返回 " + selectWhereResult.getData().size() + " 条记录");
        
        // 5. 删除数据
        System.out.println("  步骤5: 删除数据");
        ExpressionPlan deleteWhere = new BinaryExpressionPlan(
            new IdentifierExpressionPlan("id"),
            "=",
            new LiteralExpressionPlan("2", "NUMBER")
        );
        
        ExecutionResult deleteResult = engine.deleteData("products", deleteWhere);
        Assertions.assertTrue(deleteResult.isSuccess(), "删除应该成功");
        System.out.println("    ✓ 删除操作成功");
        
        // 6. 验证删除结果
        System.out.println("  步骤6: 验证删除结果");
        ExecutionResult finalSelectResult = engine.selectData("products", selectAll, null, null, null);
        Assertions.assertTrue(finalSelectResult.isSuccess(), "最终查询应该成功");
        Assertions.assertEquals(2, finalSelectResult.getData().size(), "应该剩余2条记录");
        System.out.println("    ✓ 最终剩余 " + finalSelectResult.getData().size() + " 条记录");
        
        // 7. 验证文件持久化
        System.out.println("  步骤7: 验证文件持久化");
        File tableFile = new File(testDataDir + File.separator + "products.tbl");
        Assertions.assertTrue(tableFile.exists(), "表文件应该存在");
        Assertions.assertTrue(tableFile.length() > 0, "表文件应该有内容");
        System.out.println("    ✓ 数据已持久化到文件");
        
        System.out.println("  ✓ 端到端工作流测试完成");
    }
    
    private void cleanupTestDirectory() {
        File testDir = new File(testDataDir);
        if (testDir.exists()) {
            File[] files = testDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            testDir.delete();
        }
    }
}
