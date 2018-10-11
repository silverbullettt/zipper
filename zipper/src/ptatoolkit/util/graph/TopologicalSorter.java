package ptatoolkit.util.graph;

import com.google.common.collect.Lists;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Topologically sorting a directed graph using DFS.
 * It is assumed that the given graph is a direct acyclic graph (DAG).
 * @param <N>
 */
public class TopologicalSorter<N> {

    private DirectedGraph<N> graph;
    private List<N> sortedList;
    private Set<N> visited;

    public List<N> sort(DirectedGraph<N> graph) {
        return sort(graph, false);
    }

    public List<N> sort(DirectedGraph<N> graph, boolean reverse) {
        initialize(graph);

        graph.allNodes()
                .stream()
                .filter(n -> graph.succsOf(n).isEmpty())
                .forEach(this::visit);

        List<N> result = sortedList;
        if (reverse) {
            result = Lists.reverse(sortedList);
        }
        clear();
        return result;
    }

    private void initialize(DirectedGraph<N> graph) {
        this.graph = graph;
        this.sortedList = new LinkedList<>();
        this.visited = new HashSet<>();
    }

    private void visit(N node) {
        if (!visited.contains(node)) {
            visited.add(node);
            graph.predsOf(node).forEach(this::visit);
            sortedList.add(node);
        }
    }

    private void clear() {
        this.graph = null;
        this.sortedList = null;
        this.visited = null;
    }

}
