package com.sqlcompiler.parser;

import com.sqlcompiler.ast.*;
import com.sqlcompiler.exception.SyntaxException;
import com.sqlcompiler.lexer.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SQL语法分析器
 * 使用递归下降分析法解析SQL语句
 */
public class SyntaxAnalyzer {
    private final List<Token> tokens;
    private int currentTokenIndex;
    
    public SyntaxAnalyzer(List<Token> tokens) {
        this.tokens = tokens;
        this.currentTokenIndex = 0;
    }
    
    /**
     * 解析SQL语句
     */
    public Statement parse() throws SyntaxException {
        if (tokens.isEmpty() || tokens.get(0).getType() == TokenType.EOF) {
            throw new SyntaxException("空语句", new Position(1, 1));
        }
        
        Statement statement = parseStatement();
        
        // 检查是否还有未处理的token
        if (currentToken().getType() != TokenType.EOF) {
            throw new SyntaxException(
                String.format("语句解析不完整，在 '%s' 处停止", currentToken().getValue()),
                currentToken().getPosition(),
                "语句结束符 ';' 或语句结束"
            );
        }
        
        return statement;
    }
    
    /**
     * 解析批量SQL语句
     */
    public BatchStatement parseBatch() throws SyntaxException {
        if (tokens.isEmpty() || tokens.get(0).getType() == TokenType.EOF) {
            throw new SyntaxException("空语句", new Position(1, 1));
        }
        
        List<Statement> statements = new ArrayList<>();
        Position startPos = currentToken().getPosition();
        
        while (currentToken().getType() != TokenType.EOF) {
            // 跳过空白和分号
            while (currentToken().getType() == TokenType.SEMICOLON) {
                nextToken();
            }
            
            if (currentToken().getType() == TokenType.EOF) {
                break;
            }
            
            // 解析单个语句
            Statement statement = parseStatement();
            statements.add(statement);
            
            // 跳过语句后的分号
            if (currentToken().getType() == TokenType.SEMICOLON) {
                nextToken();
            }
        }
        
        if (statements.isEmpty()) {
            throw new SyntaxException("没有找到有效的SQL语句", startPos);
        }
        
        return new BatchStatement(statements, startPos);
    }
    
    /**
     * 解析语句
     */
    private Statement parseStatement() throws SyntaxException {
        Token token = currentToken();
        
        switch (token.getType()) {
            case CREATE:
                return parseCreateStatement();
            case INSERT:
                return parseInsertStatement();
            case SELECT:
                return parseSelectStatement();
            case UPDATE:
                return parseUpdateStatement();
            case DELETE:
                return parseDeleteStatement();
            case DROP:
                return parseDropStatement();
            case CALL:
                return parseCallStatement();
            default:
                throw new SyntaxException(
                    String.format("不支持的语句类型 '%s'", token.getValue()),
                    token.getPosition(),
                    "CREATE TABLE/VIEW/FUNCTION, INSERT INTO, SELECT, UPDATE, DELETE FROM, DROP TABLE/VIEW/FUNCTION, CALL"
                );
        }
    }

    /**
     * 解析CREATE语句 (TABLE 或 VIEW)
     */
    private Statement parseCreateStatement() throws SyntaxException {
        Position startPos = currentToken().getPosition();

        // CREATE
        expect(TokenType.CREATE);

        Token nextToken = currentToken();
        if (nextToken.getType() == TokenType.TABLE) {
            // 回退一步，让parseCreateTableStatement重新处理CREATE
            currentTokenIndex--;
            return parseCreateTableStatement();
        } else if (nextToken.getType() == TokenType.VIEW) {
            return parseCreateViewStatement();
        } else if (nextToken.getType() == TokenType.FUNCTION) {
            return parseCreateFunctionStatement(false); // 非持久化函数
        } else if (nextToken.getType() == TokenType.PERMANENT) {
            // CREATE PERMANENT FUNCTION
            nextToken(); // 消费 PERMANENT
            // 不需要 expect(TokenType.FUNCTION)，因为 parseCreateFunctionStatement 会处理
            return parseCreateFunctionStatement(true); // 持久化函数
        } else if (nextToken.getType() == TokenType.SHARD) {
            return parseCreateShardStatement();
        } else {
            throw new SyntaxException(
                String.format("CREATE后面应该是TABLE、VIEW、FUNCTION、PERMANENT FUNCTION或SHARD，而不是 '%s'", nextToken.getValue()),
                nextToken.getPosition(),
                "TABLE、VIEW、FUNCTION、PERMANENT FUNCTION 或 SHARD"
                );
        }
    }

    /**
     * 解析DROP语句 (TABLE, VIEW, FUNCTION)
     */
    private Statement parseDropStatement() throws SyntaxException {
        Position startPos = currentToken().getPosition();

        // DROP
        expect(TokenType.DROP);

        Token nextToken = currentToken();
        if (nextToken.getType() == TokenType.TABLE) {
            return parseDropTableStatement();
        } else if (nextToken.getType() == TokenType.VIEW) {
            return parseDropViewStatement();
        } else if (nextToken.getType() == TokenType.FUNCTION) {
            return parseDropFunctionStatement();
        } else if (nextToken.getType() == TokenType.SHARD) {
            return parseDropShardStatement();
        } else {
            throw new SyntaxException(
                String.format("DROP后面应该是TABLE、VIEW、FUNCTION或SHARD，而不是 '%s'", nextToken.getValue()),
                nextToken.getPosition(),
                "TABLE、VIEW、FUNCTION 或 SHARD"
            );
        }
    }

    /**
     * 解析CREATE VIEW语句
     */
    private CreateViewStatement parseCreateViewStatement() throws SyntaxException {
        Position startPos = currentToken().getPosition();

        // VIEW
        expect(TokenType.VIEW);

        // 视图名
        String viewName = expectIdentifier();

        // AS
        expect(TokenType.AS);

        // SELECT查询
        SelectStatement selectStatement = parseSelectStatement();

        // 可选的分号
        if (currentToken().getType() == TokenType.SEMICOLON) {
            nextToken();
        }

        return new CreateViewStatement(viewName, selectStatement, startPos);
    }

    /**
     * 解析DROP VIEW语句
     */
    private DropViewStatement parseDropViewStatement() throws SyntaxException {
        Position startPos = currentToken().getPosition();

        // VIEW
        expect(TokenType.VIEW);

        // 可选的IF EXISTS
        boolean ifExists = false;
        if (currentToken().getType() == TokenType.IF) {
            nextToken(); // IF
            expect(TokenType.EXISTS);
            ifExists = true;
        }

        // 视图名
        String viewName = expectIdentifier();

        // 可选的分号
        if (currentToken().getType() == TokenType.SEMICOLON) {
            nextToken();
        }

        return new DropViewStatement(viewName, ifExists, startPos);
    }

