package edu.isi.bmkeg.vpdmf.model.definitions.specs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="view")
public class ViewSpec implements Serializable {
	static final long serialVersionUID = 8047039304729208683L;

	private String name;
	
	private String type;
	
	private String parent;

	private String editable;
	
	private List<PrimitiveSpec> primitives = new ArrayList<PrimitiveSpec>();

	private List<ViewLinkSpec> viewLinks = new ArrayList<ViewLinkSpec>();
	
	private VpdmfFormattedIndexSpec vpdmfLabel;

	private VpdmfFormattedIndexSpec vpdmfUri;
	
	private List<IndexElementSpec> indexElements = new ArrayList<IndexElementSpec>();

	@XmlAttribute
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@XmlAttribute
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@XmlAttribute
	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

	@XmlAttribute
	public String getEditable() {
		return editable;
	}

	public void setEditable(String editable) {
		this.editable = editable;
	}

    @XmlElementWrapper( name="primitives" )
    @XmlElement( name="primitive" )
    public List<PrimitiveSpec> getPrimitives() {
		return primitives;
	}

	public void setPrimitives(List<PrimitiveSpec> primitives) {
		this.primitives = primitives;
	}

    @XmlElementWrapper( name="viewLinks" )
    @XmlElement( name="viewLink" )
    public List<ViewLinkSpec> getViewLinks() {
		return viewLinks;
	}

	public void setViewLinks(List<ViewLinkSpec> viewLinks) {
		this.viewLinks = viewLinks;
	}

	@XmlElement(name="vpdmfLabel")
	public VpdmfFormattedIndexSpec getVpdmfLabel() {
		return vpdmfLabel;
	}

	public void setVpdmfLabel(VpdmfFormattedIndexSpec vpdmfLabel) {
		this.vpdmfLabel = vpdmfLabel;
	}

	@XmlElement(name="vpdmfUri")
	public VpdmfFormattedIndexSpec getVpdmfUri() {
		return vpdmfUri;
	}

	public void setVpdmfUri(VpdmfFormattedIndexSpec vpdmfUri) {
		this.vpdmfUri = vpdmfUri;
	}
	
    @XmlElementWrapper( name="indexElements" )
    @XmlElement( name="indexElement" )
	public List<IndexElementSpec> getIndexElements() {
		return indexElements;
	}

	public void setIndexElements(List<IndexElementSpec> indexElements) {
		this.indexElements = indexElements;
	}


	
}
