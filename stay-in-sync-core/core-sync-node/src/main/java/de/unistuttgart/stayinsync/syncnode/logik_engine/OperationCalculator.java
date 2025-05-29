package de.unistuttgart.stayinsync.syncnode.logik_engine;

import java.util.ArrayList;
import java.util.List;

public class OperationCalculator {

    public static Object evaluate(LogicNode node){

        if(node.getResult() != null){
            return node.getResult();
        }

        List<Object> values = new ArrayList<>();
        for(LogicNode parent : node.getParents()){
            values.add(evaluate(parent));
        }
        Object result = calculate(node.getOperator(), values);
        node.setResult(result);
        return result;
    }

    private static Object calculate(LogicOperator operator, List<Object> values) {
        if (values == null) {
            throw new IllegalArgumentException("Values list cannot be null");
        }
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Values list cannot be empty");
        }

        switch (operator) {
            case ADD:
                double sum = 0;
                for (Object value : values) {
                    if (value == null) {
                        throw new IllegalArgumentException("Cannot perform ADD operation: null value found");
                    }
                    if (!(value instanceof Number)) {
                        throw new IllegalArgumentException("Cannot perform ADD operation: expected numeric value, got " + value.getClass().getSimpleName());
                    }
                    sum += ((Number) value).doubleValue();
                }
                return sum;

            case SUBTRACT: {
                if (values.size() < 2) {
                    throw new IllegalArgumentException("SUBTRACT operation requires at least 2 values, got " + values.size());
                }
                Object firstValue = values.getFirst();
                if (!(firstValue instanceof Number)) {
                    throw new IllegalArgumentException("Cannot perform SUBTRACT operation: first value must be numeric");
                }
                double sub = ((Number) firstValue).doubleValue();
                for (int i = 1; i < values.size(); i++) {
                    Object value = values.get(i);
                    if (value == null) {
                        throw new IllegalArgumentException("Cannot perform SUBTRACT operation: null value found at index " + i);
                    }
                    if (!(value instanceof Number)) {
                        throw new IllegalArgumentException("Cannot perform SUBTRACT operation: expected numeric value at index " + i + ", got " + value.getClass().getSimpleName());
                    }
                    sub -= ((Number) value).doubleValue();
                }
                return sub;
            }
            case MULTIPLY:
                double mul = 1;
                for (Object value : values) {
                    if (value == null) {
                        throw new IllegalArgumentException("Cannot perform MULTIPLY operation: null value found");
                    }
                    if (!(value instanceof Number)) {
                        throw new IllegalArgumentException("Cannot perform MULTIPLY operation: expected numeric value, got " + value.getClass().getSimpleName());
                    }
                    mul *= ((Number) value).doubleValue();
                }
                return mul;

            case DIVIDE: {
                if (values.size() < 2) {
                    throw new IllegalArgumentException("DIVIDE operation requires at least 2 values, got " + values.size());
                }
                Object firstValue = values.getFirst();
                if (!(firstValue instanceof Number)) {
                    throw new IllegalArgumentException("Cannot perform DIVIDE operation: first value must be numeric");
                }
                double div = ((Number) firstValue).doubleValue();
                for (int i = 1; i < values.size(); i++) {
                    Object value = values.get(i);
                    if (value == null) {
                        throw new IllegalArgumentException("Cannot perform DIVIDE operation: null value found at index " + i);
                    }
                    if (!(value instanceof Number)) {
                        throw new IllegalArgumentException("Cannot perform DIVIDE operation: expected numeric value at index " + i + ", got " + value.getClass().getSimpleName());
                    }
                    double divisor = ((Number) value).doubleValue();
                    if (divisor == 0) {
                        throw new ArithmeticException("Cannot perform DIVIDE operation: division by zero at index " + i);
                    }
                    div /= divisor;
                }
                return div;
            }

            case MODULO: {
                if (values.size() != 2) {
                    throw new IllegalArgumentException("MODULO operation requires exactly 2 values, got " + values.size());
                }
                Object firstValue = values.getFirst();
                Object secondValue = values.get(1);

                if (!(firstValue instanceof Number)) {
                    throw new IllegalArgumentException("Cannot perform MODULO operation: first value must be numeric");
                }
                if (!(secondValue instanceof Number)) {
                    throw new IllegalArgumentException("Cannot perform MODULO operation: second value must be numeric");
                }

                double first = ((Number) firstValue).doubleValue();
                double second = ((Number) secondValue).doubleValue();

                if (second == 0) {
                    throw new ArithmeticException("Cannot perform MODULO operation: modulo by zero");
                }
                return first % second;
            }

            case GREATER_THAN: {
                if (values.size() != 2) {
                    throw new IllegalArgumentException("GREATER_THAN operation requires exactly 2 values, got " + values.size());
                }
                Object firstValue = values.getFirst();
                Object secondValue = values.get(1);

                if (!(firstValue instanceof Number)) {
                    throw new IllegalArgumentException("Cannot perform GREATER_THAN operation: first value must be numeric");
                }
                if (!(secondValue instanceof Number)) {
                    throw new IllegalArgumentException("Cannot perform GREATER_THAN operation: second value must be numeric");
                }

                double first = ((Number) firstValue).doubleValue();
                double second = ((Number) secondValue).doubleValue();
                return first > second;
            }

            case GREATER_EQUAL: {
                if (values.size() != 2) {
                    throw new IllegalArgumentException("GREATER_EQUAL operation requires exactly 2 values, got " + values.size());
                }
                Object firstValue = values.getFirst();
                Object secondValue = values.get(1);

                if (!(firstValue instanceof Number)) {
                    throw new IllegalArgumentException("Cannot perform GREATER_EQUAL operation: first value must be numeric");
                }
                if (!(secondValue instanceof Number)) {
                    throw new IllegalArgumentException("Cannot perform GREATER_EQUAL operation: second value must be numeric");
                }

                double first = ((Number) firstValue).doubleValue();
                double second = ((Number) secondValue).doubleValue();
                return first >= second;
            }

            case LESS_THAN: {
                if (values.size() != 2) {
                    throw new IllegalArgumentException("LESS_THAN operation requires exactly 2 values, got " + values.size());
                }
                Object firstValue = values.getFirst();
                Object secondValue = values.get(1);

                if (!(firstValue instanceof Number)) {
                    throw new IllegalArgumentException("Cannot perform LESS_THAN operation: first value must be numeric");
                }
                if (!(secondValue instanceof Number)) {
                    throw new IllegalArgumentException("Cannot perform LESS_THAN operation: second value must be numeric");
                }

                double first = ((Number) firstValue).doubleValue();
                double second = ((Number) secondValue).doubleValue();
                return first < second;
            }

            case LESS_EQUAL: {
                if (values.size() != 2) {
                    throw new IllegalArgumentException("LESS_EQUAL operation requires exactly 2 values, got " + values.size());
                }
                Object firstValue = values.getFirst();
                Object secondValue = values.get(1);

                if (!(firstValue instanceof Number)) {
                    throw new IllegalArgumentException("Cannot perform LESS_EQUAL operation: first value must be numeric");
                }
                if (!(secondValue instanceof Number)) {
                    throw new IllegalArgumentException("Cannot perform LESS_EQUAL operation: second value must be numeric");
                }

                double first = ((Number) firstValue).doubleValue();
                double second = ((Number) secondValue).doubleValue();
                return first <= second;
            }

            case AND:
                boolean and = true;
                for (int i = 0; i < values.size(); i++) {
                    Object value = values.get(i);
                    if (value == null) {
                        throw new IllegalArgumentException("Cannot perform AND operation: null value found at index " + i);
                    }
                    if (!(value instanceof Boolean)) {
                        throw new IllegalArgumentException("Cannot perform AND operation: expected boolean value at index " + i + ", got " + value.getClass().getSimpleName());
                    }
                    and = and && (Boolean) value;
                }
                return and;

            case OR:
                boolean or = false;
                for (int i = 0; i < values.size(); i++) {
                    Object value = values.get(i);
                    if (value == null) {
                        throw new IllegalArgumentException("Cannot perform OR operation: null value found at index " + i);
                    }
                    if (!(value instanceof Boolean)) {
                        throw new IllegalArgumentException("Cannot perform OR operation: expected boolean value at index " + i + ", got " + value.getClass().getSimpleName());
                    }
                    or = or || (Boolean) value;
                }
                return or;

            case EQUALS: {
                if (values.size() < 2) {
                    throw new IllegalArgumentException("EQUALS operation requires at least 2 values, got " + values.size());
                }
                Object first = values.getFirst();
                for (int i = 1; i < values.size(); i++) {
                    Object current = values.get(i);
                    if (first == null && current == null) {
                        continue;
                    }
                    if (first == null || current == null) {
                        return false;
                    }
                    if (!first.equals(current)) {
                        return false;
                    }
                }
                return true;
            }

            case NOT_EQUALS: {
                if (values.size() < 2) {
                    throw new IllegalArgumentException("NOT_EQUALS operation requires at least 2 values, got " + values.size());
                }
                Object first = values.getFirst();
                for (int i = 1; i < values.size(); i++) {
                    Object current = values.get(i);
                    // Handle null values properly
                    if (first == null && current == null) {
                        continue;
                    }
                    if (first == null || current == null) {
                        return true;
                    }
                    if (!first.equals(current)) {
                        return true;
                    }
                }
                return false;
            }
        }
        // This should never happen if all enum values are handled
        throw new RuntimeException("Unexpected operator: " + operator);
    }
}
