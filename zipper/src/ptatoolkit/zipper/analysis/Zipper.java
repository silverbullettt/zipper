package ptatoolkit.zipper.analysis;

import ptatoolkit.Global;
import ptatoolkit.pta.basic.Method;
import ptatoolkit.pta.basic.Obj;
import ptatoolkit.pta.basic.Type;
import ptatoolkit.pta.basic.Variable;
import ptatoolkit.util.ANSIColor;
import ptatoolkit.util.Timer;
import ptatoolkit.zipper.flowgraph.FlowAnalysis;
import ptatoolkit.zipper.flowgraph.InstanceFieldNode;
import ptatoolkit.zipper.flowgraph.Node;
import ptatoolkit.zipper.flowgraph.ObjectFlowGraph;
import ptatoolkit.zipper.flowgraph.VarNode;
import ptatoolkit.zipper.pta.PointsToAnalysis;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static ptatoolkit.util.ANSIColor.BLUE;
import static ptatoolkit.util.ANSIColor.RED;
import static ptatoolkit.util.ANSIColor.YELLOW;
import static ptatoolkit.util.ANSIColor.color;

/**
 * Main class of Zipper, which computes precision-critical methods
 * in the program being analyzed.
 */
public class Zipper {

    private static int IN_TYPE_THRESHOLD = 50;

    private final PointsToAnalysis pta;
    private final ObjectAllocationGraph oag;
    private final PotentialContextElement pce;
    private final ObjectFlowGraph ofg;
    private final InnerClassChecker innerClsChecker;
    private AtomicInteger analyzedClasses = new AtomicInteger(0);
    private AtomicInteger totalPFGNodes = new AtomicInteger(0);
    private AtomicInteger totalPFGEdges = new AtomicInteger(0);
    private Set<Method> pcm = ConcurrentHashMap.newKeySet();

    public Zipper(PointsToAnalysis pta) {
        this.pta = pta;
        this.oag = new ObjectAllocationGraph(pta);
        this.pce = new PotentialContextElement(pta, oag);
        this.innerClsChecker = new InnerClassChecker(pta);
        this.ofg = buildObjectFlowGraph(pta);
    }

    public static void outputNumberOfClasses(PointsToAnalysis pta) {
        int nrClasses = (int) pta.allObjects().stream()
                .map(Obj::getType)
                .distinct()
                .count();
        System.out.println("#classes: " +
                ANSIColor.BOLD + ANSIColor.GREEN + nrClasses + ANSIColor.RESET);
        System.out.println();
    }

    public static ObjectFlowGraph buildObjectFlowGraph(PointsToAnalysis pta) {
        Timer ofgTimer = new Timer("Object Flow Graph Timer");
        System.out.println("Building OFG (Object Flow Graph) ... ");
        ofgTimer.start();
        ObjectFlowGraph ofg = new ObjectFlowGraph(pta);
        ofgTimer.stop();
        // System.out.println(ofgTimer);
        outputObjectFlowGraphSize(ofg);
        return ofg;
    }

    public static void outputObjectFlowGraphSize(ObjectFlowGraph ofg) {
        int nrNodes = ofg.allNodes().size();
        int nrEdges = 0;
        for (Node node : ofg.allNodes()) {
            nrEdges += ofg.outEdgesOf(node).size();
        }

        System.out.println("#nodes in OFG: " +
                ANSIColor.BOLD + ANSIColor.GREEN + nrNodes + ANSIColor.RESET);
        System.out.println("#edges in OFG: " +
                ANSIColor.BOLD + ANSIColor.GREEN + nrEdges + ANSIColor.RESET);
        System.out.println();
    }

    /**
     *
     * @return set of precision-critical methods in the program
     */
    public Set<Method> analyze() {
        reset();
        System.out.println("Building PFGs (Pollution Flow Graphs) and computing precision-critical methods ...");
        List<Type> types = pta.allObjects().stream()
                .map(Obj::getType)
                .distinct()
                .sorted(Comparator.comparing(Type::toString))
                .collect(Collectors.toList());
        if (Global.getThread() == Global.UNDEFINE) {
            computePCM(types);
        } else {
            computePCMConcurrent(types, Global.getThread());
        }
        System.out.println("#avg. nodes in PFG: " +
                ANSIColor.BOLD + ANSIColor.GREEN +
                Math.round(totalPFGNodes.floatValue() / analyzedClasses.get())  +
                ANSIColor.RESET);
        System.out.println("#avg. edges in PFG: " +
                ANSIColor.BOLD + ANSIColor.GREEN +
                Math.round(totalPFGEdges.floatValue() / analyzedClasses.get()) +
                ANSIColor.RESET);
        System.out.println();

        System.out.println("#Precision-critical methods: " +
                ANSIColor.BOLD + ANSIColor.GREEN + pcm.size() + ANSIColor.RESET);
        return pcm;
    }

