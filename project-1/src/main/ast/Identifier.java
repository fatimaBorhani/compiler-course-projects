package main.ast;
import main.visitor.IVisitor;

public class Identifier extends Node {
    private String name;

    public Identifier(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public <T> T accept(IVisitor<T> visitor) {
        return visitor.visit(this);
    }
}