package main.ast.declarations;

import main.ast.core.Node;
import main.ast.types.Type;
import main.ast.types.Identifier;
import main.ast.statements.Block;
import main.visitor.IVisitor;

import java.util.List;
import java.util.ArrayList;

public class Method extends Node {
    private Type returnType;
    private Identifier name;
    private List<Parameter> parameters;
    private Block body;

    public Method(Type returnType, Identifier name, List<Parameter> parameters, Block body) {
        this.returnType = returnType;
        this.name = name;
        this.parameters = new ArrayList<>(parameters);
        this.body = body;
    }

    public void addParameter(Parameter parameter) {
        parameters.add(parameter);
    }

    @Override
    public <T> T accept(IVisitor<T> visitor) {
        return visitor.visit(this);
    }

    public Type getReturnType() {
        return returnType;
    }

    public void setReturnType(Type returnType) {
        this.returnType = returnType;
    }

    public Identifier getName() {
        return name;
    }

    public void setName(Identifier name) {
        this.name = name;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    public Block getBody() {
        return body;
    }

    public void setBody(Block body) {
        this.body = body;
    }
}