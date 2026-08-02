package main.ast.expressions;

import main.ast.types.Identifier;
import main.visitor.IVisitor;
import java.util.ArrayList;
import java.util.List;

public class ConstructorCall extends Expression {
    private Identifier structName;
    private List<Expression> args = new ArrayList<>();

    public ConstructorCall(Identifier structName) {
        this.structName = structName;
    }

    public void addArgument(Expression arg) {
        this.args.add(arg);
    }

    public Identifier getStructName() { return structName; }
    public List<Expression> getArgs() { return args; }

    @Override
    public <T> T accept(IVisitor<T> visitor) {
        return visitor.visit(this);
    }
}