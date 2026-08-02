package main.ast.expressions;

import main.visitor.IVisitor;

public class ParanthesisExpr extends Expression {
    private Expression expression;

    public ParanthesisExpr(Expression expression) {
        this.expression = expression;
    }

    @Override
    public <T> T accept(IVisitor<T> visitor) {
        return visitor.visit(this);
    }

    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }
}