    /**
     * 解析CREATE TABLE语句
     */
    private CreateTableStatement parseCreateTableStatement() throws SyntaxException {
        Position startPos = currentToken().getPosition();
        
        // CREATE TABLE
        expect(TokenType.CREATE);
        expect(TokenType.TABLE);
        
        // 表名
        String tableName = expectIdentifier();
        
        // (
        expect(TokenType.LEFT_PAREN);
        
        // 列定义列表
        List<ColumnDefinition> columns = new ArrayList<>();
        List<Constraint> constraints = new ArrayList<>();
        
        boolean first = true;
        while (currentToken().getType() != TokenType.RIGHT_PAREN) {
            if (!first) {
                expect(TokenType.COMMA);
            }
            first = false;
            
            if (isColumnDefinition()) {
                columns.add(parseColumnDefinition());
            } else {
                constraints.add(parseConstraint());
            }
        }
        
        // )
        expect(TokenType.RIGHT_PAREN);
        
        // 解析可选的存储格式
        String storageFormat = "ROW"; // 默认行式存储
        if (currentToken().getType() == TokenType.STORAGE) {
            nextToken(); // 跳过 STORAGE

            if (currentToken().getType() == TokenType.ROW ||
                currentToken().getType() == TokenType.ROW_STORAGE) {
                storageFormat = "ROW";
                nextToken();
            } else if (currentToken().getType() == TokenType.COLUMN ||
                      currentToken().getType() == TokenType.COLUMN_STORAGE) {
                storageFormat = "COLUMN";
                nextToken();
            } else {
                throw new SyntaxException(
                    "无效的存储格式，期望 ROW 或 COLUMN",
                    currentToken().getPosition(),
                    "ROW, COLUMN"
                );
            }
        }

        // 可选的分号
        if (currentToken().getType() == TokenType.SEMICOLON) {
            nextToken();
        }
        
        return new CreateTableStatement(tableName, columns, constraints, storageFormat, startPos);
    }
    
    /**
     * 判断是否为列定义
     */
    private boolean isColumnDefinition() {
        return currentToken().getType() == TokenType.IDENTIFIER;
    }
    
    /**
     * 解析列定义
     */
    private ColumnDefinition parseColumnDefinition() throws SyntaxException {
        Position startPos = currentToken().getPosition();
        
        // 列名
        String columnName = expectIdentifier();
        
        // 数据类型
        String dataType = expectDataType();
        Integer length = null;
        
        // 可选的长度参数
        if (currentToken().getType() == TokenType.LEFT_PAREN) {
            nextToken();
            length = Integer.parseInt(expectNumber());
            
            // 对于DECIMAL类型，可能有第二个参数（精度）
            if (currentToken().getType() == TokenType.COMMA) {
                nextToken();
                // 暂时忽略第二个参数，只使用第一个参数作为长度
                expectNumber();
            }
            
            expect(TokenType.RIGHT_PAREN);
        }
        
        // 约束列表
        List<Constraint> constraints = new ArrayList<>();
        while (isConstraint()) {
            constraints.add(parseColumnConstraint());
        }
        
        return new ColumnDefinition(columnName, dataType, length, constraints, startPos);
    }
    
    /**
     * 判断是否为约束
     */
    private boolean isConstraint() {
        TokenType type = currentToken().getType();
        return type == TokenType.PRIMARY || type == TokenType.PRIMARY_KEY ||
               type == TokenType.FOREIGN || type == TokenType.FOREIGN_KEY ||
               type == TokenType.UNIQUE || type == TokenType.NOT_NULL ||
               type == TokenType.DEFAULT || type == TokenType.AUTO_INCREMENT ||
               type == TokenType.CHECK;
    }
    
    /**
     * 解析列约束
     */
    private Constraint parseColumnConstraint() throws SyntaxException {
        Position startPos = currentToken().getPosition();
        TokenType type = currentToken().getType();
        
        switch (type) {
            case PRIMARY:
                nextToken();
                expect(TokenType.KEY);
                return new Constraint(Constraint.ConstraintType.PRIMARY_KEY, null, 
                                    List.of(), null, null, null, startPos);
            case PRIMARY_KEY:
                nextToken();
                return new Constraint(Constraint.ConstraintType.PRIMARY_KEY, null, 
                                    List.of(), null, null, null, startPos);
            case FOREIGN:
                nextToken();
                expect(TokenType.KEY);
                return new Constraint(Constraint.ConstraintType.FOREIGN_KEY, null, 
                                    List.of(), null, null, null, startPos);
            case FOREIGN_KEY:
                nextToken();
                return new Constraint(Constraint.ConstraintType.FOREIGN_KEY, null, 
                                    List.of(), null, null, null, startPos);
            case UNIQUE:
                nextToken();
                return new Constraint(Constraint.ConstraintType.UNIQUE, null, 
                                    List.of(), null, null, null, startPos);
            case NOT_NULL:
                nextToken();
                return new Constraint(Constraint.ConstraintType.NOT_NULL, null, 
                                    List.of(), null, null, null, startPos);
            case DEFAULT:
                nextToken();
                String defaultValue = expectLiteral();
                return new Constraint(Constraint.ConstraintType.DEFAULT, null, 
                                    List.of(), null, null, defaultValue, startPos);
            case AUTO_INCREMENT:
                nextToken();
                return new Constraint(Constraint.ConstraintType.AUTO_INCREMENT, null, 
                                    List.of(), null, null, null, startPos);
            case CHECK:
                nextToken();
                expect(TokenType.LEFT_PAREN);
                Expression checkCondition = parseExpression();
                expect(TokenType.RIGHT_PAREN);
                // CHECK约束需要保存条件表达式作为defaultValue参数
                return new Constraint(Constraint.ConstraintType.CHECK, null, 
                                    List.of(), null, null, checkCondition.toString(), startPos);
            default:
                throw new SyntaxException("未知的约束类型", currentToken().getPosition());
        }
    }
    
