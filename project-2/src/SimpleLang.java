import main.ast.core.Program;
import main.visitor.PrintVisitor; 

import java.io.IOException;
import main.grammar.SimpleLangLexer;
import main.grammar.SimpleLangParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

public class SimpleLang {
    public static void main(String[] args) throws IOException {
    
        if (args.length == 0) {
            System.err.println("Error: Please provide a file path as an argument.");
            return;
        }

        CharStream reader = CharStreams.fromFileName(args[0]);

        SimpleLangLexer lexer = new SimpleLangLexer(reader);
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        SimpleLangParser parser = new SimpleLangParser(tokens);

        Program program = parser.program().programRet;

    
        if (program != null) {
           
            
            PrintVisitor printVisitor = new PrintVisitor();
          
            program.accept(printVisitor);
            
        } else {
            System.err.println("Parse Error: AST could not be generated.");
        }
    }
}