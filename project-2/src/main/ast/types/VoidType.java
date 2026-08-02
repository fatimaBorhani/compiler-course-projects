package main.ast.types;
import main.visitor.IVisitor;
public class VoidType extends Type {
    @Override
    public <T> T accept(IVisitor<T> visitor) { return visitor.visit(this); }
}