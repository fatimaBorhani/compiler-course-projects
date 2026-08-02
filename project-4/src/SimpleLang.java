import main.ast.core.Program;
import main.grammar.SimpleLangLexer;
import main.grammar.SimpleLangParser;
import main.visitor.CodeGenerator;
import main.visitor.NameAnalyzer;
import main.visitor.TypeAnalyzer;

import java.io.IOException;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

public class SimpleLang {
    public static void main(String[] args) throws IOException {
        CharStream reader = CharStreams.fromFileName(args[0]);

        SimpleLangLexer lexer = new SimpleLangLexer(reader);
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        SimpleLangParser parser = new SimpleLangParser(tokens);

        Program program = parser.program().programRet;

        // Phase 2 - Name analysis (also builds the symbol table and prunes
        // unreachable modules / structs).
        NameAnalyzer nameAnalyzer = new NameAnalyzer();
        program.accept(nameAnalyzer);

        // Phase 2 - Type analysis (reuses the symbol structure above).
        TypeAnalyzer typeAnalyzer = new TypeAnalyzer(nameAnalyzer);
        program.accept(typeAnalyzer);

        // Report: warnings first, then name errors, then type errors.
        nameAnalyzer.printResults();
        typeAnalyzer.printResults();

        // Bonus: emit the optimized source (unreachable declarations removed).
        nameAnalyzer.writeOptimizedSource(args[0]);

        // Phase 3 - Code generation. Bytecode is only emitted for a program
        // that passed name and type analysis.
        if (nameAnalyzer.errors.isEmpty() && typeAnalyzer.errors.isEmpty()) {
            CodeGenerator codeGenerator = new CodeGenerator();
            program.accept(codeGenerator);
        }
    }
}
