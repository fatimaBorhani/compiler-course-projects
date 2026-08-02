package main.visitor;

import main.ast.*;

public class ASTPrinter implements IVisitor<Void> {

    @Override
    public Void visit(Program program) {
        System.out.println("Program:");
        for (Node declaration : program.getDeclarations()) {
            declaration.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(ModuleDeclaration moduleDec) {
        System.out.print("Module: " + moduleDec.getName());
        if (moduleDec.getIncludedModule() != null) {
            System.out.print(" includes " + moduleDec.getIncludedModule()); 
        }
        System.out.println(" begin"); 
        
    
        for (Node member : moduleDec.getMembers()) {
            member.accept(this);
        }
        System.out.println("end"); 
        return null;
    }

    @Override
    public Void visit(StructDeclaration structDec) {
        System.out.println("Struct: " + structDec.getName() + " begin"); 
        for (FieldDeclaration field : structDec.getFields()) {
            field.accept(this);
        }
        System.out.println("end");
        return null;
    }

    @Override
    public Void visit(FieldDeclaration fieldDec) {
        System.out.print("  Field: ");
        if (fieldDec.isMutable()) System.out.print("mut "); 
        System.out.println(fieldDec.getType() + " " + fieldDec.getName() + ";"); 
        return null;
    }

    @Override
    public Void visit(MethodDeclaration methodDec) {
        System.out.println("  Method: " + methodDec.getName() + " begin"); 
        for (Node stmt : methodDec.getBody()) {
            stmt.accept(this);
        }
        System.out.println("  end");
        return null;
    }

    @Override
    public Void visit(AssignmentStatement assignStat) {
        System.out.print("    Assignment: " + assignStat.getVarName() + " = "); 
        assignStat.getExpression().accept(this);
        System.out.println(";");
        return null;
    }

    @Override
    public Void visit(IfStatement ifStat) {
        System.out.print("    If ("); 
        ifStat.getCondition().accept(this);
        System.out.println(") begin");
        for (Node stmt : ifStat.getThenBody()) {
            stmt.accept(this);
        }
        if (!ifStat.getElseBody().isEmpty()) {
            System.out.println("    else begin"); 
            for (Node stmt : ifStat.getElseBody()) {
                stmt.accept(this);
            }
        }
        System.out.println("    end");
        return null;
    }

    @Override
    public Void visit(WhileStatement whileStat) {
        System.out.print("    While ("); 
        whileStat.getCondition().accept(this);
        System.out.println(") begin");
        for (Node stmt : whileStat.getBody()) {
            stmt.accept(this);
        }
        System.out.println("    end");
        return null;
    }

    @Override
    public Void visit(OutputStatement outputStat) {
        System.out.print("    Output: ");
        outputStat.getExpression().accept(this);
        System.out.println(";");
        return null;
    }

    @Override
    public Void visit(BinaryExpression binaryExpr) {
        System.out.print("(");
        binaryExpr.getLeft().accept(this);
        System.out.print(" " + binaryExpr.getOperator() + " ");
        binaryExpr.getRight().accept(this);
        System.out.print(")");
        return null;
    }

    @Override
    public Void visit(MethodCall methodCall) {
        if (methodCall.getInstance() != null) {
            methodCall.getInstance().accept(this);
            System.out.print("."); 
        }
        System.out.print(methodCall.getMethodName() + "(");
        for (int i = 0; i < methodCall.getArguments().size(); i++) {
            methodCall.getArguments().get(i).accept(this);
            if (i < methodCall.getArguments().size() - 1) System.out.print(", ");
        }
        System.out.print(")");
        return null;
    }

    @Override
    public Void visit(Identifier identifier) {
        System.out.print(identifier.getName());
        return null;
    }

    @Override
    public Void visit(IntValue intValue) {
        System.out.print(intValue.getValue());
        return null;
    }

    @Override
    public Void visit(BoolValue boolValue) {
        System.out.print(boolValue.getValue()); 
        return null;
    }
}