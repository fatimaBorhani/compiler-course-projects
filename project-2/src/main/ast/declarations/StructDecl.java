package main.ast.declarations;

import main.visitor.IVisitor;

public class StructDecl extends TopLevelDecl {
    private Struct struct;

    public StructDecl(Struct struct) {
        this.struct = struct;
    }

    @Override
    public <T> T accept(IVisitor<T> visitor) {
        return visitor.visit(this);
    }

    public Struct getStruct() {
        return struct;
    }

    public void setStruct(Struct struct) {
        this.struct = struct;
    }
}