import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * WAL管理器
 * 负责Write-Ahead Logging的写入、读取和恢复
 */
public class WALManager {
    private final String walFilePath;
    private final FileChannel walChannel;
    private final ReentrantReadWriteLock walLock;
    private long nextLsn;
    private long currentPosition;
    private final Map<Long, Long> transactionLsnMap; // 事务ID -> 最新LSN
    private final Map<Long, List<Long>> transactionLogs; // 事务ID -> LSN列表
    
    // WAL文件头部信息
    private static final int WAL_HEADER_SIZE = 32;
    private static final long WAL_MAGIC_NUMBER = 0x57414C4442L; // "WALDB"
    
    public WALManager(String dbFilePath) throws IOException {
        this.walFilePath = dbFilePath + ".wal";
        this.walLock = new ReentrantReadWriteLock();
        this.transactionLsnMap = new HashMap<>();
        this.transactionLogs = new HashMap<>();
        
        // 打开或创建WAL文件
        Path walPath = Paths.get(walFilePath);
        if (Files.exists(walPath)) {
            // 打开现有WAL文件
            this.walChannel = FileChannel.open(walPath, StandardOpenOption.READ, StandardOpenOption.WRITE);
            recoverFromWAL();
        } else {
            // 创建新的WAL文件
            this.walChannel = FileChannel.open(walPath, StandardOpenOption.CREATE, 
                                             StandardOpenOption.READ, StandardOpenOption.WRITE);
            initializeWALFile();
        }
        
        System.out.println("WAL Manager initialized: " + walFilePath);
    }
    
    /**
     * 初始化WAL文件
     */
    private void initializeWALFile() throws IOException {
        ByteBuffer header = ByteBuffer.allocate(WAL_HEADER_SIZE);
        header.putLong(WAL_MAGIC_NUMBER);
        header.putLong(0); // 下一个LSN
        header.putLong(0); // 当前位置
        header.putLong(0); // 保留字段
        
        walChannel.write(header, 0);
        walChannel.force(true);
        
        this.nextLsn = 1;
        this.currentPosition = WAL_HEADER_SIZE;
    }
    
    /**
     * 从WAL文件恢复
     */
    private void recoverFromWAL() throws IOException {
        ByteBuffer header = ByteBuffer.allocate(WAL_HEADER_SIZE);
        walChannel.read(header, 0);
        header.rewind();
        
        long magic = header.getLong();
        if (magic != WAL_MAGIC_NUMBER) {
            throw new IOException("Invalid WAL file format");
        }
        
        this.nextLsn = header.getLong();
        this.currentPosition = header.getLong();
        
        // 读取所有日志条目
        readAllLogEntries();
        
        System.out.println("WAL recovery completed. Next LSN: " + nextLsn + ", Position: " + currentPosition);
    }
    
    /**
     * 读取所有日志条目
     */
    private void readAllLogEntries() throws IOException {
        long position = WAL_HEADER_SIZE;
        
        while (position < currentPosition) {
            // 读取日志条目大小
            ByteBuffer sizeBuffer = ByteBuffer.allocate(4);
            walChannel.read(sizeBuffer, position);
            sizeBuffer.rewind();
            int entrySize = sizeBuffer.getInt();
            
            if (entrySize <= 0 || position + 4 + entrySize > currentPosition) {
                break;
            }
            
            // 读取日志条目
            ByteBuffer entryBuffer = ByteBuffer.allocate(entrySize);
            walChannel.read(entryBuffer, position + 4);
            entryBuffer.rewind();
            
            try {
                WALEntry entry = WALEntry.fromBytes(entryBuffer.array());
                
                // 更新事务映射
                transactionLsnMap.put(entry.getTransactionId(), entry.getLsn());
                transactionLogs.computeIfAbsent(entry.getTransactionId(), k -> new ArrayList<>())
                              .add(entry.getLsn());
                
                position += 4 + entrySize;
            } catch (Exception e) {
                System.err.println("Error reading WAL entry at position " + position + ": " + e.getMessage());
                break;
            }
        }
    }
    
