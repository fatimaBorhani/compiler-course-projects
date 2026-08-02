package main.ast.statements;

import main.ast.expressions.MethodCall;
import main.visitor.IVisitor;

public class MethodCallStmt extends Statement {
    private MethodCall methodCall;

    public MethodCallStmt(MethodCall methodCall) {
        this.methodCall = methodCall;
    }

    @Override
    public <T> T accept(IVisitor<T> visitor) {
        return visitor.visit(this);
    }

    public MethodCall getMethodCall() {
        return methodCall;
    }

    public void setMethodCall(MethodCall methodCall) {
        this.methodCall = methodCall;
    }
}