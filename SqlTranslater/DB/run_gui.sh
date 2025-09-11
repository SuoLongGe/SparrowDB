#!/bin/bash

echo "正在启动SparrowDB GUI界面..."
echo

# 编译所有Java文件
echo "正在编译Java文件..."
javac -cp "target/classes" -d target/classes src/main/java/com/sqlcompiler/DatabaseGUI.java src/main/java/com/sqlcompiler/EnhancedSQLCompiler.java

if [ $? -ne 0 ]; then
    echo "编译失败！请检查Java代码。"
    exit 1
fi

echo "编译成功！"
echo

# 运行GUI程序
echo "正在启动GUI界面..."
java -cp "target/classes" com.sqlcompiler.DatabaseGUI
