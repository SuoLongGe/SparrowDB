public class StorageEngineTest {
    public static void main(String[] args) {
        System.out.println("=== SparrowDB 存储引擎测试 ===");
        
        // 创建存储引擎
        StorageEngine engine = new StorageEngine("test_db.db");
        
        try {
            // 创建表
            System.out.println("\n=== 创建表 ===");
            boolean tableCreated = engine.createTable("users");
            System.out.println("创建表 'users': " + (tableCreated ? "成功" : "失败"));
            
            // 插入测试数据
            System.out.println("\n=== 插入测试数据 ===");
            boolean success1 = engine.insertRecord("users", "1,John,25,Engineer");
            boolean success2 = engine.insertRecord("users", "2,Jane,30,Manager");
            boolean success3 = engine.insertRecord("users", "3,Bob,28,Developer");
            
            System.out.println("插入记录1: " + (success1 ? "成功" : "失败"));
            System.out.println("插入记录2: " + (success2 ? "成功" : "失败"));
            System.out.println("插入记录3: " + (success3 ? "成功" : "失败"));
            
            // 强制刷新所有页面到磁盘
            System.out.println("\n=== 刷新数据到磁盘 ===");
            engine.flushAllPages();
            System.out.println("数据已刷新到磁盘");
            
            // 查询数据
            System.out.println("\n=== 查询数据 ===");
            String tableData = engine.getTableData("users");
            if (tableData != null && !tableData.isEmpty()) {
                System.out.println("表 'users' 数据:");
                System.out.println(tableData);
            } else {
                System.out.println("表 'users' 为空或不存在");
            }
            
            // 显示表信息
            System.out.println("\n=== 表信息 ===");
            engine.showTableInfo("users");
            
        } catch (Exception e) {
            System.err.println("测试过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 关闭存储引擎
            engine.close();
            System.out.println("\n存储引擎已关闭");
        }
    }
}
