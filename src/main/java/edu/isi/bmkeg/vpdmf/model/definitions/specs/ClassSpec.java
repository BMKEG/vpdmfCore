package edu.isi.bmkeg.vpdmf.model.definitions.specs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="class")
public class ClassSpec implements Serializable {
	static final long serialVersionUID = 8047039304729208683L;

	private String name;

	@XmlAttribute
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
		
}
