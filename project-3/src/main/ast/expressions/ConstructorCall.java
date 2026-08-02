package main.ast.expressions;

import main.ast.types.Identifier;
import main.visitor.IVisitor;

import java.util.List;
import java.util.ArrayList;

public class ConstructorCall extends Expression {
    private Identifier name;
    private List<Expression> arguments;

    public ConstructorCall(Identifier name) {
        this.name = name;
        this.arguments = new ArrayList<>();
    }

    public void addArgument(Expression argument) {
        arguments.add(argument);
    }

    @Override
    public <T> T accept(IVisitor<T> visitor) {
        return visitor.visit(this);
    }

    public Identifier getName() {
        return name;
    }

    public void setName(Identifier name) {
        this.name = name;
    }

    public List<Expression> getArguments() {
        return arguments;
    }

    public void setArguments(List<Expression> arguments) {
        this.arguments = arguments;
    }
}