package ptatoolkit.zipper.doop;

import com.google.common.collect.Streams;
import ptatoolkit.Global;
import ptatoolkit.Options;
import ptatoolkit.doop.DataBase;
import ptatoolkit.doop.Query;
import ptatoolkit.doop.factory.FieldFactory;
import ptatoolkit.doop.factory.MethodFactory;
import ptatoolkit.doop.factory.ObjFactory;
import ptatoolkit.doop.factory.TypeFactory;
import ptatoolkit.doop.factory.VariableFactory;
import ptatoolkit.pta.basic.Field;
import ptatoolkit.pta.basic.InstanceMethod;
import ptatoolkit.pta.basic.Method;
import ptatoolkit.pta.basic.Obj;
import ptatoolkit.pta.basic.Type;
import ptatoolkit.pta.basic.Variable;
import ptatoolkit.util.MutableInteger;
import ptatoolkit.util.Pair;
import ptatoolkit.util.Timer;
import ptatoolkit.util.Triple;
import ptatoolkit.zipper.pta.PointsToAnalysis;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import static ptatoolkit.zipper.doop.Attribute.ALLOCATED;
import static ptatoolkit.zipper.doop.Attribute.CALLEE;
import static ptatoolkit.zipper.doop.Attribute.DECLARING;
import static ptatoolkit.zipper.doop.Attribute.DECLARING_TYPE;
import static ptatoolkit.zipper.doop.Attribute.MTD_ON;
import static ptatoolkit.zipper.doop.Attribute.OBJECT_ASSIGNED;
import static ptatoolkit.zipper.doop.Attribute.POINTS_TO_SET_SIZE;
import static ptatoolkit.zipper.doop.Attribute.PTS;
import static ptatoolkit.zipper.doop.Attribute.RETURN_TO;
import static ptatoolkit.zipper.doop.Attribute.VARS_IN;

public class DoopPointsToAnalysis implements PointsToAnalysis {

    private final DataBase db;
    private Set<Obj> allObjs;
    private int totalPTSSize;
    private Set<Method> reachableMethods;
    // The following factories may be used by iterators
    private VariableFactory varFactory;
    private ObjFactory objFactory;
    private FieldFactory fieldFactory;

    private Set<String> specialObjects;
    private Map<Type, Type> directSuperType;
    private Map<Type, Set<Obj>> typeObjects;
    private Map<Type, Set<Method>> typeMethods;
    private List<Pair<Variable, Variable>> thisAssign;

    private static final String ARR_FIELD = "@ARRAY";

    public DoopPointsToAnalysis(Options options) {
        Timer ptaTimer = new Timer("Points-to Analysis Timer");
        System.out.println("Reading points-to analysis results ... ");
        ptaTimer.start();
        File dbDir = options.getDbPath() != null ?
                new File(options.getDbPath()) : null;
        File cacheDir = new File(options.getCachePath());
        this.db = new DataBase(dbDir, cacheDir, options.getApp());
        init();
        ptaTimer.stop();
    }

    @Override
    public Set<Obj> allObjects() {
        return allObjs;
    }

    @Override
    public Set<Method> reachableMethods() {
        return reachableMethods;
    }

    @Override
    public Set<Obj> pointsToSetOf(Variable var) {
        if (var.hasAttribute(PTS)) {
            return (Set<Obj>) var.getAttribute(PTS);
        } else { // in this case, the variable is a null pointer
            if (Global.isDebug()) {
                System.out.println(var + " is a null pointer.");
            }
            return Collections.emptySet();
        }
    }

    @Override
    public int pointsToSetSizeOf(Variable var) {
        if (var.hasAttribute(POINTS_TO_SET_SIZE)) {
            MutableInteger size = (MutableInteger) var.getAttribute(POINTS_TO_SET_SIZE);
            return size.intValue();
        } else {
            return 0;
        }
    }

    @Override
    public int totalPointsToSetSize() {
        return totalPTSSize;
    }

    @Override
    public Set<Variable> variablesDeclaredIn(Method method) {
        return method.getAttributeSet(VARS_IN);
    }

    @Override
    public Set<Obj> objectsAllocatedIn(Method method) {
        return method.getAttributeSet(ALLOCATED);
    }

