package edu.isi.bmkeg.vpdmf.model.definitions.specs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * The Root element for the XML-based specification of a VPDMf model.
 * 
 * Note that, by default, the LOCATION of this file must be in the top directory 
 * of the ViewSpecification files used in the model.
 * 
 * This consists of the following information:
 * 
 * A) the Group / Artifact / Version information pertaining the Maven identify of this model
 * B) the UML model file's relative path and type 
 * C) the path and type of initial data to be loaded into the model
 * D) the names of views to be converted into SOLR stores
 * E) the packages to be converted into ontologies 
 * F) the package pattern to be used to generate uima types
 * 
 * @author burns
 *
 */

@XmlRootElement(name="vpdmf")
public class VpdmfSpec implements Serializable {
	static final long serialVersionUID = 8047039304729208683L;

	private String groupId;
	
	private String artifactId;
	
	private String version;

	private ModelSpec model;

	private String viewsPath;
	
	private DataSpec data;
	
	private List<String> solrViews = new ArrayList<String>();

	private List<String> ontpkgs = new ArrayList<String>();

	private String uimaPackagePattern;
	
	 @XmlElement( name="groupId" )
	 public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	@XmlElement( name="artifactId" )
	public String getArtifactId() {
		return artifactId;
	}

	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}

	@XmlElement( name="version" )
	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}
	
	@XmlElement( name="model" )
	public ModelSpec getModel() {
		return model;
	}

	private void setModel(ModelSpec model) {
		this.model = model;
	}
	
	@XmlElement( name="viewsPath" )
	public String getViewsPath() {
		return viewsPath;
	}

	public void setViewsPath(String viewsPath) {
		this.viewsPath = viewsPath;
	}


	@XmlElement( name="data" )
	public DataSpec getData() {
		return data;
	}

	private void setData(DataSpec data) {
		this.data = data;
	}

	@XmlElementWrapper( name="solr" )
    @XmlElement( name="view" )
	public List<String> getSolrViews() {
		return solrViews;
	}

	public void setSolrViews(List<String> solrViews) {
		this.solrViews = solrViews;
	}

	@XmlElement( name="uimaPkgPattern" )
	public String getUimaPackagePattern() {
		return uimaPackagePattern;
	}

	public void setUimaPackagePattern(String uimaPackagePattern) {
		this.uimaPackagePattern = uimaPackagePattern;
	}

	@XmlElementWrapper( name="ontologize" )
    @XmlElement( name="package" )
	public List<String> getOntpkgs() {
		return ontpkgs;
	}

	public void setOntpkgs(List<String> ontpkgs) {
		this.ontpkgs = ontpkgs;
	}
	
}
