package main.ast.expressions;

import main.ast.types.Identifier;
import main.visitor.IVisitor;

public class MemberLoc extends Location {
    private Identifier memberName;
    private Location loc;

    public MemberLoc(Identifier memberName, Location loc) {
        this.memberName = memberName;
        this.loc = loc;
    }

    @Override
    public <T> T accept(IVisitor<T> visitor) {
        return visitor.visit(this);
    }

    public Identifier getMemberName() {
        return memberName;
    }

    public void setMemberName(Identifier memberName) {
        this.memberName = memberName;
    }

    public Location getLoc() {
        return loc;
    }

    public void setLoc(Location loc) {
        this.loc = loc;
    }
}