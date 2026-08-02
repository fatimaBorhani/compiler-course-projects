package main.symbolTable;

import main.symbolTable.exceptions.ItemAlreadyExistsException;
import main.symbolTable.exceptions.ItemNotFoundException;
import main.symbolTable.items.SymbolTableItem;

import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
    private SymbolTable pre; 
    private Map<String, SymbolTableItem> items;

    
    public static SymbolTable top;
    public static SymbolTable root;

    public SymbolTable() {
        this(null);
    }

    public SymbolTable(SymbolTable pre) {
        this.pre = pre;
        this.items = new HashMap<>();
    }

    
    public void put(SymbolTableItem item) throws ItemAlreadyExistsException {
        if (items.containsKey(item.getName())) {
            throw new ItemAlreadyExistsException();
        }
        items.put(item.getName(), item);
    }

    
    public SymbolTableItem get(String key) throws ItemNotFoundException {
        SymbolTableItem item = items.get(key);
        if (item != null) {
            return item;
        }
        if (pre != null) {
            return pre.get(key);
        }
        throw new ItemNotFoundException();
    }

    
    public static void push(SymbolTable symbolTable) {
        if (top != null) {
            symbolTable.pre = top;
        }
        top = symbolTable;
    }

    public static void pop() {
        if (top != null) {
            top = top.pre;
        }
    }
}