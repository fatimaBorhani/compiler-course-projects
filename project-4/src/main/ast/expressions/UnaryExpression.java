package main.ast.expressions;

import main.visitor.IVisitor;
import main.ast.expressions.operators.*;

public class UnaryExpression extends Expression{
    public UnaryExpression(String operand, Expression expression) {
        this.expression = expression;
        this.operand = UnaryOperator.fromString(operand);
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    UnaryOperator operand;

    public Expression getExpression() {
        return expression;
    }

    public UnaryOperator getOperand() {
        return operand;
    }

    public void setOperand(UnaryOperator operand) {
        this.operand = operand;
    }

    private Expression expression;

    @Override
    public <T> T accept(IVisitor<T> visitor) {
        return visitor.visit(this);
    }
}