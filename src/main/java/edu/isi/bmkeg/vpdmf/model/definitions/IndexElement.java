package edu.isi.bmkeg.vpdmf.model.definitions;

/**
 * Timestamp: Thu_Jun_19_120936_2003;
 */

import edu.isi.bmkeg.vpdmf.model.definitions.ViewDefinition;

import java.io.*;

public class IndexElement implements Serializable {

	static final long serialVersionUID = -4099348176576542439L;

	private int position;
		
	private String p;
	
	private String c;
	
	private String a;
	
	private boolean nullable;

	private boolean uniqueKey;

	public String toString() {
		return position + ": ]" + this.p + "|" + this.c + "." + this.a;
	}

	public void destroy() {
	}

	public String getAttributeAddress() {
		return "]" + this.p + "|" + this.c + "." + this.a;

	}

	public int getPosition() {
		return this.position;
	}

	public boolean getNullable() {
		return this.nullable;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public void setNullable(boolean Nullable) {
		this.nullable = Nullable;
	}

	public boolean isUniqueKey() {
		return uniqueKey;
	}

	public void setUniqueKey(boolean uniqueKey) {
		this.uniqueKey = uniqueKey;
	}

	public String getP() {
		return p;
	}

	public void setP(String p) {
		this.p = p;
	}

	public String getC() {
		return c;
	}

	public void setC(String c) {
		this.c = c;
	}

	public String getA() {
		return a;
	}

	public void setA(String a) {
		this.a = a;
	}

};
