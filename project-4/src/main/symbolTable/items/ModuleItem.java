package main.symbolTable.items;

import main.symbolTable.SymbolTable;
import main.ast.declarations.Module;

public class ModuleItem extends SymbolTableItem {
    private final SymbolTable moduleSymbolTable;
    private Module module;

    public ModuleItem(String name) {
        this.name = name;
        this.moduleSymbolTable = new SymbolTable();
    }

    public SymbolTable getModuleSymbolTable() { return moduleSymbolTable; }

    public Module getModule() { return module; }
    public void setModule(Module module) { this.module = module; }
}