    /**
     * 解析表级约束
     */
    private Constraint parseConstraint() throws SyntaxException {
        Position startPos = currentToken().getPosition();
        TokenType type = currentToken().getType();
        
        switch (type) {
            case PRIMARY:
                nextToken();
                expect(TokenType.KEY);
                expect(TokenType.LEFT_PAREN);
                List<String> columns = parseColumnList();
                expect(TokenType.RIGHT_PAREN);
                return new Constraint(Constraint.ConstraintType.PRIMARY_KEY, null, 
                                    columns, null, null, null, startPos);
            case PRIMARY_KEY:
                nextToken();
                expect(TokenType.LEFT_PAREN);
                List<String> columns2 = parseColumnList();
                expect(TokenType.RIGHT_PAREN);
                return new Constraint(Constraint.ConstraintType.PRIMARY_KEY, null, 
                                    columns2, null, null, null, startPos);
            case FOREIGN:
                nextToken();
                expect(TokenType.KEY);
                expect(TokenType.LEFT_PAREN);
                List<String> fkColumns = parseColumnList();
                expect(TokenType.RIGHT_PAREN);
                expect(TokenType.REFERENCES);
                String refTable = expectIdentifier();
                expect(TokenType.LEFT_PAREN);
                List<String> refColumns = parseColumnList();
                expect(TokenType.RIGHT_PAREN);
                return new Constraint(Constraint.ConstraintType.FOREIGN_KEY, null, 
                                    fkColumns, refTable, refColumns, null, startPos);
            case FOREIGN_KEY:
                nextToken();
                expect(TokenType.LEFT_PAREN);
                List<String> fkColumns2 = parseColumnList();
                expect(TokenType.RIGHT_PAREN);
                expect(TokenType.REFERENCES);
                String refTable2 = expectIdentifier();
                expect(TokenType.LEFT_PAREN);
                List<String> refColumns2 = parseColumnList();
                expect(TokenType.RIGHT_PAREN);
                return new Constraint(Constraint.ConstraintType.FOREIGN_KEY, null, 
                                    fkColumns2, refTable2, refColumns2, null, startPos);
            case UNIQUE:
                nextToken();
                expect(TokenType.LEFT_PAREN);
                List<String> uniqueColumns = parseColumnList();
                expect(TokenType.RIGHT_PAREN);
                return new Constraint(Constraint.ConstraintType.UNIQUE, null, 
                                    uniqueColumns, null, null, null, startPos);
            default:
                throw new SyntaxException("未知的约束类型", currentToken().getPosition());
        }
    }
    
    /**
     * 解析列名列表
     */
    private List<String> parseColumnList() throws SyntaxException {
        List<String> columns = new ArrayList<>();
        
        columns.add(expectIdentifier());
        
        while (currentToken().getType() == TokenType.COMMA) {
            nextToken();
            columns.add(expectIdentifier());
        }
        
        return columns;
    }
    
    /**
     * 解析INSERT语句
     */
    private InsertStatement parseInsertStatement() throws SyntaxException {
        Position startPos = currentToken().getPosition();
        
        // INSERT INTO
        expect(TokenType.INSERT);
        expect(TokenType.INTO);
        
        // 表名
        String tableName = expectIdentifier();
        
        // 列名列表（可选）
        List<String> columns = new ArrayList<>();
        if (currentToken().getType() == TokenType.LEFT_PAREN) {
            nextToken();
            columns = parseColumnList();
            expect(TokenType.RIGHT_PAREN);
        }
        
        // VALUES
        expect(TokenType.VALUES);
        
        // 值列表
        List<List<Expression>> values = new ArrayList<>();
        expect(TokenType.LEFT_PAREN);
        values.add(parseValueList());
        expect(TokenType.RIGHT_PAREN);
        
        // 更多值（可选）
        while (currentToken().getType() == TokenType.COMMA) {
            nextToken();
            expect(TokenType.LEFT_PAREN);
            values.add(parseValueList());
            expect(TokenType.RIGHT_PAREN);
        }
        
        // 可选的分号
        if (currentToken().getType() == TokenType.SEMICOLON) {
            nextToken();
        }
        
        return new InsertStatement(tableName, columns, values, startPos);
    }
    
    /**
     * 解析值列表
     */
    private List<Expression> parseValueList() throws SyntaxException {
        List<Expression> values = new ArrayList<>();
        
        values.add(parseExpression());
        
        while (currentToken().getType() == TokenType.COMMA) {
            nextToken();
            values.add(parseExpression());
        }
        
        return values;
    }
    
    /**
     * 解析SELECT语句
     */
    private SelectStatement parseSelectStatement() throws SyntaxException {
        Position startPos = currentToken().getPosition();
        
        // SELECT
        expect(TokenType.SELECT);
        
        // DISTINCT（可选）
        boolean distinct = false;
        if (currentToken().getType() == TokenType.DISTINCT) {
            nextToken();
            distinct = true;
        }
        
        // 选择列表
        List<Expression> selectList = parseSelectList();
        
        // FROM子句
        List<TableReference> fromClause = null;
        if (currentToken().getType() == TokenType.FROM) {
            nextToken();
            fromClause = parseFromClause();
        }
        
        // WHERE子句
        WhereClause whereClause = null;
        if (currentToken().getType() == TokenType.WHERE) {
            whereClause = parseWhereClause();
        }
        
        // GROUP BY子句
        GroupByClause groupByClause = null;
        if (currentToken().getType() == TokenType.GROUP) {
            groupByClause = parseGroupByClause();
        }
        
        // HAVING子句
        HavingClause havingClause = null;
        if (currentToken().getType() == TokenType.HAVING) {
            havingClause = parseHavingClause();
        }
        
        // ORDER BY子句
        OrderByClause orderByClause = null;
        if (currentToken().getType() == TokenType.ORDER) {
            orderByClause = parseOrderByClause();
        }
        
        // LIMIT子句
        LimitClause limitClause = null;
        if (currentToken().getType() == TokenType.LIMIT) {
            limitClause = parseLimitClause();
        }
        
        // 可选的分号
        if (currentToken().getType() == TokenType.SEMICOLON) {
            nextToken();
        }
        
        return new SelectStatement(distinct, selectList, fromClause, whereClause,
                                 groupByClause, havingClause, orderByClause, limitClause, startPos);
    }
    
    /**
     * 解析选择列表
     */
    private List<Expression> parseSelectList() throws SyntaxException {
        List<Expression> selectList = new ArrayList<>();
        
        selectList.add(parseSelectItem());
        
        while (currentToken().getType() == TokenType.COMMA) {
            nextToken();
            selectList.add(parseSelectItem());
        }
        
        return selectList;
    }
    
