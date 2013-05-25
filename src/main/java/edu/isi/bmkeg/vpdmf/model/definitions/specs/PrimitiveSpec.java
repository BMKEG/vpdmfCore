package edu.isi.bmkeg.vpdmf.model.definitions.specs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="primitive")
public class PrimitiveSpec implements Serializable {
	static final long serialVersionUID = 8047039304729208683L;

	private String name;
	
	private String lookupView;
	
	private List<PrimitiveLinkSpec> pvLinks = new ArrayList<PrimitiveLinkSpec>();

	private List<ClassSpec> classes = new ArrayList<ClassSpec>();

	private List<ConditionSpec> conditions= new ArrayList<ConditionSpec>();

	private boolean editable = true;
	
	private boolean nullable = false;
	
	@XmlAttribute
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@XmlAttribute
	public String getLookupView() {
		return lookupView;
	}

	public void setLookupView(String lookupView) {
		this.lookupView = lookupView;
	}

    @XmlElementWrapper( name="pvLinks" )
    @XmlElement( name="pvLink" )
	public List<PrimitiveLinkSpec> getPvLinks() {
		return pvLinks;
	}

	public void setPvLinks(List<PrimitiveLinkSpec> pvLinks) {
		this.pvLinks = pvLinks;
	}

    @XmlElementWrapper( name="classes" )
    @XmlElement( name="class" )
	public List<ClassSpec> getClasses() {
		return classes;
	}

	public void setClasses(List<ClassSpec> classes) {
		this.classes = classes;
	}

	@XmlElementWrapper( name="conditions" )
    @XmlElement( name="condition" )
	public List<ConditionSpec> getConditions() {
		return conditions;
	}

	public void setConditions(List<ConditionSpec> conditions) {
		this.conditions = conditions;
	}

	@XmlAttribute
	public boolean isEditable() {
		return editable;
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	@XmlAttribute
	public boolean isNullable() {
		return nullable;
	}

	public void setNullable(boolean nullable) {
		this.nullable = nullable;
	}
	
}
