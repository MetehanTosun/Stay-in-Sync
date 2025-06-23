package de.unistuttgart.stayinsync.syncnode.logik_engine.LogicOperator;

import de.unistuttgart.stayinsync.syncnode.logik_engine.LogicOperator.generell_predicates.*;

public enum LogicOperator {
    // Generall predicates
    EXISTS("exists", new ExistsOperator()),
    NOT_EXISTS("notExists", new NotExistsOperator()),
    IS_NULL("isNull", new IsNull()),
    ISNOT_NULL("isNotNull", new IsNotNull()),
    TYPE_IS("typeIs", new TypeIsOperator()),
    EQUALS("equals", new EqualsOperator()),
    NOT_EQUALS("notEquals", new NotEqualsOperator()),
    IN_SET("inSet", new InSetOperator()),
    NOTIN_SET("notInSet", new NotInSetOperator()),
    AND("and", new AndOperator()),
    ALL_OF("allOf", new AllOfOperator()),
    ONE_OF("oneOf", new OneOfOperator()),
    NONE_OF("noneOf", new NoneOfOperator()),
    OR("or", new OrOperator()),
    XOR("xor", new XorOperator()),
    NOT("not", new NotOperator()),
    MATCHES_SCHEMA("matschesSchema", new MatchesSchemaOperator()),







    // Arithmetic Operators
    ADD("+"),
    SUBTRACT("-"),
    MULTIPLY("*"),
    DIVIDE("/"),
    MODULO("%"),

    // Relational Operators


    private final String symbol;
    private final Operation operationStrategy;

    LogicOperator(String symbol, Operation operationStrategy) {
        this.symbol = symbol;
        this.operationStrategy = operationStrategy;
    }

    public String getSymbol() {
        return symbol;
    }

    public Operation getOperationStrategy() {
        return operationStrategy;
    }
}
