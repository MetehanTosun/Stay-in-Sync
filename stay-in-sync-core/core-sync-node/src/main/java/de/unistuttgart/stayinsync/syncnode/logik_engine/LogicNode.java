package de.unistuttgart.stayinsync.syncnode.logik_engine;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;

@Getter
@Setter
public class LogicNode {

    private String nodeName;
    private LogicOperator operator;
    private List<LogicNode> parents;
    private Object result;


    public LogicNode(String nodeName, LogicOperator operator, LogicNode... parents) {
        this.nodeName = nodeName;
        this.operator = operator;
        this.parents = Arrays.asList(parents);
    }

}
