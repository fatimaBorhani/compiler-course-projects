package main.ast.core;

import main.visitor.IVisitor;

public abstract class Node {
    private int line;

    public Node() {}

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public abstract <T> T accept(IVisitor<T> visitor);
}
