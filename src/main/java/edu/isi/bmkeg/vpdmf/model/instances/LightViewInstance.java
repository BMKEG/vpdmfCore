package edu.isi.bmkeg.vpdmf.model.instances;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;

import edu.isi.bmkeg.utils.superGraph.SuperGraphNode;
import edu.isi.bmkeg.vpdmf.model.definitions.ViewDefinition;

public class LightViewInstance extends SuperGraphNode {

	private static final long serialVersionUID = 1L;

	private Long vpdmfId;
	
	private String vpdmfLabel = "";

	public static String INDEX_TUPLE_SEPARATOR = "<|>";

	private String indexTuple = "";
	
	private String indexTupleFields = "";

	private String vpdmfUri = "";
	
	private String UIDString = "";

	private String defName;

//	private ImageIcon thumbnail;
	
	private ViewDefinition definition;

	public String getVpdmfLabel() {
		return vpdmfLabel;
	}

	public void setVpdmfLabel(String vpdmfLabel) {
		this.vpdmfLabel = vpdmfLabel;
	}

	public String getVpdmfUri() {
		return vpdmfUri;
	}

	public void setVpdmfUri(String vpdmfUri) {
		this.vpdmfUri = vpdmfUri;
	}

	public Long getVpdmfId() {
		return vpdmfId;
	}

	public void setVpdmfId(Long vpdmfId) {
		this.vpdmfId = vpdmfId;		
	}

	@Deprecated
	public String getUIDString() {
		return UIDString;
	}

	@Deprecated
	public void setUIDString(String UIDString) {
		this.UIDString = UIDString;
		this.setName( UIDString );
	}
	
	@Deprecated
	public int readUIDValue() {
		String s = this.getUIDString();
		String uid = s.substring(s.indexOf("=") + 1, s.length());
		return new Integer(uid).intValue();
	}

	public String getDefName() {
		return defName;
	}

	public void setDefName(String defName) {
		//
		// Need to take into account extra formatting.
		//
		Pattern p = Pattern.compile("(\\w*)\\.\\%$");
		Matcher m = p.matcher(defName);
		if (m.find()) {
			defName = m.group(1);
		}

		this.defName = defName;
		
	}

	public ViewDefinition getDefinition() {
		return definition;
	}

	public void setDefinition(ViewDefinition definition) {
		this.definition = definition;
	}

	public String getIndexTuple() {
		return indexTuple;
	}

	public void setIndexTuple(String indexTuple) {
		this.indexTuple = indexTuple;
	}

	public String getIndexTupleFields() {
		return indexTupleFields;
	}

	public void setIndexTupleFields(String indexTupleFields) {
		this.indexTupleFields = indexTupleFields;
	}

}
