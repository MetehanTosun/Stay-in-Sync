package de.unistuttgart.stayinsync.syncnode.logik_engine.LogicOperator.array_predicates;

import java.util.Collection;
import java.util.Collections;
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
}