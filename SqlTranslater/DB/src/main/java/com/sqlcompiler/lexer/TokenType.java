package com.sqlcompiler.lexer;

/**
 * SQL词法分析器中的Token类型枚举
 * 定义了SQL语言中所有可能的词法单元类型
 */
public enum TokenType {
    // 关键字
    CREATE("CREATE"),
    TABLE("TABLE"),
    INSERT("INSERT"),
    INTO("INTO"),
    VALUES("VALUES"),
    SELECT("SELECT"),
    FROM("FROM"),
    WHERE("WHERE"),
    DELETE("DELETE"),
    DROP("DROP"),
    ALTER("ALTER"),
    UPDATE("UPDATE"),
    VIEW("VIEW"),
    SET("SET"),
    FUNCTION("FUNCTION"),
    PROCEDURE("PROCEDURE"),
    CALL("CALL"),
    BEGIN("BEGIN"),
    END("END"),
    RETURN("RETURN"),
    RETURNS("RETURNS"),
    DECLARE("DECLARE"),
    AND("AND"),
    OR("OR"),
    NOT("NOT"),
    AS("AS"),
    DISTINCT("DISTINCT"),
    ORDER("ORDER"),
    BY("BY"),
    GROUP("GROUP"),
    HAVING("HAVING"),
    LIMIT("LIMIT"),
    OFFSET("OFFSET"),
    JOIN("JOIN"),
    INNER("INNER"),
    LEFT("LEFT"),
    RIGHT("RIGHT"),
    OUTER("OUTER"),
    ON("ON"),
    IS("IS"),
    NULL("NULL"),
    TRUE("TRUE"),
    PERMANENT("PERMANENT"),
    FALSE("FALSE"),
    IF("IF"),
    ELSE("ELSE"),
    ELSEIF("ELSEIF"),
    ENDIF("ENDIF"),
    WHILE("WHILE"),
    LOOP("LOOP"),
    ENDLOOP("ENDLOOP"),
    REPEAT("REPEAT"),
    UNTIL("UNTIL"),
    CASE("CASE"),
    WHEN("WHEN"),
    THEN("THEN"),
    EXISTS("EXISTS"),

    // 数据类型
    INT("INT"),
    INTEGER("INTEGER"),
    VARCHAR("VARCHAR"),
    CHAR("CHAR"),
    TEXT("TEXT"),
    DECIMAL("DECIMAL"),
    FLOAT("FLOAT"),
    DOUBLE("DOUBLE"),
    BOOLEAN("BOOLEAN"),
    DATE("DATE"),
    TIME("TIME"),
    TIMESTAMP("TIMESTAMP"),
    
    // 约束关键字
    PRIMARY("PRIMARY"),
    KEY("KEY"),
    PRIMARY_KEY("PRIMARY KEY"),
    FOREIGN("FOREIGN"),
    FOREIGN_KEY("FOREIGN KEY"),
    REFERENCES("REFERENCES"),
    UNIQUE("UNIQUE"),
    NOT_NULL("NOT NULL"),
    DEFAULT("DEFAULT"),
    AUTO_INCREMENT("AUTO_INCREMENT"),
    CHECK("CHECK"),
    ASC("ASC"),
    DESC("DESC"),
    
    // 存储格式关键字
    STORAGE("STORAGE"),
    ROW("ROW"),
    COLUMN("COLUMN"),
    ROW_STORAGE("ROW_STORAGE"),
    COLUMN_STORAGE("COLUMN_STORAGE"),

    // 聚合函数
    COUNT("COUNT"),
    SUM("SUM"),
    AVG("AVG"),
    MAX("MAX"),
    MIN("MIN"),
    
    // 数学函数
    ABS("ABS"),
    CEIL("CEIL"),
    FLOOR("FLOOR"),
    ROUND("ROUND"),
    SQRT("SQRT"),
    POWER("POWER"),
    MOD("MOD"),
    RAND("RAND"),

    // 字符串函数
    UPPER("UPPER"),
    LOWER("LOWER"),
    LENGTH("LENGTH"),
    SUBSTRING("SUBSTRING"),
    CONCAT("CONCAT"),
    TRIM("TRIM"),
    LTRIM("LTRIM"),
    RTRIM("RTRIM"),
    REPLACE("REPLACE"),

    // 日期函数
    NOW("NOW"),
    CURRENT_DATE("CURRENT_DATE"),
    CURRENT_TIME("CURRENT_TIME"),
    CURRENT_TIMESTAMP("CURRENT_TIMESTAMP"),
    YEAR("YEAR"),
    MONTH("MONTH"),
    DAY("DAY"),
    HOUR("HOUR"),
    MINUTE("MINUTE"),
    SECOND("SECOND"),
    DATE_ADD("DATE_ADD"),
    DATE_SUB("DATE_SUB"),
    DATEDIFF("DATEDIFF"),

