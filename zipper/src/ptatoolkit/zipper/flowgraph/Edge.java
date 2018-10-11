package ptatoolkit.zipper.flowgraph;

import java.util.Objects;

public class Edge {

    private final Kind kind;
    private final Node source;
    private final Node target;
    private final int hashCode;

    public Edge(Kind kind, Node source, Node target) {
        this.kind = kind;
        this.source = source;
        this.target = target;
        hashCode = Objects.hash(kind, source, target);
    }

    public Kind getKind() {
        return kind;
    }

    public Node getSource() {
        return source;
    }

    public Node getTarget() {
        return target;
    }

    @Override
    public String toString() {
        return kind + ": " + source + " --> " + target;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object other) {
        if (isOFGEdge()) { // Edges in OFG are unique and their equivalence
            return this == other; // can be directly tested by ==
        } else {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Edge)) {
                return false;
            }
            Edge otherEdge = (Edge) other;
            return kind.equals(otherEdge.kind) &&
                    source.equals(otherEdge.source) &&
                    target.equals(otherEdge.target);
        }
    }

    private boolean isOFGEdge() {
        return !(kind == Kind.WRAPPED_FLOW) &&
                !(kind == Kind.UNWRAPPED_FLOW);
    }
}
