package de.unistuttgart.graphengine.logic_operator.string_predicates;

public class EqualsIgnoreCaseOperator extends AbstractTwoStringOperator {

    /**
     * Performs a case-insensitive string comparison.
     * @param str1 The first string input.
     * @param str2 The second string input.
     * @return {@code true} if the strings are equal, ignoring case differences.
     */
    @Override
    protected boolean compareStrings(String str1, String str2) {
        return str1.equalsIgnoreCase(str2);
    }

    @Override
    public Class<?> getReturnType(){
        return Boolean.class;
    }
}