    /**
     * 解析选择项（支持AS别名）
     */
    private Expression parseSelectItem() throws SyntaxException {
        Expression expr;
        Position startPos = currentToken().getPosition();

        // 检查是否为 * 通配符
        if (currentToken().getType() == TokenType.MULTIPLY) {
            expr = new IdentifierExpression("*", currentToken().getPosition());
            nextToken();
        } else {
            expr = parseExpression();
        }
        
        // 检查是否有AS别名
        if (currentToken().getType() == TokenType.AS) {
            nextToken();
            String alias = expectIdentifier();
            // 创建带别名的表达式
            return new AliasExpression(expr, alias, startPos);
        }
        // 注意：我们不处理没有AS关键字的别名，因为这可能与FROM子句冲突
        
        return expr;
    }
    
    /**
     * 解析FROM子句
     */
    private List<TableReference> parseFromClause() throws SyntaxException {
        List<TableReference> tables = new ArrayList<>();
        
        tables.add(parseTableReference());
        
        while (currentToken().getType() == TokenType.COMMA) {
            nextToken();
            tables.add(parseTableReference());
        }
        
        return tables;
    }
    
    /**
     * 解析表引用
     */
    private TableReference parseTableReference() throws SyntaxException {
        Position startPos = currentToken().getPosition();
        
        String tableName = expectIdentifier();
        String alias = null;
        
        // 别名（可选）
        if (currentToken().getType() == TokenType.AS) {
            nextToken();
            alias = expectIdentifier();
        } else if (currentToken().getType() == TokenType.IDENTIFIER) {
            // 没有AS关键字的别名
            alias = expectIdentifier();
        }
        
        // JOIN子句（可选）
        List<JoinClause> joins = new ArrayList<>();
        while (isJoinClause()) {
            joins.add(parseJoinClause());
        }
        
        return new TableReference(tableName, alias, joins, startPos);
    }
    
    /**
     * 判断是否为JOIN子句
     */
    private boolean isJoinClause() {
        TokenType type = currentToken().getType();
        return type == TokenType.JOIN || type == TokenType.INNER || 
               type == TokenType.LEFT || type == TokenType.RIGHT;
    }
    
    /**
     * 解析JOIN子句
     */
    private JoinClause parseJoinClause() throws SyntaxException {
        Position startPos = currentToken().getPosition();
        
        JoinClause.JoinType joinType = JoinClause.JoinType.INNER;
        
        if (currentToken().getType() == TokenType.INNER) {
            nextToken();
        } else if (currentToken().getType() == TokenType.LEFT) {
            nextToken();
            joinType = JoinClause.JoinType.LEFT;
        } else if (currentToken().getType() == TokenType.RIGHT) {
            nextToken();
            joinType = JoinClause.JoinType.RIGHT;
        }
        
        expect(TokenType.JOIN);
        
        String tableName = null;
        SelectStatement subquery = null;
        String alias = null;
        
        // 检查是否为子查询
        if (currentToken().getType() == TokenType.LEFT_PAREN) {
            nextToken(); // 消费 '('
            subquery = parseSubquery();
            expect(TokenType.RIGHT_PAREN); // 消费 ')'
        } else {
            tableName = expectIdentifier();
        }
        
        // 别名（可选）
        if (currentToken().getType() == TokenType.AS) {
            nextToken();
            alias = expectIdentifier();
        } else if (currentToken().getType() == TokenType.IDENTIFIER) {
            alias = expectIdentifier();
        }
        
        expect(TokenType.ON);
        Expression condition = parseExpression();
        
        if (subquery != null) {
            return new JoinClause(joinType, subquery, alias, condition, startPos);
        } else {
            return new JoinClause(joinType, tableName, alias, condition, startPos);
        }
    }
    
    /**
     * 解析WHERE子句
     */
    private WhereClause parseWhereClause() throws SyntaxException {
        Position startPos = currentToken().getPosition();
        
        expect(TokenType.WHERE);
        Expression condition = parseExpression();
        
        return new WhereClause(condition, startPos);
    }
    
    /**
     * 解析GROUP BY子句
     */
    private GroupByClause parseGroupByClause() throws SyntaxException {
        Position startPos = currentToken().getPosition();
        
        expect(TokenType.GROUP);
        expect(TokenType.BY);
        
        List<Expression> expressions = new ArrayList<>();
        expressions.add(parseExpression());
        
        while (currentToken().getType() == TokenType.COMMA) {
            nextToken();
            expressions.add(parseExpression());
        }
        
        return new GroupByClause(expressions, startPos);
    }
    
    /**
     * 解析HAVING子句
     */
    private HavingClause parseHavingClause() throws SyntaxException {
        Position startPos = currentToken().getPosition();
        
        expect(TokenType.HAVING);
        Expression condition = parseExpression();
        
        return new HavingClause(condition, startPos);
    }
    
    /**
     * 解析ORDER BY子句
     */
    private OrderByClause parseOrderByClause() throws SyntaxException {
        Position startPos = currentToken().getPosition();
        
        expect(TokenType.ORDER);
        expect(TokenType.BY);
        
        List<OrderByClause.OrderByItem> items = new ArrayList<>();
        
        Expression expr = parseExpression();
        OrderByClause.SortOrder order = OrderByClause.SortOrder.ASC;
        
        if (currentToken().getType() == TokenType.ASC) {
            nextToken();
        } else if (currentToken().getType() == TokenType.DESC) {
            nextToken();
            order = OrderByClause.SortOrder.DESC;
        }
        
        items.add(new OrderByClause.OrderByItem(expr, order));
        
        while (currentToken().getType() == TokenType.COMMA) {
            nextToken();
            expr = parseExpression();
            order = OrderByClause.SortOrder.ASC;
            
            if (currentToken().getType() == TokenType.ASC) {
                nextToken();
            } else if (currentToken().getType() == TokenType.DESC) {
                nextToken();
                order = OrderByClause.SortOrder.DESC;
            }
            
            items.add(new OrderByClause.OrderByItem(expr, order));
        }
        
        return new OrderByClause(items, startPos);
    }
    
    /**
     * 解析LIMIT子句
     */
    private LimitClause parseLimitClause() throws SyntaxException {
        Position startPos = currentToken().getPosition();
        
        expect(TokenType.LIMIT);
        Expression limit = parseExpression();
        Expression offset = null;
        
        if (currentToken().getType() == TokenType.OFFSET) {
            nextToken();
            offset = parseExpression();
        }
        
        return new LimitClause(limit, offset, startPos);
    }
    
