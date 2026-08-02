package main.ast.declarations;

import main.ast.core.Node;
import main.ast.types.Identifier;
import main.visitor.IVisitor;

import java.util.List;
import java.util.ArrayList;

public class Module extends TopLevelDecl {
    private Identifier name;
    private List<Identifier> includes;
    private List<Member> members;

    public Module(Identifier name) {
        this.name = name;
        this.includes = new ArrayList<>();
        this.members = new ArrayList<>();
    }

    public void addInclude(Identifier include) {
        includes.add(include);
    }

    public void addMember(Member member) {
        members.add(member);
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

    public List<Identifier> getIncludes() {
        return includes;
    }

    public void setIncludes(List<Identifier> includes) {
        this.includes = includes;
    }

    public List<Member> getMembers() {
        return members;
    }

    public void setMembers(List<Member> members) {
        this.members = members;
    }
}