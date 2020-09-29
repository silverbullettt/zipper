package ptatoolkit.zipper.analysis;

import ptatoolkit.Global;
import ptatoolkit.pta.basic.Method;
import ptatoolkit.pta.basic.Obj;
import ptatoolkit.pta.basic.Type;
import ptatoolkit.util.ANSIColor;
import ptatoolkit.util.Timer;
import ptatoolkit.zipper.flowgraph.FlowAnalysis;
import ptatoolkit.zipper.flowgraph.InstanceFieldNode;
import ptatoolkit.zipper.flowgraph.Node;
import ptatoolkit.zipper.flowgraph.ObjectFlowGraph;
import ptatoolkit.zipper.flowgraph.VarNode;
import ptatoolkit.zipper.pta.PointsToAnalysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.google.common.base.Predicates.not;
import static ptatoolkit.util.ANSIColor.BLUE;
import static ptatoolkit.util.ANSIColor.YELLOW;
import static ptatoolkit.util.ANSIColor.color;

/**
 * Main class of Zipper, which computes precision-critical methods
 * in the program being analyzed.
 */
public class Zipper {

    private final PointsToAnalysis pta;
    private final ObjectAllocationGraph oag;
    private final PotentialContextElement pce;
    private final ObjectFlowGraph ofg;
    private final InnerClassChecker innerClsChecker;
    private final Map<Method, Integer> methodPts;
    private final AtomicInteger analyzedClasses = new AtomicInteger(0);
    private final AtomicInteger totalPFGNodes = new AtomicInteger(0);
    private final AtomicInteger totalPFGEdges = new AtomicInteger(0);
    private final Map<Type, Collection<Method>> pcmMap = new ConcurrentHashMap<>(1024);

    public Zipper(PointsToAnalysis pta) {
        this.pta = pta;
        this.oag = new ObjectAllocationGraph(pta);
        this.pce = new PotentialContextElement(pta, oag);
        this.innerClsChecker = new InnerClassChecker(pta);
        this.ofg = buildObjectFlowGraph(pta);
        this.methodPts = getMethodPointsToSize(pta);
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
        System.out.println("Building PFGs (Precision Flow Graphs) and computing precision-critical methods ...");
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

        Set<Method> pcm = collectAllPrecisionCriticalMethods(pcmMap,
                computePCMThreshold());
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

        // Obtain IN methods
        Set<Method> inms = ms.stream()
                .filter(not(Method::isPrivate))
                .filter(m -> m.getParameters().stream()
                        .anyMatch(p -> !pta.pointsToSetOf(p).isEmpty()))
                .collect(Collectors.toSet());

        // Obtain OUT methods
        Set<Method> outms = new HashSet<>();
        ms.stream()
                .filter(not(Method::isPrivate))
                .filter(m -> m.getRetVars().stream()
                        .anyMatch(r -> !pta.pointsToSetOf(r).isEmpty()))
                .forEach(outms::add);
        // OUT methods of inner classes and special access$ methods
        // are also considered as the OUT methods of current type
        pce.PCEMethodsOf(type).stream()
                .filter(not(Method::isPrivate).and(Method::isInstance))
                .filter(m -> innerClsChecker.isInnerClass(
                        pta.declaringTypeOf(m), type))
                .forEach(outms::add);
        pce.PCEMethodsOf(type).stream()
                .filter(not(Method::isPrivate).and(Method::isStatic))
                .filter(m -> pta.declaringTypeOf(m).equals(type)
                        && m.toString().contains("access$"))
                .forEach(outms::add);

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
        mergeAnalysisResults(type, fa.numberOfPFGNodes(),
                fa.numberOfPFGEdges(), precisionCriticalMethods);
        fa.clear();
    }

    private void mergeAnalysisResults(Type type, int nrPFGNodes, int nrPFGEdges,
                                      Set<Method> precisionCriticalMethods) {
        analyzedClasses.incrementAndGet();
        totalPFGNodes.addAndGet(nrPFGNodes);
        totalPFGEdges.addAndGet(nrPFGEdges);
        pcmMap.put(type, new ArrayList<>(precisionCriticalMethods));
    }

    private Set<Method> collectAllPrecisionCriticalMethods(
            Map<Type, Collection<Method>> pcmMap, int pcmThreshold) {
        Set<Method> pcm = new HashSet<>();
        pcmMap.forEach((type, pcms) -> {
            if (Global.isExpress() &&
                    getAccumulativePointsToSetSize(pcms) > pcmThreshold) {
                return;
            }
            pcm.addAll(pcms);
        });
        return pcm;
    }

    private int computePCMThreshold() {
        // Use points-to size of whole program as denominator
        return (int) (Global.getExpressThreshold() *
                pta.totalPointsToSetSize());
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
        pcmMap.clear();
    }

    private Map<Method, Integer> getMethodPointsToSize(PointsToAnalysis pta) {
        Map<Method, Integer> results = new HashMap<>();
        pta.reachableMethods().forEach(m ->
                results.put(m,
                        pta.variablesDeclaredIn(m)
                                .stream()
                                .mapToInt(pta::pointsToSetSizeOf)
                                .sum())
        );
        return results;
    }

    private long getAccumulativePointsToSetSize(Collection<Method> methods) {
        return methods.stream()
                .mapToInt(methodPts::get)
                .sum();
    }
}
