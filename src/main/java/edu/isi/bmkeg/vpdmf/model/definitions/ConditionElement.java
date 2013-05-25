package edu.isi.bmkeg.vpdmf.model.definitions;

/**
 * Timestamp: Thu_Jun_19_120936_2003;
 */

import edu.isi.bmkeg.vpdmf.model.definitions.ViewDefinition;

import java.io.*;

public class ConditionElement implements Serializable {

	static final long serialVersionUID = -4099348176576542439L;

	private String className;
	private String attName;
	private String value;
	private int pvCount;

	public String toString() {
		return "|" + className + "." + attName + "='" + value + "'";
	}

	public void destroy() {
	}

	public String getClassName() {
		return this.className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getAttName() {
		return this.attName;
	}

	public void setAttName(String attName) {
		this.attName = attName;
	}

	public String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public int getPvCount() {
		return this.pvCount;
	}

	public void setPvCount(int pvCount) {
		this.pvCount = pvCount;
	}

	public String readAddress() {
		return "|" + this.className + "." + this.attName;
	}

};
