package ptatoolkit.doop.factory;

import ptatoolkit.doop.basic.DoopType;
import ptatoolkit.pta.basic.Type;

public class TypeFactory extends ElementFactory<Type> {

    @Override
    protected Type createElement(String name) {
        return new DoopType(name, ++count);
    }
}
