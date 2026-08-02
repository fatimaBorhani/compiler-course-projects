package main.ast.declarations;

import main.ast.core.Node;
import main.ast.types.Type;
import main.ast.types.Identifier;
import main.visitor.IVisitor;

public class Parameter extends Node {
    private boolean isMutable;
    private Type type;
    private Identifier name;

    public Parameter(boolean isMutable, Type type, Identifier name) {
        this.isMutable = isMutable;
        this.type = type;
        this.name = name;
    }

    @Override
    public <T> T accept(IVisitor<T> visitor) {
        return visitor.visit(this);
    }

    public boolean getIsMutable() {
        return isMutable;
    }

    public void setIsMutable(boolean isMutable) {
        this.isMutable = isMutable;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Identifier getName() {
        return name;
    }

    public void setName(Identifier name) {
        this.name = name;
    }
}