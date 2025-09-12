import com.database.engine.DatabaseEngine;
import com.database.engine.ExecutionResult;

public class simple_test {
    public static void main(String[] args) {
        try {
            System.out.println("=== Simple Database Test ===");
            
            // 1. Initialize database engine
            System.out.println("1. Initializing database engine...");
            DatabaseEngine engine = new DatabaseEngine("test_db", "./test_data");
            
            // 2. Initialize the engine
            System.out.println("2. Initializing engine components...");
            boolean initResult = engine.initialize();
            if (!initResult) {
                System.out.println("Failed to initialize engine, exiting...");
                return;
            }
            System.out.println("Database engine initialized successfully");
            
            // 3. Create table
            System.out.println("\n3. Creating table...");
            ExecutionResult result1 = engine.executeSQL("CREATE TABLE test_table (id INT PRIMARY KEY, name VARCHAR(50), age INT)");
            System.out.println("Create table result: " + result1.isSuccess() + " - " + result1.getMessage());
            
            if (!result1.isSuccess()) {
                System.out.println("Failed to create table, exiting...");
                return;
            }
            
            // 4. Simple INSERT
            System.out.println("\n4. Testing simple INSERT...");
            ExecutionResult result2 = engine.executeSQL("INSERT INTO test_table VALUES (1, 'Alice', 25)");
            System.out.println("Insert result: " + result2.isSuccess() + " - " + result2.getMessage());
            
            // 5. Query to verify
            System.out.println("\n5. Querying data...");
            ExecutionResult result3 = engine.executeSQL("SELECT * FROM test_table");
            System.out.println("Query result: " + result3.isSuccess() + " - " + result3.getMessage());
            
            if (result3.getData() != null) {
                System.out.println("Data rows: " + result3.getData().size());
                for (java.util.Map<String, Object> row : result3.getData()) {
                    System.out.println("  Row: " + row);
                }
            }
            
            System.out.println("\n=== Test completed ===");
            
        } catch (Exception e) {
            System.err.println("Error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