    /**
     * 写入日志条目
     */
    public long writeLogEntry(WALEntry entry) throws IOException {
        walLock.writeLock().lock();
        try {
            // 设置LSN
            entry = new WALEntry(nextLsn, entry.getTransactionId(), entry.getLogType(),
                               entry.getPageId(), entry.getOffset(), entry.getOldData(), entry.getNewData());
            
            byte[] entryData = entry.toBytes();
            int entrySize = entryData.length;
            
            // 写入大小和条目数据
            ByteBuffer sizeBuffer = ByteBuffer.allocate(4);
            sizeBuffer.putInt(entrySize);
            sizeBuffer.rewind();
            
            ByteBuffer dataBuffer = ByteBuffer.wrap(entryData);
            
            walChannel.write(sizeBuffer, currentPosition);
            walChannel.write(dataBuffer, currentPosition + 4);
            walChannel.force(true); // 强制刷新到磁盘
            
            // 更新状态
            currentPosition += 4 + entrySize;
            nextLsn++;
            
            // 更新事务映射
            transactionLsnMap.put(entry.getTransactionId(), entry.getLsn());
            transactionLogs.computeIfAbsent(entry.getTransactionId(), k -> new ArrayList<>())
                          .add(entry.getLsn());
            
            return entry.getLsn();
        } finally {
            walLock.writeLock().unlock();
        }
    }
    
    /**
     * 开始事务
     */
    public long beginTransaction() throws IOException {
        long transactionId = System.currentTimeMillis();
        // 开始事务不需要记录具体的数据操作，只需要记录事务开始
        WALEntry entry = new WALEntry(nextLsn, transactionId, WALEntry.LogType.INSERT, -1, -1, null, null);
        writeLogEntry(entry);
        return transactionId;
    }
    
    /**
     * 提交事务
     */
    public void commitTransaction(long transactionId) throws IOException {
        WALEntry entry = new WALEntry(0, transactionId, WALEntry.LogType.COMMIT, -1, -1, null, null);
        writeLogEntry(entry);
    }
    
    /**
     * 回滚事务
     */
    public void abortTransaction(long transactionId) throws IOException {
        WALEntry entry = new WALEntry(0, transactionId, WALEntry.LogType.ABORT, -1, -1, null, null);
        writeLogEntry(entry);
    }
    
    /**
     * 记录页面修改
     */
    public long logPageModification(long transactionId, int pageId, int offset, 
                                  byte[] oldData, byte[] newData) throws IOException {
        WALEntry entry = new WALEntry(0, transactionId, WALEntry.LogType.UPDATE, 
                                    pageId, offset, oldData, newData);
        return writeLogEntry(entry);
    }
    
    /**
     * 记录页面插入
     */
    public long logPageInsert(long transactionId, int pageId, int offset, byte[] newData) throws IOException {
        WALEntry entry = new WALEntry(0, transactionId, WALEntry.LogType.INSERT, 
                                    pageId, offset, null, newData);
        return writeLogEntry(entry);
    }
    
    /**
     * 记录页面删除
     */
    public long logPageDelete(long transactionId, int pageId, int offset, byte[] oldData) throws IOException {
        WALEntry entry = new WALEntry(0, transactionId, WALEntry.LogType.DELETE, 
                                    pageId, offset, oldData, null);
        return writeLogEntry(entry);
    }
    
    /**
     * 获取事务的所有日志条目
     */
    public List<WALEntry> getTransactionLogs(long transactionId) throws IOException {
        walLock.readLock().lock();
        try {
            List<Long> lsnList = transactionLogs.get(transactionId);
            if (lsnList == null) {
                return new ArrayList<>();
            }
            
            List<WALEntry> entries = new ArrayList<>();
            for (long lsn : lsnList) {
                WALEntry entry = getLogEntry(lsn);
                if (entry != null) {
                    entries.add(entry);
                }
            }
            
            return entries;
        } finally {
            walLock.readLock().unlock();
        }
    }
    
