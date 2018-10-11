package ptatoolkit.zipper.analysis;

import ptatoolkit.Global;
import ptatoolkit.pta.basic.Method;
import ptatoolkit.pta.basic.Obj;
import ptatoolkit.pta.basic.Type;
import ptatoolkit.util.SetFactory;
import ptatoolkit.util.graph.MergedNode;
import ptatoolkit.util.graph.SCCMergedGraph;
import ptatoolkit.util.graph.TopologicalSorter;
import ptatoolkit.zipper.pta.PointsToAnalysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * For each object o, this class compute the set of methods
 * which o could potentially be their context element.
 *
 * Conversely, for each method m, this class compute the
 * set of objects which could potentially be its context element.
 */
public class PotentialContextElement {

    private static final String PCE = "Potential context element";

    private final PointsToAnalysis pta;
    // This map maps each object to the methods invoked on it.
    // For instance methods, they are the methods whose receiver is the object.
    // For static methods, they are the methods reachable from instance methods.
    private Map<Obj, Set<Method>> invokedMethods;
    private Map<Type, Set<Method>> typePCEMethods = new HashMap<>();

    PotentialContextElement(PointsToAnalysis pta, ObjectAllocationGraph oag) {
        this.pta = pta;
        init(oag);
    }

    PotentialContextElement(PointsToAnalysis pta) {
        this(pta, new ObjectAllocationGraph(pta));
    }

    public Set<Method> PCEMethodsOf(Obj obj) {
        return obj.getAttributeSet(PCE);
    }

    /**
     *
     * @param type
     * @return PCE methods of the objects of given type.
     */
    public Set<Method> PCEMethodsOf(Type type) {
        if (!typePCEMethods.containsKey(type)) {
            Set<Method> methods = new HashSet<>();
            pta.objectsOfType(type)
                    .forEach(obj -> methods.addAll(PCEMethodsOf(obj)));
            typePCEMethods.put(type, methods);
        }
        return typePCEMethods.get(type);
    }

    public Set<Obj> PCEObjectsOf(Method method) {
        return method.getAttributeSet(PCE);
    }

    /**
     * Compute PCE methods for each objects.
     */
    private void init(ObjectAllocationGraph oag) {
        SCCMergedGraph<Obj> mg = new SCCMergedGraph<>(oag);
        TopologicalSorter<MergedNode<Obj>> topoSorter = new TopologicalSorter();
        SetFactory<Method> setFactory = new SetFactory<>();
        invokedMethods = new HashMap<>();

        topoSorter.sort(mg, true).forEach(node -> {
            Set<Method> methods = setFactory.get(getPCEMethods(node, mg));
            node.getContent().forEach(obj -> obj.setAttribute(PCE, methods));
        });
        invokedMethods = null;
        if (Global.isDebug()) {
            computePCEObjects();
        }
        // Compute PCEMethodsOf(Type) in advance, otherwise it may cause concurrency error
        pta.allObjects().stream()
                .map(Obj::getType)
                .distinct()
                .forEach(this::PCEMethodsOf);
    }

    private Set<Method> getPCEMethods(MergedNode<Obj> node,
                                      SCCMergedGraph<Obj> mg) {
        Set<Method> methods = new HashSet<>();
        mg.succsOf(node).forEach(n -> {
            Obj o = n.getContent().iterator().next();
            methods.addAll(PCEMethodsOf(o));
        });
        node.getContent()
                .forEach(o -> methods.addAll(invokedMethodsOf(o)));
        return methods;
    }

    private Set<Method> invokedMethodsOf(Obj obj) {
        if (!invokedMethods.containsKey(obj)) {
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
        }
        return invokedMethods.get(obj);
    }

    private void computePCEObjects() {
        Map<Method, Set<Obj>> pceObjs = new HashMap<>();
        SetFactory<Obj> setFactory = new SetFactory<>();
        pta.allObjects().forEach(obj -> {
            PCEMethodsOf(obj).forEach(method -> {
                if (!pceObjs.containsKey(method)) {
                    pceObjs.put(method, new HashSet<>());
                }
                pceObjs.get(method).add(obj);
            });
        });
        pceObjs.forEach(((method, objs) -> {
            method.setAttribute(PCE, setFactory.get(objs));
        }));
    }
}
