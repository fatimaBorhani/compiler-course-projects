package main.ast;
import main.visitor.IVisitor;
import java.util.ArrayList;

public class IfStatement extends Node {
    private Node condition;
    private ArrayList<Node> thenBody = new ArrayList<>();
    private ArrayList<Node> elseBody = new ArrayList<>();
    // برای سادگی elifها را می‌توان در پیاده‌سازی‌های پیشرفته‌تر اضافه کرد

    public IfStatement(Node condition) { this.condition = condition; }
    public void addThenStatement(Node stmt) { this.thenBody.add(stmt); }
    public void addElseStatement(Node stmt) { this.elseBody.add(stmt); }
    
    public Node getCondition() { return condition; }
    public ArrayList<Node> getThenBody() { return thenBody; }
    public ArrayList<Node> getElseBody() { return elseBody; }

    @Override
    public <T> T accept(IVisitor<T> visitor) { return visitor.visit(this); }
}