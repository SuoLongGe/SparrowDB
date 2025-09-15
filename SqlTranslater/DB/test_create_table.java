import com.database.engine.DatabaseEngine;

public class test_create_table {
    public static void main(String[] args) {
        try {
            System.out.println("测试建表时的系统表插入...");
            
            // 创建数据库引擎
            DatabaseEngine engine = new DatabaseEngine("main", "data/main");
            System.out.println("数据库引擎创建成功");
            
            // 创建一个测试表
            System.out.println("创建测试表...");
            String createTableSQL = "CREATE TABLE test_metadata (" +
                "id INT PRIMARY KEY, " +
                "name VARCHAR(50) NOT NULL, " +
                "age INT, " +
                "email VARCHAR(100) UNIQUE" +
                ")";
            
            var createResult = engine.executeSQL(createTableSQL);
            System.out.println("创建表结果: " + createResult.isSuccess() + " - " + createResult.getMessage());
            
            if (createResult.isSuccess()) {
                // 检查系统表记录
                System.out.println("\n检查__system_tables__记录...");
                var tablesResult = engine.executeSQL("SELECT * FROM __system_tables__ WHERE table_name = 'test_metadata'");
                if (tablesResult.isSuccess() && tablesResult.getData() != null) {
                    System.out.println("表记录数: " + tablesResult.getData().size());
                    for (var row : tablesResult.getData()) {
                        System.out.println("表记录: " + row);
                    }
                }
                
                System.out.println("\n检查__system_columns__记录...");
                var columnsResult = engine.executeSQL("SELECT * FROM __system_columns__ WHERE table_name = 'test_metadata'");
                if (columnsResult.isSuccess() && columnsResult.getData() != null) {
                    System.out.println("列记录数: " + columnsResult.getData().size());
                    for (var row : columnsResult.getData()) {
                        System.out.println("列记录: " + row);
                    }
                } else {
                    System.out.println("没有找到列记录！");
                }
                
                System.out.println("\n检查__system_constraints__记录...");
                var constraintsResult = engine.executeSQL("SELECT * FROM __system_constraints__ WHERE table_name = 'test_metadata'");
                if (constraintsResult.isSuccess() && constraintsResult.getData() != null) {
                    System.out.println("约束记录数: " + constraintsResult.getData().size());
                    for (var row : constraintsResult.getData()) {
                        System.out.println("约束记录: " + row);
                    }
                } else {
                    System.out.println("没有找到约束记录！");
                }
            }
            
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