    // 运算符
    EQUALS("="),
    NOT_EQUALS("!="),
    LESS_THAN("<"),
    GREATER_THAN(">"),
    LESS_EQUAL("<="),
    GREATER_EQUAL(">="),
    LIKE("LIKE"),
    IN("IN"),
    BETWEEN("BETWEEN"),
    PLUS("+"),
    MINUS("-"),
    MULTIPLY("*"),
    DIVIDE("/"),
    MODULO("%"),
    
    // 分隔符
    SEMICOLON(";"),
    COMMA(","),
    DOT("."),
    LEFT_PAREN("("),
    RIGHT_PAREN(")"),
    LEFT_BRACKET("["),
    RIGHT_BRACKET("]"),
    LEFT_BRACE("{"),
    RIGHT_BRACE("}"),
    SINGLE_QUOTE("'"),
    DOUBLE_QUOTE("\""),
    BACKTICK("`"),
    
    // 字面量
    IDENTIFIER("IDENTIFIER"),
    STRING_LITERAL("STRING_LITERAL"),
    NUMBER_LITERAL("NUMBER_LITERAL"),
    BOOLEAN_LITERAL("BOOLEAN_LITERAL"),
    
    // 特殊Token
    EOF("EOF"),
    UNKNOWN("UNKNOWN");
    
    private final String value;
    
    TokenType(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * 判断是否为关键字
     */
    public boolean isKeyword() {
        return this == CREATE || this == TABLE || this == INSERT || this == INTO ||
               this == VALUES || this == SELECT || this == FROM || this == WHERE ||
               this == DELETE || this == DROP || this == ALTER || this == UPDATE ||
               this == VIEW || this == FUNCTION || this == PROCEDURE || this == CALL ||
               this == BEGIN || this == END || this == RETURN || this == RETURNS || this == DECLARE ||
               this == SET || this == AND || this == OR || this == NOT || this == AS ||
               this == DISTINCT || this == ORDER || this == BY || this == GROUP ||
               this == HAVING || this == LIMIT || this == OFFSET || this == JOIN ||
               this == INNER || this == LEFT || this == RIGHT || this == OUTER ||
               this == ON || this == IS || this == NULL || this == TRUE || this == FALSE ||
               this == IF || this == EXISTS ||
               this == IF || this == ELSE || this == ELSEIF || this == ENDIF ||
               this == WHILE || this == LOOP || this == ENDLOOP || this == REPEAT ||
               this == UNTIL || this == CASE || this == WHEN || this == THEN || this == EXISTS ||
               this == INT || this == INTEGER || this == VARCHAR || this == CHAR ||
               this == TEXT || this == DECIMAL || this == FLOAT || this == DOUBLE ||
               this == BOOLEAN || this == DATE || this == TIME || this == TIMESTAMP ||
               this == PRIMARY || this == KEY || this == PRIMARY_KEY || this == FOREIGN || 
               this == FOREIGN_KEY || this == REFERENCES || this == UNIQUE || this == NOT_NULL || 
               this == DEFAULT || this == AUTO_INCREMENT || this == CHECK ||
               this == ASC || this == DESC ||
               this == STORAGE || this == ROW || this == COLUMN || this == ROW_STORAGE || this == COLUMN_STORAGE ||
               this == COUNT || this == SUM || this == AVG || this == MAX || this == MIN ||
               this == LIKE || this == IN || this == BETWEEN;
    }
    
    /**
     * 判断是否为运算符
     */
    public boolean isOperator() {
        return this == EQUALS || this == NOT_EQUALS || this == LESS_THAN ||
               this == GREATER_THAN || this == LESS_EQUAL || this == GREATER_EQUAL ||
               this == LIKE || this == IN || this == BETWEEN || this == PLUS ||
               this == MINUS || this == MULTIPLY || this == DIVIDE || this == MODULO;
    }
    
    /**
     * 判断是否为分隔符
     */
    public boolean isDelimiter() {
        return this == SEMICOLON || this == COMMA || this == DOT ||
               this == LEFT_PAREN || this == RIGHT_PAREN ||
               this == LEFT_BRACKET || this == RIGHT_BRACKET ||
               this == LEFT_BRACE || this == RIGHT_BRACE ||
               this == SINGLE_QUOTE || this == DOUBLE_QUOTE || this == BACKTICK;
    }
    
    /**
     * 判断是否为字面量
     */
    public boolean isLiteral() {
        return this == IDENTIFIER || this == STRING_LITERAL ||
               this == NUMBER_LITERAL || this == BOOLEAN_LITERAL;
    }
}
