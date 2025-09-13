#!/bin/bash

echo "正在启动SparrowDB GUI界面..."
echo

# 创建目标目录
mkdir -p target/classes

# 编译所有Java文件
echo "正在编译Java文件..."
find src/main/java -name "*.java" -exec javac -cp "src/main/java" -d target/classes {} +

if [ $? -ne 0 ]; then
    echo "编译失败！请检查Java代码。"
    exit 1
fi

echo "编译成功！"
echo

# 运行GUI程序
echo "正在启动GUI界面..."
java -cp "target/classes" com.sqlcompiler.DatabaseGUI
