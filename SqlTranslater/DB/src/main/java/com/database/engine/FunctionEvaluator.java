package com.database.engine;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;

/**
 * 函数计算器 - 负责计算各种SQL函数
 * 支持数学函数、字符串函数、日期函数和聚合函数
 */
public class FunctionEvaluator {
    
    private static final Random random = new Random();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * 计算函数值
     */
    public static Object evaluateFunction(String functionName, List<Object> arguments) {
        if (functionName == null) {
            return null;
        }
        
        String funcName = functionName.toUpperCase();
        
        try {
            switch (funcName) {
                // 数学函数
                case "ABS":
                    return evaluateAbs(arguments);
                case "CEIL":
                    return evaluateCeil(arguments);
                case "FLOOR":
                    return evaluateFloor(arguments);
                case "ROUND":
                    return evaluateRound(arguments);
                case "SQRT":
                    return evaluateSqrt(arguments);
                case "POWER":
                    return evaluatePower(arguments);
                case "MOD":
                    return evaluateMod(arguments);
                case "RAND":
                    return evaluateRand(arguments);
                
                // 字符串函数
                case "UPPER":
                    return evaluateUpper(arguments);
                case "LOWER":
                    return evaluateLower(arguments);
                case "LENGTH":
                    return evaluateLength(arguments);
                case "SUBSTRING":
                    return evaluateSubstring(arguments);
                case "CONCAT":
                    return evaluateConcat(arguments);
                case "TRIM":
                    return evaluateTrim(arguments);
                case "LTRIM":
                    return evaluateLTrim(arguments);
                case "RTRIM":
                    return evaluateRTrim(arguments);
                case "REPLACE":
                    return evaluateReplace(arguments);
                
                // 日期函数
                case "NOW":
                case "CURRENT_TIMESTAMP":
                    return evaluateNow(arguments);
                case "CURRENT_DATE":
                    return evaluateCurrentDate(arguments);
                case "CURRENT_TIME":
                    return evaluateCurrentTime(arguments);
                case "YEAR":
                    return evaluateYear(arguments);
                case "MONTH":
                    return evaluateMonth(arguments);
                case "DAY":
                    return evaluateDay(arguments);
                case "HOUR":
                    return evaluateHour(arguments);
                case "MINUTE":
                    return evaluateMinute(arguments);
                case "SECOND":
                    return evaluateSecond(arguments);
                case "DATE_ADD":
                    return evaluateDateAdd(arguments);
                case "DATE_SUB":
                    return evaluateDateSub(arguments);
                case "DATEDIFF":
                    return evaluateDateDiff(arguments);
                
                default:
                    throw new RuntimeException("不支持的函数: " + functionName);
            }
        } catch (Exception e) {
            throw new RuntimeException("函数 " + functionName + " 执行错误: " + e.getMessage(), e);
        }
    }
    
    // ==================== 数学函数 ====================
    
