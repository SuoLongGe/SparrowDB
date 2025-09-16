import com.sqlcompiler.lexer.LexicalAnalyzer;
import com.sqlcompiler.lexer.Token;
import com.sqlcompiler.parser.SyntaxAnalyzer;
import com.sqlcompiler.ast.Statement;
import java.util.List;

public class TestMultilineInsert {
    public static void main(String[] args) {
        String sql = "INSERT INTO test VALUES (1, 'Product 1', 'Electronics'), (2, 'Product 2', 'Books');";
        
        try {
            System.out.println("SQL: " + sql);
            
            // 词法分析
            LexicalAnalyzer lexer = new LexicalAnalyzer(sql);
            List<Token> tokens = lexer.tokenize();
            
            System.out.println("\nTokens:");
            for (Token token : tokens) {
                if (token.getType().toString().equals("EOF")) break;
                System.out.println("  " + token.toString());
            }
            
            // 语法分析
            System.out.println("\n语法分析:");
            SyntaxAnalyzer parser = new SyntaxAnalyzer(tokens);
            Statement statement = parser.parse();
            System.out.println("解析成功: " + statement.getClass().getSimpleName());
            
        } catch (Exception e) {
            System.out.println("错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

