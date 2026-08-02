package main.ast.statements;

import main.ast.expressions.Location;
import main.visitor.IVisitor;

public class InputStmt extends Statement {
    private Location loc;

    public InputStmt(Location loc) {
        this.loc = loc;
    }

    @Override
    public <T> T accept(IVisitor<T> visitor) {
        return visitor.visit(this);
    }

    public Location getLoc() {
        return loc;
    }

    public void setLoc(Location loc) {
        this.loc = loc;
    }
}