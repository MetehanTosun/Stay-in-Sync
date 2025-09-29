package de.unistuttgart.graphengine.logic_operator.string_predicates;

public class StringContainsOperator extends AbstractTwoStringOperator {

    /**
     * Checks if the first string contains the second string as a substring.
     * @param str1 The string to be searched in.
     * @param str2 The substring to search for.
     * @return {@code true} if str2 is a substring of str1.
     */
    @Override
    protected boolean compareStrings(String str1, String str2) {

        return str1.contains(str2);
    }

    @Override
    public Class<?> getReturnType(){
        return Boolean.class;
    }
}