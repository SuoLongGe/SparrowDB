import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 磁盘管理器类 - 负责页面的磁盘I/O操作
 */
public class DiskManager {
    private final String dbFilename;
    private final ReentrantReadWriteLock fileLock;
    
    /**
     * 构造函数
     */
    public DiskManager(String filename) {
        this.dbFilename = filename;
        this.fileLock = new ReentrantReadWriteLock();
        initializeDatabase();
    }
    
    /**
     * 初始化数据库文件
     */
    private void initializeDatabase() {
        try {
            Path path = Paths.get(dbFilename);
            if (!Files.exists(path)) {
                // 创建新文件
                Files.createFile(path);
                System.out.println("Created new database file: " + dbFilename);
            } else {
                System.out.println("Opened existing database file: " + dbFilename);
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot create or open database file: " + dbFilename, e);
        }
    }
    
    /**
     * 从磁盘读取页面
     */
    public boolean readPage(int pageId, byte[] pageData) {
        if (pageId < 0) {
            System.err.println("Invalid page ID: " + pageId);
            return false;
        }
        
        fileLock.readLock().lock();
        try (RandomAccessFile file = new RandomAccessFile(dbFilename, "r")) {
            // 确保文件足够大
            if (!ensureFileSize(file, pageId)) {
                return false;
            }
            
            // 计算文件中的偏移位置
            long offset = (long) pageId * Page.PAGE_SIZE;
            file.seek(offset);
            
            // 读取页面数据
            int bytesRead = file.read(pageData);
            if (bytesRead != Page.PAGE_SIZE) {
                System.err.println("Failed to read complete page " + pageId);
                return false;
            }
            
            return true;
        } catch (IOException e) {
            System.err.println("Error reading page " + pageId + ": " + e.getMessage());
            return false;
        } finally {
            fileLock.readLock().unlock();
        }
    }
    
    /**
     * 将页面写入磁盘
     */
    public boolean writePage(int pageId, byte[] pageData) {
        if (pageId < 0) {
            System.err.println("Invalid page ID: " + pageId);
            return false;
        }
        
        fileLock.writeLock().lock();
        try (RandomAccessFile file = new RandomAccessFile(dbFilename, "rw")) {
            // 确保文件足够大
            if (!ensureFileSize(file, pageId)) {
                return false;
            }
            
            // 计算文件中的偏移位置
            long offset = (long) pageId * Page.PAGE_SIZE;
            file.seek(offset);
            
            // 写入页面数据
            file.write(pageData);
            file.getFD().sync(); // 强制同步到磁盘
            
            return true;
        } catch (IOException e) {
            System.err.println("Error writing page " + pageId + ": " + e.getMessage());
            return false;
        } finally {
            fileLock.writeLock().unlock();
        }
    }
    
    /**
     * 分配新页面
     */
    public int allocatePage() {
        fileLock.writeLock().lock();
        try (RandomAccessFile file = new RandomAccessFile(dbFilename, "rw")) {
            // 获取当前文件大小以确定下一个页面ID
            long fileSize = file.length();
            int nextPageId = (int) (fileSize / Page.PAGE_SIZE);
            
            // 确保文件足够大以容纳新页面
            if (!ensureFileSize(file, nextPageId)) {
                return Page.INVALID_PAGE_ID;
            }
            
            System.out.println("Allocated new page: " + nextPageId);
            return nextPageId;
        } catch (IOException e) {
            System.err.println("Error allocating page: " + e.getMessage());
            return Page.INVALID_PAGE_ID;
        } finally {
            fileLock.writeLock().unlock();
        }
    }
    
    /**
     * 释放页面（在简单实现中，我们实际上不释放页面）
     */
    public boolean deallocatePage(int pageId) {
        System.out.println("Deallocated page: " + pageId);
        return true;
    }
    
    /**
     * 获取数据库文件大小（页数）
     */
    public int getFileSize() {
        fileLock.readLock().lock();
        try (RandomAccessFile file = new RandomAccessFile(dbFilename, "r")) {
            long fileSize = file.length();
            return (int) (fileSize / Page.PAGE_SIZE);
        } catch (IOException e) {
            System.err.println("Error getting file size: " + e.getMessage());
            return 0;
        } finally {
            fileLock.readLock().unlock();
        }
    }
    
    /**
     * 确保文件足够大以容纳指定页面
     */
    private boolean ensureFileSize(RandomAccessFile file, int pageId) throws IOException {
        long currentSize = file.length();
        long requiredSize = (long) (pageId + 1) * Page.PAGE_SIZE;
        
        if (currentSize < requiredSize) {
            // 扩展文件
            file.setLength(requiredSize);
        }
        
        return true;
    }
    
    /**
     * 获取数据库文件名
     */
    public String getDbFilename() {
        return dbFilename;
    }
}
