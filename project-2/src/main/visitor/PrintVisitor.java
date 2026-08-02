package main.visitor;

import main.ast.core.*;
import main.ast.declarations.*;

import main.ast.declarations.Module; 
import main.ast.statements.*;
import main.ast.types.*;

public class PrintVisitor extends Visitor<Void> {

    @Override
    public Void visit(Program program) {
        int moduleCount = 0;
        int structCount = 0;

        for (TopLevelDecl decl : program.getTopLevelDeclarations()) {
            if (decl instanceof ModuleDecl) moduleCount++;
            else if (decl instanceof StructDecl) structCount++;
        }

        
        System.out.printf("program [modules:%d structs:%d]\n", moduleCount, structCount);

        for (TopLevelDecl decl : program.getTopLevelDeclarations()) {
            decl.accept(this);
        }

        return null;
    }
    
    @Override
    public Void visit(ModuleDecl moduleDecl) {
        if (moduleDecl.getModule() != null) moduleDecl.getModule().accept(this);
        return null;
    }

    @Override
    public Void visit(StructDecl structDecl) {
        if (structDecl.getStruct() != null) structDecl.getStruct().accept(this);
        return null;
    }

    @Override
    public Void visit(Module module) {
        int methodCount = 0;
        int fieldCount = 0;

        for (Member member : module.getMembers()) {
            if (member instanceof MethodDecl) methodCount++;
            else if (member instanceof VarDecl) fieldCount++;
        }

        System.out.printf("    module %s [methods:%d fields:%d]\n", module.getName().getName(), methodCount, fieldCount);

        for (Member member : module.getMembers()) {
            member.accept(this);
        }

        return null;
    }

    @Override
    public Void visit(Struct struct) {
        int fieldCount = 0;
        for (Member member : struct.getMembers()) {
            if (member instanceof VarDecl) fieldCount++;
        }

   
        System.out.printf("    struct %s [fields:%d]\n", struct.getName().getName(), fieldCount);

        for (Member member : struct.getMembers()) {
            member.accept(this);
        }

        return null;
    }

    @Override
    public Void visit(VarDecl varDecl) {
        Var var = varDecl.getVar();
        if (var == null) return null;

        String typeName = getTypeName(var.getType());
     
        String modifierStr = "";
        if (varDecl.getAccessModifier() != null) {
            modifierStr = ":" + varDecl.getAccessModifier().toString().toLowerCase();
        }

        System.out.printf("        field %s %s%s\n", var.getName().getName(), typeName, modifierStr);

        return null;
    }

    @Override
    public Void visit(MethodDecl methodDecl) {
        Method method = methodDecl.getMethod();
        if (method == null) return null;

        String returnType = getTypeName(method.getReturnType());
        
        
        String modifierStr = "";
        if (methodDecl.getAccessModifier() != null) {
            modifierStr = ":" + methodDecl.getAccessModifier().toString().toLowerCase();
        }
        
        StringBuilder paramTypes = new StringBuilder();
        if (method.getParameters() != null && !method.getParameters().isEmpty()) {
            for (int i = 0; i < method.getParameters().size(); i++) {
                paramTypes.append(getTypeName(method.getParameters().get(i).getType()));
                if (i < method.getParameters().size() - 1) {
                    paramTypes.append("*");
                }
            }
        } else {
            paramTypes.append("void");
        }

        int statementCount = 0;
        if (method.getBody() != null && method.getBody().getStatements() != null) {
            statementCount = method.getBody().getStatements().size();
        }

      
        System.out.printf("        method %s (%s -> %s)%s [statements:%d]\n", 
                method.getName().getName(), paramTypes.toString(), returnType, modifierStr, statementCount);

        return null;
    }

    private String getTypeName(Type type) {
        if (type == null) return "void";
        
        if (type instanceof StructType) {
            return ((StructType) type).getName().getName();
        }
        
        return type.getClass().getSimpleName().replace("Type", "").toLowerCase();
    }
}