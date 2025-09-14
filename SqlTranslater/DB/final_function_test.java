import com.database.engine.DatabaseEngine;
import com.database.engine.ExecutionResult;

public class final_function_test {
    public static void main(String[] args) {
        try {
            System.out.println("Final Function Test");
            System.out.println("==================");
            
            // Initialize database engine
            DatabaseEngine engine = new DatabaseEngine("SparrowDB", "data");
            if (!engine.initialize()) {
                System.err.println("Database initialization failed");
                return;
            }
            
            // Test 1: Call multiply function
            System.out.println("\nTest 1: Call multiply function");
            String callMultiplySQL = "CALL multiply(4, 5)";
            ExecutionResult result1 = engine.executeSQL(callMultiplySQL);
            System.out.println("SQL: " + callMultiplySQL);
            System.out.println("Result: " + (result1.isSuccess() ? "SUCCESS" : "FAILED"));
            if (result1.isSuccess() && result1.getData() != null) {
                System.out.println("Return value: " + result1.getData().get(0).get("result"));
            }
            
            // Test 2: Call add_numbers function
            System.out.println("\nTest 2: Call add_numbers function");
            String callAddSQL = "CALL add_numbers(5, 3)";
            ExecutionResult result2 = engine.executeSQL(callAddSQL);
            System.out.println("SQL: " + callAddSQL);
            System.out.println("Result: " + (result2.isSuccess() ? "SUCCESS" : "FAILED"));
            if (result2.isSuccess() && result2.getData() != null) {
                System.out.println("Return value: " + result2.getData().get(0).get("result"));
            }
            
            // Test 3: Call add_test function
            System.out.println("\nTest 3: Call add_test function");
            String callAddTestSQL = "CALL add_test(10, 20)";
            ExecutionResult result3 = engine.executeSQL(callAddTestSQL);
            System.out.println("SQL: " + callAddTestSQL);
            System.out.println("Result: " + (result3.isSuccess() ? "SUCCESS" : "FAILED"));
            if (result3.isSuccess() && result3.getData() != null) {
                System.out.println("Return value: " + result3.getData().get(0).get("result"));
            }
            
            System.out.println("\nAll tests completed!");
            
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
