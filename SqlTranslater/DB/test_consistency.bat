@echo off
chcp 65001 >nul 2>&1
echo ========================================
echo   SparrowDB 数据一致性测试
echo ========================================
echo.

echo 1. 编译所有相关文件...
javac -encoding UTF-8 -cp "target/classes" -d target/classes src/main/java/com/database/config/DatabaseConfig.java src/main/java/com/sqlcompiler/DatabaseGUI.java src/main/java/com/database/SparrowDBApplication.java

if %errorlevel% neq 0 (
    echo ❌ 编译失败！
    pause
    exit /b 1
)

echo ✅ 编译成功！
echo.

echo 2. 检查数据目录结构...
echo 项目根目录data文件夹:
dir ..\..\data\*.tbl /B 2>nul
echo.
echo SqlTranslater/DB/data文件夹:
dir data\*.tbl /B 2>nul
echo.

echo 3. 显示users表数据对比:
echo.
echo === 项目根目录 data/users.tbl ===
type ..\..\data\users.tbl | findstr "RECORD:"
echo.
echo === SqlTranslater/DB/data/users.tbl ===
type data\users.tbl | findstr "RECORD:"
echo.

echo 4. 测试配置类路径检测...
java -cp "target/classes" com.database.config.DatabaseConfig
echo.

echo ========================================
echo   测试完成
echo ========================================
pause
