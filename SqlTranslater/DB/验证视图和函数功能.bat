@echo off
chcp 65001 >nul
echo.
echo ========================================
echo   SparrowDB 视图和函数功能验证工具
echo ========================================
echo.

:MENU
echo 请选择验证方式:
echo.
echo 1. 启动GUI界面进行交互式验证
echo 2. 启动命令行应用进行验证  
echo 3. 运行自动化测试脚本
echo 4. 查看验证指南
echo 5. 退出
echo.
set /p choice=请输入选择 (1-5): 

if "%choice%"=="1" goto GUI_TEST
if "%choice%"=="2" goto CLI_TEST  
if "%choice%"=="3" goto AUTO_TEST
if "%choice%"=="4" goto SHOW_GUIDE
if "%choice%"=="5" goto EXIT
goto MENU

:GUI_TEST
echo.
echo 正在启动GUI界面...
echo.
echo 验证步骤：
echo 1. 在SQL输入框中输入测试SQL语句
echo 2. 点击"执行SQL"或按F9执行
echo 3. 观察执行结果、Token列表和AST可视化
echo 4. 参考"视图和函数验证指南.md"中的测试用例
echo.
pause
call run_gui.bat
goto MENU

:CLI_TEST
echo.
echo 正在编译并启动命令行应用...
echo.

REM 编译命令行应用
javac -encoding UTF-8 -cp "target/classes" -d target/classes src/main/java/com/database/SparrowDBApplication.java

if %errorlevel% neq 0 (
    echo 编译失败！请检查Java代码。
    pause
    goto MENU
)

echo 编译成功！
echo.
echo 验证提示：
echo - 输入 "help" 查看可用命令
echo - 输入 "examples" 查看示例查询
echo - 输入 "tables" 查看现有表
echo - 直接输入SQL语句进行测试
echo - 输入 "quit" 退出应用
echo.
pause
java -cp "target/classes" com.database.SparrowDBApplication
goto MENU

:AUTO_TEST
echo.
echo 正在运行自动化测试...
echo.

REM 编译测试类
javac -encoding UTF-8 -cp "target/classes" -d target/classes QuickFunctionTest.java

if %errorlevel% neq 0 (
    echo 创建快速测试类...
    call :CREATE_TEST_CLASS
    javac -encoding UTF-8 -cp "target/classes" -d target/classes QuickFunctionTest.java
)

if %errorlevel% neq 0 (
    echo 自动化测试编译失败！
    pause
    goto MENU
)

echo 运行测试...
java -cp "target/classes" QuickFunctionTest
echo.
echo 测试完成！
pause
goto MENU

:SHOW_GUIDE
echo.
echo 正在显示验证指南...
echo.
if exist "视图和函数验证指南.md" (
    type "视图和函数验证指南.md"
) else (
    echo 验证指南文件不存在！
)
echo.
pause
goto MENU

:CREATE_TEST_CLASS
echo 创建快速测试类...
(
echo import com.database.engine.DatabaseEngine;
echo import com.database.engine.ExecutionResult;
echo.
echo public class QuickFunctionTest {
echo     public static void main^(String[] args^) {
echo         System.out.println^("=== SparrowDB 快速功能验证 ==="^);
echo         
echo         try {
echo             DatabaseEngine engine = new DatabaseEngine^("test_db", "data"^);
echo             engine.initialize^(^);
echo             
echo             // 基础表创建测试
echo             System.out.println^("1. 测试基础表创建..."^);
echo             testBasicTables^(engine^);
echo             
echo             // 函数功能测试  
echo             System.out.println^("2. 测试函数功能..."^);
echo             testFunctions^(engine^);
echo             
echo             // 视图功能测试
echo             System.out.println^("3. 测试视图功能..."^);
echo             testViews^(engine^);
echo             
echo             engine.shutdown^(^);
echo             System.out.println^("验证完成！"^);
echo             
echo         } catch ^(Exception e^) {
echo             System.err.println^("验证过程中发生错误: " + e.getMessage^(^)^);
echo         }
echo     }
echo     
echo     private static void testBasicTables^(DatabaseEngine engine^) {
echo         String[] sqls = {
echo             "CREATE TABLE test_users ^(id INT PRIMARY KEY, name VARCHAR^(50^)^)",
echo             "INSERT INTO test_users VALUES ^(1, 'Alice'^)"
echo         };
echo         
echo         for ^(String sql : sqls^) {
echo             ExecutionResult result = engine.executeSQL^(sql^);
echo             System.out.println^("  " + ^(result.isSuccess^(^) ? "✓" : "✗"^) + " " + sql^);
echo         }
echo     }
echo     
echo     private static void testFunctions^(DatabaseEngine engine^) {
echo         String[] sqls = {
echo             "SELECT ABS^(-5^) AS abs_test FROM test_users",
echo             "SELECT UPPER^('hello'^) AS upper_test FROM test_users", 
echo             "SELECT LENGTH^('test'^) AS length_test FROM test_users"
echo         };
echo         
echo         for ^(String sql : sqls^) {
echo             ExecutionResult result = engine.executeSQL^(sql^);
echo             System.out.println^("  " + ^(result.isSuccess^(^) ? "✓" : "✗"^) + " " + sql^);
echo         }
echo     }
echo     
echo     private static void testViews^(DatabaseEngine engine^) {
echo         String[] sqls = {
echo             "CREATE VIEW test_view AS SELECT name FROM test_users",
echo             "DROP VIEW IF EXISTS test_view"
echo         };
echo         
echo         for ^(String sql : sqls^) {
echo             ExecutionResult result = engine.executeSQL^(sql^);
echo             System.out.println^("  " + ^(result.isSuccess^(^) ? "✓" : "✗"^) + " " + sql^);
echo         }
echo     }
echo }
) > QuickFunctionTest.java
goto :eof

:EXIT
echo.
echo 感谢使用SparrowDB验证工具！
echo.
exit /b 0
