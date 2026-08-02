package main.visitor;

import main.ast.core.*;
import main.ast.declarations.*;
import main.ast.statements.*;
import main.ast.expressions.*;
import main.ast.types.*;
import main.ast.declarations.Module;

public interface IVisitor<T> {
    
    // Core & Declarations
    T visit(Program program);
    T visit(Module module);
    T visit(ModuleDecl moduleDecl); // اگر هنوز این کلاس را نگه داشته‌اید
    T visit(Struct struct);
    T visit(StructDecl structDecl); // اگر هنوز این کلاس را نگه داشته‌اید
    T visit(Method method);
    T visit(Var var);
    T visit(Parameter parameter);

    // Statements
    T visit(Block block);
    T visit(AssignStmt assignStmt);
    T visit(IfStmt ifStmt);
    T visit(MethodCallStmt methodCallStmt);
    T visit(VarDeclStmt varDeclStmt);
    T visit(InputStmt inputStmt);
    T visit(OutputStmt outputStmt);
    T visit(ReturnStmt returnStmt);
    T visit(BreakJump breakJump);
    T visit(ContinueJump continueJump);

    // Expressions & Locations
    T visit(Identifier identifier);
    T visit(SimpleLoc simpleLoc);
    T visit(MemberLoc memberLoc);
    T visit(ThisLoc thisLoc);
    T visit(ConstructorCall constructorCall);
    T visit(MethodCall methodCall);
    T visit(IntType intType);
    T visit(FloatType floatType);
    T visit(DoubleType doubleType);
    T visit(CharType charType);
    T visit(BoolType boolType);
    T visit(VoidType voidType);
    T visit(StructType structType);
    T visit(MethodDecl methodDecl);
    T visit(VarDecl varDecl);
}