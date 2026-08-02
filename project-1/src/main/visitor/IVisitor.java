package main.visitor;

import main.ast.*;

public interface IVisitor<T> {

    T visit(Program program);
    
    // Declarations
    T visit(ModuleDeclaration moduleDeclaration);
    T visit(StructDeclaration structDeclaration);
    T visit(FieldDeclaration fieldDeclaration);
    T visit(MethodDeclaration methodDeclaration);
    
    // Statements
    T visit(AssignmentStatement assignmentStatement);
    T visit(IfStatement ifStatement);
    T visit(WhileStatement whileStatement);
    T visit(OutputStatement outputStatement);
    
    // Expressions & Literals
    T visit(BinaryExpression binaryExpression);
    T visit(MethodCall methodCall);
    T visit(Identifier identifier);
    T visit(IntValue intValue);
    T visit(BoolValue boolValue);

}