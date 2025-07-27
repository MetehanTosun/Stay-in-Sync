package de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.array_predicates;

import java.util.Collection;
import java.util.Set;

public class ContainsAnyOperator extends AbstractSetComparisonOperator {

    @Override
    protected boolean compareSets(Set<?> sourceSet, Collection<?> referenceCollection) {
        if (referenceCollection.isEmpty()) {
            return false;
        }
        // This is more efficient now, as it iterates through referenceCollection
        // and performs O(1) lookups in sourceSet.
        for (Object item : referenceCollection) {
            if (sourceSet.contains(item)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Class<?> getReturnType(){
        return Boolean.class;
    }
}