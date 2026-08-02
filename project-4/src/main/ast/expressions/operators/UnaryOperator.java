package main.ast.expressions.operators;

public enum UnaryOperator {
    // Define the enum values with their string representations
    MINUS("-"),
    NOT("not");

    // Field to store the string representation
    private final String symbol;

    // Constructor to initialize the symbol
    UnaryOperator(String symbol) {
        this.symbol = symbol;
    }

    // Method to get the symbol
    public String getSymbol() {
        return this.symbol;
    }

    // Static method to get the enum by string symbol
    public static UnaryOperator fromString(String symbol) {
        for (UnaryOperator operator : UnaryOperator.values()) {
            if (operator.getSymbol().equals(symbol)) {
                return operator;
            }
        }
        throw new IllegalArgumentException("Unknown operator: " + symbol);
    }
}