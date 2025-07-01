package de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator;

import de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.array_predicates.*;
import de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.generell_predicates.*;
import de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.number_predicates.*;
import de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.object_predicates.*;
import de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.string_predicates.*;


public enum LogicOperator {
    // General predicates
    EXISTS("exists", new ExistsOperator()),
    NOT_EXISTS("notExists", new NotExistsOperator()),
    IS_NULL("isNull", new IsNull()),
    IS_NOT_NULL("isNotNull", new IsNotNull()),
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

    // String predicates
    EQUALS_CASE_SENSITIVE("equalsCaseSensitive", new EqualsCaseSensitiveOperator()),
    EQUALS_IGNORE_CASE("equalsIgnoreCase", new EqualsIgnoreCaseOperator()),
    STRING_CONTAINS("contains", new StringContainsOperator()),
    STRING_NOT_CONTAINS("notContains", new StringNotContainsOperator()),
    STRING_STARTS_WITH("startsWith", new StringStartsWithOperator()),
    STRING_ENDS_WITH("endsWith", new StringEndsWithOperator()),
    REGEX_MATCH("regexMatch", new RegexMatchOperator()),
    STRING_LENGTH_EQUALS("lengthEquals", new StringLengthEqualsOperator()),
    STRING_LENGTH_GT("lengthGt", new StringLengthGtOperator()),
    STRING_LENGTH_LT("lengthLt", new StringLengthLtOperator()),
    STRING_LENGTH_BETWEEN("lengthBetween", new StringLengthBetweenOperator()),

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

    //Object predicates
    HAS_KEY("hasKey", new HasKeyOperator()),
    LACKS_KEY("lacksKey", new LacksKeyOperator()),
    HAS_ALL_KEYS("hasAllKeys", new HasAllKeysOperator()),
    HAS_ANY_KEY("hasAnyKey", new HasAnyKeyOperator()),
    HAS_NO_KEYS("hasNoKeys", new HasNoKeysOperator()),




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
