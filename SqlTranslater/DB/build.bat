@echo off
echo Compiling SparrowDB with function support...

REM Create target directory
if not exist target\classes mkdir target\classes

REM Compile core classes first
echo Compiling core classes...
javac -encoding UTF-8 -cp "src/main/java" -d target/classes src/main/java/com/database/exception/DatabaseException.java
javac -encoding UTF-8 -cp "src/main/java" -d target/classes src/main/java/com/sqlcompiler/execution/ExecutionResult.java
javac -encoding UTF-8 -cp "src/main/java" -d target/classes src/main/java/com/sqlcompiler/lexer/TokenType.java
javac -encoding UTF-8 -cp "src/main/java" -d target/classes src/main/java/com/sqlcompiler/lexer/LexicalAnalyzer.java

REM Compile AST classes
echo Compiling AST classes...
javac -encoding UTF-8 -cp "src/main/java;target/classes" -d target/classes src/main/java/com/sqlcompiler/ast/CreateFunctionStatement.java
javac -encoding UTF-8 -cp "src/main/java;target/classes" -d target/classes src/main/java/com/sqlcompiler/ast/CallStatement.java
javac -encoding UTF-8 -cp "src/main/java;target/classes" -d target/classes src/main/java/com/sqlcompiler/ast/DropFunctionStatement.java

REM Compile execution classes
echo Compiling execution classes...
javac -encoding UTF-8 -cp "src/main/java;target/classes" -d target/classes src/main/java/com/sqlcompiler/execution/CreateFunctionPlan.java
javac -encoding UTF-8 -cp "src/main/java;target/classes" -d target/classes src/main/java/com/sqlcompiler/execution/CallPlan.java
javac -encoding UTF-8 -cp "src/main/java;target/classes" -d target/classes src/main/java/com/sqlcompiler/execution/DropFunctionPlan.java

REM Compile function manager
echo Compiling function manager...
javac -encoding UTF-8 -cp "src/main/java;target/classes" -d target/classes src/main/java/com/database/engine/FunctionManager.java

echo Compilation completed!
