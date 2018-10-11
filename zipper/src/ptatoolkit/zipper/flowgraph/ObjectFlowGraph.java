package ptatoolkit.zipper.flowgraph;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import ptatoolkit.pta.basic.Field;
import ptatoolkit.pta.basic.Obj;
import ptatoolkit.pta.basic.Variable;
import ptatoolkit.zipper.pta.PointsToAnalysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ObjectFlowGraph implements IObjectFlowGraph {

    private Set<Node> nodes;
    private Map<Variable, VarNode> var2node;
    private Table<Obj, Field, InstanceFieldNode> field2node;

    public ObjectFlowGraph(PointsToAnalysis pta) {
        init(pta);
    }

    /**
     *
     * @param var
     * @return the VarNode representing var
     */
    public VarNode nodeOf(Variable var) {
//        if (!var2node.containsKey(var)) {
//            throw new RuntimeException(var + " does not exist in Flow Graph");
//        }
        return var2node.get(var);
    }

    public Set<Node> succsOf(Node node) {
        return node.getOutEdges()
                .stream()
                .map(Edge::getTarget)
                .collect(Collectors.toSet());
    }

    public Set<Edge> outEdgesOf(Node node) {
        return node.getOutEdges();
    }

    public Set<Node> allNodes() {
        return nodes;
    }

    private void init(PointsToAnalysis pta) {
        nodes = new HashSet<>();
        var2node = new HashMap<>();

        // Add local assignment edges.
        pta.localAssignIterator().forEachRemaining(pair -> {
            Variable to = pair.getFirst();
            Variable from = pair.getSecond();
            VarNode toNode = getVarNode(to);
            VarNode fromNode = getVarNode(from);
            fromNode.addOutEdge(new Edge(Kind.LOCAL_ASSIGN, fromNode, toNode));
        });

        // Add inter-procedural assignment edges.
        pta.interProceduralAssignIterator().forEachRemaining(pair -> {
            Variable to = pair.getFirst();
            Variable from = pair.getSecond();
            VarNode toNode = getVarNode(to);
            VarNode fromNode = getVarNode(from);
            fromNode.addOutEdge(new Edge(Kind.INTERPROCEDURAL_ASSIGN, fromNode, toNode));
        });

        // Add this-passing assignment edges
        pta.thisAssignIterator().forEachRemaining(pair -> {
            Variable thisVar = pair.getFirst();
            Variable baseVar = pair.getSecond();
            VarNode toNode = getVarNode(thisVar);
            VarNode fromNode = getVarNode(baseVar);
            fromNode.addOutEdge(new Edge(Kind.INTERPROCEDURAL_ASSIGN, fromNode, toNode));
        });

        field2node = HashBasedTable.create();
        // Add instance load edges;
        pta.instanceLoadIterator().forEachRemaining(triple -> {
            Variable var = triple.getFirst();
            Obj base = triple.getSecond();
            Field field = triple.getThird();
            VarNode varNode = getVarNode(var);
            InstanceFieldNode fieldNode = getInstanceFieldNode(base, field);
            fieldNode.addOutEdge(new Edge(Kind.INSTANCE_LOAD, fieldNode, varNode));
        });

        // Add instance store edges;
        pta.instanceStoreIterator().forEachRemaining(triple -> {
            Obj base = triple.getFirst();
            Field field = triple.getSecond();
            Variable var = triple.getThird();
            VarNode varNode = getVarNode(var);
            InstanceFieldNode fieldNode = getInstanceFieldNode(base, field);
            varNode.addOutEdge(new Edge(Kind.INSTANCE_STORE, varNode, fieldNode));
        });
    }

    /**
     *
     * @param var
     * @return the VarNode representing var.
     * If the node does not exist, it will be created.
     */
    private VarNode getVarNode(Variable var) {
        if (!var2node.containsKey(var)) {
            VarNode node = new VarNode(var);
            var2node.put(var, node);
            nodes.add(node);
            return node;
        }
        return var2node.get(var);
    }

    /**
     *
     * @param base
     * @param field
     * @return the instance field representing base.field.
     * If the node does not exist, it will be created.
     */
    private InstanceFieldNode getInstanceFieldNode(Obj base, Field field) {
        if (!field2node.contains(base, field)) {
            InstanceFieldNode node = new InstanceFieldNode(base, field);
            field2node.put(base, field, node);
            nodes.add(node);
            return node;
        }
        return field2node.get(base, field);
    }
}
