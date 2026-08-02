package main.ast.types;

import main.ast.core.Node;
import main.visitor.IVisitor;

public class Identifier extends Node {
    private String name;

    public Identifier(String name) {
        this.name = name;
    }

    @Override
    public <T> T accept(IVisitor<T> visitor) {
        return visitor.visit(this);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}