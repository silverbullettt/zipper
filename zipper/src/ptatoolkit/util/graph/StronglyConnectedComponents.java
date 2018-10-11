package ptatoolkit.util.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Finding strongly connected components in a directed graph
 * using Tarjan's algorithm.
 * @param <N>
 */
public class StronglyConnectedComponents<N> {

    private final List<List<N>> componentList = new ArrayList<>();
    private final List<List<N>> trueComponentList = new ArrayList<>();

    private int index = 0;
    private Map<N,Integer> indexForNode, lowlinkForNode;
    private Stack<N> stack;
    private DirectedGraph<N> graph;

    public StronglyConnectedComponents(DirectedGraph<N> graph) {
        this.graph = graph;
        stack = new Stack<>();
        indexForNode = new HashMap<>();
        lowlinkForNode = new HashMap<>();

        for (N node : graph.allNodes()) {
            if (!indexForNode.containsKey(node)) {
                recurse(node);
            }
        }

        validate(graph, componentList);

        // release memory
        indexForNode = null;
        lowlinkForNode = null;
        stack = null;
        this.graph = null;
    }

    /**
     *   @return the list of the strongly-connected components
     */
    public List<List<N>> getComponents() {
        return componentList;
    }

    /**
     *   @return the list of the strongly-connected components, but only those
     *   that are true components, i.e. components which have more than one element
     *   or consists of one node that has itself as a successor
     */
    public List<List<N>> getTrueComponents() {
        return trueComponentList;
    }

    private void recurse(N node) {
        indexForNode.put(node, index);
        lowlinkForNode.put(node, index);
        ++index;
        stack.push(node);
        for (N succ : graph.succsOf(node)) {
            if (!indexForNode.containsKey(succ)) {
                recurse(succ);
                lowlinkForNode.put(node, Math.min(lowlinkForNode.get(node),
                        lowlinkForNode.get(succ)));
            } else if (stack.contains(succ)) {
                lowlinkForNode.put(node, Math.min(lowlinkForNode.get(node),
                        indexForNode.get(succ)));
            }
        }
        if (lowlinkForNode.get(node).intValue() ==
                indexForNode.get(node).intValue()) {
            List<N> scc = new ArrayList<N>();
            N v2;
            do {
                v2 = stack.pop();
                scc.add(v2);
            } while (node != v2);
            componentList.add(scc);
            if (scc.size() > 1) {
                trueComponentList.add(scc);
            } else {
                N n = scc.get(0);
                if (graph.succsOf(n).contains(n))
                    trueComponentList.add(scc);
            }
        }
    }

    /**
     * Validate whether the number of nodes in all SCCs is
     * equal to the number of nodes in the given graph.
     */
    private void validate(DirectedGraph<N> graph, List<List<N>> SCCs) {
        assert graph.allNodes().size() ==
                SCCs.stream().mapToInt(List::size).sum();
    }
}
