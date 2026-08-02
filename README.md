# Compiler Course Projects

Four progressive assignments building a compiler for **MOL**, a small module/struct-based language, as part of a Principles of Compiler course. Each project builds on the previous one, adding a new stage of the compiler pipeline.

| Project | Stage | Description |
|---|---|---|
| [project-1](./project-1) | Lexer, Parser & AST | ANTLR4 grammar for MOL, a hand-built AST, and a visitor that prints it. |
| [project-2](./project-2) | Refactored AST & Print Visitor | The AST redesigned into sub-packages (`core`, `declarations`, `expressions`, `statements`, `types`) with an updated print visitor. |
| [project-3](./project-3) | Semantic Analysis | A symbol table plus `NameAnalyzer` (name resolution, reachability) and `TypeAnalyzer` (type checking) visitors. |
| [project-4](./project-4) | Code Generation | A `CodeGenerator` visitor that emits JVM bytecode (via Jasmin assembly) for valid MOL programs. |

Each project folder has its own README with details on what it contains.

## Structure

Every project keeps the same shape:

- `src/main/grammar/` — the ANTLR4 `.g4` grammar for MOL.
- `src/main/ast/` — hand-written AST node classes.
- `src/main/visitor/` — visitors implementing each phase's logic.
- `src/main/symbolTable/` — symbol table (projects 3–4 only).
- `samples/` — sample `.mol` programs with expected output, used for testing.

ANTLR-generated lexer/parser/visitor source, build output, and IDE files are not included — only the hand-authored `.g4` grammar and source are kept, since generated files are reproducible by running ANTLR4 against the grammar.
