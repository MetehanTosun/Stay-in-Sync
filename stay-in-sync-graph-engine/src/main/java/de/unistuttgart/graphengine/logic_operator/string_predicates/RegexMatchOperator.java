package de.unistuttgart.graphengine.logic_operator.string_predicates;

import java.util.regex.PatternSyntaxException;

public class RegexMatchOperator extends AbstractTwoStringOperator {

    /**
     * Checks if the first string matches the regular expression pattern provided by the second string.
     * @param str1 The string to be tested.
     * @param str2 The regular expression pattern.
     * @return {@code true} if str1 matches the pattern str2. Returns {@code false} if the pattern is invalid.
     */
    @Override
    protected boolean compareStrings(String str1, String str2) {
        try {
            return str1.matches(str2);
        } catch (PatternSyntaxException e) {
            // If the regex pattern is invalid, it cannot match.
            return false;
        }
    }

    @Override
    public Class<?> getReturnType(){
        return Boolean.class;
    }
}