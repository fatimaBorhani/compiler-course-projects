package main.ast.core;

import main.ast.declarations.TopLevelDecl;
import main.visitor.IVisitor;

import java.util.List;
import java.util.ArrayList;

public class Program extends Node {
    private List<TopLevelDecl> topLevelDeclarations;

    public Program() {
        this.topLevelDeclarations = new ArrayList<>();
    }

    public void addTopLevelDeclaration(TopLevelDecl topLevelDeclaration) {
        topLevelDeclarations.add(topLevelDeclaration);
    }

    @Override
    public <T> T accept(IVisitor<T> visitor) {
        return visitor.visit(this);
    }

    public List<TopLevelDecl> getTopLevelDeclarations() {
        return topLevelDeclarations;
    }

    public void setTopLevelDeclarations(List<TopLevelDecl> topLevelDeclarations) {
        this.topLevelDeclarations = topLevelDeclarations;
    }
}