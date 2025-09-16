import com.sqlcompiler.lexer.LexicalAnalyzer;
import com.sqlcompiler.parser.SyntaxAnalyzer;
import com.sqlcompiler.ast.Statement;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class TestFullSQL {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java TestFullSQL <SQL_FILE_PATH>");
            return;
        }
        
        String fileName = args[0];
        
        try {
            // 读取SQL文件
            String sqlContent = new String(Files.readAllBytes(Paths.get(fileName)));
            System.out.println("Reading SQL file: " + fileName);
            System.out.println("File size: " + sqlContent.length() + " characters");
            
            // Split SQL statements by semicolon
            String[] statements = sqlContent.split(";");
            System.out.println("Found " + statements.length + " SQL statements");
            
            int successCount = 0;
            int errorCount = 0;
            
            for (int i = 0; i < statements.length; i++) {
                String sql = statements[i].trim();
                if (sql.isEmpty()) continue;
                
                try {
                    System.out.println("\n--- Parsing statement " + (i + 1) + " ---");
                    System.out.println("SQL: " + sql.substring(0, Math.min(sql.length(), 100)) + 
                                     (sql.length() > 100 ? "..." : ""));
                    
                    // 词法分析
                    LexicalAnalyzer lexer = new LexicalAnalyzer(sql);
                    
                    // 语法分析
                    SyntaxAnalyzer parser = new SyntaxAnalyzer(lexer.tokenize());
                    Statement statement = parser.parse();
                    
                    System.out.println("SUCCESS: " + statement.getClass().getSimpleName());
                    successCount++;
                    
                } catch (Exception e) {
                    System.out.println("ERROR: " + e.getMessage());
                    errorCount++;
                }
            }
            
            System.out.println("\n=== SUMMARY ===");
            System.out.println("Success: " + successCount + " statements");
            System.out.println("Failed: " + errorCount + " statements");
            System.out.println("Success Rate: " + (successCount * 100.0 / (successCount + errorCount)) + "%");
            
        } catch (IOException e) {
            System.err.println("Failed to read file: " + e.getMessage());
        }
    }
}
