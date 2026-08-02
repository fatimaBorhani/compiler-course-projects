package main.ast;
import main.visitor.IVisitor;
import java.util.ArrayList;

public class StructDeclaration extends Node {
    private String name;
    private ArrayList<FieldDeclaration> fields = new ArrayList<>();

    public StructDeclaration(String name) { this.name = name; }
    public void addField(FieldDeclaration field) { this.fields.add(field); }
    public String getName() { return name; }
    public ArrayList<FieldDeclaration> getFields() { return fields; }

    @Override
    public <T> T accept(IVisitor<T> visitor) { return visitor.visit(this); }
}