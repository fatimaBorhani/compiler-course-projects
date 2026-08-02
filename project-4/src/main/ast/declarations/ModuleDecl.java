package main.ast.declarations;

import main.visitor.IVisitor;

public class ModuleDecl extends TopLevelDecl {
    private Module module;

    public ModuleDecl(Module module) {
        this.module = module;
    }

    @Override
    public <T> T accept(IVisitor<T> visitor) {
        return visitor.visit(this);
    }

    public Module getModule() {
        return module;
    }

    public void setModule(Module module) {
        this.module = module;
    }
}