    /**
     * 解析UPDATE语句
     */
    private UpdateStatement parseUpdateStatement() throws SyntaxException {
        Position startPos = currentToken().getPosition();
        
        // UPDATE
        expect(TokenType.UPDATE);
        
        // 表名
        String tableName = expectIdentifier();
        
        // SET子句
        expect(TokenType.SET);
        Map<String, Expression> setClause = parseSetClause();
        
        // WHERE子句（可选）
        WhereClause whereClause = null;
        if (currentToken().getType() == TokenType.WHERE) {
            whereClause = parseWhereClause();
        }
        
        // 可选的分号
        if (currentToken().getType() == TokenType.SEMICOLON) {
            nextToken();
        }
        
        return new UpdateStatement(tableName, setClause, whereClause, startPos);
    }
    
    /**
     * 解析SET子句
     */
    private Map<String, Expression> parseSetClause() throws SyntaxException {
        Map<String, Expression> setClause = new java.util.HashMap<>();
        
        // 解析第一个赋值
        String column = expectIdentifier();
        expect(TokenType.EQUALS);
        Expression value = parseExpression();
        setClause.put(column, value);
        
        // 解析更多赋值
        while (currentToken().getType() == TokenType.COMMA) {
            nextToken();
            column = expectIdentifier();
            expect(TokenType.EQUALS);
            value = parseExpression();
            setClause.put(column, value);
        }
        
        return setClause;
    }
    
    /**
     * 解析DELETE语句
     */
    private DeleteStatement parseDeleteStatement() throws SyntaxException {
        Position startPos = currentToken().getPosition();
        
        // DELETE FROM
        expect(TokenType.DELETE);
        expect(TokenType.FROM);
        
        // 表名
        String tableName = expectIdentifier();
        
        // WHERE子句
        WhereClause whereClause = null;
        if (currentToken().getType() == TokenType.WHERE) {
            whereClause = parseWhereClause();
        }
        
        // 可选的分号
        if (currentToken().getType() == TokenType.SEMICOLON) {
            nextToken();
        }
        
        return new DeleteStatement(tableName, whereClause, startPos);
    }
    
    /**
     * 解析DROP TABLE语句
     */
    private DropTableStatement parseDropTableStatement() throws SyntaxException {
        Position startPos = currentToken().getPosition();

        // TABLE (DROP已经在parseDropStatement中消费了)
        expect(TokenType.TABLE);

        // 可选的IF EXISTS
        boolean ifExists = false;
        if (currentToken().getType() == TokenType.IF) {
            nextToken();
            expect(TokenType.EXISTS);
            ifExists = true;
        }

        // 表名
        String tableName = expectIdentifier();

        // 可选的分号
        if (currentToken().getType() == TokenType.SEMICOLON) {
            nextToken();
        }

        return new DropTableStatement(tableName, ifExists, startPos);
    }

    /**
     * 解析表达式
     */
    private Expression parseExpression() throws SyntaxException {
        return parseOrExpression();
    }
    
    /**
     * 解析OR表达式
     */
    private Expression parseOrExpression() throws SyntaxException {
        Expression left = parseAndExpression();
        
        while (currentToken().getType() == TokenType.OR) {
            Position pos = currentToken().getPosition();
            nextToken();
            Expression right = parseAndExpression();
            left = new BinaryExpression(left, TokenType.OR, right, pos);
        }
        
        return left;
    }
    
    /**
     * 解析AND表达式
     */
    private Expression parseAndExpression() throws SyntaxException {
        Expression left = parseEqualityExpression();
        
        while (currentToken().getType() == TokenType.AND) {
            Position pos = currentToken().getPosition();
            nextToken();
            Expression right = parseEqualityExpression();
            left = new BinaryExpression(left, TokenType.AND, right, pos);
        }
        
        return left;
    }
    
    /**
     * 解析相等性表达式
     */
    private Expression parseEqualityExpression() throws SyntaxException {
        Expression left = parseRelationalExpression();
        
        while (currentToken().getType() == TokenType.EQUALS || 
               currentToken().getType() == TokenType.NOT_EQUALS) {
            Position pos = currentToken().getPosition();
            TokenType operator = currentToken().getType();
            nextToken();
            Expression right = parseRelationalExpression();
            left = new BinaryExpression(left, operator, right, pos);
        }
        
        return left;
    }
    
    /**
     * 解析关系表达式
     */
    private Expression parseRelationalExpression() throws SyntaxException {
        Expression left = parseAdditiveExpression();
        
        while (currentToken().getType() == TokenType.LESS_THAN ||
               currentToken().getType() == TokenType.GREATER_THAN ||
               currentToken().getType() == TokenType.LESS_EQUAL ||
               currentToken().getType() == TokenType.GREATER_EQUAL ||
               currentToken().getType() == TokenType.IN ||
               currentToken().getType() == TokenType.LIKE ||
               currentToken().getType() == TokenType.BETWEEN ||
               currentToken().getType() == TokenType.IS) {
            Position pos = currentToken().getPosition();
            TokenType operator = currentToken().getType();
            nextToken();
            
            if (operator == TokenType.IN) {
                // 处理IN子查询
                left = parseInExpression(left, pos);
            } else if (operator == TokenType.LIKE) {
                // 处理LIKE操作符
                Expression right = parseAdditiveExpression();
                left = new BinaryExpression(left, TokenType.LIKE, right, pos);
            } else if (operator == TokenType.BETWEEN) {
                // 处理BETWEEN操作符
                left = parseBetweenExpression(left, pos);
            } else if (operator == TokenType.IS) {
                // 处理IS NULL / IS NOT NULL
                left = parseIsNullExpression(left, pos);
            } else {
                Expression right = parseAdditiveExpression();
                left = new BinaryExpression(left, operator, right, pos);
            }
        }
        
        return left;
    }
    
    /**
     * 解析IN表达式
     */
    private Expression parseInExpression(Expression left, Position pos) throws SyntaxException {
        expect(TokenType.LEFT_PAREN);
        
        // 检查是否为子查询（以SELECT开头）
        if (currentToken().getType() == TokenType.SELECT) {
            // 解析子查询
            SelectStatement subquery = parseSubquery();
            expect(TokenType.RIGHT_PAREN);
            return new InExpression(left, subquery, pos);
        } else {
            // 解析值列表
            List<Expression> values = new ArrayList<>();
            values.add(parseExpression());
            
            while (currentToken().getType() == TokenType.COMMA) {
                nextToken();
                values.add(parseExpression());
            }
            
            expect(TokenType.RIGHT_PAREN);
            return new InExpression(left, values, pos);
        }
    }
    
