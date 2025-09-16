@echo off
chcp 65001 >nul
echo 正在启动SparrowDB GUI界面...
echo.

REM 编译所有Java文件
echo 正在编译Java文件...
javac -encoding UTF-8 -cp "target/classes" -d target/classes src/main/java/com/sqlcompiler/*.java src/main/java/com/sqlcompiler/lexer/*.java src/main/java/com/sqlcompiler/ast/*.java src/main/java/com/sqlcompiler/parser/*.java src/main/java/com/sqlcompiler/semantic/*.java src/main/java/com/sqlcompiler/execution/*.java src/main/java/com/sqlcompiler/catalog/*.java src/main/java/com/sqlcompiler/exception/*.java src/main/java/com/sqlcompiler/gui/*.java src/main/java/com/database/*.java

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
