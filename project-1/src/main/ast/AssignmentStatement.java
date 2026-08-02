package main.ast;
import main.visitor.IVisitor;

public class AssignmentStatement extends Node {
    private String varName;
    private Node expression;

    public AssignmentStatement(String varName, Node expression) {
        this.varName = varName;
        this.expression = expression;
    }

    public String getVarName() { return varName; }
    public Node getExpression() { return expression; }

    @Override
    public <T> T accept(IVisitor<T> visitor) { return visitor.visit(this); }
}