    private static Object evaluateAbs(List<Object> args) {
        if (args.size() != 1) {
            throw new IllegalArgumentException("ABS函数需要1个参数");
        }
        
        Object arg = args.get(0);
        if (arg == null) return null;
        
        if (arg instanceof Integer) {
            return Math.abs((Integer) arg);
        } else if (arg instanceof Double) {
            return Math.abs((Double) arg);
        } else if (arg instanceof Float) {
            return Math.abs((Float) arg);
        } else if (arg instanceof Long) {
            return Math.abs((Long) arg);
        } else {
            // 尝试转换为数字
            try {
                double value = Double.parseDouble(arg.toString());
                return Math.abs(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("ABS函数参数必须是数字");
            }
        }
    }
    
    private static Object evaluateCeil(List<Object> args) {
        if (args.size() != 1) {
            throw new IllegalArgumentException("CEIL函数需要1个参数");
        }
        
        Object arg = args.get(0);
        if (arg == null) return null;
        
        double value = getNumericValue(arg);
        return (int) Math.ceil(value);
    }
    
    private static Object evaluateFloor(List<Object> args) {
        if (args.size() != 1) {
            throw new IllegalArgumentException("FLOOR函数需要1个参数");
        }
        
        Object arg = args.get(0);
        if (arg == null) return null;
        
        double value = getNumericValue(arg);
        return (int) Math.floor(value);
    }
    
    private static Object evaluateRound(List<Object> args) {
        if (args.size() < 1 || args.size() > 2) {
            throw new IllegalArgumentException("ROUND函数需要1-2个参数");
        }
        
        Object arg = args.get(0);
        if (arg == null) return null;
        
        double value = getNumericValue(arg);
        
        if (args.size() == 1) {
            return Math.round(value);
        } else {
            int precision = (int) getNumericValue(args.get(1));
            double factor = Math.pow(10, precision);
            return Math.round(value * factor) / factor;
        }
    }
    
    private static Object evaluateSqrt(List<Object> args) {
        if (args.size() != 1) {
            throw new IllegalArgumentException("SQRT函数需要1个参数");
        }
        
        Object arg = args.get(0);
        if (arg == null) return null;
        
        double value = getNumericValue(arg);
        if (value < 0) {
            throw new IllegalArgumentException("SQRT函数参数不能为负数");
        }
        
        return Math.sqrt(value);
    }
    
    private static Object evaluatePower(List<Object> args) {
        if (args.size() != 2) {
            throw new IllegalArgumentException("POWER函数需要2个参数");
        }
        
        Object base = args.get(0);
        Object exponent = args.get(1);
        
        if (base == null || exponent == null) return null;
        
        double baseValue = getNumericValue(base);
        double expValue = getNumericValue(exponent);
        
        return Math.pow(baseValue, expValue);
    }
    
    private static Object evaluateMod(List<Object> args) {
        if (args.size() != 2) {
            throw new IllegalArgumentException("MOD函数需要2个参数");
        }
        
        Object dividend = args.get(0);
        Object divisor = args.get(1);
        
        if (dividend == null || divisor == null) return null;
        
        double dividendValue = getNumericValue(dividend);
        double divisorValue = getNumericValue(divisor);
        
        if (divisorValue == 0) {
            throw new IllegalArgumentException("MOD函数除数不能为0");
        }
        
        return dividendValue % divisorValue;
    }
    
    private static Object evaluateRand(List<Object> args) {
        if (args.size() > 1) {
            throw new IllegalArgumentException("RAND函数最多需要1个参数");
        }
        
        if (args.size() == 1) {
            // 带种子的随机数
            Object seed = args.get(0);
            if (seed != null) {
                long seedValue = (long) getNumericValue(seed);
                Random seededRandom = new Random(seedValue);
                return seededRandom.nextDouble();
            }
        }
        
        return random.nextDouble();
    }
    
    // ==================== 字符串函数 ====================
    
    private static Object evaluateUpper(List<Object> args) {
        if (args.size() != 1) {
            throw new IllegalArgumentException("UPPER函数需要1个参数");
        }
        
        Object arg = args.get(0);
        if (arg == null) return null;
        
        return arg.toString().toUpperCase();
    }
    
    private static Object evaluateLower(List<Object> args) {
        if (args.size() != 1) {
            throw new IllegalArgumentException("LOWER函数需要1个参数");
        }
        
        Object arg = args.get(0);
        if (arg == null) return null;
        
        return arg.toString().toLowerCase();
    }
    
    private static Object evaluateLength(List<Object> args) {
        if (args.size() != 1) {
            throw new IllegalArgumentException("LENGTH函数需要1个参数");
        }
        
        Object arg = args.get(0);
        if (arg == null) return null;
        
        return arg.toString().length();
    }
    
    private static Object evaluateSubstring(List<Object> args) {
        if (args.size() < 2 || args.size() > 3) {
            throw new IllegalArgumentException("SUBSTRING函数需要2-3个参数");
        }
        
        Object str = args.get(0);
        Object start = args.get(1);
        
        if (str == null || start == null) return null;
        
        String string = str.toString();
        int startIndex = (int) getNumericValue(start) - 1; // SQL索引从1开始
        
        if (startIndex < 0) startIndex = 0;
        if (startIndex >= string.length()) return "";
        
        if (args.size() == 3) {
            Object length = args.get(2);
            if (length != null) {
                int len = (int) getNumericValue(length);
                int endIndex = Math.min(startIndex + len, string.length());
                return string.substring(startIndex, endIndex);
            }
        }
        
        return string.substring(startIndex);
    }
    
    private static Object evaluateConcat(List<Object> args) {
        if (args.isEmpty()) {
            throw new IllegalArgumentException("CONCAT函数需要至少1个参数");
        }
        
        StringBuilder result = new StringBuilder();
        for (Object arg : args) {
            if (arg != null) {
                result.append(arg.toString());
            }
        }
        
        return result.toString();
    }
    
    private static Object evaluateTrim(List<Object> args) {
        if (args.size() != 1) {
            throw new IllegalArgumentException("TRIM函数需要1个参数");
        }
        
        Object arg = args.get(0);
        if (arg == null) return null;
        
        return arg.toString().trim();
    }
    
    private static Object evaluateLTrim(List<Object> args) {
        if (args.size() != 1) {
            throw new IllegalArgumentException("LTRIM函数需要1个参数");
        }
        
        Object arg = args.get(0);
        if (arg == null) return null;
        
        String str = arg.toString();
        return str.replaceAll("^\\s+", "");
    }
    
    private static Object evaluateRTrim(List<Object> args) {
        if (args.size() != 1) {
            throw new IllegalArgumentException("RTRIM函数需要1个参数");
        }
        
        Object arg = args.get(0);
        if (arg == null) return null;
        
        String str = arg.toString();
        return str.replaceAll("\\s+$", "");
    }
    
    private static Object evaluateReplace(List<Object> args) {
        if (args.size() != 3) {
            throw new IllegalArgumentException("REPLACE函数需要3个参数");
        }
        
        Object str = args.get(0);
        Object search = args.get(1);
        Object replacement = args.get(2);
        
        if (str == null) return null;
        if (search == null) return str;
        if (replacement == null) replacement = "";
        
        return str.toString().replace(search.toString(), replacement.toString());
    }
    
    // ==================== 日期函数 ====================
    
    private static Object evaluateNow(List<Object> args) {
        if (args.size() != 0) {
            throw new IllegalArgumentException("NOW函数不需要参数");
        }
        
        return LocalDateTime.now().format(DATETIME_FORMAT);
    }
    
    private static Object evaluateCurrentDate(List<Object> args) {
        if (args.size() != 0) {
            throw new IllegalArgumentException("CURRENT_DATE函数不需要参数");
        }
        
        return LocalDate.now().format(DATE_FORMAT);
    }
    
    private static Object evaluateCurrentTime(List<Object> args) {
        if (args.size() != 0) {
            throw new IllegalArgumentException("CURRENT_TIME函数不需要参数");
        }
        
        return LocalTime.now().format(TIME_FORMAT);
    }
    
    private static Object evaluateYear(List<Object> args) {
        if (args.size() != 1) {
            throw new IllegalArgumentException("YEAR函数需要1个参数");
        }
        
        Object arg = args.get(0);
        if (arg == null) return null;
        
        LocalDate date = parseDate(arg.toString());
        return date.getYear();
    }
    
    private static Object evaluateMonth(List<Object> args) {
        if (args.size() != 1) {
            throw new IllegalArgumentException("MONTH函数需要1个参数");
        }
        
        Object arg = args.get(0);
        if (arg == null) return null;
        
        LocalDate date = parseDate(arg.toString());
        return date.getMonthValue();
    }
    
    private static Object evaluateDay(List<Object> args) {
        if (args.size() != 1) {
            throw new IllegalArgumentException("DAY函数需要1个参数");
        }
        
        Object arg = args.get(0);
        if (arg == null) return null;
        
        LocalDate date = parseDate(arg.toString());
        return date.getDayOfMonth();
    }
    
    private static Object evaluateHour(List<Object> args) {
        if (args.size() != 1) {
            throw new IllegalArgumentException("HOUR函数需要1个参数");
        }
        
        Object arg = args.get(0);
        if (arg == null) return null;
        
        LocalDateTime dateTime = parseDateTime(arg.toString());
        return dateTime.getHour();
    }
    
    private static Object evaluateMinute(List<Object> args) {
        if (args.size() != 1) {
            throw new IllegalArgumentException("MINUTE函数需要1个参数");
        }
        
        Object arg = args.get(0);
        if (arg == null) return null;
        
        LocalDateTime dateTime = parseDateTime(arg.toString());
        return dateTime.getMinute();
    }
    
    private static Object evaluateSecond(List<Object> args) {
        if (args.size() != 1) {
            throw new IllegalArgumentException("SECOND函数需要1个参数");
        }
        
        Object arg = args.get(0);
        if (arg == null) return null;
        
        LocalDateTime dateTime = parseDateTime(arg.toString());
        return dateTime.getSecond();
    }
    
    private static Object evaluateDateAdd(List<Object> args) {
        if (args.size() != 3) {
            throw new IllegalArgumentException("DATE_ADD函数需要3个参数: date, interval, unit");
        }
        
        Object dateArg = args.get(0);
        Object intervalArg = args.get(1);
        Object unitArg = args.get(2);
        
        if (dateArg == null || intervalArg == null || unitArg == null) return null;
        
        LocalDate date = parseDate(dateArg.toString());
        long interval = (long) getNumericValue(intervalArg);
        String unit = unitArg.toString().toUpperCase();
        
        switch (unit) {
            case "DAY":
            case "DAYS":
                return date.plusDays(interval).format(DATE_FORMAT);
            case "MONTH":
            case "MONTHS":
                return date.plusMonths(interval).format(DATE_FORMAT);
            case "YEAR":
            case "YEARS":
                return date.plusYears(interval).format(DATE_FORMAT);
            default:
                throw new IllegalArgumentException("不支持的日期单位: " + unit);
        }
    }
    
    private static Object evaluateDateSub(List<Object> args) {
        if (args.size() != 3) {
            throw new IllegalArgumentException("DATE_SUB函数需要3个参数: date, interval, unit");
        }
        
        Object dateArg = args.get(0);
        Object intervalArg = args.get(1);
        Object unitArg = args.get(2);
        
        if (dateArg == null || intervalArg == null || unitArg == null) return null;
        
        LocalDate date = parseDate(dateArg.toString());
        long interval = (long) getNumericValue(intervalArg);
        String unit = unitArg.toString().toUpperCase();
        
        switch (unit) {
            case "DAY":
            case "DAYS":
                return date.minusDays(interval).format(DATE_FORMAT);
            case "MONTH":
            case "MONTHS":
                return date.minusMonths(interval).format(DATE_FORMAT);
            case "YEAR":
            case "YEARS":
                return date.minusYears(interval).format(DATE_FORMAT);
            default:
                throw new IllegalArgumentException("不支持的日期单位: " + unit);
        }
    }
    
    private static Object evaluateDateDiff(List<Object> args) {
        if (args.size() != 2) {
            throw new IllegalArgumentException("DATEDIFF函数需要2个参数");
        }
        
        Object date1Arg = args.get(0);
        Object date2Arg = args.get(1);
        
        if (date1Arg == null || date2Arg == null) return null;
        
        LocalDate date1 = parseDate(date1Arg.toString());
        LocalDate date2 = parseDate(date2Arg.toString());
        
        return ChronoUnit.DAYS.between(date2, date1);
    }
    
    // ==================== 工具方法 ====================
    
    private static double getNumericValue(Object obj) {
        if (obj == null) {
            throw new IllegalArgumentException("数值参数不能为null");
        }
        
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        }
        
        try {
            return Double.parseDouble(obj.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无法转换为数字: " + obj);
        }
    }
    
    private static LocalDate parseDate(String dateStr) {
        try {
            // 尝试不同的日期格式
            if (dateStr.length() == 10) {
                return LocalDate.parse(dateStr, DATE_FORMAT);
            } else if (dateStr.length() >= 19) {
                // 如果包含时间，只取日期部分
                return LocalDateTime.parse(dateStr.substring(0, 19), DATETIME_FORMAT).toLocalDate();
            } else {
                throw new IllegalArgumentException("无效的日期格式: " + dateStr);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("无法解析日期: " + dateStr, e);
        }
    }
    
    private static LocalDateTime parseDateTime(String dateTimeStr) {
        try {
            if (dateTimeStr.length() >= 19) {
                return LocalDateTime.parse(dateTimeStr.substring(0, 19), DATETIME_FORMAT);
            } else if (dateTimeStr.length() == 10) {
                // 如果只有日期，添加默认时间
                return LocalDate.parse(dateTimeStr, DATE_FORMAT).atStartOfDay();
            } else {
                throw new IllegalArgumentException("无效的日期时间格式: " + dateTimeStr);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("无法解析日期时间: " + dateTimeStr, e);
        }
    }
}
