package de.unistuttgart.stayinsync.syncnode.logik_engine.LogicOperator.array_predicates;

import java.util.Collection;
import java.util.Set;

public class ContainsAllOperator extends AbstractSetComparisonOperator {

    @Override
    protected boolean compareSets(Set<?> sourceSet, Collection<?> referenceCollection) {
        return sourceSet.containsAll(referenceCollection);
    }
}