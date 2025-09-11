import com.database.engine.DatabaseEngine;
import com.database.engine.ExecutionResult;
import com.sqlcompiler.execution.*;
import com.sqlcompiler.catalog.*;
import java.util.*;
import java.io.File;

/**
 * File Storage Location Demo
 * Shows where system table files and user table files are stored
 */
public class FileLocationDemo {
    public static void main(String[] args) {
        System.out.println("=== SparrowDB File Storage Location Demo ===\n");
        
        // 1. Default data directory
        String defaultDataDir = "./data";
        System.out.println("1. Default data directory: " + new File(defaultDataDir).getAbsolutePath());
        
        // 2. Test data directory
        String testDataDir = "./test_data";
        System.out.println("2. Test data directory: " + new File(testDataDir).getAbsolutePath());
        
        // 3. Custom data directory
        String customDataDir = "./my_database";
        System.out.println("3. Custom data directory: " + new File(customDataDir).getAbsolutePath());
        
        System.out.println("\n=== System Table Files Storage Location ===");
        
        // System table file locations
        String[] systemTables = {
            "__system_tables__.tbl",    // System table metadata
            "__system_columns__.tbl",   // System column metadata  
            "__system_constraints__.tbl" // System constraint metadata
        };
        
        for (String systemTable : systemTables) {
            String filePath = defaultDataDir + File.separator + systemTable;
            File file = new File(filePath);
            System.out.println("System Table: " + systemTable);
            System.out.println("  Storage Location: " + file.getAbsolutePath());
            System.out.println("  File Exists: " + (file.exists() ? "Yes" : "No"));
            if (file.exists()) {
                System.out.println("  File Size: " + file.length() + " bytes");
                System.out.println("  Last Modified: " + new Date(file.lastModified()));
            }
            System.out.println();
        }
        
        System.out.println("=== User Table Files Storage Location ===");
        
        // User table file locations
        String[] userTables = {
            "users.tbl",        // User table
            "products.tbl",     // Product table
            "students.tbl",     // Student table
            "courses.tbl"       // Course table
        };
        
        for (String userTable : userTables) {
            String filePath = defaultDataDir + File.separator + userTable;
            File file = new File(filePath);
            System.out.println("User Table: " + userTable);
            System.out.println("  Storage Location: " + file.getAbsolutePath());
            System.out.println("  File Exists: " + (file.exists() ? "Yes" : "No"));
            if (file.exists()) {
                System.out.println("  File Size: " + file.length() + " bytes");
                System.out.println("  Last Modified: " + new Date(file.lastModified()));
            }
            System.out.println();
        }
        
        System.out.println("=== File Storage Rules ===");
        System.out.println("1. All table files are stored in the specified data directory");
        System.out.println("2. File naming rule: table_name + \".tbl\" suffix");
        System.out.println("3. System table files start with \"__system_\"");
        System.out.println("4. User table files use table name directly");
        System.out.println("5. File format: metadata header + data pages");
        
        System.out.println("\n=== New Table Creation Storage Location Demo ===");
        
        // Demo creating new table
        try {
            DatabaseEngine engine = new DatabaseEngine("demo_db", customDataDir);
            engine.initialize();
            
            // Create new table
            List<ColumnPlan> columns = new ArrayList<>();
            columns.add(new ColumnPlan("id", "INT", 4, true, true, false, null, true));
            columns.add(new ColumnPlan("name", "VARCHAR", 100, true, false, false, null, false));
            columns.add(new ColumnPlan("description", "TEXT", 0, false, false, false, null, false));
            
            ExecutionResult result = engine.createTable("demo_table", columns, new ArrayList<>());
            
            if (result.isSuccess()) {
                System.out.println("SUCCESS: New table 'demo_table' created successfully");
                
                // Show new table file location
                String newTableFile = customDataDir + File.separator + "demo_table.tbl";
                File newFile = new File(newTableFile);
                System.out.println("  Storage Location: " + newFile.getAbsolutePath());
                System.out.println("  File Exists: " + (newFile.exists() ? "Yes" : "No"));
                
                // Show corresponding system table files
                String systemTablesFile = customDataDir + File.separator + "__system_tables__.tbl";
                String systemColumnsFile = customDataDir + File.separator + "__system_columns__.tbl";
                
                System.out.println("  System Tables File: " + new File(systemTablesFile).getAbsolutePath());
                System.out.println("  System Columns File: " + new File(systemColumnsFile).getAbsolutePath());
                
            } else {
                System.out.println("FAILED: New table creation failed: " + result.getMessage());
            }
            
            engine.shutdown();
            
        } catch (Exception e) {
            System.err.println("Error during demo: " + e.getMessage());
        }
        
        System.out.println("\n=== Summary ===");
        System.out.println("- System table files: Store database metadata information");
        System.out.println("- User table files: Store user data");
        System.out.println("- All files: Unified storage in specified data directory");
        System.out.println("- File format: Supports metadata header and data page structure");
        System.out.println("- Storage location: Fully configurable, supports multiple database instances");
    }
}
