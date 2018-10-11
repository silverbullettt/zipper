package ptatoolkit.util.graph;

import com.google.common.collect.Sets;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Reachability<N> {

    private final DirectedGraph<N> graph;
    private final Map<N, Set<N>> reachableNodes = new HashMap<>();
    private final Map<N, Set<N>> reachToNodes = new HashMap<>();

    public Reachability(DirectedGraph<N> graph) {
        this.graph = graph;
    }

    /**
     *
     * @param source
     * @return all nodes those can be reached from source.
     */
    public Set<N> reachableNodesFrom(N source) {
        if (!reachableNodes.containsKey(source)) {
            Set<N> visited = new HashSet<>();
            Deque<N> stack = new ArrayDeque<>();
            stack.push(source);
            while (!stack.isEmpty()) {
                N node = stack.pop();
                visited.add(node);
                graph.succsOf(node)
                        .stream()
                        .filter(n -> !visited.contains(n))
                        .forEach(stack::push);
            }
            reachableNodes.put(source, visited);
        }
        return reachableNodes.get(source);
    }

    /**
     *
     * @param target
     * @return all nodes those can reach target.
     */
    public Set<N> nodesReach(N target) {
        if (!reachToNodes.containsKey(target)) {
            Set<N> visited = new HashSet<>();
            Deque<N> stack = new ArrayDeque<>();
            stack.push(target);
            while (!stack.isEmpty()) {
                N node = stack.pop();
                visited.add(node);
                graph.predsOf(node)
                        .stream()
                        .filter(n -> !visited.contains(n))
                        .forEach(stack::push);
            }
            reachToNodes.put(target, visited);
        }
        return reachToNodes.get(target);
    }

    /**
     *
     * @param source
     * @param target
     * @return all nodes on the paths from source to target.
     */
    public Set<N> passedNodes(N source, N target) {
        Set<N> reachableFromSource = reachableNodesFrom(source);
        Set<N> reachToTarget = nodesReach(target);
        return Sets.intersection(reachableFromSource, reachToTarget);
    }
}
