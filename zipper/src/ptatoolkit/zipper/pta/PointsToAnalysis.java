package ptatoolkit.zipper.pta;

import ptatoolkit.pta.basic.Field;
import ptatoolkit.pta.basic.InstanceCallSite;
import ptatoolkit.pta.basic.Method;
import ptatoolkit.pta.basic.Obj;
import ptatoolkit.pta.basic.Type;
import ptatoolkit.pta.basic.Variable;
import ptatoolkit.util.Pair;
import ptatoolkit.util.Triple;

import java.util.Iterator;
import java.util.Set;

public interface PointsToAnalysis {

    // For points-to set.
    /**
     *
     * @return all objects in the points-to analysis
     */
    Set<Obj> allObjects();

    /**
     *
     * @param var
     * @return the objects pointed by variable var,
     * i.e., the points-to set of var
     */
    Set<Obj> pointsToSetOf(Variable var);

    // For pointer flow.
    Iterator<Pair<Variable, Variable>> localAssignIterator();

    // Inter-procedural assignment, including:
    // 1. parameter passing
    // 2. return value
    Iterator<Pair<Variable, Variable>> interProceduralAssignIterator();

    Iterator<Triple<Variable, Obj, Field>> instanceLoadIterator();

    Iterator<Triple<Obj, Field, Variable>> instanceStoreIterator();

    Iterator<Pair<Variable, Variable>> thisAssignIterator();

    // For object allocation relations.

    /**
     *
     * @param method
     * @return the objects allocated in method
     */
    Set<Obj> objectsAllocatedIn(Method method);

    /**
     *
     * @param obj
     * @return the method containing the allocation site of obj.
     */
    Method containingMethodOf(Obj obj);

    /**
     *
     * @param var
     * @return the method where var is declared.
     */
    Method declaringMethodOf(Variable var);

    /**
     *
     * @param obj
     * @return the variable which the obj is assigned to on creation.
     */
    Variable assignedVarOf(Obj obj);


    // For method calls.

    /**
     *
     * @param method
     * @return the callee methods of method
     */
    Set<Method> calleesOf(Method method);

    /**
     *
     * @return all reachable methods in points-to analysis
     */
    Set<Method> reachableMethods();

    /**
     *
     * @param obj
     * @return the methods whose receiver object is obj
     */
    Set<Method> methodsInvokedOn(Obj obj);

    /**
     *
     * @param type
     * @return the methods whose receiver object is of type
     */
    Set<Method> methodsInvokedOn(Type type);

    /**
     *
     * @param recv
     * @return the variables which hold the return values from
     * the method call(s) on recv
     */
    Set<Variable> returnToVariablesOf(Variable recv);

    /**
     *
     * @param method
     * @return the type that declares method
     */
    Type declaringTypeOf(Method method);

    /**
     *
     * @param type
     * @return the direct super type of type
     */
    Type directSuperTypeOf(Type type);

    /**
     *
     * @param type
     * @return all objects of the given type
     */
    Set<Obj> objectsOfType(Type type);
}
