import com.database.engine.DatabaseEngine;
import com.sqlcompiler.SQLCompiler;
import com.database.engine.ExecutionResult;

/**
 * User-defined function functionality test
 */
public class FunctionTest {
    public static void main(String[] args) {
        try {
            // Create database engine
            System.out.println("=== User-Defined Function Test ===");
            DatabaseEngine engine = new DatabaseEngine("TestDB", "data");
            engine.initialize();
            
            System.out.println("\n--- Test 1: Create Function ---");
            testCreateFunction(engine);
            
            System.out.println("\n--- Test 2: Call Function ---");
            testCallFunction(engine);
            
            System.out.println("\n--- Test 3: Drop Function ---");
            testDropFunction(engine);
            
            System.out.println("\n--- Test 4: Create Complex Function ---");
            testComplexFunction(engine);
            
            System.out.println("\n=== All tests completed ===");
            
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testCreateFunction(DatabaseEngine engine) throws Exception {
        String sql = "CREATE FUNCTION add_numbers(a INT, b INT) RETURNS INT BEGIN RETURN a + b; END";
        
        System.out.println("SQL: " + sql);
        ExecutionResult result = engine.executeSQL(sql);
        
        if (result.isSuccess()) {
            System.out.println("✅ Function created successfully: " + result.getMessage());
        } else {
            System.out.println("❌ Failed to create function: " + result.getMessage());
        }
    }
    
    private static void testCallFunction(DatabaseEngine engine) throws Exception {
        String sql = "CALL add_numbers(5, 3)";
        
        System.out.println("SQL: " + sql);
        ExecutionResult result = engine.executeSQL(sql);
        
        if (result.isSuccess()) {
            System.out.println("✅ Function called successfully: " + result.getMessage());
            if (result.getData() != null && !result.getData().isEmpty()) {
                System.out.println("Result: " + result.getData().get(0).get(0));
            }
        } else {
            System.out.println("❌ Failed to call function: " + result.getMessage());
        }
    }
    
    private static void testDropFunction(DatabaseEngine engine) throws Exception {
        String sql = "DROP FUNCTION add_numbers";
        
        System.out.println("SQL: " + sql);
        ExecutionResult result = engine.executeSQL(sql);
        
        if (result.isSuccess()) {
            System.out.println("✅ Function dropped successfully: " + result.getMessage());
        } else {
            System.out.println("❌ Failed to drop function: " + result.getMessage());
        }
    }
    
    private static void testComplexFunction(DatabaseEngine engine) throws Exception {
        // Create a function that multiplies two numbers
        String createSql = "CREATE FUNCTION multiply(x INT, y INT) RETURNS INT BEGIN RETURN x * y; END";
        
        System.out.println("Creating function: " + createSql);
        ExecutionResult createResult = engine.executeSQL(createSql);
        
        if (createResult.isSuccess()) {
            System.out.println("✅ Multiply function created");
            
            // Call the function
            String callSql = "CALL multiply(7, 6)";
            System.out.println("Calling function: " + callSql);
            ExecutionResult callResult = engine.executeSQL(callSql);
            
            if (callResult.isSuccess()) {
                System.out.println("✅ Function result: " + callResult.getData().get(0).get(0));
            } else {
                System.out.println("❌ Failed to call multiply function: " + callResult.getMessage());
            }
        } else {
            System.out.println("❌ Failed to create multiply function: " + createResult.getMessage());
        }
    }
}
