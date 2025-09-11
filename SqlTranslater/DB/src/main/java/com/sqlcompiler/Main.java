package com.sqlcompiler;

import java.util.Scanner;

/**
 * SQL编译器主程序
 */
public class Main {
    public static void main(String[] args) {
        SQLCompiler compiler = new SQLCompiler();
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== SQL编译器 ===");
        System.out.println("支持CREATE TABLE、INSERT、SELECT、DELETE语句");
        System.out.println("输入'exit'退出程序");
        System.out.println("输入'catalog'查看目录信息");
        System.out.println("输入'clear'清空目录");
        System.out.println();
        
        while (true) {
            System.out.print("SQL> ");
            StringBuilder inputBuilder = new StringBuilder();
            String line;
            
            // 读取多行输入，直到遇到分号或空行
            while (scanner.hasNextLine()) {
                line = scanner.nextLine().trim();
                if (line.isEmpty() && inputBuilder.length() == 0) {
                    continue;
                }
                
                inputBuilder.append(line).append(" ");
                
                // 如果行以分号结尾，说明SQL语句结束
                if (line.endsWith(";")) {
                    break;
                }
                
                // 如果输入为空且已有内容，也结束输入
                if (line.isEmpty() && inputBuilder.length() > 0) {
                    break;
                }
            }
            
            String input = inputBuilder.toString().trim();
            
            if (input.isEmpty()) {
                continue;
            }
            
            if ("exit".equalsIgnoreCase(input)) {
                System.out.println("再见！");
                break;
            }
            
            if ("catalog".equalsIgnoreCase(input)) {
                System.out.println(compiler.getCatalogInfo());
                continue;
            }
            
            if ("clear".equalsIgnoreCase(input)) {
                compiler.clearCatalog();
                System.out.println("目录已清空");
                continue;
            }
            
            try {
                SQLCompiler.CompilationResult result = compiler.compile(input);
                
                if (result.isSuccess()) {
                    System.out.println("\n编译成功！");
                } else {
                    System.out.println("\n编译失败！");
                    if (result.getErrors() != null) {
                        for (String error : result.getErrors()) {
                            System.out.println("错误: " + error);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("程序错误: " + e.getMessage());
                e.printStackTrace();
            }
            
            System.out.println();
        }
        
        scanner.close();
    }
}
