package main.ast.statements;

import main.visitor.IVisitor;

public class ForStmt extends Statement {
    @Override
    public <T> T accept(IVisitor<T> visitor) {
      
        if (visitor instanceof Visitor) {
           
        }
        return null;
    }
}