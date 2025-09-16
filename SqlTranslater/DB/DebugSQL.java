import com.sqlcompiler.lexer.LexicalAnalyzer;
import com.sqlcompiler.lexer.Token;
import com.sqlcompiler.lexer.TokenType;
import com.sqlcompiler.parser.SyntaxAnalyzer;
import com.sqlcompiler.ast.Statement;

public class DebugSQL {
    public static void main(String[] args) {
        String sql = "SELECT * FROM test_table WHERE year = 2024";
        
        try {
            System.out.println("Testing SQL: " + sql);
            
            // 词法分析
            LexicalAnalyzer lexer = new LexicalAnalyzer(sql);
            var tokens = lexer.tokenize();
            
            System.out.println("\n=== TOKENS ===");
            for (int i = 0; i < tokens.size(); i++) {
                Token token = tokens.get(i);
                System.out.println(i + ": " + token.getType() + " = '" + token.getValue() + "'");
            }
            
            // 语法分析
            System.out.println("\n=== PARSING ===");
            SyntaxAnalyzer parser = new SyntaxAnalyzer(tokens);
            Statement statement = parser.parse();
            
            System.out.println("SUCCESS: " + statement.getClass().getSimpleName());
            
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
