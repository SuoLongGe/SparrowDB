import com.database.engine.DatabaseEngine;
import com.database.engine.ExecutionResult;

public class CompilerTest {
    public static void main(String[] args) {
        try {
            System.out.println("=== SQL Compiler Full Test ===\n");
            
            // 1. Initialize database engine
            System.out.println("1. Initializing database engine...");
            DatabaseEngine engine = new DatabaseEngine("compiler_test_db", "./compiler_test_data");
            
            boolean initResult = engine.initialize();
            if (!initResult) {
                System.out.println("Database engine initialization failed, exiting test");
                return;
            }
            System.out.println("✓ Database engine initialized successfully\n");
            
            // 2. Test CREATE TABLE
            System.out.println("2. Testing CREATE TABLE statement...");
            ExecutionResult result1 = engine.executeSQL("CREATE TABLE students (id INT PRIMARY KEY, name VARCHAR(100), age INT, gpa DECIMAL(3,2))");
            System.out.println("CREATE TABLE result: " + (result1.isSuccess() ? "✓ SUCCESS" : "✗ FAILED") + " - " + result1.getMessage());
            
            if (!result1.isSuccess()) {
                System.out.println("Table creation failed, exiting test");
                return;
            }
            
            // 3. Test INSERT
            System.out.println("\n3. Testing INSERT statements...");
            String[] insertSQLs = {
                "INSERT INTO students VALUES (1, 'Alice', 20, 3.5)",
                "INSERT INTO students VALUES (2, 'Bob', 21, 3.2)",
                "INSERT INTO students VALUES (3, 'Charlie', 19, 3.8)"
            };
            
            for (String sql : insertSQLs) {
                ExecutionResult result = engine.executeSQL(sql);
                System.out.println("INSERT result: " + (result.isSuccess() ? "✓" : "✗") + " - " + result.getMessage());
            }
            
            // 4. Test SELECT
            System.out.println("\n4. Testing SELECT statement...");
            ExecutionResult selectResult = engine.executeSQL("SELECT * FROM students");
            System.out.println("SELECT result: " + (selectResult.isSuccess() ? "✓ SUCCESS" : "✗ FAILED") + " - " + selectResult.getMessage());
            
            if (selectResult.isSuccess() && selectResult.getData() != null) {
                System.out.println("Found " + selectResult.getData().size() + " rows:");
                for (java.util.Map<String, Object> row : selectResult.getData()) {
                    System.out.println("  " + row);
                }
            }
            
            // 5. Test SELECT with WHERE
            System.out.println("\n5. Testing SELECT with WHERE condition...");
            ExecutionResult whereResult = engine.executeSQL("SELECT name, age FROM students WHERE age > 20");
            System.out.println("WHERE SELECT result: " + (whereResult.isSuccess() ? "✓ SUCCESS" : "✗ FAILED") + " - " + whereResult.getMessage());
            
            if (whereResult.isSuccess() && whereResult.getData() != null) {
                System.out.println("Matching " + whereResult.getData().size() + " rows:");
                for (java.util.Map<String, Object> row : whereResult.getData()) {
                    System.out.println("  " + row);
                }
            }
            
            // 6. Test UPDATE
            System.out.println("\n6. Testing UPDATE statement...");
            ExecutionResult updateResult = engine.executeSQL("UPDATE students SET age = 22 WHERE name = 'Alice'");
            System.out.println("UPDATE result: " + (updateResult.isSuccess() ? "✓ SUCCESS" : "✗ FAILED") + " - " + updateResult.getMessage());
            
            // 7. Test DELETE
            System.out.println("\n7. Testing DELETE statement...");
            ExecutionResult deleteResult = engine.executeSQL("DELETE FROM students WHERE id = 3");
            System.out.println("DELETE result: " + (deleteResult.isSuccess() ? "✓ SUCCESS" : "✗ FAILED") + " - " + deleteResult.getMessage());
            
            // 8. Final verification
            System.out.println("\n8. Final data verification...");
            ExecutionResult finalResult = engine.executeSQL("SELECT * FROM students");
            if (finalResult.isSuccess() && finalResult.getData() != null) {
                System.out.println("Final remaining " + finalResult.getData().size() + " rows:");
                for (java.util.Map<String, Object> row : finalResult.getData()) {
                    System.out.println("  " + row);
                }
            }
            
            System.out.println("\n=== SQL Compiler Test Completed ===");
            
        } catch (Exception e) {
            System.err.println("Error occurred during test: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
