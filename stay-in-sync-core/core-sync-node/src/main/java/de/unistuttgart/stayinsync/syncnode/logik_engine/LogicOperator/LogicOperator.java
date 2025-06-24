package de.unistuttgart.stayinsync.syncnode.logik_engine.LogicOperator;

import de.unistuttgart.stayinsync.syncnode.logik_engine.LogicOperator.array_predicates.*;
import de.unistuttgart.stayinsync.syncnode.logik_engine.LogicOperator.generell_predicates.*;
import de.unistuttgart.stayinsync.syncnode.logik_engine.LogicOperator.number_predicates.*;


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

    // Number predicates
    GREATER_THAN("greaterThan", new GreaterThanOperator()),
    LESS_THAN("lessThan", new LessThanOperator()),
    GREATER_OR_EQUAL("greaterOrEqual", new GreaterOrEqualOperator()),
    LESS_OR_EQUAL("lessOrEqual", new LessOrEqualOperator()),
    BETWEEN("between", new BetweenOperator()),
    NOT_BETWEEN("notBetween", new NotBetweenOperator()),

    // Array/List predicates
    LENGTH_EQUALS("lengthEquals", new LengthEqualsOperator()),
    LENGTH_GT("lengthGt", new LengthGtOperator()),
    LENGTH_LT("lengthLt", new LengthLtOperator()),
    NOT_EMPTY("notEmpty", new NotEmptyOperator()),
    CONTAINS_ELEMENT("containsElement", new ContainsElementOperator()),
    NOT_CONTAINS_ELEMENT("notContainsElement", new NotContainsElementOperator()),
    CONTAINS_ALL("containsAll", new ContainsAllOperator()),
    CONTAINS_ANY("containsAny", new ContainsAnyOperator()),
    CONTAINS_NONE("containsNone", new ContainsNoneOperator()),

    // Aggregate predicates
    SUM("sum", new SumOperator()),
    AVG("avg", new AvgOperator()),
    MIN("min", new MinOperator()),
    MAX("max", new MaxOperator()),



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
