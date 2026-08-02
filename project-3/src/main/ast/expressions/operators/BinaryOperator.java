package main.ast.expressions.operators;

public enum BinaryOperator {
    // Define the enum values with their string representations
    MULTIPLICATION("*"),
    DIVISION("/"),
    ADDITION("+"),
    SUBTRACTION("-"),
    LESS_THAN("<"),
    GREATER_THAN(">"),
    LESS_THAN_OR_EQUAL_TO("<="),
    GREATER_THAN_OR_EQUAL_TO(">="),
    EQUALITY("=="),
    INEQUALITY("!="),
    AND("and"),
    OR("or");

    // Field to store the string representation
    private final String symbol;

    // Constructor to initialize the symbol
    BinaryOperator(String symbol) {
        this.symbol = symbol;
    }

    // Method to get the symbol
    public String getSymbol() {
        return this.symbol;
    }

    // Static method to get the enum by string symbol
    public static BinaryOperator fromString(String symbol) {
        for (BinaryOperator operator : BinaryOperator.values()) {
            if (operator.getSymbol().equals(symbol)) {
                return operator;
            }
        }
        throw new IllegalArgumentException("Unknown operator: " + symbol);
    }
}