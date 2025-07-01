package de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.generell_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.inputNodes.InputNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.Operation;

import java.util.List;
import java.util.Map;

public class OneOfOperator implements Operation {

    @Override
    public void validate(LogicNode node) {
        List<InputNode> inputs = node.getInputProviders();
        if(inputs == null || inputs.isEmpty()){
            throw new IllegalArgumentException("ONE_OF operation is for node" + node.getNodeName() + "requires at least 1 input");
        }
    }

    @Override
    public Object execute(LogicNode node, Map<String, JsonNode> dataContext){
        for(InputNode input: node.getInputProviders()){
            Object value;
            try{
                value = input.getValue(dataContext);
            }
            catch(IllegalStateException e){
                continue;
            }

            if(Boolean.TRUE.equals(value)){
                return true;
            }

        }
        return false;
    }
}
