package de.unistuttgart.stayinsync.syncnode.logik_engine;

public enum LogicOperator {
    // Arithmetic Operators
    ADD("+"),
    SUBTRACT("-"),
    MULTIPLY("*"),
    DIVIDE("/"),
    MODULO("%"),

    // Relational Operators
    GREATER_THAN(">"),
    LESS_THAN("<"),
    GREATER_EQUAL(">="),
    LESS_EQUAL("<="),
    EQUALS("=="),
    NOT_EQUALS("!="),

    // Logic Operator
    AND("&&"),
    OR("||");

    private final String symbol;

    LogicOperator(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}
