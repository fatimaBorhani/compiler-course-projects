package main.ast.statements;

import main.ast.expressions.Expression;
import main.ast.expressions.Location;
import main.visitor.IVisitor;

public class AssignStmt extends Statement {
    private Location left;
    private Expression right;

    public AssignStmt(Location left, Expression right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public <T> T accept(IVisitor<T> visitor) {
        return visitor.visit(this);
    }

    public Location getLeft() {
        return left;
    }

    public void setLeft(Location left) {
        this.left = left;
    }

    public Expression getRight() {
        return right;
    }

    public void setRight(Expression right) {
        this.right = right;
    }
}