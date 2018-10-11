package ptatoolkit.zipper.flowgraph;

import ptatoolkit.pta.basic.Field;
import ptatoolkit.pta.basic.Obj;

import java.util.Objects;

public class InstanceFieldNode extends Node {

    private final Obj base;
    private final Field field;

    public InstanceFieldNode(Obj base, Field field) {
        this.base = base;
        this.field = field;
    }

    public Obj getBase() {
        return base;
    }

    public Field getField() {
        return field;
    }

    @Override
    public int hashCode() {
        return Objects.hash(base, field);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (! (other instanceof InstanceFieldNode)) {
            return false;
        }
        InstanceFieldNode anoNode = (InstanceFieldNode) other;
        return base.equals(anoNode.base) &&
                field.equals(anoNode.field);
    }

    @Override
    public String toString() {
        return "InstanceFieldNode: [" + base + "] " + field;
    }

}
