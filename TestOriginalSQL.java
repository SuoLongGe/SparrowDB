import com.sqlcompiler.lexer.LexicalAnalyzer;
import com.sqlcompiler.lexer.Token;
import com.sqlcompiler.parser.SyntaxAnalyzer;
import com.sqlcompiler.ast.Statement;
import java.util.List;

public class TestOriginalSQL {
    public static void main(String[] args) {
        // 这是从原始脚本复制的确切SQL
        String sql = "INSERT INTO perf_test_products VALUES \n" +
                    "(1, 'Product 1', 'Electronics', 99.99, 10, 'Supplier A', 'Description 1', '2024-01-01', CURRENT_TIMESTAMP),\n" +
                    "(2, 'Product 2', 'Books', 29.99, 20, 'Supplier B', 'Description 2', '2024-01-02', CURRENT_TIMESTAMP),\n" +
                    "(3, 'Product 3', 'Electronics', 199.99, 5, 'Supplier C', 'Description 3', '2024-01-03', CURRENT_TIMESTAMP),\n" +
                    "(4, 'Product 4', 'Clothing', 59.99, 15, 'Supplier A', 'Description 4', '2024-01-04', CURRENT_TIMESTAMP),\n" +
                    "(5, 'Product 5', 'Electronics', 299.99, 8, 'Supplier D', 'Description 5', '2024-01-05', CURRENT_TIMESTAMP);";
        
        try {
            System.out.println("SQL: " + sql.replace('\n', ' '));
            
            // 词法分析
            LexicalAnalyzer lexer = new LexicalAnalyzer(sql);
            List<Token> tokens = lexer.tokenize();
            
            System.out.println("\nFirst 20 Tokens:");
            for (int i = 0; i < Math.min(20, tokens.size()); i++) {
                Token token = tokens.get(i);
                if (token.getType().toString().equals("EOF")) break;
                System.out.println("  " + token.toString());
            }
            
            // 语法分析
            System.out.println("\nSyntax Analysis:");
            SyntaxAnalyzer parser = new SyntaxAnalyzer(tokens);
            Statement statement = parser.parse();
            System.out.println("Parse Success: " + statement.getClass().getSimpleName());
            
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