    /**
     * 根据LSN获取日志条目
     */
    public WALEntry getLogEntry(long lsn) throws IOException {
        walLock.readLock().lock();
        try {
            long position = WAL_HEADER_SIZE;
            
            while (position < currentPosition) {
                // 读取日志条目大小
                ByteBuffer sizeBuffer = ByteBuffer.allocate(4);
                walChannel.read(sizeBuffer, position);
                sizeBuffer.rewind();
                int entrySize = sizeBuffer.getInt();
                
                if (entrySize <= 0) {
                    break;
                }
                
                // 读取日志条目
                ByteBuffer entryBuffer = ByteBuffer.allocate(entrySize);
                walChannel.read(entryBuffer, position + 4);
                entryBuffer.rewind();
                
                WALEntry entry = WALEntry.fromBytes(entryBuffer.array());
                if (entry.getLsn() == lsn) {
                    return entry;
                }
                
                position += 4 + entrySize;
            }
            
            return null;
        } finally {
            walLock.readLock().unlock();
        }
    }
    
    /**
     * 创建检查点
     */
    public void createCheckpoint() throws IOException {
        walLock.writeLock().lock();
        try {
            WALEntry entry = new WALEntry(0, 0, WALEntry.LogType.CHECKPOINT, -1, -1, null, null);
            writeLogEntry(entry);
            
            // 更新WAL文件头部
            updateWALHeader();
            
            System.out.println("Checkpoint created at LSN: " + (nextLsn - 1));
        } finally {
            walLock.writeLock().unlock();
        }
    }
    
    /**
     * 更新WAL文件头部
     */
    private void updateWALHeader() throws IOException {
        ByteBuffer header = ByteBuffer.allocate(WAL_HEADER_SIZE);
        header.putLong(WAL_MAGIC_NUMBER);
        header.putLong(nextLsn);
        header.putLong(currentPosition);
        header.putLong(0); // 保留字段
        
        walChannel.write(header, 0);
        walChannel.force(true);
    }
    
    /**
     * 清理已提交的事务日志
     */
    public void cleanupCommittedTransactions() throws IOException {
        walLock.writeLock().lock();
        try {
            // 这里可以实现日志清理逻辑
            // 简单实现：清理已提交的事务
            List<Long> committedTransactions = new ArrayList<>();
            
            for (Map.Entry<Long, List<Long>> entry : transactionLogs.entrySet()) {
                long transactionId = entry.getKey();
                List<Long> lsnList = entry.getValue();
                
                // 检查最后一个日志条目是否为COMMIT
                if (!lsnList.isEmpty()) {
                    WALEntry lastEntry = getLogEntry(lsnList.get(lsnList.size() - 1));
                    if (lastEntry != null && lastEntry.getLogType() == WALEntry.LogType.COMMIT) {
                        committedTransactions.add(transactionId);
                    }
                }
            }
            
            // 清理已提交的事务
            for (long transactionId : committedTransactions) {
                transactionLogs.remove(transactionId);
                transactionLsnMap.remove(transactionId);
            }
            
            System.out.println("Cleaned up " + committedTransactions.size() + " committed transactions");
        } finally {
            walLock.writeLock().unlock();
        }
    }
    
    /**
     * 获取WAL统计信息
     */
    public Map<String, Object> getWALStats() {
        walLock.readLock().lock();
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("nextLsn", nextLsn);
            stats.put("currentPosition", currentPosition);
            stats.put("activeTransactions", transactionLsnMap.size());
            stats.put("totalLogEntries", transactionLogs.values().stream().mapToInt(List::size).sum());
            
            try {
                stats.put("walFileSize", Files.size(Paths.get(walFilePath)));
            } catch (IOException e) {
                stats.put("walFileSize", -1);
            }
            
            return stats;
        } finally {
            walLock.readLock().unlock();
        }
    }
    
    /**
     * 关闭WAL管理器
     */
    public void close() throws IOException {
        walLock.writeLock().lock();
        try {
            updateWALHeader();
            walChannel.close();
            System.out.println("WAL Manager closed");
        } finally {
            walLock.writeLock().unlock();
        }
    }
}
