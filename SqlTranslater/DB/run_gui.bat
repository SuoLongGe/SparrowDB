@echo off
chcp 65001 >nul 2>&1
echo Starting SparrowDB GUI...
echo.

REM Compile all Java files
echo Compiling Java files...
javac -encoding UTF-8 -cp "target/classes" -d target/classes src/main/java/com/database/config/DatabaseConfig.java src/main/java/com/sqlcompiler/DatabaseGUI.java src/main/java/com/sqlcompiler/EnhancedSQLCompiler.java

if %errorlevel% neq 0 (
    echo Compilation failed! Please check Java code.
    pause
    exit /b 1
)

echo Compilation successful!
echo.

REM Run GUI program
echo Starting GUI interface...
java -cp "target/classes" com.sqlcompiler.DatabaseGUI

pause
