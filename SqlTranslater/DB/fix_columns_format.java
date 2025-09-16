import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class fix_columns_format {
    public static void main(String[] args) {
        try {
            String filePath = "E:\\SQL实训\\data\\smxx\\__system_columns__.tbl";
            System.out.println("修复 __system_columns__.tbl 文件格式...");
            
            // 读取现有文件
            List<String> lines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }
            
            // 重写文件，为数据记录添加 RECORD: 前缀
            try (PrintWriter writer = new PrintWriter(new FileWriter(filePath, StandardCharsets.UTF_8))) {
                boolean inDataSection = false;
                
                for (String line : lines) {
                    if (line.startsWith("PAGE:")) {
                        inDataSection = true;
                        writer.println(line);
                    } else if (line.trim().isEmpty() && inDataSection) {
                        writer.println(line);
                    } else if (inDataSection && line.contains("=") && !line.startsWith("RECORD:") && !line.startsWith("#")) {
                        // 这是数据记录，添加 RECORD: 前缀
                        writer.println("RECORD:" + line);
                        System.out.println("修复记录: " + line.substring(0, Math.min(50, line.length())) + "...");
                    } else {
                        // 元数据或其他行，保持不变
                        writer.println(line);
                    }
                }
            }
            
            System.out.println("文件格式修复完成！");
            
        } catch (Exception e) {
            System.err.println("修复文件格式失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
