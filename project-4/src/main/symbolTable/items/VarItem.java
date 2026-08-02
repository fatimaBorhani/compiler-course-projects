package main.symbolTable.items;

import main.ast.types.Type;

public class VarItem extends SymbolTableItem {
    private Type type;
    private boolean isMut;
    private boolean isInitialized;
    private String visibility; 

    public VarItem(String name, Type type, boolean isMut, String visibility) {
        this.name = name;
        this.type = type;
        this.isMut = isMut;
        this.isInitialized = false; 
        this.visibility = visibility;
    }

    
    public Type getType() { return type; }
    public boolean isMut() { return isMut; }
    public boolean isInitialized() { return isInitialized; }
    public void setInitialized(boolean initialized) { isInitialized = initialized; }
    public String getVisibility() { return visibility; }
}