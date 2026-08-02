package main.visitor;

import main.ast.core.*;
import main.ast.declarations.Module;
import main.ast.declarations.*;
import main.ast.statements.*;
import main.ast.expressions.*;
import main.ast.expressions.literals.*;
import main.ast.types.*;
import main.symbolTable.SymbolTable;
import main.symbolTable.exceptions.ItemAlreadyExistsException;
import main.symbolTable.exceptions.ItemNotFoundException;
import main.symbolTable.items.*;

import java.util.*;

/**
 * Phase 2 - Type analysis.
 *
 * Reuses the symbol structure built by {@link NameAnalyzer} and reports:
 *   - Type mismatch in assignment
 *   - Condition type must be bool
 *   - Return type mismatch
 *   - Cannot modify immutable variable
 *   - Argument count / type mismatch on method calls
 */
public class TypeAnalyzer extends Visitor<Type> {

    private final NameAnalyzer na;
    public final List<String> errors = new ArrayList<>();

    private SymbolTable currentScope;
    private String currentTypeName;
    private Type currentReturnType;

    private static final Type BOOL = new PrimitiveType(PrimitiveType.Primitive.BOOL);

    public TypeAnalyzer(NameAnalyzer nameAnalyzer) {
        this.na = nameAnalyzer;
    }

    private void error(int line, String message) {
        errors.add("Line " + line + " : " + message);
    }

    // ---------------------------------------------------------------------

    @Override
    public Type visit(Program program) {
        for (TopLevelDecl decl : program.getTopLevelDeclarations()) {
            decl.accept(this);
        }
        return null;
    }

    @Override
    public Type visit(ModuleDecl moduleDecl) {
        return moduleDecl.getModule().accept(this);
    }

    @Override
    public Type visit(StructDecl structDecl) {
        return structDecl.getStruct().accept(this);
    }

    @Override
    public Type visit(Module module) {
        currentTypeName = module.getName().getName();
        for (Member m : module.getMembers()) {
            m.accept(this);
        }
        currentTypeName = null;
        return null;
    }

    @Override
    public Type visit(Struct struct) {
        currentTypeName = struct.getName().getName();
        for (Member m : struct.getMembers()) {
            m.accept(this);
        }
        currentTypeName = null;
        return null;
    }

    @Override
    public Type visit(VarDecl varDecl) {
        return null; // fields carry no body to type-check
    }

    @Override
    public Type visit(MethodDecl methodDecl) {
        Method method = methodDecl.getMethod();
        SymbolTable methodScope = new SymbolTable();
        SymbolTable saved = currentScope;
        Type savedReturn = currentReturnType;
        currentScope = methodScope;
        currentReturnType = method.getReturnType();

        for (Parameter p : method.getParameters()) {
            VarItem pItem = new VarItem(p.getName().getName(), p.getType(),
                    p.getIsMutable(), "public");
            pItem.setInitialized(true);
            try {
                currentScope.put(pItem);
            } catch (ItemAlreadyExistsException ignored) {
            }
        }

        if (method.getBody() != null) {
            method.getBody().accept(this);
        }

        currentScope = saved;
        currentReturnType = savedReturn;
        return null;
    }

    @Override
    public Type visit(Block block) {
        SymbolTable saved = currentScope;
        currentScope = new SymbolTable(saved);
        for (Statement stmt : block.getStatements()) {
            stmt.accept(this);
        }
        currentScope = saved;
        return null;
    }

    private Type declaredType(Var var) {
        if (var.getType() != null) {
            return var.getType();
        }
        if (var.getConstructorCall() != null) {
            return new UserDefinedType(var.getConstructorCall().getName());
        }
        return null;
    }

    @Override
    public Type visit(VarDeclStmt varDeclStmt) {
        Var var = varDeclStmt.getVar();
        Type declared = declaredType(var);
        VarItem item = new VarItem(var.getName().getName(), declared,
                var.getIsMutable(), "public");
        item.setInitialized(varDeclStmt.getInitial() != null
                || var.getConstructorCall() != null);
        try {
            currentScope.put(item);
        } catch (ItemAlreadyExistsException ignored) {
        }

        if (var.getConstructorCall() != null) {
            var.getConstructorCall().accept(this);
        }
        if (varDeclStmt.getInitial() != null) {
            Type rhs = varDeclStmt.getInitial().accept(this);
            if (declared != null && rhs != null && !TypeUtils.same(rhs, declared)) {
                error(var.getLine(), "Type mismatch in assignment. Cannot assign "
                        + TypeUtils.name(rhs) + " to " + TypeUtils.name(declared));
            }
        }
        return null;
    }

