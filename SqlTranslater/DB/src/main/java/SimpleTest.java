import com.database.engine.DatabaseEngine;
import com.database.engine.ExecutionResult;
import java.io.File;

public class SimpleTest {
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
            
            // 3. Manually clean up table files (since DROP TABLE is not supported)
            System.out.println("\n3. Cleaning up existing table files...");
            
            boolean cleaned = cleanupTableFiles("test_table");
            System.out.println("Table cleanup result: " + (cleaned ? "Success" : "No files to clean"));
            
            // 3.5. Re-initialize the engine after cleanup to refresh system tables
            if (cleaned) {
                System.out.println("Re-initializing engine after cleanup...");
                // Clear any in-memory catalog data before reinitializing
                engine.getCatalogManager().clear();
                
                // Small delay to ensure file system operations complete
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // ignore
                }
                
                engine = new DatabaseEngine("test_db", "./test_data");
                boolean reinitResult = engine.initialize();
                if (!reinitResult) {
                    System.out.println("Failed to re-initialize engine, exiting...");
                    return;
                }
                System.out.println("Engine re-initialized successfully");
            }
            
            System.out.println("\n4. Creating table...");
            ExecutionResult result1 = engine.executeSQL("CREATE TABLE test_table (id INT PRIMARY KEY, name VARCHAR(50), age INT)");
            System.out.println("Create table result: " + result1.isSuccess() + " - " + result1.getMessage());
            
            if (!result1.isSuccess()) {
                System.out.println("Failed to create table, exiting...");
                return;
            }
            
            // 5. Simple INSERT
            System.out.println("\n5. Testing simple INSERT...");
            ExecutionResult result2 = engine.executeSQL("INSERT INTO test_table VALUES (1, 'Alice', 25)");
            System.out.println("Insert result: " + result2.isSuccess() + " - " + result2.getMessage());
            
            // 6. Query to verify
            System.out.println("\n6. Querying data...");
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
    
    /**
     * Clean up table files manually (workaround for missing DROP TABLE support)
     */
    private static boolean cleanupTableFiles(String tableName) {
        boolean anyDeleted = false;
        String dataDir = "./test_data";
        
        // List of files to clean up
        String[] filesToDelete = {
            dataDir + File.separator + tableName + ".tbl",           // Table data file
            dataDir + File.separator + "__system_tables__.tbl",     // System tables metadata
            dataDir + File.separator + "__system_columns__.tbl",    // System columns metadata
            dataDir + File.separator + "__system_constraints__.tbl" // System constraints metadata
        };
        
        for (String filePath : filesToDelete) {
            File file = new File(filePath);
            if (file.exists()) {
                if (file.delete()) {
                    System.out.println("  Deleted: " + file.getName());
                    anyDeleted = true;
                } else {
                    System.out.println("  Failed to delete: " + file.getName());
                }
            }
        }
        
        // Also try to clean up any remaining files in data directory
        File dataDirectory = new File(dataDir);
        if (dataDirectory.exists() && dataDirectory.isDirectory()) {
            File[] files = dataDirectory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".tbl")) {
                        String fileName = file.getName();
                        // Delete any remaining .tbl files that might contain table references
                        if (fileName.contains(tableName) || fileName.startsWith("__system_")) {
                            if (file.delete()) {
                                System.out.println("  Additional cleanup - Deleted: " + fileName);
                                anyDeleted = true;
                            }
                        }
                    }
                }
            }
        }
        
        return anyDeleted;
    }
}
