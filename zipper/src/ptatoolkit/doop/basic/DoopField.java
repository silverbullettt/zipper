package ptatoolkit.doop.basic;

import ptatoolkit.pta.basic.Field;

public class DoopField extends Field {

	private final String sig;
	private final int id;
	
	public DoopField(String sig, int id) {
		this.sig = sig;
		this.id = id;
	}

	@Override
	public int getID() {
		return id;
	}
	
	@Override
	public String toString() {
		return sig;
	}

}
