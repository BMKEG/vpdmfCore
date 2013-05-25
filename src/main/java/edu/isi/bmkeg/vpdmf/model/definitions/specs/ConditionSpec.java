package edu.isi.bmkeg.vpdmf.model.definitions.specs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="condition")
public class ConditionSpec implements Serializable {
	static final long serialVersionUID = 8047039304729208683L;

	private String c;

	private String a;

	private String v;

	@XmlAttribute
	public String getC() {
		return c;
	}

	public void setC(String c) {
		this.c = c;
	}

	@XmlAttribute
	public String getA() {
		return a;
	}

	public void setA(String a) {
		this.a = a;
	}

	@XmlAttribute
	public String getV() {
		return v;
	}

	public void setV(String v) {
		this.v = v;
	}
		
}
