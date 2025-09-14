import com.database.engine.DatabaseEngine;
import com.database.engine.ExecutionResult;

/**
 * Database functions test
 */
public class TestDatabaseFunctions {
    
    public static void main(String[] args) {
        System.out.println("=== Database Functions Test ===\n");
        
        // Create database engine
        DatabaseEngine engine = new DatabaseEngine("TestFunctionsDB", "./data");
        if (!engine.initialize()) {
            System.err.println("Database engine initialization failed");
            return;
        }
        
        // Setup test data
        setupTestData(engine);
        
        // Test math functions
        testMathFunctions(engine);
        
        // Test string functions
        testStringFunctions(engine);
        
        // Test date functions
        testDateFunctions(engine);
        
        System.out.println("\n=== Database Functions Test Complete ===");
    }
    
    /**
     * Setup test data
     */
    private static void setupTestData(DatabaseEngine engine) {
        System.out.println("1. Setting up test data");
        
        // Create test table
        String createTableSQL = "CREATE TABLE test_functions (" +
            "id INT PRIMARY KEY, " +
            "name VARCHAR(50), " +
            "age INT, " +
            "salary DECIMAL(10,2), " +
            "email VARCHAR(100)" +
            ")";
        
        ExecutionResult result = engine.executeSQL(createTableSQL);
        if (result.isSuccess()) {
            System.out.println("+ Test table created successfully");
        } else {
            System.out.println("- Test table creation failed: " + result.getMessage());
        }
        
        // Insert test data
        String[] insertSQLs = {
            "INSERT INTO test_functions VALUES (1, 'Alice', 25, 5000.50, 'alice@example.com')",
            "INSERT INTO test_functions VALUES (2, 'Bob', 30, 7500.75, 'bob@test.org')",
            "INSERT INTO test_functions VALUES (3, 'Charlie', 35, 9000.00, 'charlie@demo.net')",
            "INSERT INTO test_functions VALUES (4, 'Diana', 28, 6200.25, 'diana@sample.com')"
        };
        
        for (String sql : insertSQLs) {
            result = engine.executeSQL(sql);
            if (!result.isSuccess()) {
                System.out.println("- Data insertion failed: " + result.getMessage());
            }
        }
        System.out.println("+ Test data insertion complete\n");
    }
    
    /**
     * Test math functions
     */
    private static void testMathFunctions(DatabaseEngine engine) {
        System.out.println("2. Testing math functions");
        
        String[] mathTests = {
            // Basic math functions
            "SELECT name, age, ABS(age - 30) FROM test_functions",
            "SELECT name, salary, ROUND(salary, 0) FROM test_functions",
            "SELECT name, salary, CEIL(salary) FROM test_functions",
            "SELECT name, salary, FLOOR(salary) FROM test_functions",
            "SELECT name, age, SQRT(age) FROM test_functions",
            "SELECT name, age, POWER(age, 2) FROM test_functions",
            "SELECT name, age, MOD(age, 3) FROM test_functions"
        };
        
        for (String sql : mathTests) {
            System.out.println("Executing: " + sql);
            ExecutionResult result = engine.executeSQL(sql);
            if (result.isSuccess()) {
                System.out.println("+ Success");
                printResults(result);
            } else {
                System.out.println("- Failed: " + result.getMessage());
            }
            System.out.println();
        }
    }
    
    /**
     * Test string functions
     */
    private static void testStringFunctions(DatabaseEngine engine) {
        System.out.println("3. Testing string functions");
        
        String[] stringTests = {
            // String functions
            "SELECT name, UPPER(name) FROM test_functions",
            "SELECT name, LOWER(name) FROM test_functions",
            "SELECT name, LENGTH(name) FROM test_functions",
            "SELECT email, SUBSTRING(email, 1, 5) FROM test_functions",
            "SELECT name, TRIM(name) FROM test_functions"
        };
        
        for (String sql : stringTests) {
            System.out.println("Executing: " + sql);
            ExecutionResult result = engine.executeSQL(sql);
            if (result.isSuccess()) {
                System.out.println("+ Success");
                printResults(result);
            } else {
                System.out.println("- Failed: " + result.getMessage());
            }
            System.out.println();
        }
    }
    
    /**
     * Test date functions
     */
    private static void testDateFunctions(DatabaseEngine engine) {
        System.out.println("4. Testing date functions");
        
        String[] dateTests = {
            // Date functions
            "SELECT NOW() FROM test_functions LIMIT 1",
            "SELECT CURRENT_DATE() FROM test_functions LIMIT 1",
            "SELECT CURRENT_TIME() FROM test_functions LIMIT 1"
        };
        
        for (String sql : dateTests) {
            System.out.println("Executing: " + sql);
            ExecutionResult result = engine.executeSQL(sql);
            if (result.isSuccess()) {
                System.out.println("+ Success");
                printResults(result);
            } else {
                System.out.println("- Failed: " + result.getMessage());
            }
            System.out.println();
        }
    }
    
    /**
     * Print query results
     */
    private static void printResults(ExecutionResult result) {
        if (result.getData() != null && result.getData() instanceof java.util.List) {
            @SuppressWarnings("unchecked")
            java.util.List<java.util.Map<String, Object>> rows = 
                (java.util.List<java.util.Map<String, Object>>) result.getData();
            
            if (!rows.isEmpty()) {
                // Print header
                java.util.Set<String> columns = rows.get(0).keySet();
                System.out.print("Result: ");
                for (String col : columns) {
                    System.out.print(col + "\t");
                }
                System.out.println();
                
                // Print data rows
                for (java.util.Map<String, Object> row : rows) {
                    System.out.print("        ");
                    for (String col : columns) {
                        Object value = row.get(col);
                        System.out.print((value != null ? value.toString() : "NULL") + "\t");
                    }
                    System.out.println();
                }
            } else {
                System.out.println("Result: No data");
            }
        } else {
            System.out.println("Result: " + result.getMessage());
        }
    }
}