package ptatoolkit.zipper.flowgraph;

import ptatoolkit.pta.basic.Variable;

public class VarNode extends Node {

    private final Variable var;

    public VarNode(Variable var) {
        this.var = var;
    }

    public Variable getVar() {
        return var;
    }

    @Override
    public int hashCode() {
        return var.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (! (other instanceof VarNode)) {
            return false;
        }
        VarNode anoNode = (VarNode) other;
        return var.equals(anoNode.var);
    }

    @Override
    public String toString() {
        return "VarNode: <" + var + ">";
    }

}
