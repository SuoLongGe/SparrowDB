import com.database.engine.DatabaseEngine;
import com.database.engine.ExecutionResult;

public class TestSQL {
    public static void main(String[] args) {
        try {
            System.out.println("=== SQL Compiler Test ===");
            
            // Initialize database engine
            DatabaseEngine engine = new DatabaseEngine("test_db_new", "./test_data_new");
            boolean initResult = engine.initialize();
            
            if (!initResult) {
                System.out.println("Failed to initialize database engine");
                return;
            }
            System.out.println("Database engine initialized successfully");
            
            // Test CREATE TABLE
            System.out.println("\nTesting CREATE TABLE...");
            ExecutionResult result1 = engine.executeSQL("CREATE TABLE persons (id INT PRIMARY KEY, name VARCHAR(50), age INT)");
            System.out.println("CREATE TABLE result: " + result1.isSuccess() + " - " + result1.getMessage());
            
            if (!result1.isSuccess()) {
                System.out.println("Failed to create table, exiting...");
                return;
            }
            
            // Test INSERT
            System.out.println("\nTesting INSERT...");
            ExecutionResult result2 = engine.executeSQL("INSERT INTO persons VALUES (1, 'John', 25)");
            System.out.println("INSERT result: " + result2.isSuccess() + " - " + result2.getMessage());
            
            // Test SELECT
            System.out.println("\nTesting SELECT...");
            ExecutionResult result3 = engine.executeSQL("SELECT * FROM persons");
            System.out.println("SELECT result: " + result3.isSuccess() + " - " + result3.getMessage());
            
            if (result3.getData() != null) {
                System.out.println("Data rows: " + result3.getData().size());
                for (java.util.Map<String, Object> row : result3.getData()) {
                    System.out.println("  Row: " + row);
                }
            }
            
            System.out.println("\n=== Test Completed ===");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