    private void computePCM(List<Type> types) {
        FlowAnalysis fa = new FlowAnalysis(pta, oag, pce, ofg);
        types.forEach(type -> analyze(type, fa));
    }

    private void computePCMConcurrent(List<Type> types, int nThread) {
        ExecutorService executorService = Executors.newFixedThreadPool(nThread);
        types.forEach(type ->
                executorService.execute(() -> {
                    FlowAnalysis fa = new FlowAnalysis(pta, oag, pce, ofg);
                    analyze(type, fa);
                }));
        executorService.shutdown();
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param type
     * @param fa
     * Compute the set of precision-critical methods for a class/type and add these methods
     * to the pcm collection.
     */
    private void analyze(Type type, FlowAnalysis fa) {
        if (Global.isDebug()) {
            System.out.println("----------------------------------------");
        }
        // System.out.println(color(YELLOW, "Zipper: analyzing ") + type);

        // Obtain all methods of type (including inherited methods)
        Set<Method> ms = pta.objectsOfType(type).stream()
                .map(pta::methodsInvokedOn)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        // Obtain the IN methods
        Set<Method> inms = ms.stream()
                .filter(m -> {
                    if (!m.isPrivate()) {
                        for (Variable param : m.getParameters()) {
                            if (!pta.pointsToSetOf(param).isEmpty()) {
                                // for zipper*
                                if (!Global.isExpress()) {
                                    return true;
                                } else {
                                    return satifyInTypeThreshold(m);
                                }
                            }
                        }
                    }
                    return false;
                })
                .collect(Collectors.toSet());

        // Obtain OUT methods
        Set<Method> outms = new HashSet<>();
        ms.forEach(m -> {
            if (!m.isPrivate()) {
                for (Variable ret : m.getRetVars()) {
                    if (!pta.pointsToSetOf(ret).isEmpty()) {
                        outms.add(m);
                        break;
                    }
                }
            }
        });
        // OUT methods of inner classes and special access$ methods
        // are also considered as the OUT methods of current type
        pce.PCEMethodsOf(type).forEach(m -> {
            Type mtdType = pta.declaringTypeOf(m);
            if (!m.isPrivate()) {
                if (m.isInstance()) {
                    if (innerClsChecker.isInnerClass(mtdType, type)) {
                        outms.add(m);
                    }
                } else {
                    if (mtdType.equals(type)
                            && m.toString().contains("access$")
                            ) {
                        outms.add(m);
                    }
                }
            }
        });

        if (Global.isDebug()) {
            System.out.println(color(YELLOW, "In methods:"));
            inms.stream()
                    .sorted(Comparator.comparing(Method::toString))
                    .forEach(m -> System.out.println("  " + m));
            System.out.println(color(YELLOW, "Out methods:"));
            outms.stream()
                    .sorted(Comparator.comparing(Method::toString))
                    .forEach(m -> System.out.println("  " + m));
        }

        fa.initialize(type, inms, outms);
        inms.forEach(fa::analyze);
        Set<Node> flowNodes = fa.getFlowNodes();
        Set<Method> precisionCriticalMethods = getPrecisionCriticalMethods(type, flowNodes);
        if (Global.isDebug()) {
            if (!precisionCriticalMethods.isEmpty()) {
                System.out.println(color(BLUE, "Flow found: ") + type);
            }
        }
        mergeAnalysisResults(fa.numberOfPFGNodes(), fa.numberOfPFGEdges(), precisionCriticalMethods);
        fa.clear();
    }

    private void mergeAnalysisResults(int nrPFGNodes, int nrPFGEdges, Set<Method> precisionCriticalMethods) {
        analyzedClasses.incrementAndGet();
        totalPFGNodes.addAndGet(nrPFGNodes);
        totalPFGEdges.addAndGet(nrPFGEdges);
        pcm.addAll(precisionCriticalMethods);
    }

    private Set<Method> getPrecisionCriticalMethods(Type type, Set<Node> nodes) {
        return nodes.stream()
                .map(this::node2ContainingMethod)
                .filter(pce.PCEMethodsOf(type)::contains)
                .collect(Collectors.toSet());
    }

    private Method node2ContainingMethod(Node node) {
        if (node instanceof VarNode) {
            VarNode varNode = (VarNode) node;
            return pta.declaringMethodOf(varNode.getVar());
        } else {
            InstanceFieldNode ifNode = (InstanceFieldNode) node;
            return pta.containingMethodOf(ifNode.getBase());
        }
    }

    private void reset() {
        analyzedClasses.set(0);
        totalPFGNodes.set(0);
        totalPFGEdges.set(0);
        pcm.clear();
    }

    private boolean satifyInTypeThreshold(Method inMethod) {
        for (Variable param : inMethod.getParameters()) {
            Set<Type> inTypes = new HashSet<>();
            for (Obj obj : pta.pointsToSetOf(param)) {
                inTypes.add(obj.getType());
                if (inTypes.size() > IN_TYPE_THRESHOLD) {
                    return true;
                }
            }
        }
        return false;
    }

}
