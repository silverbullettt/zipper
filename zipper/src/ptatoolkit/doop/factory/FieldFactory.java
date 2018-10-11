package ptatoolkit.doop.factory;

import ptatoolkit.doop.basic.DoopField;
import ptatoolkit.pta.basic.Field;

public class FieldFactory extends ElementFactory<Field> {

	@Override
	protected Field createElement(String name) {
		return new DoopField(name, ++count);
	}
	
}
