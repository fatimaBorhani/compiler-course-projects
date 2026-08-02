package main.ast.types;

import main.visitor.IVisitor;

public class PrimitiveType extends Type {
    public enum Primitive { INT, FLOAT, DOUBLE, CHAR, BOOL, VOID }

    private Primitive primitive;

    public PrimitiveType(Primitive primitive) {
        this.primitive = primitive;
    }

    @Override
    public <T> T accept(IVisitor<T> visitor) {
        return visitor.visit(this);
    }

    public Primitive getPrimitive() {
        return primitive;
    }

    public void setPrimitive(Primitive primitive) {
        this.primitive = primitive;
    }

    public String getStr() {
        return this.primitive.toString().toLowerCase();
    }
}