package de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.string_predicates;

public class EqualsCaseSensitiveOperator extends AbstractTwoStringOperator {

    /**
     * Performs a case-sensitive string comparison.
     * @param str1 The first string input.
     * @param str2 The second string input.
     * @return {@code true} if the strings are exactly equal.
     */
    @Override
    protected boolean compareStrings(String str1, String str2) {
        return str1.equals(str2);
    }
}