package de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.string_predicates;

public class StringEndsWithOperator extends AbstractTwoStringOperator {

    /**
     * Checks if the first string ends with the suffix specified by the second string.
     * @param str1 The string to be checked.
     * @param str2 The suffix.
     * @return {@code true} if str1 ends with the suffix str2.
     */
    @Override
    protected boolean compareStrings(String str1, String str2) {
        return str1.endsWith(str2);
    }

    @Override
    public Class<?> getReturnType(){
        return Boolean.class;
    }
}