package main.ast.statements;

import main.visitor.IVisitor;

public class BreakJump extends JumpStmt {
    public BreakJump() {}

    @Override
    public <T> T accept(IVisitor<T> visitor) {
        return visitor.visit(this);
    }
}