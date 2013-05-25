package edu.isi.bmkeg.vpdmf.model.definitions.specs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlMixed;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="grid")
public class GridSpec extends FormControlSpec implements Serializable {
	static final long serialVersionUID = 8047039304729208683L;

	private List<FormControlSpec> elements = new ArrayList<FormControlSpec>();
	
    @XmlElementRefs( {
        @XmlElementRef(name = "grid", type = GridSpec.class),
        @XmlElementRef(name = "textRegion", type = TextRegionSpec.class),
        @XmlElementRef(name = "lookup", type = LookupSpec.class),
        @XmlElementRef(name = "label", type = LabelSpec.class),
        @XmlElementRef(name = "textInput", type = TextInputSpec.class)
        })
    @XmlMixed
	public List<FormControlSpec> getElements() {
		return elements;
	}

    public void setElements(List<FormControlSpec> elements) {
		this.elements = elements;
	}

}
