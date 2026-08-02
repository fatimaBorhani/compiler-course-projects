package main.ast.declarations;

import main.ast.core.Node;
import main.ast.types.Identifier;
import main.visitor.IVisitor;

import java.util.List;
import java.util.ArrayList;

public class Struct extends TopLevelDecl {
    private Identifier name;
    private List<Member> members;

    public Struct(Identifier name) {
        this.name = name;
        this.members = new ArrayList<>();
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

    public List<Member> getMembers() {
        return members;
    }

    public void setMembers(List<Member> members) {
        this.members = members;
    }
}