    /**
     * 解析BETWEEN表达式
     */
    private Expression parseBetweenExpression(Expression left, Position pos) throws SyntaxException {
        // 解析下界
        Expression lowerBound = parseAdditiveExpression();
        
        // 期望AND关键字
        expect(TokenType.AND);
        
        // 解析上界
        Expression upperBound = parseAdditiveExpression();
        
        // 创建BETWEEN表达式
        return new BetweenExpression(left, lowerBound, upperBound, pos);
    }
    
    /**
     * 解析IS NULL / IS NOT NULL表达式
     */
    private Expression parseIsNullExpression(Expression left, Position pos) throws SyntaxException {
        if (currentToken().getType() == TokenType.NOT) {
            nextToken();
            expect(TokenType.NULL);
            return new IsNullExpression(left, true, pos); // IS NOT NULL
        } else if (currentToken().getType() == TokenType.NOT_NULL) {
            nextToken(); // 消费NOT_NULL复合Token
            return new IsNullExpression(left, true, pos); // IS NOT NULL
        } else {
            expect(TokenType.NULL);
            return new IsNullExpression(left, false, pos); // IS NULL
        }
    }
    
    /**
     * 解析子查询（不包含分号）
     */
    private SelectStatement parseSubquery() throws SyntaxException {
        Position startPos = currentToken().getPosition();
        
        // SELECT
        expect(TokenType.SELECT);
        
        // DISTINCT（可选）
        boolean distinct = false;
        if (currentToken().getType() == TokenType.DISTINCT) {
            nextToken();
            distinct = true;
        }
        
        // 选择列表
        List<Expression> selectList = parseSelectList();
        
        // FROM子句
        List<TableReference> fromClause = null;
        if (currentToken().getType() == TokenType.FROM) {
            nextToken();
            fromClause = parseFromClause();
        }
        
        // WHERE子句
        WhereClause whereClause = null;
        if (currentToken().getType() == TokenType.WHERE) {
            whereClause = parseWhereClause();
        }
        
        // GROUP BY子句
        GroupByClause groupByClause = null;
        if (currentToken().getType() == TokenType.GROUP) {
            groupByClause = parseGroupByClause();
        }
        
        // HAVING子句
        HavingClause havingClause = null;
        if (currentToken().getType() == TokenType.HAVING) {
            havingClause = parseHavingClause();
        }
        
        // ORDER BY子句
        OrderByClause orderByClause = null;
        if (currentToken().getType() == TokenType.ORDER) {
            orderByClause = parseOrderByClause();
        }
        
        // LIMIT子句
        LimitClause limitClause = null;
        if (currentToken().getType() == TokenType.LIMIT) {
            limitClause = parseLimitClause();
        }
        
        // 注意：子查询不包含分号
        
        return new SelectStatement(distinct, selectList, fromClause, whereClause,
                                 groupByClause, havingClause, orderByClause, limitClause, startPos);
    }
    
    /**
     * 解析加法表达式
     */
    private Expression parseAdditiveExpression() throws SyntaxException {
        Expression left = parseMultiplicativeExpression();
        
        while (currentToken().getType() == TokenType.PLUS ||
               currentToken().getType() == TokenType.MINUS) {
            Position pos = currentToken().getPosition();
            TokenType operator = currentToken().getType();
            nextToken();
            Expression right = parseMultiplicativeExpression();
            left = new BinaryExpression(left, operator, right, pos);
        }
        
        return left;
    }
    
    /**
     * 解析乘法表达式
     */
    private Expression parseMultiplicativeExpression() throws SyntaxException {
        Expression left = parseUnaryExpression();
        
        while (currentToken().getType() == TokenType.MULTIPLY ||
               currentToken().getType() == TokenType.DIVIDE ||
               currentToken().getType() == TokenType.MODULO) {
            Position pos = currentToken().getPosition();
            TokenType operator = currentToken().getType();
            nextToken();
            Expression right = parseUnaryExpression();
            left = new BinaryExpression(left, operator, right, pos);
        }
        
        return left;
    }
    
    /**
     * 解析一元表达式
     */
    private Expression parseUnaryExpression() throws SyntaxException {
        if (currentToken().getType() == TokenType.MINUS ||
            currentToken().getType() == TokenType.NOT) {
            Position pos = currentToken().getPosition();
            TokenType operator = currentToken().getType();
            nextToken();
            Expression operand = parseUnaryExpression();
            return new UnaryExpression(operator, operand, pos);
        }
        
        return parsePrimaryExpression();
    }
    
    /**
     * 解析主表达式
     */
    private Expression parsePrimaryExpression() throws SyntaxException {
        Token token = currentToken();
        
        switch (token.getType()) {
            case IDENTIFIER:
                return parseIdentifierExpression();
            case STRING_LITERAL:
            case NUMBER_LITERAL:
            case BOOLEAN_LITERAL:
                return parseLiteralExpression();
            case LEFT_PAREN:
                return parseParenthesizedExpression();
            // 聚合函数
            case COUNT:
            case SUM:
            case AVG:
            case MAX:
            case MIN:
            // 数学函数
            case ABS:
            case CEIL:
            case FLOOR:
            case ROUND:
            case SQRT:
            case POWER:
            case MOD:
            case RAND:
            // 字符串函数
            case UPPER:
            case LOWER:
            case LENGTH:
            case SUBSTRING:
            case CONCAT:
            case TRIM:
            case LTRIM:
            case RTRIM:
            case REPLACE:
            // 日期函数
            case NOW:
            case CURRENT_DATE:
            case CURRENT_TIME:
            case CURRENT_TIMESTAMP:
            case YEAR:
            case MONTH:
            case DAY:
            case HOUR:
            case MINUTE:
            case SECOND:
            case DATE_ADD:
            case DATE_SUB:
            case DATEDIFF:
                return parseFunctionCallExpression();
            default:
                throw new SyntaxException("意外的token: " + token.getValue(), 
                                        token.getPosition(), "标识符、字面量、'('或聚合函数");
        }
    }
    
    /**
     * 解析函数调用表达式
     */
    private Expression parseFunctionCallExpression() throws SyntaxException {
        Position pos = currentToken().getPosition();
        String functionName = currentToken().getValue();
        nextToken(); // 消费函数名
        
        expect(TokenType.LEFT_PAREN);
        List<Expression> arguments = new ArrayList<>();
        
        if (currentToken().getType() != TokenType.RIGHT_PAREN) {
            // 特殊处理COUNT(*)
            if (currentToken().getType() == TokenType.MULTIPLY) {
                arguments.add(new IdentifierExpression("*", currentToken().getPosition()));
                nextToken();
            } else {
                arguments.add(parseExpression());
                while (currentToken().getType() == TokenType.COMMA) {
                    nextToken();
                    arguments.add(parseExpression());
                }
            }
        }
        
        expect(TokenType.RIGHT_PAREN);
        return new FunctionCallExpression(functionName, arguments, pos);
    }
    
