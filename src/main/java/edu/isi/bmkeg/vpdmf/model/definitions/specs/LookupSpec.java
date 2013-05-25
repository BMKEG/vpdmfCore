package edu.isi.bmkeg.vpdmf.model.definitions.specs;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="lookup")
public class LookupSpec extends FormControlSpec implements Serializable {
	static final long serialVersionUID = 8047039304729208683L;
	private String lookupView;

	@XmlAttribute
	public String getLookupView() {
		return lookupView;
	}

	public void setLookupView(String lookupView) {
		this.lookupView = lookupView;
	}
	
}
