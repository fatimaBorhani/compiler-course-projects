package main.ast.types;

import main.visitor.IVisitor;

public class StructType extends Type {
    private Identifier name;

    public StructType(Identifier name) {
        this.name = name;
    }

    public Identifier getName() {
        return name;
    }

    public void setName(Identifier name) {
        this.name = name;
    }

    @Override
    public <T> T accept(IVisitor<T> visitor) {
        return visitor.visit(this);
    }
}