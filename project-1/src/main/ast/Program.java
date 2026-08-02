package main.ast;
import main.visitor.IVisitor;
import java.util.ArrayList;

public class Program extends Node {
    private ArrayList<Node> declarations = new ArrayList<>();

    public void addDeclaration(Node declaration) {
        this.declarations.add(declaration);
    }

  
    public ArrayList<Node> getDeclarations() {
        return declarations;
    }

    @Override
    public <T> T accept(IVisitor<T> visitor) {
        return visitor.visit(this);
    }
}