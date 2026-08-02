package main.ast.expressions;

import main.ast.expressions.operators.*;
import main.visitor.IVisitor;

public class BinaryExpression extends Expression {
    private Expression leftOperand;
    private BinaryOperator Operator;
    private Expression rightOperand;

    public BinaryExpression(Expression leftOperand, String Operator, Expression rightOperand) {
        this.leftOperand = leftOperand;
        this.Operator = BinaryOperator.fromString(Operator);
        this.rightOperand = rightOperand;
    }

    public Expression getLeftOperand() {
        return leftOperand;
    }

    public void setLeftOperand(Expression leftOperand) {
        this.leftOperand = leftOperand;
    }

    public BinaryOperator getOperator() {
        return Operator;
    }

    public void setOperator(BinaryOperator operator) {
        this.Operator = operator;
    }

    public Expression getRightOperand() {
        return rightOperand;
    }

    public void setRightOperand(Expression rightOperand) {
        this.rightOperand = rightOperand;
    }

    @Override
    public <T> T accept(IVisitor<T> visitor) {
        return visitor.visit(this);
    }
}