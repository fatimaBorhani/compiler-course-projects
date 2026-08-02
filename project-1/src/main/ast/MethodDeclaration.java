package main.ast;
import main.visitor.IVisitor;
import java.util.ArrayList;

public class MethodDeclaration extends Node {
    private String name;
    private String returnType;
    private ArrayList<Node> body = new ArrayList<>();

    public MethodDeclaration(String name, String returnType) {
        this.name = name;
        this.returnType = returnType;
    }

    public void addStatement(Node stmt) { this.body.add(stmt); }
    public String getName() { return name; }
    public ArrayList<Node> getBody() { return body; }

    @Override
    public <T> T accept(IVisitor<T> visitor) { return visitor.visit(this); }
}