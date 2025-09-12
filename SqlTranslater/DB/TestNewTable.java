import com.database.engine.DatabaseEngine;
import com.database.engine.ExecutionResult;

public class TestNewTable {
    public static void main(String[] args) {
        System.out.println("=== Test New Table Storage Location ===");
        
        // Test default data directory
        System.out.println("\n1. Using default data directory (./data):");
        testWithDataDirectory("./data", "new_table_default");
        
        // Test test_data directory
        System.out.println("\n2. Using test_data directory:");
        testWithDataDirectory("./test_data", "new_table_test");
    }
    
    private static void testWithDataDirectory(String dataDir, String tableName) {
        try {
            System.out.println("  Data Directory: " + dataDir);
            System.out.println("  Table Name: " + tableName);
            
            // Create database engine
            DatabaseEngine engine = new DatabaseEngine("test_db", dataDir);
            
            if (!engine.initialize()) {
                System.out.println("  FAILED: Engine initialization failed");
                return;
            }
            
            System.out.println("  SUCCESS: Engine initialized");
            
            // Create new table
            String createSQL = String.format(
                "CREATE TABLE %s (id INT PRIMARY KEY, name VARCHAR(50), description TEXT)", 
                tableName
            );
            
            ExecutionResult result = engine.executeSQL(createSQL);
            System.out.println("  Create table result: " + (result.isSuccess() ? "SUCCESS" : "FAILED"));
            System.out.println("  Message: " + result.getMessage());
            
            if (result.isSuccess()) {
                // Insert test data
                String insertSQL = String.format(
                    "INSERT INTO %s VALUES (1, 'Test Item', 'This is a test description')", 
                    tableName
                );
                ExecutionResult insertResult = engine.executeSQL(insertSQL);
                System.out.println("  Insert result: " + (insertResult.isSuccess() ? "SUCCESS" : "FAILED"));
                
                // Check if file exists
                java.io.File tableFile = new java.io.File(dataDir + "/" + tableName + ".tbl");
                System.out.println("  Table file path: " + tableFile.getAbsolutePath());
                System.out.println("  File exists: " + (tableFile.exists() ? "YES" : "NO"));
                if (tableFile.exists()) {
                    System.out.println("  File size: " + tableFile.length() + " bytes");
                }
            }
            
        } catch (Exception e) {
            System.out.println("  ERROR: " + e.getMessage());
        }
    }
}
