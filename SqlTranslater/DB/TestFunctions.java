import com.database.engine.FunctionEvaluator;
import java.util.Arrays;
import java.util.List;

/**
 * Simple function test
 */
public class TestFunctions {
    
    public static void main(String[] args) {
        System.out.println("=== Function Evaluator Test ===\n");
        
        testMathFunctions();
        testStringFunctions();
        testDateFunctions();
        
        System.out.println("=== Test Complete ===");
    }
    
    private static void testMathFunctions() {
        System.out.println("1. Testing Math Functions:");
        
        try {
            // ABS function
            Object result = FunctionEvaluator.evaluateFunction("ABS", Arrays.asList(-10));
            System.out.println("ABS(-10) = " + result);
            
            // ROUND function
            result = FunctionEvaluator.evaluateFunction("ROUND", Arrays.asList(3.14159, 2));
            System.out.println("ROUND(3.14159, 2) = " + result);
            
            // SQRT function
            result = FunctionEvaluator.evaluateFunction("SQRT", Arrays.asList(16));
            System.out.println("SQRT(16) = " + result);
            
            // POWER function
            result = FunctionEvaluator.evaluateFunction("POWER", Arrays.asList(2, 3));
            System.out.println("POWER(2, 3) = " + result);
            
            System.out.println("+ Math functions test passed\n");
        } catch (Exception e) {
            System.out.println("- Math functions test failed: " + e.getMessage() + "\n");
        }
    }
    
    private static void testStringFunctions() {
        System.out.println("2. Testing String Functions:");
        
        try {
            // UPPER function
            Object result = FunctionEvaluator.evaluateFunction("UPPER", Arrays.asList("hello world"));
            System.out.println("UPPER('hello world') = " + result);
            
            // LENGTH function
            result = FunctionEvaluator.evaluateFunction("LENGTH", Arrays.asList("Hello"));
            System.out.println("LENGTH('Hello') = " + result);
            
            // SUBSTRING function
            result = FunctionEvaluator.evaluateFunction("SUBSTRING", Arrays.asList("Hello World", 1, 5));
            System.out.println("SUBSTRING('Hello World', 1, 5) = " + result);
            
            // CONCAT function
            result = FunctionEvaluator.evaluateFunction("CONCAT", Arrays.asList("Hello", " ", "World"));
            System.out.println("CONCAT('Hello', ' ', 'World') = " + result);
            
            System.out.println("+ String functions test passed\n");
        } catch (Exception e) {
            System.out.println("- String functions test failed: " + e.getMessage() + "\n");
        }
    }
    
    private static void testDateFunctions() {
        System.out.println("3. Testing Date Functions:");
        
        try {
            // NOW function
            Object result = FunctionEvaluator.evaluateFunction("NOW", Arrays.asList());
            System.out.println("NOW() = " + result);
            
            // CURRENT_DATE function
            result = FunctionEvaluator.evaluateFunction("CURRENT_DATE", Arrays.asList());
            System.out.println("CURRENT_DATE() = " + result);
            
            // YEAR function
            result = FunctionEvaluator.evaluateFunction("YEAR", Arrays.asList("2023-05-15"));
            System.out.println("YEAR('2023-05-15') = " + result);
            
            // DATEDIFF function
            result = FunctionEvaluator.evaluateFunction("DATEDIFF", Arrays.asList("2023-05-20", "2023-05-15"));
            System.out.println("DATEDIFF('2023-05-20', '2023-05-15') = " + result);
            
            System.out.println("+ Date functions test passed\n");
        } catch (Exception e) {
            System.out.println("- Date functions test failed: " + e.getMessage() + "\n");
        }
    }
}
