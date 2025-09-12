package de.unistuttgart.graphengine.logic_operator.array_predicates;

import java.util.Collection;
import java.util.Set;

public class ContainsNoneOperator extends AbstractSetComparisonOperator {

    @Override
    protected boolean compareSets(Set<?> sourceSet, Collection<?> referenceCollection) {
        if (referenceCollection.isEmpty()) {
            return true;
        }
        // Similar to ContainsAny, iterate and check for absence.
        for (Object item : referenceCollection) {
            if (sourceSet.contains(item)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Class<?> getReturnType(){
        return Boolean.class;
    }
}