package main.ast;
import main.visitor.IVisitor;

public class BinaryExpression extends Node {
    private Node left;
    private Node right;
    private String operator;

    public BinaryExpression(Node left, Node right, String operator) {
        this.left = left;
        this.right = right;
        this.operator = operator;
    }

    public Node getLeft() { return left; }
    public Node getRight() { return right; }
    public String getOperator() { return operator; }

    @Override
    public <T> T accept(IVisitor<T> visitor) { return visitor.visit(this); }
}