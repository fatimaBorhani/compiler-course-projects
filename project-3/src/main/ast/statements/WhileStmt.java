package main.ast.statements;

import main.ast.expressions.Expression;
import main.visitor.IVisitor;

public class WhileStmt extends Statement {
    private Expression condition;
    private Statement body;

    public WhileStmt(Expression condition, Statement body) {
        this.condition = condition;
        this.body = body;
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

    public Statement getBody() {
        return body;
    }

    public void setBody(Statement body) {
        this.body = body;
    }
}