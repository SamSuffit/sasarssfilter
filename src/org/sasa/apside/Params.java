package org.sasa.apside;

class Params {
	String name;
	String value;
	
	public Params(String name, String value) {
		this.name = name;
		this.value= value;
	}

	public String getName() {
		return name;
	}

	public String getValue() {
		return value;
	}
}