    /**
     * 解析标识符表达式
     */
    private Expression parseIdentifierExpression() throws SyntaxException {
        Position pos = currentToken().getPosition();
        String name = expectIdentifier();
        
        // 检查是否为函数调用
        if (currentToken().getType() == TokenType.LEFT_PAREN) {
            nextToken();
            List<Expression> arguments = new ArrayList<>();
            
            if (currentToken().getType() != TokenType.RIGHT_PAREN) {
                arguments.add(parseExpression());
                while (currentToken().getType() == TokenType.COMMA) {
                    nextToken();
                    arguments.add(parseExpression());
                }
            }
            
            expect(TokenType.RIGHT_PAREN);
            return new FunctionCallExpression(name, arguments, pos);
        }
        
        // 检查是否为点号分隔的标识符（如 u.name）
        if (currentToken().getType() == TokenType.DOT) {
            nextToken();
            String fieldName = expectIdentifier();
            return new DotExpression(name, fieldName, pos);
        }
        
        return new IdentifierExpression(name, pos);
    }
    
    /**
     * 解析字面量表达式
     */
    private Expression parseLiteralExpression() throws SyntaxException {
        Token token = currentToken();
        nextToken();
        return new LiteralExpression(token.getType(), token.getValue(), token.getPosition());
    }
    
    /**
     * 解析括号表达式
     */
    private Expression parseParenthesizedExpression() throws SyntaxException {
        expect(TokenType.LEFT_PAREN);
        
        // 检查是否为子查询（以SELECT开头）
        if (currentToken().getType() == TokenType.SELECT) {
            // 解析子查询
            SelectStatement subquery = parseSubquery();
            expect(TokenType.RIGHT_PAREN);
            return new SubqueryExpression(subquery, currentToken().getPosition());
        } else {
            // 解析普通表达式
            Expression expr = parseExpression();
            expect(TokenType.RIGHT_PAREN);
            return expr;
        }
    }
    
    /**
     * 获取当前token
     */
    private Token currentToken() {
        if (currentTokenIndex >= tokens.size()) {
            return new Token(TokenType.EOF, "", new Position(1, 1));
        }
        return tokens.get(currentTokenIndex);
    }
    
    /**
     * 移动到下一个token
     */
    private void nextToken() {
        if (currentTokenIndex < tokens.size()) {
            currentTokenIndex++;
        }
    }
    
    /**
     * 期望特定类型的token
     */
    private void expect(TokenType expectedType) throws SyntaxException {
        Token token = currentToken();
        if (token.getType() != expectedType) {
            throw new SyntaxException("期望 " + expectedType.getValue() + "，但得到 " + token.getValue(),
                                    token.getPosition(), expectedType.getValue());
        }
        nextToken();
    }
    
    /**
     * 期望标识符
     */
    private String expectIdentifier() throws SyntaxException {
        Token token = currentToken();
        // 允许某些关键字作为标识符使用（如别名）
        if (token.getType() != TokenType.IDENTIFIER && 
            !isKeywordAsIdentifier(token.getType())) {
            throw new SyntaxException("期望标识符，但得到 " + token.getValue(),
                                    token.getPosition(), "标识符");
        }
        nextToken();
        return token.getValue();
    }
    
    /**
     * 判断关键字是否可以作为标识符使用
     */
    private boolean isKeywordAsIdentifier(TokenType type) {
        // 聚合函数关键字在某些上下文中可以作为标识符（如别名）
        return type == TokenType.COUNT || type == TokenType.SUM || 
               type == TokenType.AVG || type == TokenType.MAX || 
               type == TokenType.MIN ||
               // 数学函数
               type == TokenType.ABS || type == TokenType.CEIL ||
               type == TokenType.FLOOR || type == TokenType.ROUND ||
               type == TokenType.SQRT || type == TokenType.POWER ||
               type == TokenType.MOD || type == TokenType.RAND ||
               // 字符串函数
               type == TokenType.UPPER || type == TokenType.LOWER ||
               type == TokenType.LENGTH || type == TokenType.SUBSTRING ||
               type == TokenType.CONCAT || type == TokenType.TRIM ||
               type == TokenType.LTRIM || type == TokenType.RTRIM ||
               type == TokenType.REPLACE ||
               // 日期函数
               type == TokenType.NOW || type == TokenType.CURRENT_DATE ||
               type == TokenType.CURRENT_TIME || type == TokenType.CURRENT_TIMESTAMP ||
               type == TokenType.YEAR || type == TokenType.MONTH ||
               type == TokenType.DAY || type == TokenType.HOUR ||
               type == TokenType.MINUTE || type == TokenType.SECOND ||
               type == TokenType.DATE_ADD || type == TokenType.DATE_SUB ||
               type == TokenType.DATEDIFF;
    }
    
    /**
     * 期望数据类型
     */
    private String expectDataType() throws SyntaxException {
        Token token = currentToken();
        if (!token.getType().isKeyword() || !isDataType(token.getType())) {
            throw new SyntaxException("期望数据类型，但得到 " + token.getValue(),
                                    token.getPosition(), "数据类型");
        }
        nextToken();
        return token.getValue();
    }
    
    /**
     * 判断是否为数据类型
     */
    private boolean isDataType(TokenType type) {
        return type == TokenType.INT || type == TokenType.INTEGER ||
               type == TokenType.VARCHAR || type == TokenType.CHAR ||
               type == TokenType.TEXT || type == TokenType.DECIMAL ||
               type == TokenType.FLOAT || type == TokenType.DOUBLE ||
               type == TokenType.BOOLEAN || type == TokenType.DATE ||
               type == TokenType.TIME || type == TokenType.TIMESTAMP;
    }
    
    /**
     * 期望数字
     */
    private String expectNumber() throws SyntaxException {
        Token token = currentToken();
        if (token.getType() != TokenType.NUMBER_LITERAL) {
            throw new SyntaxException("期望数字，但得到 " + token.getValue(),
                                    token.getPosition(), "数字");
        }
        nextToken();
        return token.getValue();
    }
    
