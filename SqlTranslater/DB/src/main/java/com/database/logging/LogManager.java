package com.database.logging;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 日志管理器 - 使用可读的文本格式
 * 日志文件存储在 data/log/ 目录下
 */
public class LogManager {
    private final String logDirectory;
    private final ReentrantReadWriteLock logLock;
    private long nextLsn;
    private final Map<Long, Long> transactionLsnMap; // 事务ID -> 最新LSN
    private final Map<Long, List<Long>> transactionLogs; // 事务ID -> LSN列表
    private final String currentLogFile;
    
    public LogManager(String dataDirectory) throws IOException {
        // 创建日志目录
        this.logDirectory = dataDirectory + File.separator + "log";
        this.logLock = new ReentrantReadWriteLock();
        this.transactionLsnMap = new HashMap<>();
        this.transactionLogs = new HashMap<>();
        
        // 确保日志目录存在
        Path logPath = Paths.get(logDirectory);
        if (!Files.exists(logPath)) {
            Files.createDirectories(logPath);
        }
        
        // 生成当前日志文件名（按日期）
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        this.currentLogFile = logDirectory + File.separator + "sparrow_" + today + ".log";
        
        // 初始化LSN
        this.nextLsn = initializeLSN();
        
        System.out.println("日志管理器初始化: " + logDirectory);
        System.out.println("当前日志文件: " + currentLogFile);
    }
    