    @Override
    public Type visit(AssignStmt assignStmt) {
        Type rhs = assignStmt.getRight() == null ? null : assignStmt.getRight().accept(this);
        Location left = assignStmt.getLeft();

        // Immutability: enforced on plain local / parameter targets.
        if (left instanceof SimpleLoc) {
            String name = ((SimpleLoc) left).getId().getName();
            VarItem v = tryGetVar(name);
            if (v != null && !v.isMut()) {
                error(assignStmt.getLine(), "Cannot modify immutable variable " + name);
            }
        }

        Type lhs = typeOfLocation(left);
        if (lhs != null && rhs != null && !TypeUtils.same(rhs, lhs)) {
            error(assignStmt.getLine(), "Type mismatch in assignment. Cannot assign "
                    + TypeUtils.name(rhs) + " to " + TypeUtils.name(lhs));
        }
        return null;
    }

    @Override
    public Type visit(MethodCallStmt methodCallStmt) {
        methodCallStmt.getMethodCall().accept(this);
        return null;
    }

    @Override
    public Type visit(ReturnStmt returnStmt) {
        Type expected = currentReturnType;
        Type actual = returnStmt.getValue() == null ? null : returnStmt.getValue().accept(this);
        boolean expectedVoid = expected == null || TypeUtils.isPrimitive(expected, PrimitiveType.Primitive.VOID);

        if (expectedVoid) {
            if (returnStmt.getValue() != null && actual != null) {
                error(returnStmt.getLine(), "Return type mismatch. Expected void, got "
                        + TypeUtils.name(actual));
            }
        } else {
            if (returnStmt.getValue() == null) {
                error(returnStmt.getLine(), "Return type mismatch. Expected "
                        + TypeUtils.name(expected) + ", got void");
            } else if (actual != null && !TypeUtils.same(actual, expected)) {
                error(returnStmt.getLine(), "Return type mismatch. Expected "
                        + TypeUtils.name(expected) + ", got " + TypeUtils.name(actual));
            }
        }
        return null;
    }

    @Override
    public Type visit(IfStmt ifStmt) {
        checkCondition(ifStmt.getCondition(), ifStmt.getLine());
        if (ifStmt.getThenBranch() != null) ifStmt.getThenBranch().accept(this);
        if (ifStmt.getElseBranch() != null) ifStmt.getElseBranch().accept(this);
        return null;
    }

    @Override
    public Type visit(WhileStmt whileStmt) {
        checkCondition(whileStmt.getCondition(), whileStmt.getLine());
        if (whileStmt.getBody() != null) whileStmt.getBody().accept(this);
        return null;
    }

    @Override
    public Type visit(ForStmt forStmt) {
        SymbolTable saved = currentScope;
        currentScope = new SymbolTable(saved);
        for (Statement init : forStmt.getInitializers()) init.accept(this);
        if (forStmt.getCondition() != null) {
            checkCondition(forStmt.getCondition(), forStmt.getLine());
        }
        for (AssignStmt upd : forStmt.getUpdaters()) upd.accept(this);
        if (forStmt.getBody() != null) forStmt.getBody().accept(this);
        currentScope = saved;
        return null;
    }

    private void checkCondition(Expression condition, int line) {
        if (condition == null) {
            return;
        }
        Type t = condition.accept(this);
        if (t != null && !TypeUtils.isBool(t)) {
            error(line, "Condition type must be bool");
        }
    }

    @Override
    public Type visit(OutputStmt outputStmt) {
        if (outputStmt.getValue() != null) outputStmt.getValue().accept(this);
        return null;
    }

    @Override
    public Type visit(InputStmt inputStmt) {
        return null;
    }

    // -------------------- expressions --------------------

    @Override
    public Type visit(ParanthesisExpr paranthesisExpr) {
        return paranthesisExpr.getExpression() == null ? null
                : paranthesisExpr.getExpression().accept(this);
    }

