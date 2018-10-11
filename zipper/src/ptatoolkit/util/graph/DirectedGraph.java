package ptatoolkit.util.graph;

import java.util.Collection;

public interface DirectedGraph<N> {

    Collection<N> allNodes();

    Collection<N> predsOf(N n);

    Collection<N> succsOf(N n);
}
