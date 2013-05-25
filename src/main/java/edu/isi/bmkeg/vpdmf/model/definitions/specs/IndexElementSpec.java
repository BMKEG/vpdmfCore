package edu.isi.bmkeg.vpdmf.model.definitions.specs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="indexElement")
public class IndexElementSpec implements Serializable {
	static final long serialVersionUID = 8047039304729208683L;

	private int pos;

	private String p;
	
	private String c;

	private String a;

	private Boolean uniqueKey;

	@XmlAttribute
	public int getPos() {
		return pos;
	}

	public void setPos(int pos) {
		this.pos = pos;
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

	@XmlAttribute
	public Boolean getUniqueKey() {
		return uniqueKey;
	}

	public void setUniqueKey(Boolean uniqueKey) {
		this.uniqueKey = uniqueKey;
	}
		
}
