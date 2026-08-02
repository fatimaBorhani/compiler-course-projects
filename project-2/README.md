# Project 2 — Refactored AST & Print Visitor

The second phase: the AST from Project 1 is redesigned into a cleaner, more extensible structure, organized into sub-packages, and paired with an updated print visitor.

## What this does

- `src/main/grammar/SimpleLang.g4` — the ANTLR4 grammar for MOL (same language as Project 1, grammar refined for the new AST).
- `src/main/ast/` — the AST, now split into sub-packages:
  - `core/` — `Node`, `Program`.
  - `declarations/` — `ModuleDecl`, `StructDecl`, `MethodDecl`, `VarDecl`, `Parameter`, and the `Module`/`Struct`/`Method`/`Var`/`Member`/`TopLevelDecl` interfaces they implement.
  - `expressions/` — `Expression`, `Location` (`SimpleLoc`, `MemberLoc`, `ThisLoc`), `MethodCall`, `ConstructorCall`.
  - `statements/` — `Statement`, `Block`, `IfStmt`, `WhileStmt`, `ForStmt`, `AssignStmt`, `VarDeclStmt`, `ReturnStmt`, `OutputStmt`, `InputStmt`, `JumpStmt` (`BreakJump`, `ContinueJump`), `MethodCallStmt`.
  - `types/` — `Type` and its implementations (`IntType`, `FloatType`, `DoubleType`, `BoolType`, `CharType`, `VoidType`, `StructType`, `Identifier`), plus `AccessModifier`.
- `src/main/visitor/` — `IVisitor`, `Visitor` (base class), and `PrintVisitor`, which walks the new AST and prints it.
- `src/SimpleLang.java` — the entry point: parses a `.mol` file and runs `PrintVisitor` over the resulting AST.
- `samples/` — sample `.mol` programs (`sample1`–`sample8`) with expected `.out` output.

## Notes

The ANTLR-generated lexer/parser/visitor source files are not included since they're regenerated automatically from the grammar (`.g4` file).
