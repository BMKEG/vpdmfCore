package edu.isi.bmkeg.vpdmf.model.definitions.specs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="pvLink")
public class PrimitiveLinkSpec implements Serializable {
	static final long serialVersionUID = 8047039304729208683L;

	private boolean paged = true;
		
	private String c1;
	
	private String role;
	
	private String pv2;

	private String c2;

	public PrimitiveLinkSpec() {}
	
	public PrimitiveLinkSpec(String c1, String role, String pv2, String c2) {
		this.c1 = c1;
		this.role = role;
		this.pv2 = pv2;
		this.c2 = c2;
	}
	
	@XmlAttribute
	public boolean isPaged() {
		return paged;
	}

	public void setPaged(boolean paged) {
		this.paged = paged;
	}
	
	@XmlAttribute
	public String getC1() {
		return c1;
	}

	public void setC1(String c1) {
		this.c1 = c1;
	}

	@XmlAttribute
	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	@XmlAttribute
	public String getPv2() {
		return pv2;
	}

	public void setPv2(String pv2) {
		this.pv2 = pv2;
	}

	@XmlAttribute
	public String getC2() {
		return c2;
	}

	public void setC2(String c2) {
		this.c2 = c2;
	}

	public String toString() {
		return "|" + c1 + "." + role + "--->]" + pv2 + "|" + c2;  
	}

	
}
