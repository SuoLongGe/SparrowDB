import com.sqlcompiler.lexer.LexicalAnalyzer;
import com.sqlcompiler.lexer.Token;
import java.util.List;

public class TestInsertTokens {
    public static void main(String[] args) {
        String sql = "INSERT INTO test VALUES (1, 'Product 1', 'Electronics');";
        
        LexicalAnalyzer lexer = new LexicalAnalyzer(sql);
        List<Token> tokens = lexer.tokenize();
        
        System.out.println("SQL: " + sql);
        System.out.println("Tokens:");
        for (Token token : tokens) {
            System.out.println("  " + token.toString());
        }
    }
}

