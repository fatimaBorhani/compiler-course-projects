package main.ast.statements;

import main.visitor.IVisitor;

import java.util.ArrayList;
import java.util.List;

public class Block extends Statement {
    private List<Statement> statements;

    public Block() {
        this.statements = new ArrayList<>();
    }

    public void addStatement(Statement statement) {
        statements.add(statement);
    }

    @Override
    public <T> T accept(IVisitor<T> visitor) {
        return visitor.visit(this);
    }

    public List<Statement> getStatements() {
        return statements;
    }

    public void setStatements(List<Statement> statements) {
        this.statements = statements;
    }
}