package main.ast;
import main.visitor.IVisitor;
import java.util.ArrayList;

public class MethodCall extends Node {
    private Node instance; // مثل this یا نام یک شیء
    private String methodName;
    private ArrayList<Node> arguments = new ArrayList<>();

    public MethodCall(Node instance, String methodName) {
        this.instance = instance;
        this.methodName = methodName;
    }

    public void addArgument(Node arg) { this.arguments.add(arg); }
    public Node getInstance() { return instance; }
    public String getMethodName() { return methodName; }
    public ArrayList<Node> getArguments() { return arguments; }

    @Override
    public <T> T accept(IVisitor<T> visitor) { return visitor.visit(this); }
}