    @Override
    public Set<Method> calleesOf(Method method) {
        return method.getAttributeSet(CALLEE);
    }

    @Override
    public Set<Method> methodsInvokedOn(Obj obj) {
        return obj.getAttributeSet(MTD_ON);
    }

    @Override
    public Set<Variable> returnToVariablesOf(Variable recv) {
        return recv.getAttributeSet(RETURN_TO);
    }

    @Override
    public Iterator<Pair<Variable, Variable>> localAssignIterator() {
        return new Iterator<Pair<Variable, Variable>>() {

            private Iterator<List<String>> assignIter =
                    db.query(Query.LOCAL_ASSIGN);

            @Override
            public boolean hasNext() {
                return assignIter.hasNext();
            }

            @Override
            public Pair<Variable, Variable> next() {
                if (assignIter.hasNext()) {
                    List<String> assign = assignIter.next();
                    Variable to = varFactory.get(assign.get(0));
                    Variable from = varFactory.get(assign.get(1));
                    return new Pair<>(to, from);
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }

    @Override
    public Iterator<Pair<Variable, Variable>> interProceduralAssignIterator() {
        return new Iterator<Pair<Variable, Variable>>() {

            private Iterator<List<String>> assignIter =
                    db.query(Query.INTERPROCEDURAL_ASSIGN);

            @Override
            public boolean hasNext() {
                return assignIter.hasNext();
            }

            @Override
            public Pair<Variable, Variable> next() {
                if (assignIter.hasNext()) {
                    List<String> assign = assignIter.next();
                    Variable to = varFactory.get(assign.get(0));
                    Variable from = varFactory.get(assign.get(1));
                    return new Pair<>(to, from);
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }

    @Override
    public Iterator<Triple<Variable, Obj, Field>> instanceLoadIterator() {
        return new Iterator<Triple<Variable, Obj, Field>>() {

            private Iterator<List<String>> loadIter =
                    db.query(Query.INSTANCE_LOAD);
            private Iterator<List<String>> arrayLoadIter =
                    db.query(Query.ARRAY_LOAD);

            @Override
            public boolean hasNext() {
                return loadIter.hasNext() || arrayLoadIter.hasNext();
            }

            @Override
            public Triple<Variable, Obj, Field> next() {
                if (loadIter.hasNext()) {
                    List<String> load = loadIter.next();
                    Variable to = varFactory.get(load.get(0));
                    Obj base = objFactory.get(load.get(1));
                    Field field = fieldFactory.get(load.get(2));
                    return new Triple<>(to, base, field);
                } else if (arrayLoadIter.hasNext()) {
                    List<String> load = arrayLoadIter.next();
                    Variable to = varFactory.get(load.get(0));
                    Obj array = objFactory.get(load.get(1));
                    Field field = fieldFactory.get(ARR_FIELD);
                    return new Triple<>(to, array, field);
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }

    @Override
    public Iterator<Triple<Obj, Field, Variable>> instanceStoreIterator() {
        return new Iterator<Triple<Obj, Field, Variable>>() {

            private Iterator<List<String>> storeIter =
                    db.query(Query.INSTANCE_STORE);
            private Iterator<List<String>> arrayStoreIter =
                    db.query(Query.ARRAY_STORE);

            @Override
            public boolean hasNext() {
                return storeIter.hasNext() || arrayStoreIter.hasNext();
            }

            @Override
            public Triple<Obj, Field, Variable> next() {
                if (storeIter.hasNext()) {
                    List<String> store = storeIter.next();
                    Obj base = objFactory.get(store.get(0));
                    Field field = fieldFactory.get(store.get(1));
                    Variable from = varFactory.get(store.get(2));
                    return new Triple<>(base, field, from);
                } else if (arrayStoreIter.hasNext()) {
                    List<String> store = arrayStoreIter.next();
                    Obj array = objFactory.get(store.get(0));
                    Field field = fieldFactory.get(ARR_FIELD);
                    Variable from = varFactory.get(store.get(1));
                    return new Triple<>(array, field, from);
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }

    @Override
    public Iterator<Pair<Variable, Variable>> thisAssignIterator() {
        return thisAssign.iterator();
    }

    @Override
    public Type declaringTypeOf(Method method) {
        return (Type) method.getAttribute(DECLARING_TYPE);
    }

    @Override
    public Type directSuperTypeOf(Type type) {
        return directSuperType.get(type);
    }

    @Override
    public Set<Obj> objectsOfType(Type type) {
        return typeObjects.get(type);
    }

    @Override
    public Set<Method> methodsInvokedOn(Type type) {
        return typeMethods.get(type);
    }

    private void init() {
        TypeFactory typeFactory = new TypeFactory();
        varFactory = new VariableFactory();
        objFactory =new ObjFactory(db, typeFactory);
        fieldFactory = new FieldFactory();
        MethodFactory mtdFactory = new MethodFactory(db, varFactory);

        // Set of variable names whose points-to sets may be needed
        Set<String> interestingVarNames = new HashSet<>();

        // 2. obtain all reachable instance methods
        db.query(Query.INST_METHODS).forEachRemaining(list -> {
            String mtdSig = list.get(0);
            InstanceMethod instMtd =
                    (InstanceMethod) mtdFactory.get(mtdSig);
            interestingVarNames.add(instMtd.getThis().toString());
            Streams.concat(instMtd.getParameters().stream(),
                    instMtd.getRetVars().stream())
                    .forEach(var -> interestingVarNames.add(var.toString()));
        });

        db.query(Query.CALL_RETURN_TO).forEachRemaining(list -> {
            Variable recv = varFactory.get(list.get(0));
            Variable to = varFactory.get(list.get(1));
            recv.addToAttributeSet(RETURN_TO, to);
            interestingVarNames.add(recv.toString());
        });

        // 3. build points-to sets of interesting variables
        totalPTSSize = 0;
        buildPointsToSet(varFactory, objFactory, interestingVarNames);

        // compute the objects allocated in each method
        specialObjects = Streams.stream(db.query(Query.SPECIAL_OBJECTS))
                .map(list -> list.get(0))
                .collect(Collectors.toSet());
        computeAllocatedObjects(objFactory, mtdFactory);

        // 4. build caller-callee relations
        buildCallees(mtdFactory, varFactory);

        // 6. build relations between objects and instance methods
        buildMethodsInvokedOnObjects(mtdFactory);

        // 7. map variables to their declaring methods
        buildVarDeclaringMethods(varFactory, mtdFactory);

        // 8. map objects to their assigned variables
        buildObjectAssignedVariables(objFactory, varFactory);

        buildDeclaringType(mtdFactory, typeFactory);

        buildDirectSuperType(typeFactory);

        typeObjects = new HashMap<>();
        typeMethods = new HashMap<>();
        allObjects().forEach(obj -> {
            Type type = obj.getType();
            typeObjects.putIfAbsent(type, new HashSet<>());
            typeObjects.get(type).add(obj);
            typeMethods.putIfAbsent(type, new HashSet<>());
            typeMethods.get(type).addAll(methodsInvokedOn(obj));
        });
    }

    private void buildPointsToSet(VariableFactory varFactory, ObjFactory objFactory,
                                  Set<String> interestingVarNames) {
        allObjs = new HashSet<>();
        db.query(Query.VPT).forEachRemaining(list -> {
            String objName = list.get(0);
            String varName = list.get(1);
            Obj obj = objFactory.get(objName);
            Variable var = varFactory.get(varName);
            if (interestingVarNames.contains(varName)) {
                // add points-to set to var as its attribute
                var.addToAttributeSet(PTS, obj);
            }
            allObjs.add(obj);
            increasePointsToSetSizeOf(var);
            ++totalPTSSize;
        });
    }

    private void increasePointsToSetSizeOf(Variable var) {
        if (var.hasAttribute(POINTS_TO_SET_SIZE)) {
            MutableInteger size = (MutableInteger) var.getAttribute(POINTS_TO_SET_SIZE);
            size.increase();
        } else {
            var.setAttribute(POINTS_TO_SET_SIZE, new MutableInteger(1));
        }
    }

    @Override
    public Method containingMethodOf(Obj obj) {
        return (Method) obj.getAttribute(ALLOCATED);
    }

    @Override
    public Method declaringMethodOf(Variable var) {
        return (Method) var.getAttribute(DECLARING);
    }

    @Override
    public Variable assignedVarOf(Obj obj) {
        return (Variable) obj.getAttribute(OBJECT_ASSIGNED);
    }

    private void computeAllocatedObjects(ObjFactory objFactory,
                                         MethodFactory mtdFactory) {
        db.query(Query.OBJECT_IN).forEachRemaining(list -> {
            String objName = list.get(0);
            if (isNormalObject(objName)) {
                Obj obj = objFactory.get(objName);
                Method method = mtdFactory.get(list.get(1));
                method.addToAttributeSet(ALLOCATED, obj);
                obj.setAttribute(ALLOCATED, method);
            }
        });
    }

    private boolean isNormalObject(String objName) {
        return !specialObjects.contains(objName) &&
                !objName.startsWith("<class "); // class constant
    }

    private void buildCallees(MethodFactory mtdFactory, VariableFactory varFactory) {
        reachableMethods = new HashSet<>();
        thisAssign = new LinkedList<>();
        Map<String, String> callIn = new HashMap<>();
        Map<String, String> callBase = new HashMap<>();
        db.query(Query.CALLSITEIN).forEachRemaining(list -> {
            String call = list.get(0);
            String methodSig = list.get(1);
            callIn.put(call, methodSig);
        });
        db.query(Query.INST_CALL_RECV).forEachRemaining(list -> {
            String call = list.get(0);
            String baseVar = list.get(1);
            callBase.put(call, baseVar);
        });
        db.query(Query.CALL_EDGE).forEachRemaining(list -> {
            String callsiteStr = list.get(0);
            String callerSig = callIn.get(callsiteStr);
            if (callerSig != null) {
                Method caller = mtdFactory.get(callerSig);
                Method callee = mtdFactory.get(list.get(1));
                caller.addToAttributeSet(CALLEE, callee);
                reachableMethods.add(caller);
                reachableMethods.add(callee);

                // Prepare information for this-assignment
                if (callee.isInstance()) {
                    InstanceMethod m = (InstanceMethod) callee;
                    Variable thisVar = m.getThis();
                    String baseVarStr = callBase.get(callsiteStr);
                    if (baseVarStr != null) {
                        Variable baseVar = varFactory.get(baseVarStr);
                        thisAssign.add(new Pair<>(thisVar, baseVar));
                    }
                }

            } else if (Global.isDebug()) {
                System.out.println("Null caller of: " + list.get(0));
            }
        });
    }

    private void buildMethodsInvokedOnObjects(MethodFactory mtdFactory) {
        mtdFactory.getAllElements()
                .stream()
                .filter(m -> m.isInstance())
                .map(m -> (InstanceMethod) m)
                .forEach(instMtd -> {
                    Variable thisVar = instMtd.getThis();
                    pointsToSetOf(thisVar).forEach(obj -> {
                        obj.addToAttributeSet(MTD_ON, instMtd);
                    });
                });
    }

    private void buildVarDeclaringMethods(VariableFactory varFactory,
                                          MethodFactory mtdFactory) {
        db.query(Query.VAR_IN).forEachRemaining(list -> {
            Variable var = varFactory.get(list.get(0));
            Method inMethod = mtdFactory.get(list.get(1));
            var.setAttribute(DECLARING, inMethod);
            inMethod.addToAttributeSet(VARS_IN, var);
        });
    }

    private void buildObjectAssignedVariables(ObjFactory objFactory,
                                              VariableFactory varFactory) {
        db.query(Query.OBJECT_ASSIGN).forEachRemaining(list -> {
            Obj obj = objFactory.get(list.get(0));
            Variable var = varFactory.get(list.get(1));
            obj.setAttribute(OBJECT_ASSIGNED, var);
        });
    }

    private void buildDeclaringType(MethodFactory mtdFactory,
                                    TypeFactory typeFactory) {
        mtdFactory.getAllElements().forEach(m -> {
            String sig = m.toString();
            String typeName = sig.substring(1, sig.indexOf(':'));
            Type type = typeFactory.get(typeName);
            m.setAttribute(DECLARING_TYPE, type);
        });
    }

    private void buildDirectSuperType(TypeFactory typeFactory) {
        directSuperType = new HashMap<>();
        db.query(Query.DIRECT_SUPER_TYPE).forEachRemaining(list -> {
            Type type = typeFactory.get(list.get(0));
            Type superType = typeFactory.get(list.get(1));
            directSuperType.put(type, superType);
        });
    }
}
