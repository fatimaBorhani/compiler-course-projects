package main.ast.expressions;

import main.visitor.IVisitor;

public class ConstantExpression extends Expression{
    private Object value;

    public ConstantExpression(int value) {
        this.value = value;
    }

    public ConstantExpression(float value) {
        this.value = value;
    }

    public ConstantExpression(double value) {
        this.value = value;
    }

    public ConstantExpression(char value) {
        this.value = value;
    }

    public ConstantExpression(boolean value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public <T> T accept(IVisitor<T> visitor) {
        return visitor.visit(this);
    }
}