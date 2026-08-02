package main.ast.expressions;

import main.ast.types.Identifier;
import main.visitor.IVisitor;

public class SimpleLoc extends Location {
    private Identifier variable;

    public SimpleLoc(Identifier variable) {
        this.variable = variable;
    }

    public Identifier getVariable() { return variable; }
    public void setVariable(Identifier variable) { this.variable = variable; }

    @Override
    public <T> T accept(IVisitor<T> visitor) {
        return visitor.visit(this);
    }
}