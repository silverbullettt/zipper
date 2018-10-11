package ptatoolkit.util.graph;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class MergedNode<N> {

    private Set<MergedNode<N>> preds;
    private Set<MergedNode<N>> succs;
    private Set<N> content;

    public MergedNode(Collection<N> content) {
        this.content = new HashSet<>(content);
    }

    public void addPred(MergedNode<N> pred) {
        if (preds == null) {
            preds = new HashSet<>(4);
        }
        preds.add(pred);
    }

    public Set<MergedNode<N>> getPreds() {
        return preds == null ? Collections.emptySet() : preds;
    }

    public void addSucc(MergedNode<N> succ) {
        if (succs == null) {
            succs = new HashSet<>(4);
        }
        succs.add(succ);
    }

    public Set<MergedNode<N>> getSuccs() {
        return succs == null ? Collections.emptySet() : succs;
    }

    public Set<N> getContent() {
        return content;
    }

    @Override
    public String toString() {
        return content.toString();
    }
}
