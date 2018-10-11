package ptatoolkit.util.graph;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DirectedGraphImpl<N> implements DirectedGraph<N> {

    protected Set<N> nodes = new HashSet<>();
    protected Map<N, Set<N>> preds = new HashMap<>();
    protected Map<N, Set<N>> succs = new HashMap<>();

    public void addNode(N node) {
        nodes.add(node);
    }

    public void addEdge(N from, N to) {
        addNode(from);
        addNode(to);
        if (!preds.containsKey(to)) {
            preds.put(to, new HashSet<>());
        }
        preds.get(to).add(from);
        if (!succs.containsKey(from)) {
            succs.put(from, new HashSet<>());
        }
        succs.get(from).add(to);
    }

    @Override
    public Collection<N> allNodes() {
        return nodes;
    }

    @Override
    public Collection<N> predsOf(N n) {
        return preds.getOrDefault(n, Collections.emptySet());
    }

    @Override
    public Collection<N> succsOf(N n) {
        return succs.getOrDefault(n, Collections.emptySet());
    }
}
