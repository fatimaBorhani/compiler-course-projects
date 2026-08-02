package main.ast.expressions;

import main.ast.types.Identifier;
import main.visitor.IVisitor;

public class SimpleLoc extends Location {
    private Identifier id;

    public SimpleLoc(Identifier id) {
        this.id = id;
    }

    @Override
    public <T> T accept(IVisitor<T> visitor) {
        return visitor.visit(this);
    }

    public Identifier getId() {
        return id;
    }

    public void setId(Identifier id) {
        this.id = id;
    }
}