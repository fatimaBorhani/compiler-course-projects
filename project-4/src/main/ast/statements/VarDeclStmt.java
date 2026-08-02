package main.ast.statements;

import main.ast.declarations.Var;
import main.ast.expressions.Expression;
import main.visitor.IVisitor;

public class VarDeclStmt extends Statement {
    private Var var;
    private Expression initial;

    public VarDeclStmt(Var var) {
        this.var = var;
    }

    @Override
    public <T> T accept(IVisitor<T> visitor) {
        return visitor.visit(this);
    }

    public Var getVar() {
        return var;
    }

    public void setVar(Var var) {
        this.var = var;
    }

    public Expression getInitial() {
        return initial;
    }

    public void setInitial(Expression initial) {
        this.initial = initial;
    }
}