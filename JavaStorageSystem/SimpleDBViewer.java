import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SimpleDBViewer {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java SimpleDBViewer <database_file>");
            return;
        }
        
        String dbFile = args[0];
        try {
            // Check if file exists
            if (!Files.exists(Paths.get(dbFile))) {
                System.out.println("Database file does not exist: " + dbFile);
                return;
            }
            
            // Get file size
            long fileSize = Files.size(Paths.get(dbFile));
            System.out.println("Database file: " + dbFile);
            System.out.println("File size: " + fileSize + " bytes");
            
            if (fileSize == 0) {
                System.out.println("File is empty!");
                return;
            }
            
            // Read and display first 200 bytes as hex
            byte[] buffer = new byte[Math.min(200, (int)fileSize)];
            try (FileInputStream fis = new FileInputStream(dbFile)) {
                int bytesRead = fis.read(buffer);
                System.out.println("\nFirst " + bytesRead + " bytes (hex):");
                
                for (int i = 0; i < bytesRead; i++) {
                    if (i % 16 == 0) {
                        System.out.printf("\n%04X: ", i);
                    }
                    System.out.printf("%02X ", buffer[i] & 0xFF);
                }
                
                System.out.println("\n\nFirst " + bytesRead + " bytes (ASCII):");
                for (int i = 0; i < bytesRead; i++) {
                    char c = (char)(buffer[i] & 0xFF);
                    if (c >= 32 && c <= 126) {
                        System.out.print(c);
                    } else {
                        System.out.print('.');
                    }
                    if ((i + 1) % 64 == 0) {
                        System.out.println();
                    }
                }
                System.out.println();
                
                // Check for non-zero bytes (data presence)
                boolean hasData = false;
                for (byte b : buffer) {
                    if (b != 0) {
                        hasData = true;
                        break;
                    }
                }
                
                if (hasData) {
                    System.out.println("\n[OK] Data detected! File contains non-zero bytes.");
                } else {
                    System.out.println("\n[WARNING] File contains only zero bytes.");
                }
            }
            
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }
}
