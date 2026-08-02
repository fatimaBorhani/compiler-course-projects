package main.ast.expressions;

import main.ast.types.Identifier;
import main.visitor.IVisitor;

public class MemberLoc extends Location {
    private Identifier member;
    private Location instance;

    public MemberLoc(Identifier member, Location instance) {
        this.member = member;
        this.instance = instance;
    }

    public Identifier getMember() { return member; }
    public Location getInstance() { return instance; }

    @Override
    public <T> T accept(IVisitor<T> visitor) {
        return visitor.visit(this);
    }
}