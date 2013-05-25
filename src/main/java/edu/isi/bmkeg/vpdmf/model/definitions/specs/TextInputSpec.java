package edu.isi.bmkeg.vpdmf.model.definitions.specs;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="textInput")
public class TextInputSpec extends FormControlSpec implements Serializable {
	static final long serialVersionUID = 8047039304729208683L;
	
	private int size;

	@XmlAttribute
	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

}
