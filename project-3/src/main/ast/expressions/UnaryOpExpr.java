package main.ast.expressions;

import main.visitor.IVisitor;

public class UnaryOpExpr extends Expression {
    public enum Operator { NOT, MINUS }

    private Operator operator;
    private Expression operand;

    public UnaryOpExpr(Operator operator, Expression operand) {
        this.operator = operator;
        this.operand = operand;
    }

    @Override
    public <T> T accept(IVisitor<T> visitor) {
        return visitor.visit(this);
    }

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    public Expression getOperand() {
        return operand;
    }

    public void setOperand(Expression operand) {
        this.operand = operand;
    }
}