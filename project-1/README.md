# Project 1 — Lexer, Parser & AST

The first phase of the MOL language compiler: an ANTLR4 grammar for MOL together with a hand-built Abstract Syntax Tree (AST) and a visitor that prints it.

## What this does

- `src/main/grammar/simpleLang.g4` — the ANTLR4 grammar for MOL (modules, structs, fields, methods, constructors, statements, expressions).
- `src/main/ast/` — hand-written AST node classes (`Program`, `ModuleDeclaration`, `StructDeclaration`, `FieldDeclaration`, `MethodDeclaration`, `IfStatement`, `WhileStatement`, `AssignmentStatement`, `BinaryExpression`, `MethodCall`, `Identifier`, `IntValue`, `BoolValue`, `OutputStatement`, etc.), built from the parse tree.
- `src/main/visitor/` — `IVisitor` interface and `ASTPrinter`, a visitor that walks the AST and prints its structure.
- `src/main/ast/simpleLang.g4.parse-tree.svg` — a visual rendering of a sample parse tree, generated from the grammar.
- `samples/` — sample `.mol` programs (`sample1`–`sample5`) with their expected `.out` output.

## Notes

The ANTLR-generated lexer/parser/visitor source files are not included since they're regenerated automatically from the grammar (`.g4` file). To regenerate them, run ANTLR4 against `simpleLang.g4`.
