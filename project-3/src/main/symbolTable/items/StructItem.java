package main.symbolTable.items;

import main.symbolTable.SymbolTable;
import main.ast.declarations.Struct;

public class StructItem extends SymbolTableItem {
    private final SymbolTable structSymbolTable;
    private Struct struct;

    public StructItem(String name) {
        this.name = name;
        this.structSymbolTable = new SymbolTable();
    }

    public SymbolTable getStructSymbolTable() { return structSymbolTable; }

    public Struct getStruct() { return struct; }
    public void setStruct(Struct struct) { this.struct = struct; }
}
