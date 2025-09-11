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
     * 解析语句
     */
    private Statement parseStatement() throws SyntaxException {
        Token token = currentToken();
        
        switch (token.getType()) {
            case CREATE:
                return parseCreateTableStatement();
            case INSERT:
                return parseInsertStatement();
            case SELECT:
                return parseSelectStatement();
            case UPDATE:
                return parseUpdateStatement();
            case DELETE:
                return parseDeleteStatement();
            default:
                throw new SyntaxException(
                    String.format("不支持的语句类型 '%s'", token.getValue()),
                    token.getPosition(),
                    "CREATE TABLE, INSERT INTO, SELECT, UPDATE, DELETE FROM"
                );
        }
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
        
        // 可选的分号
        if (currentToken().getType() == TokenType.SEMICOLON) {
            nextToken();
        }
        
        return new CreateTableStatement(tableName, columns, constraints, startPos);
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
                return new Constraint(Constraint.ConstraintType.CHECK, null, 
                                    List.of(), null, null, null, startPos);
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
            // 对于有别名的情况，我们暂时返回原表达式
            // 在实际实现中，可能需要创建一个带别名的表达式类型
            return expr;
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
        
        String tableName = expectIdentifier();
        String alias = null;
        
        // 别名（可选）
        if (currentToken().getType() == TokenType.AS) {
            nextToken();
            alias = expectIdentifier();
        } else if (currentToken().getType() == TokenType.IDENTIFIER) {
            alias = expectIdentifier();
        }
        
        expect(TokenType.ON);
        Expression condition = parseExpression();
        
        return new JoinClause(joinType, tableName, alias, condition, startPos);
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
               currentToken().getType() == TokenType.IN) {
            Position pos = currentToken().getPosition();
            TokenType operator = currentToken().getType();
            nextToken();
            
            if (operator == TokenType.IN) {
                // 处理IN子查询
                left = parseInExpression(left, pos);
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
               type == TokenType.MIN;
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
}