    @Override
    public Type visit(BinaryExpression expr) {
        Type left = expr.getLeftOperand() == null ? null : expr.getLeftOperand().accept(this);
        Type right = expr.getRightOperand() == null ? null : expr.getRightOperand().accept(this);
        switch (expr.getOperator()) {
            case LESS_THAN:
            case GREATER_THAN:
            case LESS_THAN_OR_EQUAL_TO:
            case GREATER_THAN_OR_EQUAL_TO:
            case EQUALITY:
            case INEQUALITY:
            case AND:
            case OR:
                return BOOL;
            default: // arithmetic
                return left != null ? left : right;
        }
    }

    @Override
    public Type visit(UnaryExpression unaryExpression) {
        Type operand = unaryExpression.getExpression() == null ? null
                : unaryExpression.getExpression().accept(this);
        if (unaryExpression.getOperand() != null
                && "not".equals(unaryExpression.getOperand().getSymbol())) {
            return BOOL;
        }
        return operand;
    }

    @Override
    public Type visit(UnaryOpExpr unaryOpExpr) {
        Type operand = unaryOpExpr.getOperand() == null ? null
                : unaryOpExpr.getOperand().accept(this);
        if (unaryOpExpr.getOperator() == UnaryOpExpr.Operator.NOT) {
            return BOOL;
        }
        return operand;
    }

    @Override
    public Type visit(ConstantExpression constantExpression) {
        Object v = constantExpression.getValue();
        if (v instanceof Integer) return new PrimitiveType(PrimitiveType.Primitive.INT);
        if (v instanceof Float) return new PrimitiveType(PrimitiveType.Primitive.FLOAT);
        if (v instanceof Double) return new PrimitiveType(PrimitiveType.Primitive.DOUBLE);
        if (v instanceof Character) return new PrimitiveType(PrimitiveType.Primitive.CHAR);
        if (v instanceof Boolean) return new PrimitiveType(PrimitiveType.Primitive.BOOL);
        return null;
    }

    @Override
    public Type visit(IntLiteral intLiteral) {
        return new PrimitiveType(PrimitiveType.Primitive.INT);
    }

    @Override
    public Type visit(FloatLiteral floatLiteral) {
        return new PrimitiveType(PrimitiveType.Primitive.FLOAT);
    }

    @Override
    public Type visit(DoubleLiteral doubleLiteral) {
        return new PrimitiveType(PrimitiveType.Primitive.DOUBLE);
    }

    @Override
    public Type visit(CharLiteral charLiteral) {
        return new PrimitiveType(PrimitiveType.Primitive.CHAR);
    }

    @Override
    public Type visit(BoolLiteral boolLiteral) {
        return BOOL;
    }

    @Override
    public Type visit(ConstructorCall constructorCall) {
        for (Expression a : constructorCall.getArguments()) a.accept(this);
        return new UserDefinedType(constructorCall.getName());
    }

    @Override
    public Type visit(SimpleLoc simpleLoc) {
        return typeOfLocation(simpleLoc);
    }

    @Override
    public Type visit(MemberLoc memberLoc) {
        return typeOfLocation(memberLoc);
    }

    @Override
    public Type visit(ThisLoc thisLoc) {
        return typeOfLocation(thisLoc);
    }

    @Override
    public Type visit(MethodCallLoc methodCallLoc) {
        return methodCallLoc.getMethodCall().accept(this);
    }

    @Override
    public Type visit(MethodCall methodCall) {
        return handleMethodCall(methodCall);
    }

    // ---------------------------------------------------------------------
    // Resolution helpers
    // ---------------------------------------------------------------------

    private VarItem tryGetVar(String name) {
        if (currentScope == null) {
            return null;
        }
        try {
            SymbolTableItem item = currentScope.get(name);
            return item instanceof VarItem ? (VarItem) item : null;
        } catch (ItemNotFoundException e) {
            return null;
        }
    }

