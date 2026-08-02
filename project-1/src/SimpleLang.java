// lexer, parser and visitor

import main.ast.Program;
import main.grammar.simpleLangLexer;
import main.grammar.simpleLangParser;
import main.visitor.ASTPrinter;

// utils

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class SimpleLang {

    public static void main(String[] args) throws IOException {

        // parse 
        CharStream reader = CharStreams.fromFileName(args[0]);
        simpleLangLexer simpleLangLexer = new simpleLangLexer(reader);
        CommonTokenStream tokens = new CommonTokenStream(simpleLangLexer);
        simpleLangParser flParser = new simpleLangParser(tokens);
        Program program = flParser.program().programRet;

        // visit
        ASTPrinter printer = new ASTPrinter();
        printer.visit(program);

    }
}