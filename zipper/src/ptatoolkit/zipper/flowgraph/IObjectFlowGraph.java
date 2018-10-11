package ptatoolkit.zipper.flowgraph;

import java.util.Set;

public interface IObjectFlowGraph {


    Set<Edge> outEdgesOf(Node node);

    Set<Node> allNodes();
}