    private Type typeOfLocation(Location loc) {
        if (loc instanceof SimpleLoc) {
            VarItem v = tryGetVar(((SimpleLoc) loc).getId().getName());
            return v == null ? null : v.getType();
        }
        if (loc instanceof ThisLoc) {
            ThisLoc t = (ThisLoc) loc;
            if (currentTypeName == null) {
                return null;
            }
            if (t.getLoc() == null) {
                return new UserDefinedType(new Identifier(currentTypeName));
            }
            return memberType(currentTypeName, t.getLoc());
        }
        if (loc instanceof MemberLoc) {
            MemberLoc m = (MemberLoc) loc;
            VarItem base = tryGetVar(m.getMemberName().getName());
            if (base == null) {
                return null;
            }
            return memberType(TypeUtils.name(base.getType()), m.getLoc());
        }
        if (loc instanceof MethodCallLoc) {
            return ((MethodCallLoc) loc).getMethodCall().accept(this);
        }
        return null;
    }

    /** Type of the member chain {@code loc} within {@code typeName}. */
    private Type memberType(String typeName, Location loc) {
        if (loc instanceof SimpleLoc) {
            SymbolTableItem item = na.lookupMember(typeName, ((SimpleLoc) loc).getId().getName());
            if (item instanceof VarItem) {
                return ((VarItem) item).getType();
            }
            return null;
        }
        if (loc instanceof MemberLoc) {
            MemberLoc m = (MemberLoc) loc;
            SymbolTableItem item = na.lookupMember(typeName, m.getMemberName().getName());
            if (item instanceof VarItem) {
                return memberType(TypeUtils.name(((VarItem) item).getType()), m.getLoc());
            }
            return null;
        }
        if (loc instanceof MethodCallLoc) {
            return ((MethodCallLoc) loc).getMethodCall().accept(this);
        }
        return null;
    }

    private Type handleMethodCall(MethodCall mc) {
        // Evaluate the arguments (also type-checks nested calls).
        List<Type> argTypes = new ArrayList<>();
        for (Expression arg : mc.getArguments()) {
            argTypes.add(arg.accept(this));
        }

        MethodItem method = resolveMethodItem(mc);
        if (method == null) {
            return null; // name analysis already reported the problem
        }
        checkArguments(mc, method, argTypes);
        return method.getReturnType();
    }

    private MethodItem resolveMethodItem(MethodCall mc) {
        String callee = mc.getCallee().getName();
        Location inst = mc.getInstance();
        String owner = null;

        if (inst == null) {
            // bare call: current module/struct + includes, then global.
            return na.resolveBareMethod(currentTypeName, callee);
        } else if (inst instanceof SimpleLoc) {
            String iName = ((SimpleLoc) inst).getId().getName();
            VarItem v = tryGetVar(iName);
            if (v != null) {
                owner = TypeUtils.name(v.getType());
            } else if (na.isModuleOrStruct(iName)) {
                owner = iName;
            }
        } else if (inst instanceof ThisLoc) {
            ThisLoc t = (ThisLoc) inst;
            if (t.getLoc() == null) {
                owner = currentTypeName;
            } else {
                Type base = memberType(currentTypeName, t.getLoc());
                owner = base == null ? null : TypeUtils.name(base);
            }
        } else if (inst instanceof MemberLoc) {
            Type base = typeOfLocation(inst);
            owner = base == null ? null : TypeUtils.name(base);
        }

        if (owner == null) {
            return null;
        }
        SymbolTableItem item = na.lookupMember(owner, callee);
        return item instanceof MethodItem ? (MethodItem) item : null;
    }

    private void checkArguments(MethodCall mc, MethodItem method, List<Type> argTypes) {
        List<Type> params = method.getParameterTypes();
        String name = method.getName();
        int line = mc.getLine();

        if (argTypes.size() != params.size()) {
            error(line, "Argument count mismatch for method " + name + ". Expected "
                    + params.size() + ", got " + argTypes.size());
            return;
        }
        for (int i = 0; i < params.size(); i++) {
            Type expected = params.get(i);
            Type actual = argTypes.get(i);
            if (actual != null && !TypeUtils.same(actual, expected)) {
                error(line, "Argument type mismatch for method " + name + ", parameter "
                        + (i + 1) + ". Expected " + TypeUtils.name(expected) + ", got "
                        + TypeUtils.name(actual));
                return; // report only the first mismatching parameter
            }
        }
    }

    // ---------------------------------------------------------------------

    public void printResults() {
        List<String> sortedErrors = new ArrayList<>(errors);
        sortedErrors.sort(Comparator.comparingInt(NameAnalyzer::lineOf));
        for (String e : sortedErrors) {
            System.out.println(e);
        }
    }
}
