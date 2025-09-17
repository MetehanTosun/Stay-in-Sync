package de.unistuttgart.graphengine.logic_operator;

import de.unistuttgart.graphengine.logic_operator.array_predicates.*;
import de.unistuttgart.graphengine.logic_operator.datetime_predicates.*;
import de.unistuttgart.graphengine.logic_operator.general_predicates.*;
import de.unistuttgart.graphengine.logic_operator.number_predicates.*;
import de.unistuttgart.graphengine.logic_operator.object_predicates.*;
import de.unistuttgart.graphengine.logic_operator.string_predicates.*;
import de.unistuttgart.graphengine.logic_operator.boolean_predicates.IsFalseOperator;
import de.unistuttgart.graphengine.logic_operator.boolean_predicates.IsTrueOperator;



public enum LogicOperator {
    // General predicates
    EXISTS(new ExistsOperator()),
    NOT_EXISTS(new NotExistsOperator()),
    IS_NULL(new IsNullOperator()),
    IS_NOT_NULL(new IsNotNullOperator()),
    TYPE_IS(new TypeIsOperator()),
    EQUALS(new EqualsOperator()),
    NOT_EQUALS(new NotEqualsOperator()),
    IN_SET(new InSetOperator()),
    NOTIN_SET(new NotInSetOperator()),
    AND(new AndOperator()),
    ALL_OF(new AllOfOperator()),
    ONE_OF(new OneOfOperator()),
    NONE_OF(new NoneOfOperator()),
    OR(new OrOperator()),
    XOR(new XorOperator()),
    NOT(new NotOperator()),
    MATCHES_SCHEMA(new MatchesSchemaOperator()),

    // Number predicates
    GREATER_THAN(new GreaterThanOperator()),
    LESS_THAN(new LessThanOperator()),
    GREATER_OR_EQUAL(new GreaterOrEqualOperator()),
    LESS_OR_EQUAL(new LessOrEqualOperator()),
    BETWEEN(new BetweenOperator()),
    NOT_BETWEEN(new NotBetweenOperator()),
    ADD(new AddOperator()),

    // String predicates
    EQUALS_CASE_SENSITIVE(new EqualsCaseSensitiveOperator()),
    EQUALS_IGNORE_CASE(new EqualsIgnoreCaseOperator()),
    STRING_CONTAINS(new StringContainsOperator()),
    STRING_NOT_CONTAINS(new StringNotContainsOperator()),
    STRING_STARTS_WITH(new StringStartsWithOperator()),
    STRING_ENDS_WITH(new StringEndsWithOperator()),
    REGEX_MATCH(new RegexMatchOperator()),
    STRING_LENGTH_EQUALS(new StringLengthEqualsOperator()),
    STRING_LENGTH_GT(new StringLengthGtOperator()),
    STRING_LENGTH_LT(new StringLengthLtOperator()),
    STRING_LENGTH_BETWEEN(new StringLengthBetweenOperator()),

    // Boolean predicates (NEU)
    IS_TRUE(new IsTrueOperator()),
    IS_FALSE(new IsFalseOperator()),

    // Array/List predicates
    LENGTH_EQUALS(new LengthEqualsOperator()),
    LENGTH_GT(new LengthGtOperator()),
    LENGTH_LT(new LengthLtOperator()),
    NOT_EMPTY(new NotEmptyOperator()),
    CONTAINS_ELEMENT(new ContainsElementOperator()),
    NOT_CONTAINS_ELEMENT(new NotContainsElementOperator()),
    CONTAINS_ALL(new ContainsAllOperator()),
    CONTAINS_ANY(new ContainsAnyOperator()),
    CONTAINS_NONE(new ContainsNoneOperator()),

    // Aggregate predicates
    SUM(new SumOperator()),
    AVG( new AvgOperator()),
    MIN(new MinOperator()),
    MAX(new MaxOperator()),

    //Object predicates
    HAS_KEY(new HasKeyOperator()),
    LACKS_KEY(new LacksKeyOperator()),
    HAS_ALL_KEYS(new HasAllKeysOperator()),
    HAS_ANY_KEY(new HasAnyKeyOperator()),
    HAS_NO_KEYS(new HasNoKeysOperator()),

    // Date/Time predicates
    BEFORE(new BeforeOperator()),
    AFTER(new AfterOperator()),
    BETWEEN_DATES(new BetweenDatesOperator()),
    SAME_DAY(new SameDayOperator()),
    SAME_MONTH(new SameMonthOperator()),
    SAME_YEAR(new SameYearOperator()),
    WEEKDAY_IS(new WeekdayIsOperator()),
    MONTH_IS(new MonthIsOperator()),
    AGE_GREATER_THAN(new AgeGreaterThanOperator()),
    WITHIN_LAST(new WithinLastOperator()),
    WITHIN_NEXT(new WithinNextOperator()),
    TIMEZONE_OFFSET_EQUALS(new TimezoneOffsetEqualsOperator());

    private final Operation operationStrategy;

    LogicOperator(Operation operationStrategy) {
        this.operationStrategy = operationStrategy;
    }

    public Operation getOperationStrategy() {
        return operationStrategy;
    }
}