    /**
     * 期望字面量
     */
    private String expectLiteral() throws SyntaxException {
        Token token = currentToken();
        if (!token.getType().isLiteral()) {
            throw new SyntaxException("期望字面量，但得到 " + token.getValue(),
                                    token.getPosition(), "字面量");
        }
        nextToken();
        return token.getValue();
    }

    /**
     * 解析CREATE FUNCTION语句
     */
    private CreateFunctionStatement parseCreateFunctionStatement(boolean isPermanent) throws SyntaxException {
        Position startPos = currentToken().getPosition();

        // FUNCTION
        expect(TokenType.FUNCTION);

        // 可选的OR REPLACE
        boolean orReplace = false;
        if (currentToken().getType() == TokenType.OR) {
            nextToken(); // OR
            expect(TokenType.REPLACE);
            orReplace = true;
        }

        // 函数名
        String functionName = expectIdentifier();

        // 参数列表
        expect(TokenType.LEFT_PAREN);
        List<CreateFunctionStatement.FunctionParameter> parameters = new ArrayList<>();

        if (currentToken().getType() != TokenType.RIGHT_PAREN) {
            do {
                String paramName = expectIdentifier();
                String paramType = expectDataType();
                parameters.add(new CreateFunctionStatement.FunctionParameter(paramName, paramType));

                if (currentToken().getType() == TokenType.COMMA) {
                    nextToken();
                } else {
                    break;
                }
            } while (true);
        }

        expect(TokenType.RIGHT_PAREN);

        // RETURNS
        expect(TokenType.RETURNS);
        String returnType = expectDataType();

        // BEGIN
        expect(TokenType.BEGIN);

        // 函数体 - 增强实现，支持复杂控制结构
        String functionBody = parseComplexFunctionBody();

        return new CreateFunctionStatement(functionName, parameters, returnType, functionBody, orReplace, isPermanent, startPos);
    }

    /**
     * 解析DROP FUNCTION语句
     */
    private DropFunctionStatement parseDropFunctionStatement() throws SyntaxException {
        Position startPos = currentToken().getPosition();

        // FUNCTION
        expect(TokenType.FUNCTION);

        // 可选的IF EXISTS
        boolean ifExists = false;
        if (currentToken().getType() == TokenType.IF) {
            nextToken(); // IF
            expect(TokenType.EXISTS);
            ifExists = true;
        }

        // 函数名
        String functionName = expectIdentifier();

        return new DropFunctionStatement(functionName, ifExists, startPos);
    }

    /**
     * 解析CALL语句
     */
    private CallStatement parseCallStatement() throws SyntaxException {
        Position startPos = currentToken().getPosition();

        // CALL
        expect(TokenType.CALL);

        // 函数名
        String functionName = expectIdentifier();

        // 参数列表
        expect(TokenType.LEFT_PAREN);
        List<Expression> arguments = new ArrayList<>();

        if (currentToken().getType() != TokenType.RIGHT_PAREN) {
            do {
                arguments.add(parseExpression());

                if (currentToken().getType() == TokenType.COMMA) {
                    nextToken();
                } else {
                    break;
                }
            } while (true);
        }

        expect(TokenType.RIGHT_PAREN);

        return new CallStatement(functionName, arguments, startPos);
    }

    /**
     * 解析复杂的函数体 - 支持控制结构
     */
    private String parseComplexFunctionBody() throws SyntaxException {
        StringBuilder bodyBuilder = new StringBuilder();
        int nestingLevel = 0;

        while (currentToken().getType() != TokenType.EOF) {
            TokenType currentType = currentToken().getType();
            String currentValue = currentToken().getValue();

            // 处理嵌套结构
            if (currentType == TokenType.BEGIN ||
                currentType == TokenType.IF ||
                currentType == TokenType.WHILE ||
                currentType == TokenType.CASE) {
                nestingLevel++;
            } else if (currentType == TokenType.END) {
                if (nestingLevel == 0) {
                    // 到达函数体结束
                    expect(TokenType.END);
                    break;
                } else {
                    nestingLevel--;
                }
            } else if (currentType == TokenType.ENDIF ||
                       currentType == TokenType.ENDLOOP) {
                nestingLevel--;
            }

            bodyBuilder.append(currentValue).append(" ");
            nextToken();
        }

        return bodyBuilder.toString().trim();
    }
    
    /**
     * 解析CREATE SHARD语句
     * 格式: CREATE SHARD table_name BY shard_key_column USING strategy (shard_count)
     */
    private CreateShardStatement parseCreateShardStatement() throws SyntaxException {
        Position startPos = currentToken().getPosition();

        // SHARD
        expect(TokenType.SHARD);

        // 表名
        String tableName = expectIdentifier();

        // BY
        expect(TokenType.BY);

        // 分片键列名
        String shardKeyColumn = expectIdentifier();

        // USING
        expect(TokenType.USING);

        // 分片策略 (HASH, RANGE)
        Token strategyToken = currentToken();
        String strategy;
        if (strategyToken.getType() == TokenType.HASH) {
            strategy = "HASH";
            nextToken(); // 消费HASH
        } else if (strategyToken.getType() == TokenType.RANGE) {
            strategy = "RANGE";
            nextToken(); // 消费RANGE
        } else {
            throw new SyntaxException(
                String.format("USING后面应该是HASH或RANGE，而不是 '%s'", strategyToken.getValue()),
                strategyToken.getPosition(),
                "HASH 或 RANGE"
            );
        }

        // (
        expect(TokenType.LEFT_PAREN);

        // 分片数量
        Token shardCountToken = currentToken();
        if (shardCountToken.getType() != TokenType.NUMBER_LITERAL) {
            throw new SyntaxException(
                "分片数量必须是整数",
                shardCountToken.getPosition(),
                "整数"
            );
        }
        int shardCount = Integer.parseInt(shardCountToken.getValue());
        nextToken(); // 消费数字

        // )
        expect(TokenType.RIGHT_PAREN);

        return new CreateShardStatement(tableName, shardKeyColumn, strategy, shardCount, startPos);
    }

    /**
     * 解析DROP SHARD语句
     * 格式: DROP SHARD table_name
     */
    private DropShardStatement parseDropShardStatement() throws SyntaxException {
        Position startPos = currentToken().getPosition();

        // SHARD
        expect(TokenType.SHARD);

        // 表名
        String tableName = expectIdentifier();

        // 可选的分号
        if (currentToken().getType() == TokenType.SEMICOLON) {
            nextToken();
        }

        return new DropShardStatement(tableName, startPos);
    }
}
