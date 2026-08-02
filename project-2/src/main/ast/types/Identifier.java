package main.ast.types;

import main.ast.expressions.Expression;
import main.visitor.IVisitor;

public class Identifier extends Expression {
    private String name;

    public Identifier(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public <T> T accept(IVisitor<T> visitor) {
        // فعلا نیازی به ویزیت کردن مستقیم Identifier در این پروژه نیست، اما برای رعایت اصول مینویسیم
        return visitor.visit(this); 
    }
}