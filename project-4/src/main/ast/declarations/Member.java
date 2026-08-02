package main.ast.declarations;

import main.ast.core.Node;
import main.ast.types.AccessModifier;

public abstract class Member extends Node {
    private AccessModifier accessModifier;

    public Member() {}

    public AccessModifier getAccessModifier() {
        return accessModifier;
    }

    public void setAccessModifier(AccessModifier accessModifier) {
        this.accessModifier = accessModifier;
    }
}