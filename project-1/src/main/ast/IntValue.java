package main.ast;
import main.visitor.IVisitor;

public class IntValue extends Node {
    private int value;

    public IntValue(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public <T> T accept(IVisitor<T> visitor) {
        return visitor.visit(this);
    }
}