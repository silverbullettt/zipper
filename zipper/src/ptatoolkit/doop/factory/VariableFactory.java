package ptatoolkit.doop.factory;

import ptatoolkit.doop.basic.DoopVariable;
import ptatoolkit.pta.basic.Variable;

public class VariableFactory extends ElementFactory<Variable> {

    @Override
    protected Variable createElement(String name) {
        return new DoopVariable(name, ++count);
    }

}
