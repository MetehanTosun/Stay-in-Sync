package de.unistuttgart.graphengine.logic_operator.string_predicates;

public class StringStartsWithOperator extends AbstractTwoStringOperator {

    /**
     * Checks if the first string starts with the prefix specified by the second string.
     * @param str1 The string to be checked.
     * @param str2 The prefix.
     * @return {@code true} if str1 starts with the prefix str2.
     */
    @Override
    protected boolean compareStrings(String str1, String str2) {
        return str1.startsWith(str2);
    }

    @Override
    public Class<?> getReturnType(){
        return Boolean.class;
    }
}