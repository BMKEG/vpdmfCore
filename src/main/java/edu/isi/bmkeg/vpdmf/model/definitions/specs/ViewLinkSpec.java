package edu.isi.bmkeg.vpdmf.model.definitions.specs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="viewLink")
public class ViewLinkSpec implements Serializable {
	static final long serialVersionUID = 8047039304729208683L;

	private String name;
	
	private String setRelation;
	
	private String v1;

	private String p1;

	private String c1;

	private String v2;

	private String p2;

	private String c2;

	@XmlAttribute
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@XmlAttribute
	public String getSetRelation() {
		return setRelation;
	}

	public void setSetRelation(String setRelation) {
		this.setRelation = setRelation;
	}

	@XmlAttribute
	public String getV1() {
		return v1;
	}

	public void setV1(String v1) {
		this.v1 = v1;
	}

	@XmlAttribute
	public String getP1() {
		return p1;
	}

	public void setP1(String p1) {
		this.p1 = p1;
	}

	@XmlAttribute
	public String getC1() {
		return c1;
	}

	public void setC1(String c1) {
		this.c1 = c1;
	}

	@XmlAttribute
	public String getV2() {
		return v2;
	}

	public void setV2(String v2) {
		this.v2 = v2;
	}

	@XmlAttribute
	public String getP2() {
		return p2;
	}

	public void setP2(String p2) {
		this.p2 = p2;
	}

	@XmlAttribute
	public String getC2() {
		return c2;
	}

	public void setC2(String c2) {
		this.c2 = c2;
	}	
	
}
