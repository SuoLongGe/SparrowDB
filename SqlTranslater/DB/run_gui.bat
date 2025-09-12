@echo off
echo 正在启动SparrowDB GUI界面...
echo.

REM 编译所有Java文件
echo 正在编译Java文件...
javac -encoding UTF-8 -cp "target/classes" -d target/classes src/main/java/com/sqlcompiler/DatabaseGUI.java src/main/java/com/sqlcompiler/EnhancedSQLCompiler.java

if %errorlevel% neq 0 (
    echo 编译失败！请检查Java代码。
    pause
    exit /b 1
)

echo 编译成功！
echo.

REM 运行GUI程序
echo 正在启动GUI界面...
java -cp "target/classes" com.sqlcompiler.DatabaseGUI

pause
