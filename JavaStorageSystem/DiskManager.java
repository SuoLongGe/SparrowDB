import java.io.*; // 导入Java I/O相关类
import java.nio.file.Files; // 导入文件操作类
import java.nio.file.Path; // 导入路径类
import java.nio.file.Paths; // 导入路径操作类
import java.util.concurrent.locks.ReentrantReadWriteLock; // 导入读写锁类

/**
 * 磁盘管理器类 - 负责页面的磁盘I/O操作
 */
public class DiskManager {
    private final String dbFilename; // 数据库文件名
    private final ReentrantReadWriteLock fileLock; // 文件操作的读写锁

    /**
     * 构造函数
     */
    public DiskManager(String filename) {
        this.dbFilename = filename; // 初始化数据库文件名
        this.fileLock = new ReentrantReadWriteLock(); 
        // 初始化读写锁，“读锁”可以被多个线程同时持有（读不影响读）。“写锁”只能被一个线程持有，并且写的时候不允许其他线程读。“可重入”表示同一个线程如果已经获得了锁，可以再次进入，不会死锁。
        initializeDatabase(); // 初始化数据库文件
    }

    /**
     * 初始化数据库文件
     */
    private void initializeDatabase() {
        try {
            Path path = Paths.get(dbFilename); // 获取数据库文件路径
            if (!Files.exists(path)) { // 如果文件不存在
                Files.createFile(path); // 创建新文件
                System.out.println("Created new database file: " + dbFilename); // 输出创建文件信息
            } else {
                System.out.println("Opened existing database file: " + dbFilename); // 输出打开文件信息
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot create or open database file: " + dbFilename, e); // 抛出异常
        }
    }

    /**
     * 从磁盘读取页面
     */
    public boolean readPage(int pageId, byte[] pageData) {
        if (pageId < 0) { // 检查页面ID是否有效
            System.err.println("Invalid page ID: " + pageId); // 输出错误信息
            return false; // 返回失败
        }

        fileLock.readLock().lock(); // 获取读锁
        try (RandomAccessFile file = new RandomAccessFile(dbFilename, "r")) { // 打开文件进行只读操作
            if (!ensureFileSize(file, pageId)) { // 确保文件大小足够
                return false; // 返回失败
            }

            long offset = (long) pageId * Page.PAGE_SIZE; // 计算页面偏移量
            file.seek(offset); // 定位到页面位置

            int bytesRead = file.read(pageData); // 读取页面数据
            if (bytesRead != Page.PAGE_SIZE) { // 检查是否读取完整页面
                System.err.println("Failed to read complete page " + pageId); // 输出错误信息
                return false; // 返回失败
            }

            return true; // 返回成功
        } catch (IOException e) {
            System.err.println("Error reading page " + pageId + ": " + e.getMessage()); // 输出异常信息
            return false; // 返回失败
        } finally {
            fileLock.readLock().unlock(); // 释放读锁
        }
    }

    /**
     * 将页面写入磁盘
     */
    public boolean writePage(int pageId, byte[] pageData) {
        if (pageId < 0) { // 检查页面ID是否有效
            System.err.println("Invalid page ID: " + pageId); // 输出错误信息
            return false; // 返回失败
        }

        fileLock.writeLock().lock(); // 获取写锁
        try (RandomAccessFile file = new RandomAccessFile(dbFilename, "rw")) { // 打开文件进行读写操作
            if (!ensureFileSize(file, pageId)) { // 确保文件大小足够
                return false; // 返回失败
            }

            long offset = (long) pageId * Page.PAGE_SIZE; // 计算页面偏移量
            file.seek(offset); // 定位到页面位置

            file.write(pageData); // 写入页面数据，把内存中的pageData写入操作系统的文件缓存（page cache），但此时还没真正落到磁盘。
            file.getFD().sync(); // 强制同步到磁盘，.sync() 的作用是强制把操作系统缓存里的数据刷新到磁盘，确保数据真正写到硬盘。

            return true; // 返回成功
        } catch (IOException e) {
            System.err.println("Error writing page " + pageId + ": " + e.getMessage()); // 输出异常信息
            return false; // 返回失败
        } finally {
            fileLock.writeLock().unlock(); // 释放写锁
        }
    }

    /**
     * 分配新页面,在磁盘里，文件本质上是一串字节流。它的大小可以通过写操作来 增加，也可以截断来 减少。
     */
    public int allocatePage() {
        fileLock.writeLock().lock(); // 获取写锁
        try (RandomAccessFile file = new RandomAccessFile(dbFilename, "rw")) { // 打开文件进行读写操作
            long fileSize = file.length(); // 获取文件大小
            int nextPageId = (int) (fileSize / Page.PAGE_SIZE); // 计算下一个页面ID

            if (!ensureFileSize(file, nextPageId)) { // 确保文件大小足够
                return Page.INVALID_PAGE_ID; // 返回无效页面ID
            }

            System.out.println("Allocated new page: " + nextPageId); // 输出分配页面信息
            return nextPageId; // 返回新页面ID
        } catch (IOException e) {
            System.err.println("Error allocating page: " + e.getMessage()); // 输出异常信息
            return Page.INVALID_PAGE_ID; // 返回无效页面ID
        } finally {
            fileLock.writeLock().unlock(); // 释放写锁
        }
    }

    /**
     * 释放页面（在简单实现中，我们实际上不释放页面）
     */
    public boolean deallocatePage(int pageId) {
        System.out.println("Deallocated page: " + pageId); // 输出释放页面信息
        return true; // 返回成功
    }

    /**
     * 获取数据库文件大小（页数）
     */
    public int getFileSize() {
        fileLock.readLock().lock(); // 获取读锁
        try (RandomAccessFile file = new RandomAccessFile(dbFilename, "r")) { // 打开文件进行只读操作
            long fileSize = file.length(); // 获取文件大小
            return (int) (fileSize / Page.PAGE_SIZE); // 返回页面数
        } catch (IOException e) {
            System.err.println("Error getting file size: " + e.getMessage()); // 输出异常信息
            return 0; // 返回0
        } finally {
            fileLock.readLock().unlock(); // 释放读锁
        }
    }

    /**
     * 确保文件足够大以容纳指定页面
     */
    private boolean ensureFileSize(RandomAccessFile file, int pageId) throws IOException {
        long currentSize = file.length(); // 获取当前文件大小
        long requiredSize = (long) (pageId + 1) * Page.PAGE_SIZE; // 计算所需文件大小

        if (currentSize < requiredSize) { // 如果当前大小不足
            file.setLength(requiredSize); // 扩展文件大小
        }

        return true; // 返回成功
    }

    /**
     * 获取数据库文件名
     */
    public String getDbFilename() {
        return dbFilename; // 返回数据库文件名
    }
}
