package main.ast.statements;

import main.visitor.IVisitor;

public class ContinueJump extends JumpStmt {
    public ContinueJump() {}

    @Override
    public <T> T accept(IVisitor<T> visitor) {
        return visitor.visit(this);
    }
}