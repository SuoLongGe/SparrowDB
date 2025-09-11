import com.database.engine.*;
import com.sqlcompiler.execution.*;
import com.sqlcompiler.SQLCompiler;
import java.util.*;

public class debug_catalog {
    public static void main(String[] args) {
        System.out.println("=== Debug Catalog Sync Issue ===");
        
        try {
            // Create database engine
            DatabaseEngine engine = new DatabaseEngine("debug_db", "./debug_data");
            
            if (!engine.initialize()) {
                System.err.println("Database engine initialization failed!");
                return;
            }
            
            System.out.println("Database engine initialized successfully");
            
            // Test table creation
            List<ColumnPlan> columns = Arrays.asList(
                new ColumnPlan("id", "INT", 0, false, true, false, null, false),
                new ColumnPlan("name", "VARCHAR", 50, false, false, false, null, false)
            );
            
            ExecutionResult result = engine.createTable("users", columns, new ArrayList<>());
            if (result.isSuccess()) {
                System.out.println("Table 'users' created successfully");
            } else {
                System.out.println("Table creation failed: " + result.getMessage());
                return;
            }
            
            // Check if table exists in CatalogManager
            CatalogManager catalogManager = engine.getCatalogManager();
            if (catalogManager.tableExists("users")) {
                System.out.println("[OK] Table 'users' exists in CatalogManager");
            } else {
                System.out.println("[ERROR] Table 'users' NOT found in CatalogManager");
            }
            
            // Check if table exists in SQLCompiler's catalog
            SQLCompiler compiler = engine.getSQLCompiler();
            if (compiler.getCatalog().tableExists("users")) {
                System.out.println("[OK] Table 'users' exists in SQLCompiler's catalog");
            } else {
                System.out.println("[ERROR] Table 'users' NOT found in SQLCompiler's catalog");
            }
            
            // List all tables in both catalogs
            System.out.println("\nTables in CatalogManager: " + catalogManager.getAllTableNames());
            System.out.println("Tables in SQLCompiler: " + compiler.getCatalog().getAllTableNames());
            
            engine.shutdown();
            
        } catch (Exception e) {
            System.err.println("Error during debugging: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
