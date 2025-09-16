import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * WAL日志条目
 * 记录数据库操作的详细信息
 */
public class WALEntry {
    // 日志条目类型
    public enum LogType {
        INSERT(1),      // 插入操作
        UPDATE(2),      // 更新操作
        DELETE(3),      // 删除操作
        COMMIT(4),      // 事务提交
        ABORT(5),       // 事务回滚
        CHECKPOINT(6);  // 检查点
        
        private final int value;
        
        LogType(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
        
        public static LogType fromValue(int value) {
            for (LogType type : LogType.values()) {
                if (type.value == value) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Invalid log type: " + value);
        }
    }
    
    private long lsn;           // 日志序列号 (Log Sequence Number)
    private long transactionId; // 事务ID
    private LogType logType;    // 日志类型
    private int pageId;         // 页面ID
    private int offset;         // 页面内偏移量
    private byte[] oldData;     // 旧数据 (用于回滚)
    private byte[] newData;     // 新数据
    private long timestamp;     // 时间戳
    private long checksum;      // 校验和
    
    public WALEntry(long lsn, long transactionId, LogType logType, int pageId, 
                   int offset, byte[] oldData, byte[] newData) {
        this.lsn = lsn;
        this.transactionId = transactionId;
        this.logType = logType;
        this.pageId = pageId;
        this.offset = offset;
        this.oldData = oldData != null ? oldData.clone() : null;
        this.newData = newData != null ? newData.clone() : null;
        this.timestamp = System.currentTimeMillis();
        this.checksum = calculateChecksum();
    }
    
    /**
     * 序列化为字节数组
     */
    public byte[] toBytes() {
        int oldDataSize = oldData != null ? oldData.length : 0;
        int newDataSize = newData != null ? newData.length : 0;
        
        // 计算总大小: 8(lsn) + 8(transactionId) + 4(logType) + 4(pageId) + 4(offset) + 
        // 4(oldDataSize) + oldData + 4(newDataSize) + newData + 8(timestamp) + 8(checksum)
        int totalSize = 8 + 8 + 4 + 4 + 4 + 4 + oldDataSize + 4 + newDataSize + 8 + 8;
        
        ByteBuffer buffer = ByteBuffer.allocate(totalSize);
        
        buffer.putLong(lsn);
        buffer.putLong(transactionId);
        buffer.putInt(logType.getValue());
        buffer.putInt(pageId);
        buffer.putInt(offset);
        
        // 写入旧数据
        buffer.putInt(oldDataSize);
        if (oldData != null) {
            buffer.put(oldData);
        }
        
        // 写入新数据
        buffer.putInt(newDataSize);
        if (newData != null) {
            buffer.put(newData);
        }
        
        buffer.putLong(timestamp);
        buffer.putLong(checksum);
        
        return buffer.array();
    }
    
    /**
     * 从字节数组反序列化
     */
    public static WALEntry fromBytes(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        
        long lsn = buffer.getLong();
        long transactionId = buffer.getLong();
        LogType logType = LogType.fromValue(buffer.getInt());
        int pageId = buffer.getInt();
        int offset = buffer.getInt();
        
        // 读取旧数据
        int oldDataSize = buffer.getInt();
        byte[] oldData = null;
        if (oldDataSize > 0) {
            oldData = new byte[oldDataSize];
            buffer.get(oldData);
        }
        
        // 读取新数据
        int newDataSize = buffer.getInt();
        byte[] newData = null;
        if (newDataSize > 0) {
            newData = new byte[newDataSize];
            buffer.get(newData);
        }
        
        long timestamp = buffer.getLong();
        long checksum = buffer.getLong();
        
        WALEntry entry = new WALEntry(lsn, transactionId, logType, pageId, offset, oldData, newData);
        entry.timestamp = timestamp;
        entry.checksum = checksum;
        
        // 验证校验和
        if (entry.checksum != entry.calculateChecksum()) {
            throw new RuntimeException("WAL entry checksum mismatch");
        }
        
        return entry;
    }
    
    /**
     * 计算校验和
     */
    private long calculateChecksum() {
        long sum = lsn + transactionId + logType.getValue() + pageId + offset + timestamp;
        if (oldData != null) {
            for (byte b : oldData) {
                sum += b;
            }
        }
        if (newData != null) {
            for (byte b : newData) {
                sum += b;
            }
        }
        return sum;
    }
    
    /**
     * 获取日志条目大小
     */
    public int getSize() {
        int oldDataSize = oldData != null ? oldData.length : 0;
        int newDataSize = newData != null ? newData.length : 0;
        return 8 + 8 + 4 + 4 + 4 + 4 + oldDataSize + 4 + newDataSize + 8 + 8;
    }
    
    // Getters
    public long getLsn() { return lsn; }
    public long getTransactionId() { return transactionId; }
    public LogType getLogType() { return logType; }
    public int getPageId() { return pageId; }
    public int getOffset() { return offset; }
    public byte[] getOldData() { return oldData != null ? oldData.clone() : null; }
    public byte[] getNewData() { return newData != null ? newData.clone() : null; }
    public long getTimestamp() { return timestamp; }
    public long getChecksum() { return checksum; }
    
    @Override
    public String toString() {
        return String.format("WALEntry{LSN=%d, TxnID=%d, Type=%s, PageID=%d, Offset=%d, Size=%d}", 
                           lsn, transactionId, logType, pageId, offset, getSize());
    }
}
