package main.ast;
import main.visitor.IVisitor;
import java.util.ArrayList;

public class ModuleDeclaration extends Node {
    private String name;
    private String includedModule;
    private ArrayList<Node> members = new ArrayList<>();

    public ModuleDeclaration(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setIncludedModule(String includedModule) {
        this.includedModule = includedModule;
    }

    public String getIncludedModule() {
        return includedModule;
    }

    public void addMember(Node member) {
        this.members.add(member);
    }

    public ArrayList<Node> getMembers() {
        return members;
    }

    @Override
    public <T> T accept(IVisitor<T> visitor) {
        return visitor.visit(this);
    }
}