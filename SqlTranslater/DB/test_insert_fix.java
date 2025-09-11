import com.database.engine.DatabaseEngine;
import com.database.engine.ExecutionResult;

public class test_insert_fix {
    public static void main(String[] args) {
        DatabaseEngine engine = new DatabaseEngine("test_db", "./test_data");
        
        // Initialize the engine
        if (!engine.initialize()) {
            System.err.println("Failed to initialize database engine!");
            return;
        }
        
        System.out.println("=== Testing INSERT statement fix ===");
        
        // 1. Create test table
        System.out.println("\n1. Create test table:");
        ExecutionResult result1 = engine.executeSQL("CREATE TABLE test_table (id INT, name VARCHAR(50), age INT)");
        System.out.println("Result: " + result1.getMessage());
        
        // 2. Test INSERT without column names (should work now)
        System.out.println("\n2. Test INSERT without column names:");
        ExecutionResult result2 = engine.executeSQL("INSERT INTO test_table VALUES (1, 'Alice', 25)");
        System.out.println("Result: " + result2.getMessage());
        
        // 3. Test INSERT with column names
        System.out.println("\n3. Test INSERT with column names:");
        ExecutionResult result3 = engine.executeSQL("INSERT INTO test_table (id, name, age) VALUES (2, 'Bob', 30)");
        System.out.println("Result: " + result3.getMessage());
        
        // 4. Test INSERT with partial column names
        System.out.println("\n4. Test INSERT with partial column names:");
        ExecutionResult result4 = engine.executeSQL("INSERT INTO test_table (name, age) VALUES ('Charlie', 35)");
        System.out.println("Result: " + result4.getMessage());
        
        // 5. Query to verify data
        System.out.println("\n5. Query to verify data:");
        ExecutionResult result5 = engine.executeSQL("SELECT * FROM test_table");
        System.out.println("Result: " + result5.getMessage());
        if (result5.getData() != null) {
            System.out.println("Data:");
            for (java.util.Map<String, Object> row : result5.getData()) {
                System.out.println("  " + row);
            }
        }
        
        // 6. Test column count mismatch
        System.out.println("\n6. Test column count mismatch:");
        ExecutionResult result6 = engine.executeSQL("INSERT INTO test_table VALUES (4, 'David')");
        System.out.println("Result: " + result6.getMessage());
        
        System.out.println("\n=== Test completed ===");
    }
}