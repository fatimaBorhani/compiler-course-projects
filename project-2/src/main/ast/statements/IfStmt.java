package main.ast.statements;

import main.ast.expressions.Expression;
import main.visitor.IVisitor;

public class IfStmt extends Statement {
    private Expression condition;
    private Statement thenBranch;
    private Statement elseBranch;

    public IfStmt(Expression condition, Statement thenBranch) {
        this.condition = condition;
        this.thenBranch = thenBranch;
    }

    @Override
    public <T> T accept(IVisitor<T> visitor) {
        return visitor.visit(this);
    }

    public Expression getCondition() {
        return condition;
    }

    public void setCondition(Expression condition) {
        this.condition = condition;
    }

    public Statement getThenBranch() {
        return thenBranch;
    }

    public void setThenBranch(Statement thenBranch) {
        this.thenBranch = thenBranch;
    }

    public Statement getElseBranch() {
        return elseBranch;
    }

    public void setElseBranch(Statement elseBranch) {
        this.elseBranch = elseBranch;
    }
}