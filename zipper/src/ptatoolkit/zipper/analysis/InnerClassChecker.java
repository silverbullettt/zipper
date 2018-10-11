package ptatoolkit.zipper.analysis;

import ptatoolkit.pta.basic.Type;
import ptatoolkit.zipper.pta.PointsToAnalysis;

/**
 * Check whether a class is inner class of another class
 */
public class InnerClassChecker {

    private final PointsToAnalysis pta;

    public InnerClassChecker(PointsToAnalysis pta) {
        this.pta = pta;
    }

    /**
     *
     * @param pInner potential inner class
     * @param pOuter potential outer class
     * @return whether pInner is an inner class of pOuter
     */
    public boolean isInnerClass(Type pInner, Type pOuter) {
        String pInnerStr = pInner.toString();
        do {
            if (pInnerStr.startsWith(pOuter.toString() + "$")) {
                return true;
            }
            pOuter = pta.directSuperTypeOf(pOuter);
        } while (pOuter != null);
        return false;
    }

}
