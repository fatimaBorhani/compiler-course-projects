package main.symbolTable.items;

import main.ast.types.Type;
import main.ast.declarations.Method;
import java.util.ArrayList;

public class MethodItem extends SymbolTableItem {
    private final Type returnType;
    private final ArrayList<Type> parameterTypes;
    private final String visibility; // "public" / "private"
    private Method method;

    public MethodItem(String name, Type returnType, String visibility) {
        this.name = name;
        this.returnType = returnType;
        this.parameterTypes = new ArrayList<>();
        this.visibility = visibility;
    }

    public void addParameterType(Type type) {
        this.parameterTypes.add(type);
    }

    public Type getReturnType() { return returnType; }
    public ArrayList<Type> getParameterTypes() { return parameterTypes; }
    public String getVisibility() { return visibility; }

    public Method getMethod() { return method; }
    public void setMethod(Method method) { this.method = method; }
}
