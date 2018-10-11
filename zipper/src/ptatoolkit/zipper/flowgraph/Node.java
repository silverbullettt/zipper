package ptatoolkit.zipper.flowgraph;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Node {

    private Set<Edge> outEdges = Collections.emptySet();

    public void addOutEdge(Edge e) {
        if (outEdges.isEmpty()) {
            outEdges = new HashSet<>();
        }
        outEdges.add(e);
    }

    public Set<Edge> getOutEdges() {
        return outEdges;
    }
}
