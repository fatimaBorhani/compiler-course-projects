package main.ast.statements;

import main.ast.expressions.Expression;
import main.visitor.IVisitor;

public class ReturnStmt extends Statement {
    private  Expression value;

    public ReturnStmt() {}

    @Override
    public <T> T accept(IVisitor<T> visitor) {
        return visitor.visit(this);
    }

    public Expression getValue() {
        return value;
    }

    public void setValue(Expression value) {
        this.value = value;
    }
}