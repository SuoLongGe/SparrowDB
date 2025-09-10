@echo off
echo Compiling Java Storage System...

REM 编译所有Java文件
javac *.java

if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b 1
)

echo Compilation successful!
echo.
echo Running Storage System Test...
echo =====================================
echo.

REM 运行测试程序
java StorageSystemTest

echo.
echo =====================================
echo Program execution completed!
echo.
pause
