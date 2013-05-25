package edu.isi.bmkeg.vpdmf.model.definitions.specs;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="textRegion")
public class TextRegionSpec extends FormControlSpec implements Serializable {
	static final long serialVersionUID = 8047039304729208683L;

	private int height;

	private int width;
	
	@XmlAttribute
	public int getHeight() {
		return height;
	}
	public void setHeight(int height) {
		this.height = height;
	}

	@XmlAttribute
	public int getWidth() {
		return width;
	}
	public void setWidth(int width) {
		this.width = width;
	}
	
}
