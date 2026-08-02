package main.ast.expressions;

import main.visitor.IVisitor;

public class MethodCallLoc extends Location {
    private MethodCall methodCall;

    public MethodCallLoc(MethodCall methodCall) {
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