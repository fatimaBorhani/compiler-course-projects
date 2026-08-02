package main.ast.types;

import main.visitor.IVisitor;

public class UserDefinedType extends Type {
    private Identifier id;

    public UserDefinedType(Identifier id) {
        this.id = id;
    }

    @Override
    public <T> T accept(IVisitor<T> visitor) {
        return visitor.visit(this);
    }

    public Identifier getId() {
        return id;
    }

    public void setId(Identifier id) {
        this.id = id;
    }

    public String getStr() {
        return id.getName();
    }
}