package main.ast;
import main.visitor.IVisitor;

public class OutputStatement extends Node {
    private Node expression;

    public OutputStatement(Node expression) { this.expression = expression; }
    public Node getExpression() { return expression; }

    @Override
    public <T> T accept(IVisitor<T> visitor) { return visitor.visit(this); }
}