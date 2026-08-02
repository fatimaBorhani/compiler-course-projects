package main.ast;
import main.visitor.IVisitor;
import java.util.ArrayList;

public class WhileStatement extends Node {
    private Node condition;
    private ArrayList<Node> body = new ArrayList<>();

    public WhileStatement(Node condition) { this.condition = condition; }
    public void addStatement(Node stmt) { this.body.add(stmt); }
    public Node getCondition() { return condition; }
    public ArrayList<Node> getBody() { return body; }

    @Override
    public <T> T accept(IVisitor<T> visitor) { return visitor.visit(this); }
}