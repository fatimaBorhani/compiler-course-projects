package main.ast.types;
import main.visitor.IVisitor;
public class BoolType extends Type {
    @Override
    public <T> T accept(IVisitor<T> visitor) { return visitor.visit(this); }
}