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
echo Choose what to run:
echo 1. Storage System Test
echo 2. B+ Tree Index Test
echo 3. B+ Tree Demo
echo.
set /p choice="Enter your choice (1-3): "

if "%choice%"=="1" (
    echo Running Storage System Test...
    echo =====================================
    echo.
    java StorageSystemTest
) else if "%choice%"=="2" (
    echo Running B+ Tree Index Test...
    echo =====================================
    echo.
    java BPlusTreeTest
) else if "%choice%"=="3" (
    echo Running B+ Tree Demo...
    echo =====================================
    echo.
    java BPlusTreeDemo
) else (
    echo Invalid choice!
)

echo.
echo =====================================
echo Program execution completed!
echo.
pause
