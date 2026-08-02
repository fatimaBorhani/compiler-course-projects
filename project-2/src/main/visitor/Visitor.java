package main.visitor;

import main.ast.core.*;
import main.ast.declarations.*;
import main.ast.statements.*;
import main.ast.expressions.*;
import main.ast.types.*;
import main.ast.declarations.Module;
public abstract class Visitor<T> implements IVisitor<T> {
    
    // Core & Declarations
    @Override public T visit(Program program) { return null; }
    @Override public T visit(Module module) { return null; }
    @Override public T visit(ModuleDecl moduleDecl) { return null; }
    @Override public T visit(Struct struct) { return null; }
    @Override public T visit(StructDecl structDecl) { return null; }
    @Override public T visit(Method method) { return null; }
    @Override public T visit(Var var) { return null; }
    @Override public T visit(Parameter parameter) { return null; }

    // Statements
    @Override public T visit(Block block) { return null; }
    @Override public T visit(AssignStmt assignStmt) { return null; }
    @Override public T visit(IfStmt ifStmt) { return null; }
    @Override public T visit(MethodCallStmt methodCallStmt) { return null; }
    @Override public T visit(VarDeclStmt varDeclStmt) { return null; }
    @Override public T visit(InputStmt inputStmt) { return null; }
    @Override public T visit(OutputStmt outputStmt) { return null; }
    @Override public T visit(ReturnStmt returnStmt) { return null; }
    @Override public T visit(BreakJump breakJump) { return null; }
    @Override public T visit(ContinueJump continueJump) { return null; }

    // Expressions & Locations
    @Override public T visit(Identifier identifier) { return null; }
    @Override public T visit(SimpleLoc simpleLoc) { return null; }
    @Override public T visit(MemberLoc memberLoc) { return null; }
    @Override public T visit(ThisLoc thisLoc) { return null; }
    @Override public T visit(ConstructorCall constructorCall) { return null; }
    @Override public T visit(MethodCall methodCall) { return null; }

    @Override public T visit(IntType intType) { return null; }
    @Override public T visit(FloatType floatType) { return null; }
    @Override public T visit(DoubleType doubleType) { return null; }
    @Override public T visit(CharType charType) { return null; }
    @Override public T visit(BoolType boolType) { return null; }
    @Override public T visit(VoidType voidType) { return null; }
    @Override public T visit(StructType structType) { return null; }
    @Override 
    public T visit(MethodDecl methodDecl) { return null; }
    
    @Override 
    public T visit(VarDecl varDecl) { return null; }
}