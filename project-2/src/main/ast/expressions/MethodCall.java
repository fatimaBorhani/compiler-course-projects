package main.ast.expressions;

import main.ast.types.Identifier;
import main.visitor.IVisitor;
import java.util.ArrayList;
import java.util.List;

public class MethodCall extends Expression {
    private Location instance; 
    private Identifier callee;
    private List<Expression> args = new ArrayList<>();

    public MethodCall() {
    }

    public void setInstance(Location instance) { this.instance = instance; }
    public void setCallee(Identifier callee) { this.callee = callee; }
    public void addArgument(Expression arg) { this.args.add(arg); }

    public Location getInstance() { return instance; }
    public Identifier getCallee() { return callee; }
    public List<Expression> getArgs() { return args; }

    @Override
    public <T> T accept(IVisitor<T> visitor) {
        return visitor.visit(this);
    }
}