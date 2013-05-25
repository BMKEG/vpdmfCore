package edu.isi.bmkeg.vpdmf.model.definitions.specs;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="formControl")
public class FormControlSpec implements Serializable {
	static final long serialVersionUID = 8047039304729208683L;

	private String name;

	private int position;
	
	private String p;

	private String c;
	
	private String a;
	
	private String label;
	
	@XmlAttribute
	public int getPosition() {
		return position;
	}
	
	public void setPosition(int position) {
		this.position = position;
	}
	
	@XmlAttribute
	private String getLabel() {
		return label;
	}
	
	private void setLabel(String label) {
		this.label = label;
	}
	
	@XmlAttribute
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	@XmlAttribute
	public String getP() {
		return p;
	}

	public void setP(String p) {
		this.p = p;
	}

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
	
	
}
