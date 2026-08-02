package main.ast.declarations;

import main.ast.core.Node;
import main.ast.types.Type;
import main.ast.types.Identifier;
import main.ast.expressions.ConstructorCall;
import main.visitor.IVisitor;

public class Var extends Node {
    private boolean isMutable;
    private Identifier name;
    private Type type;
    private ConstructorCall constructorCall;

    public Var(boolean isMutable, Type type) {
        this.isMutable = isMutable;
        this.type = type;
    }

    public Var(boolean isMutable, ConstructorCall constructorCall) {
        this.isMutable = isMutable;
        this.constructorCall = constructorCall;
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

    public ConstructorCall getConstructorCall() {
        return constructorCall;
    }

    public void setConstructorCall(ConstructorCall constructorCall) {
        this.constructorCall = constructorCall;
    }
}