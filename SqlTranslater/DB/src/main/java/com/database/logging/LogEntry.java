package com.database.logging;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日志条目类 - 使用可读的文本格式
 */
public class LogEntry {
    
    public enum LogType {
        TRANSACTION_START("TXN_START"),
        TRANSACTION_COMMIT("TXN_COMMIT"),
        TRANSACTION_ABORT("TXN_ABORT"),
        INSERT("INSERT"),
        UPDATE("UPDATE"),
        DELETE("DELETE"),
        SELECT("SELECT"),
        CREATE_TABLE("CREATE_TABLE"),
        DROP_TABLE("DROP_TABLE"),
        CHECKPOINT("CHECKPOINT"),
        SYSTEM("SYSTEM");
        
        private final String code;
        
        LogType(String code) {
            this.code = code;
        }
        
        public String getCode() {
            return code;
        }
        
        public static LogType fromCode(String code) {
            for (LogType type : LogType.values()) {
                if (type.code.equals(code)) {
                    return type;
                }
            }
            return SYSTEM;
        }
    }
    
    private final long lsn;                    // 日志序列号
    private final LocalDateTime timestamp;     // 时间戳
    private final LogType logType;             // 日志类型
    private final long transactionId;          // 事务ID
    private final String tableName;            // 表名
    private final String operation;            // 操作描述
    private final String oldData;              // 旧数据
    private final String newData;              // 新数据
    private final String additionalInfo;       // 附加信息
    
    public LogEntry(long lsn, LogType logType, long transactionId, String tableName, 
                   String operation, String oldData, String newData, String additionalInfo) {
        this.lsn = lsn;
        this.timestamp = LocalDateTime.now();
        this.logType = logType;
        this.transactionId = transactionId;
        this.tableName = tableName;
        this.operation = operation;
        this.oldData = oldData;
        this.newData = newData;
        this.additionalInfo = additionalInfo;
    }
    
    /**
     * 带时间戳的构造函数
     */
    public LogEntry(long lsn, LocalDateTime timestamp, LogType logType, long transactionId, String tableName, 
                   String operation, String oldData, String newData, String additionalInfo) {
        this.lsn = lsn;
        this.timestamp = timestamp;
        this.logType = logType;
        this.transactionId = transactionId;
        this.tableName = tableName;
        this.operation = operation;
        this.oldData = oldData;
        this.newData = newData;
        this.additionalInfo = additionalInfo;
    }
    
    /**
     * 格式化为可读的日志行
     */
    public String toLogLine() {
        StringBuilder sb = new StringBuilder();
        
        // 时间戳
        sb.append(timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")));
        sb.append(" | ");
        
        // LSN
        sb.append(String.format("LSN:%08d", lsn));
        sb.append(" | ");
        
        // 日志类型
        sb.append(String.format("%-12s", logType.getCode()));
        sb.append(" | ");
        
        // 事务ID
        if (transactionId > 0) {
            sb.append(String.format("TXN:%d", transactionId));
        } else {
            sb.append("TXN:----");
        }
        sb.append(" | ");
        
        // 表名
        if (tableName != null && !tableName.isEmpty()) {
            sb.append(String.format("%-15s", tableName));
        } else {
            sb.append("               ");
        }
        sb.append(" | ");
        
        // 操作描述
        if (operation != null && !operation.isEmpty()) {
            sb.append(operation);
        }
        
        // 数据变更信息
        if (oldData != null || newData != null) {
            sb.append(" | DATA:");
            if (oldData != null) {
                sb.append("OLD[").append(truncateData(oldData)).append("]");
            }
            if (newData != null) {
                sb.append("NEW[").append(truncateData(newData)).append("]");
            }
        }
        
        // 附加信息
        if (additionalInfo != null && !additionalInfo.isEmpty()) {
            sb.append(" | INFO:").append(additionalInfo);
        }
        
        return sb.toString();
    }
    
    /**
     * 截断过长的数据用于显示
     */
    private String truncateData(String data) {
        if (data == null) return "null";
        if (data.length() <= 50) return data;
        return data.substring(0, 47) + "...";
    }
    
    /**
     * 从日志行解析LogEntry
     */
    public static LogEntry fromLogLine(String logLine) {
        try {
            String[] parts = logLine.split(" \\| ");
            if (parts.length < 5) {
                return null;
            }
            
            // 解析时间戳
            LocalDateTime timestamp = LocalDateTime.parse(parts[0], 
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
            
            // 解析LSN
            long lsn = Long.parseLong(parts[1].substring(4)); // 去掉"LSN:"
            
            // 解析日志类型
            LogType logType = LogType.fromCode(parts[2].trim());
            
            // 解析事务ID
            long transactionId = 0;
            if (!parts[3].equals("TXN:----")) {
                transactionId = Long.parseLong(parts[3].substring(4)); // 去掉"TXN:"
            }
            
            // 解析表名
            String tableName = parts[4].trim();
            if (tableName.isEmpty()) {
                tableName = null;
            }
            
            // 解析操作描述
            String operation = parts.length > 5 ? parts[5] : "";
            
            // 解析数据变更信息
            String oldData = null;
            String newData = null;
            String additionalInfo = null;
            
            for (int i = 6; i < parts.length; i++) {
                String part = parts[i];
                if (part.startsWith("DATA:")) {
                    // 解析数据变更
                    String dataPart = part.substring(5);
                    if (dataPart.contains("OLD[")) {
                        int start = dataPart.indexOf("OLD[") + 4;
                        int end = dataPart.indexOf("]", start);
                        if (end > start) {
                            oldData = dataPart.substring(start, end);
                        }
                    }
                    if (dataPart.contains("NEW[")) {
                        int start = dataPart.indexOf("NEW[") + 4;
                        int end = dataPart.indexOf("]", start);
                        if (end > start) {
                            newData = dataPart.substring(start, end);
                        }
                    }
                } else if (part.startsWith("INFO:")) {
                    additionalInfo = part.substring(5);
                }
            }
            
            LogEntry entry = new LogEntry(lsn, logType, transactionId, tableName, 
                                        operation, oldData, newData, additionalInfo);
            return entry;
            
        } catch (Exception e) {
            System.err.println("解析日志行失败: " + logLine + ", 错误: " + e.getMessage());
            return null;
        }
    }
    
    // Getters
    public long getLsn() { return lsn; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public LogType getLogType() { return logType; }
    public long getTransactionId() { return transactionId; }
    public String getTableName() { return tableName; }
    public String getOperation() { return operation; }
    public String getOldData() { return oldData; }
    public String getNewData() { return newData; }
    public String getAdditionalInfo() { return additionalInfo; }
    
    @Override
    public String toString() {
        return toLogLine();
    }
}
