package main.visitor;

import main.ast.core.*;
import main.ast.declarations.Module;
import main.ast.types.*;
import main.ast.declarations.*;
import main.ast.statements.*;
import main.ast.expressions.*;
import main.ast.expressions.literals.*;

public interface IVisitor<T> {
    T visit(Program program);

    T visit(Module module);

    T visit(ModuleDecl moduleDecl);

    T visit(Struct struct);

    T visit(StructDecl structDecl);

    T visit(Method method);

    T visit(MethodDecl methodDecl);

    T visit(Parameter parameter);

    T visit(Var var);

    T visit(VarDecl varDecl);

    T visit(Block block);

    T visit(VarDeclStmt varDeclStmt);

    T visit(AssignStmt assignStmt);

    T visit(MethodCallStmt methodCallStmt);

    T visit(ReturnStmt returnStmt);

    T visit(IfStmt ifStmt);

    T visit(ForStmt forStmt);

    T visit(WhileStmt whileStmt);

    T visit(BreakJump breakJump);

    T visit(ContinueJump continueJump);

    T visit(InputStmt inputStmt);

    T visit(OutputStmt outputStmt);

    T visit(MethodCall methodCall);

    T visit(ConstructorCall constructorCall);

    T visit(SimpleLoc simpleLoc);

    T visit(MemberLoc memberLoc);

    T visit(ThisLoc thisLoc);

    T visit(MethodCallLoc methodCallLoc);

    T visit(ParanthesisExpr paranthesisExpr);

    T visit(UnaryOpExpr unaryOpExpr);

    T visit(BinaryExpression binaryOpExpr);

    T visit(IntLiteral intLiteral);

    T visit(FloatLiteral floatLiteral);

    T visit(DoubleLiteral doubleLiteral);

    T visit(CharLiteral charLiteral);

    T visit(BoolLiteral boolLiteral);

    T visit(Identifier identifier);

    T visit(PrimitiveType primitiveType);
    T visit(UnaryExpression unaryExpression);
    T visit(ConstantExpression constantExpression);


    T visit(UserDefinedType userDefinedType);
}