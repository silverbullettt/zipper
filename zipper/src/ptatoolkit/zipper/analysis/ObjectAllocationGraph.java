package ptatoolkit.zipper.analysis;

import ptatoolkit.Global;
import ptatoolkit.pta.basic.Method;
import ptatoolkit.pta.basic.Obj;
import ptatoolkit.pta.basic.Type;
import ptatoolkit.util.SetFactory;
import ptatoolkit.util.graph.DirectedGraph;
import ptatoolkit.util.graph.MergedNode;
import ptatoolkit.util.graph.SCCMergedGraph;
import ptatoolkit.util.graph.TopologicalSorter;
import ptatoolkit.zipper.pta.PointsToAnalysis;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class ObjectAllocationGraph implements DirectedGraph<Obj> {

    private static final String PREDS = "Predecessors";
    private static final String SUCCS = "Successors";
    private static final String ALLOCATEES = "Allocatees";

    private final PointsToAnalysis pta;
    private final Map<Type, Set<Obj>> typeAllocatees = new HashMap<>();

    ObjectAllocationGraph(PointsToAnalysis pta) {
        this.pta = pta;
        init();
    }

    @Override
    public Set<Obj> allNodes() {
        return pta.allObjects();
    }

    @Override
    public Set<Obj> predsOf(Obj obj) {
        return obj.getAttributeSet(PREDS);
    }

    @Override
    public Set<Obj> succsOf(Obj obj) {
        return obj.getAttributeSet(SUCCS);
    }

    public Set<Obj> allocateesOf(Obj obj) {
        return obj.getAttributeSet(ALLOCATEES);
    }

    public Set<Obj> allocateesOf(Type type) {
        return typeAllocatees.get(type);
    }

    private void init() {
        Map<Obj, Set<Method>> invokedMethods = computeInvokedMethods();
        invokedMethods.entrySet()
                .stream()
                .filter(e -> !isArray(e.getKey()))
                .forEach(e -> {
                    Obj obj = e.getKey();
                    Set<Method> methods = e.getValue();
                    methods.stream()
                            .map(pta::objectsAllocatedIn)
                            .flatMap(Collection::stream)
                            .forEach(o -> {
                                obj.addToAttributeSet(SUCCS, o);
                                o.addToAttributeSet(PREDS, obj);
                            });
                });
        computeAllocatees();
        pta.allObjects().forEach(obj -> {
            Type type = obj.getType();
            typeAllocatees.putIfAbsent(type, new HashSet<>());
            typeAllocatees.get(type).addAll(allocateesOf(obj));
        });
    }

    private Map<Obj, Set<Method>> computeInvokedMethods() {
        Map<Obj, Set<Method>> invokedMethods = new HashMap<>();
        pta.allObjects().forEach(obj -> {
            Set<Method> methods = new HashSet<>();
            Queue<Method> queue = new LinkedList<>(pta.methodsInvokedOn(obj));
            while (!queue.isEmpty()) {
                Method method = queue.poll();
                methods.add(method);
                pta.calleesOf(method).stream()
                        .filter(m -> m.isStatic() && !methods.contains(m))
                        .forEach(queue::offer);
            }
            invokedMethods.put(obj, methods);
        });
        return invokedMethods;
    }

    private void computeAllocatees() {
        SCCMergedGraph<Obj> mg = new SCCMergedGraph<>(this);
        TopologicalSorter<MergedNode<Obj>> sorter = new TopologicalSorter<>();
        SetFactory<Obj> setFactory = new SetFactory<>();
        sorter.sort(mg, true).forEach(node -> {
            Set<Obj> allocatees = setFactory.get(getAllocatees(node, mg));
            node.getContent()
                    .forEach(obj -> obj.setAttribute(ALLOCATEES, allocatees));
        });
    }

    private Set<Obj> getAllocatees(MergedNode<Obj> node,
                                   SCCMergedGraph<Obj> mg) {
        Set<Obj> allocatees = new HashSet<>();
        mg.succsOf(node).forEach(n -> {
            // direct allocatees
            allocatees.addAll(n.getContent());
            // indirect allocatees
            Obj o = n.getContent().iterator().next();
            allocatees.addAll(allocateesOf(o));
        });
        Obj obj = node.getContent().iterator().next();
        if (node.getContent().size() > 1 ||
                succsOf(obj).contains(obj)) { // self-loop
            // The merged node is a true SCC
            allocatees.addAll(node.getContent());
        }
        return allocatees;
    }

    private boolean isArray(Obj obj) {
        return obj.getType().toString().endsWith("[]");
    }
}