    /**
     * 初始化LSN（从现有日志文件中获取最大LSN）
     */
    private long initializeLSN() throws IOException {
        long maxLsn = 0;
        
        // 检查当前日志文件
        if (Files.exists(Paths.get(currentLogFile))) {
            maxLsn = Math.max(maxLsn, getMaxLsnFromFile(currentLogFile));
        }
        
        // 检查其他日志文件
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(logDirectory), "sparrow_*.log")) {
            for (Path logFile : stream) {
                maxLsn = Math.max(maxLsn, getMaxLsnFromFile(logFile.toString()));
            }
        } catch (IOException e) {
            // 忽略错误，使用默认值
        }
        
        return maxLsn + 1;
    }
    
    /**
     * 从日志文件中获取最大LSN
     */
    private long getMaxLsnFromFile(String filePath) throws IOException {
        long maxLsn = 0;
        
        // 检查文件是否存在且不为空
        Path file = Paths.get(filePath);
        if (!Files.exists(file) || Files.size(file) == 0) {
            return maxLsn;
        }
        
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue; // 跳过空行
                }
                
                if (line.contains("LSN:")) {
                    try {
                        int lsnStart = line.indexOf("LSN:") + 4;
                        int lsnEnd = line.indexOf(" ", lsnStart);
                        if (lsnEnd == -1) lsnEnd = line.length();
                        String lsnStr = line.substring(lsnStart, lsnEnd).trim();
                        if (!lsnStr.isEmpty()) {
                            long lsn = Long.parseLong(lsnStr);
                            maxLsn = Math.max(maxLsn, lsn);
                        }
                    } catch (NumberFormatException e) {
                        // 忽略解析错误
                        System.err.println("解析LSN失败: " + line + ", 错误: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("读取日志文件失败: " + filePath + ", 错误: " + e.getMessage());
        }
        
        return maxLsn;
    }
    
    /**
     * 写入日志条目
     */
    public long writeLogEntry(LogEntry entry) throws IOException {
        logLock.writeLock().lock();
        try {
            // 设置LSN
            LogEntry newEntry = new LogEntry(nextLsn, entry.getLogType(), entry.getTransactionId(),
                                           entry.getTableName(), entry.getOperation(),
                                           entry.getOldData(), entry.getNewData(), entry.getAdditionalInfo());
            
            // 写入日志文件 - 使用UTF-8编码
            try (FileWriter writer = new FileWriter(currentLogFile, StandardCharsets.UTF_8, true);
                 BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
                bufferedWriter.write(newEntry.toLogLine());
                bufferedWriter.newLine();
                bufferedWriter.flush();
            }
            
            // 更新状态
            nextLsn++;
            
            // 更新事务映射
            if (newEntry.getTransactionId() > 0) {
                transactionLsnMap.put(newEntry.getTransactionId(), newEntry.getLsn());
                transactionLogs.computeIfAbsent(newEntry.getTransactionId(), k -> new ArrayList<>())
                              .add(newEntry.getLsn());
            }
            
            return newEntry.getLsn();
        } finally {
            logLock.writeLock().unlock();
        }
    }
    
    /**
     * 开始事务
     */
    public long beginTransaction() throws IOException {
        long transactionId = System.currentTimeMillis();
        LogEntry entry = new LogEntry(0, LogEntry.LogType.TRANSACTION_START, transactionId, 
                                    null, "事务开始", null, null, "事务ID: " + transactionId);
        writeLogEntry(entry);
        return transactionId;
    }
    
    /**
     * 提交事务
     */
    public void commitTransaction(long transactionId) throws IOException {
        LogEntry entry = new LogEntry(0, LogEntry.LogType.TRANSACTION_COMMIT, transactionId, 
                                    null, "事务提交", null, null, "事务ID: " + transactionId);
        writeLogEntry(entry);
    }
    
    /**
     * 回滚事务
     */
    public void abortTransaction(long transactionId) throws IOException {
        LogEntry entry = new LogEntry(0, LogEntry.LogType.TRANSACTION_ABORT, transactionId, 
                                    null, "事务回滚", null, null, "事务ID: " + transactionId);
        writeLogEntry(entry);
    }
    
    /**
     * 记录SQL操作
     */
    public long logSQLOperation(long transactionId, String sql, String tableName, 
                              String operation, String oldData, String newData) throws IOException {
        LogEntry.LogType logType = LogEntry.LogType.SYSTEM;
        
        // 根据SQL类型确定日志类型
        String upperSql = sql.toUpperCase().trim();
        if (upperSql.startsWith("INSERT")) {
            logType = LogEntry.LogType.INSERT;
        } else if (upperSql.startsWith("UPDATE")) {
            logType = LogEntry.LogType.UPDATE;
        } else if (upperSql.startsWith("DELETE")) {
            logType = LogEntry.LogType.DELETE;
        } else if (upperSql.startsWith("SELECT")) {
            logType = LogEntry.LogType.SELECT;
        } else if (upperSql.startsWith("CREATE TABLE")) {
            logType = LogEntry.LogType.CREATE_TABLE;
        } else if (upperSql.startsWith("DROP TABLE")) {
            logType = LogEntry.LogType.DROP_TABLE;
        }
        
        LogEntry entry = new LogEntry(0, logType, transactionId, tableName, 
                                    operation, oldData, newData, "SQL: " + sql);
        return writeLogEntry(entry);
    }
    
    /**
     * 记录系统操作
     */
    public long logSystemOperation(String operation, String additionalInfo) throws IOException {
        LogEntry entry = new LogEntry(0, LogEntry.LogType.SYSTEM, 0, null, 
                                    operation, null, null, additionalInfo);
        return writeLogEntry(entry);
    }
    
    /**
     * 创建检查点
     */
    public void createCheckpoint() throws IOException {
        LogEntry entry = new LogEntry(0, LogEntry.LogType.CHECKPOINT, 0, null, 
                                    "检查点创建", null, null, "LSN: " + nextLsn);
        writeLogEntry(entry);
        
        // 记录检查点信息到单独文件
        String checkpointFile = logDirectory + File.separator + "checkpoint.log";
        try (FileWriter writer = new FileWriter(checkpointFile, StandardCharsets.UTF_8, true);
             BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
            // 使用简单的时间格式避免兼容性问题
            String timestamp = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            bufferedWriter.write(timestamp);
            bufferedWriter.write(" | CHECKPOINT | LSN: " + nextLsn);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (Exception e) {
            System.err.println("写入检查点文件失败: " + e.getMessage());
            // 不抛出异常，避免影响主程序
        }
    }
    
    /**
     * 获取事务的所有日志条目
     */
    public List<LogEntry> getTransactionLogs(long transactionId) throws IOException {
        logLock.readLock().lock();
        try {
            List<Long> lsnList = transactionLogs.get(transactionId);
            if (lsnList == null) {
                return new ArrayList<>();
            }
            
            List<LogEntry> entries = new ArrayList<>();
            for (long lsn : lsnList) {
                LogEntry entry = getLogEntry(lsn);
                if (entry != null) {
                    entries.add(entry);
                }
            }
            
            return entries;
        } finally {
            logLock.readLock().unlock();
        }
    }
    
    /**
     * 根据LSN获取日志条目
     */
    public LogEntry getLogEntry(long lsn) throws IOException {
        logLock.readLock().lock();
        try {
            // 在当前日志文件中查找
            if (Files.exists(Paths.get(currentLogFile))) {
                LogEntry entry = findLogEntryInFile(currentLogFile, lsn);
                if (entry != null) {
                    return entry;
                }
            }
            
            // 在其他日志文件中查找
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(logDirectory), "sparrow_*.log")) {
                for (Path logFile : stream) {
                    LogEntry entry = findLogEntryInFile(logFile.toString(), lsn);
                    if (entry != null) {
                        return entry;
                    }
                }
            }
            
            return null;
        } finally {
            logLock.readLock().unlock();
        }
    }
    
    /**
     * 在指定文件中查找日志条目
     */
    private LogEntry findLogEntryInFile(String filePath, long lsn) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("LSN:" + String.format("%08d", lsn))) {
                    return LogEntry.fromLogLine(line);
                }
            }
        }
        return null;
    }
    
    /**
     * 清理已提交的事务日志
     */
    public void cleanupCommittedTransactions() throws IOException {
        logLock.writeLock().lock();
        try {
            List<Long> committedTransactions = new ArrayList<>();
            
            for (Map.Entry<Long, List<Long>> entry : transactionLogs.entrySet()) {
                long transactionId = entry.getKey();
                List<Long> lsnList = entry.getValue();
                
                // 检查最后一个日志条目是否为COMMIT
                if (!lsnList.isEmpty()) {
                    LogEntry lastEntry = getLogEntry(lsnList.get(lsnList.size() - 1));
                    if (lastEntry != null && lastEntry.getLogType() == LogEntry.LogType.TRANSACTION_COMMIT) {
                        committedTransactions.add(transactionId);
                    }
                }
            }
            
            // 清理已提交的事务
            for (long transactionId : committedTransactions) {
                transactionLogs.remove(transactionId);
                transactionLsnMap.remove(transactionId);
            }
            
            // 记录清理操作
            logSystemOperation("清理已提交事务", "清理了 " + committedTransactions.size() + " 个已提交的事务");
            
        } finally {
            logLock.writeLock().unlock();
        }
    }
    
    /**
     * 获取日志统计信息
     */
    public Map<String, Object> getLogStats() {
        logLock.readLock().lock();
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("nextLsn", nextLsn);
            stats.put("activeTransactions", transactionLsnMap.size());
            stats.put("totalLogEntries", transactionLogs.values().stream().mapToInt(List::size).sum());
            stats.put("logDirectory", logDirectory);
            stats.put("currentLogFile", currentLogFile);
            
            try {
                if (Files.exists(Paths.get(currentLogFile))) {
                    stats.put("currentLogFileSize", Files.size(Paths.get(currentLogFile)));
                } else {
                    stats.put("currentLogFileSize", 0L);
                }
            } catch (IOException e) {
                stats.put("currentLogFileSize", -1L);
            }
            
            return stats;
        } finally {
            logLock.readLock().unlock();
        }
    }
    
    /**
     * 获取日志文件列表
     */
    public List<String> getLogFiles() throws IOException {
        List<String> logFiles = new ArrayList<>();
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(logDirectory), "sparrow_*.log")) {
            for (Path logFile : stream) {
                logFiles.add(logFile.getFileName().toString());
            }
        }
        
        return logFiles;
    }
    
    /**
     * 关闭日志管理器
     */
    public void close() throws IOException {
        logLock.writeLock().lock();
        try {
            // 记录关闭信息
            logSystemOperation("日志管理器关闭", "正常关闭");
            System.out.println("日志管理器已关闭");
        } finally {
            logLock.writeLock().unlock();
        }
    }
}
