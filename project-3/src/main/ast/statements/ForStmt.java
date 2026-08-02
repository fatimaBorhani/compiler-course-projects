package main.ast.statements;

import main.ast.expressions.Expression;
import main.visitor.IVisitor;

import java.util.List;
import java.util.ArrayList;

public class ForStmt extends Statement {
    private List<Statement> initializers;
    private Expression condition;
    private List<AssignStmt> updaters;
    private Statement body;

    public ForStmt() {
        this.initializers = new ArrayList<>();
        this.updaters = new ArrayList<>();
    }

    public void addInitializer(Statement initializer) {
        initializers.add(initializer);
    }

    public void addUpdater(AssignStmt updater) {
        updaters.add(updater);
    }

    @Override
    public <T> T accept(IVisitor<T> visitor) {
        return visitor.visit(this);
    }

    public List<Statement> getInitializers() {
        return initializers;
    }

    public void setInitializers(List<Statement> initializers) {
        this.initializers = initializers;
    }

    public Expression getCondition() {
        return condition;
    }

    public void setCondition(Expression condition) {
        this.condition = condition;
    }

    public List<AssignStmt> getUpdaters() {
        return updaters;
    }

    public void setUpdaters(List<AssignStmt> updaters) {
        this.updaters = updaters;
    }

    public Statement getBody() {
        return body;
    }

    public void setBody(Statement body) {
        this.body = body;
    }
}