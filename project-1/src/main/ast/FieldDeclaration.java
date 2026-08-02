package main.ast;
import main.visitor.IVisitor;

public class FieldDeclaration extends Node {
    private String type;
    private String name;
    private boolean isMutable;

    public FieldDeclaration(String type, String name, boolean isMutable) {
        this.type = type;
        this.name = name;
        this.isMutable = isMutable;
    }

    public String getType() { return type; }
    public String getName() { return name; }
    public boolean isMutable() { return isMutable; }

    @Override
    public <T> T accept(IVisitor<T> visitor) { return visitor.visit(this); }
}