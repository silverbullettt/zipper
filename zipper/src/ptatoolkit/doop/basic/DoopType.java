package ptatoolkit.doop.basic;

import ptatoolkit.pta.basic.Type;

public class DoopType extends Type {

	private final String typeName;
	private final int id;
	
	public DoopType(String typeName, int id) {
		this.typeName = typeName;
		this.id = id;
	}
	
	@Override
	public int getID() {
		return id;
	}

	@Override
	public String toString() {
		return typeName;
	}
	
}
