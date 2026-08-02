# Project 3 — Semantic Analysis (Name & Type Checking)

The third phase adds semantic analysis on top of the Project 2 AST: a symbol table plus two visitors that resolve names and check types.

## What this does

- `src/main/grammar/SimpleLang.g4` — the MOL grammar (unchanged from Project 2).
- `src/main/ast/` — same AST as Project 2, with `expressions/` further split into `literals/` (`IntLiteral`, `FloatLiteral`, `DoubleLiteral`, `BoolLiteral`, `CharLiteral`) and `operators/` (`BinaryOperator`, `UnaryOperator`), plus `BinaryExpression`, `UnaryExpression`, `ConstantExpression`, and related nodes.
- `src/main/symbolTable/` — `SymbolTable`, with `items/` (`ModuleItem`, `StructItem`, `MethodItem`, `VarItem`) representing declared entities, and `exceptions/` (`ItemAlreadyExistsException`, `ItemNotFoundException`) for lookup/insertion errors.
- `src/main/visitor/`:
  - `NameAnalyzer` — a two-pass visitor. Pass one registers every module, struct and member in the symbol table (enabling forward references and catching `already defined` errors). Pass two walks method bodies, catching `not declared`, `is private`, and `is uninitialized` errors, and (as a bonus) prunes AST nodes unreachable from `main`, reporting them as `is unreachable`.
  - `TypeAnalyzer` — computes and checks types across the AST, catching `Type mismatch in assignment`, `Condition type must be bool`, `Return type mismatch`, `Cannot modify immutable variable`, and argument count/type mismatches on method calls.
  - `TypeUtils` — helpers for readable type names and exact type comparison (MOL disallows implicit conversions, e.g. no implicit `float` → `int`).
- `REPORT_phase2.md` — a detailed write-up (in Persian) of the design: message format, key assumptions (e.g. unqualified method calls are looked up in the current module/struct and its `includes` first, then globally), and known limitations.
- `run_phase2_tests.sh` — compiles the project, runs it against every sample, and diffs the output against the expected `.out` file.
- `samples/Name/`, `samples/Type/` — `.mol` test cases targeting name-resolution and type-checking errors respectively, each paired with an expected `.out` file.

## Notes

The ANTLR-generated lexer/parser/visitor source files are not included since they're regenerated automatically from the grammar (`.g4` file). See `REPORT_phase2.md` for the full design rationale and test results.
