package main.ast.types;

import main.visitor.IVisitor;

public class IntType extends Type {
    @Override
    public <T> T accept(IVisitor<T> visitor) {
        return visitor.visit(this);
    }
}