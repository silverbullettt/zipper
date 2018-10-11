package ptatoolkit.zipper.flowgraph;

/**
 * Kind of flow edges.
 */
public enum Kind {

    LOCAL_ASSIGN,
    INTERPROCEDURAL_ASSIGN,
    INSTANCE_LOAD,
    INSTANCE_STORE,
    WRAPPED_FLOW,
    UNWRAPPED_FLOW,
}
