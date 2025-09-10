import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 数据库文件查看器 - 用于查看.db文件的结构和内容
 */
public class DatabaseViewer {
    private static final int PAGE_SIZE = 4096;
    
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java DatabaseViewer <database_file.db>");
            System.out.println("Example: java DatabaseViewer test_db.db");
            return;
        }
        
        String dbFile = args[0];
        viewDatabaseStructure(dbFile);
        analyzeDatabase(dbFile);
    }
    
    /**
     * 查看数据库文件结构
     */
    public static void viewDatabaseStructure(String filename) {
        System.out.println("=== Database File Structure Viewer ===");
        System.out.println("File: " + filename);
        System.out.println("Page Size: " + PAGE_SIZE + " bytes");
        System.out.println("============================================================");
        
        try (RandomAccessFile file = new RandomAccessFile(filename, "r")) {
            // 获取文件大小
            long fileSize = file.length();
            int totalPages = (int) (fileSize / PAGE_SIZE);
            
            System.out.println("File Size: " + fileSize + " bytes");
            System.out.println("Total Pages: " + totalPages);
            System.out.println();
            
            if (totalPages == 0) {
                System.out.println("Database file is empty.");
                return;
            }
            
            // 读取并显示每个页面
            for (int pageId = 0; pageId < totalPages; pageId++) {
                displayPage(file, pageId);
            }
            
        } catch (IOException e) {
            System.out.println("Error: Cannot open file " + filename + ": " + e.getMessage());
        }
    }
    
    /**
     * 显示页面信息
     */
    public static void displayPage(RandomAccessFile file, int pageId) throws IOException {
        System.out.println("--- Page " + pageId + " ---");
        
        // 定位到页面位置
        long pageOffset = (long) pageId * PAGE_SIZE;
        file.seek(pageOffset);
        
        // 读取页面数据
        byte[] pageData = new byte[PAGE_SIZE];
        int bytesRead = file.read(pageData);
        
        if (bytesRead != PAGE_SIZE) {
            System.out.println("Error: Cannot read complete page " + pageId);
            return;
        }
        
        // 显示页面信息
        System.out.println("Offset: 0x" + Long.toHexString(pageOffset));
        
        // 检查页面是否为空
        boolean isEmpty = true;
        for (byte b : pageData) {
            if (b != 0) {
                isEmpty = false;
                break;
            }
        }
        
        if (isEmpty) {
            System.out.println("Status: Empty page");
        } else {
            System.out.println("Status: Contains data");
            
            // 找到第一个非null字节
            int firstData = 0;
            for (int i = 0; i < PAGE_SIZE; i++) {
                if (pageData[i] != 0) {
                    firstData = i;
                    break;
                }
            }
            
            // 找到最后一个非null字节
            int lastData = 0;
            for (int i = PAGE_SIZE - 1; i >= 0; i--) {
                if (pageData[i] != 0) {
                    lastData = i;
                    break;
                }
            }
            
            System.out.println("Data Range: " + firstData + " - " + lastData + " (" 
                              + (lastData - firstData + 1) + " bytes)");
            
            // 显示前200字节的十六进制
            System.out.println("First 200 bytes (hex):");
            displayHexDump(pageData, Math.min(200, lastData + 1));
            
            // 尝试显示为文本
            System.out.println("Text content (first 500 chars):");
            displayTextContent(pageData, Math.min(500, lastData + 1));
        }
        
        System.out.println();
    }
    
    /**
     * 显示十六进制转储
     */
    public static void displayHexDump(byte[] data, int length) {
        for (int i = 0; i < length; i += 16) {
            System.out.printf("%04x: ", i);
            
            // 显示十六进制字节
            for (int j = 0; j < 16 && (i + j) < length; j++) {
                System.out.printf("%02x ", data[i + j] & 0xFF);
            }
            
            // 如果需要填充
            for (int j = length - i; j < 16; j++) {
                System.out.print("   ");
            }
            
            System.out.print(" ");
            
            // 显示ASCII表示
            for (int j = 0; j < 16 && (i + j) < length; j++) {
                char c = (char) (data[i + j] & 0xFF);
                if (c >= 32 && c <= 126) {
                    System.out.print(c);
                } else {
                    System.out.print(".");
                }
            }
            
            System.out.println();
        }
    }
    
    /**
     * 显示文本内容
     */
    public static void displayTextContent(byte[] data, int length) {
        StringBuilder textContent = new StringBuilder();
        for (int i = 0; i < length; i++) {
            char c = (char) (data[i] & 0xFF);
            if (c >= 32 && c <= 126) {
                textContent.append(c);
            } else if (c == 0) {
                textContent.append("\\0");
            } else {
                textContent.append(".");
            }
        }
        
        String text = textContent.toString();
        if (!text.isEmpty()) {
            System.out.println("  Content: \"" + text + "\"");
        } else {
            System.out.println("  No readable text found");
        }
    }
    
    /**
     * 分析数据库
     */
    public static void analyzeDatabase(String filename) {
        System.out.println("\n=== Database Analysis ===");
        
        try (RandomAccessFile file = new RandomAccessFile(filename, "r")) {
            long fileSize = file.length();
            int totalPages = (int) (fileSize / PAGE_SIZE);
            
            System.out.println("Total pages: " + totalPages);
            
            int emptyPages = 0;
            int dataPages = 0;
            int totalDataBytes = 0;
            
            for (int pageId = 0; pageId < totalPages; pageId++) {
                long pageOffset = (long) pageId * PAGE_SIZE;
                file.seek(pageOffset);
                
                byte[] pageData = new byte[PAGE_SIZE];
                file.read(pageData);
                
                boolean isEmpty = true;
                int dataBytes = 0;
                
                for (byte b : pageData) {
                    if (b != 0) {
                        isEmpty = false;
                        dataBytes++;
                    }
                }
                
                if (isEmpty) {
                    emptyPages++;
                } else {
                    dataPages++;
                    totalDataBytes += dataBytes;
                }
            }
            
            System.out.println("Empty pages: " + emptyPages);
            System.out.println("Data pages: " + dataPages);
            System.out.println("Total data bytes: " + totalDataBytes);
            System.out.printf("Storage efficiency: %.2f%%\n", 
                             (double) totalDataBytes / (totalPages * PAGE_SIZE) * 100);
            
        } catch (IOException e) {
            System.out.println("Error analyzing database: " + e.getMessage());
        }
    }
}
