package ptatoolkit.util.graph;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This graph represents a directed graph with each of its SCC components
 * is merged as one node. As a result, the type of node can only be Object.
 */
public class SCCMergedGraph<N> implements DirectedGraph<MergedNode<N>> {

    private Set<MergedNode<N>> nodes;

    public SCCMergedGraph(DirectedGraph<N> graph) {
        init(graph);
    }

    @Override
    public Collection<MergedNode<N>> allNodes() {
        return nodes;
    }

    @Override
    public Collection<MergedNode<N>> predsOf(MergedNode<N> node) {
        return node.getPreds();
    }

    @Override
    public Collection<MergedNode<N>> succsOf(MergedNode<N> node) {
        return node.getSuccs();
    }

    private void init(DirectedGraph<N> graph) {
        nodes = new HashSet<>();
        Map<N, MergedNode<N>> nodeMap = new HashMap<>();
        StronglyConnectedComponents<N> scc =
                new StronglyConnectedComponents<>(graph);
        scc.getComponents().forEach(component -> {
            MergedNode<N> node = new MergedNode<>(component);
            component.forEach(n -> nodeMap.put(n, node));
            nodes.add(node);
        });

        nodes.forEach(node -> {
            node.getContent()
                    .stream()
                    .map(graph::succsOf)
                    .flatMap(succs -> succs.stream())
                    .map(nodeMap::get)
                    .filter(succ -> succ != node) // exclude self-loop
                    .forEach(succ -> {
                        node.addSucc(succ);
                        succ.addPred(node);
                    });
        });
    }

}
