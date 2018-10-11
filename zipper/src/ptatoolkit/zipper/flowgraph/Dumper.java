package ptatoolkit.zipper.flowgraph;

import soot.util.dot.DotGraph;
import soot.util.dot.DotGraphEdge;
import soot.util.dot.DotGraphNode;

import static soot.util.dot.DotGraphConstants.*;

public class Dumper {
    
    private static final String COLOR = "color";

    private static final String BLACK = "black";
    private static final String BLUE = "blue";
    private static final String GREEN = "green";
    private static final String RED = "red";
    
    public static void dumpObjectFlowGraph(IObjectFlowGraph g, String fileName) {
        DotGraph dotGraph = drawObjectFlowGraph(g);
        dotGraph.plot(fileName);
    }

    private static DotGraph drawObjectFlowGraph(IObjectFlowGraph g) {
        DotGraph canvas = new DotGraph("Object Flow Graph");
        g.allNodes().forEach(node -> {
            drawNode(canvas, node);
            g.outEdgesOf(node).forEach(edge -> {
                drawEdge(canvas, edge);
            });
        });
        return canvas;
    }

    private static void drawNode(DotGraph canvas, Node node) {
        DotGraphNode graphNode = canvas.drawNode(node.toString());
        if (node instanceof VarNode) {
            graphNode.setShape(NODE_SHAPE_BOX);
        } else if (node instanceof InstanceFieldNode) {
            graphNode.setShape(NODE_SHAPE_ELLIPSE);
            graphNode.setAttribute(COLOR, RED);
        }
    }

    private static void drawEdge(DotGraph canvas, Edge edge) {
        Node from = edge.getSource();
        Node to = edge.getTarget();
        DotGraphEdge graphEdge = canvas.drawEdge(
                from.toString(), to.toString());
        switch (edge.getKind()) {
            case LOCAL_ASSIGN:
                graphEdge.setAttribute(COLOR, BLACK);
                break;
            case INTERPROCEDURAL_ASSIGN:
                graphEdge.setAttribute(COLOR, BLUE);
                break;
            case INSTANCE_LOAD:
                graphEdge.setAttribute(COLOR, GREEN);
                break;
            case INSTANCE_STORE:
                graphEdge.setAttribute(COLOR, RED);
                break;
            case WRAPPED_FLOW:
                graphEdge.setStyle(NODE_STYLE_DOTTED);
                break;
            case UNWRAPPED_FLOW:
                graphEdge.setAttribute(COLOR, GREEN);
                graphEdge.setStyle(NODE_STYLE_DOTTED);
                break;
        }
    }
}
