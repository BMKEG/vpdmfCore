package edu.isi.bmkeg.vpdmf.model.definitions.specs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlMixed;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="formDesign")
public class FormDesignSpec implements Serializable {
	static final long serialVersionUID = 8047039304729208683L;

	private String name;

	private String view;
	
	private List<FormControlSpec> elements = new ArrayList<FormControlSpec>();

	@XmlAttribute
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	@XmlAttribute
	public String getView() {
		return view;
	}
	
	public void setView(String view) {
		this.view = view;
	}
	
    @XmlElementRefs( {
        @XmlElementRef(name = "grid", type = GridSpec.class),
        @XmlElementRef(name = "textRegion", type = TextRegionSpec.class),
        @XmlElementRef(name = "lookup", type = LookupSpec.class),
        @XmlElementRef(name = "textInput",   type = TextInputSpec.class),
        @XmlElementRef(name = "label", type = LabelSpec.class)
        })
    @XmlMixed
	public List<FormControlSpec> getElements() {
		return elements;
	}
	
	public void setElements(List<FormControlSpec> elements) {
		this.elements = elements;
	}
	
	public void cleanUpElements() {
		
		List<FormControlSpec> l = new ArrayList<FormControlSpec>();
		
		Iterator it = this.elements.iterator();
		while(it.hasNext()) {
			Object o = it.next();
			if( (o instanceof FormControlSpec) ) {
				l.add( (FormControlSpec) o);
			}
		}	
		
		this.elements = l;

	}
	
}
