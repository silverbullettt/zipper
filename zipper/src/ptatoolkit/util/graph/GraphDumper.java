package ptatoolkit.util.graph;

import soot.util.dot.DotGraph;

public class GraphDumper<N> {

    public void dumpGraph(DirectedGraph<N> g, String fileName) {
        DotGraph dotGraph = drawGraph(g);
        dotGraph.plot(fileName);
    }

    protected DotGraph drawGraph(DirectedGraph<N> g) {
        DotGraph canvas = new DotGraph("Graph");
        g.allNodes().forEach(node -> {
            drawNode(canvas, node);
            g.succsOf(node).forEach(succ -> {
                drawEdge(canvas, node, succ);
            });
        });
        return canvas;
    }

    protected void drawNode(DotGraph canvas, N node) {
        canvas.drawNode(node.toString());
    }

    protected void drawEdge(DotGraph canvas, N from, N to) {
        canvas.drawEdge(from.toString(), to.toString());
    }

}
