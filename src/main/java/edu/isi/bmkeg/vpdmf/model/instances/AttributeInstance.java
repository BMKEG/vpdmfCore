package edu.isi.bmkeg.vpdmf.model.instances;

/**
 * Timestamp: Thu_Jun_19_120936_2003;
 */

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import edu.isi.bmkeg.uml.model.UMLattribute;
import edu.isi.bmkeg.uml.model.UMLclass;
import edu.isi.bmkeg.uml.utils.UMLDataConverters;
import edu.isi.bmkeg.vpdmf.exceptions.VPDMfException;

/**
 * AttributeInstances will have three attributes
 * 
 * conditionValue serverValue clientValue
 * 
 */

public class AttributeInstance implements Serializable {

	private ClassInstance object;
	private Object value;
	private UMLattribute definition;
	private String defName;
	private boolean notNull;

	private String andOrCode = AND;
	public static String OR = "or";
	public static String AND = "and";
	
	private String[] queryCode = {EQ};
	public static String EQ = "<vpdmf-eq>";
	public static String NOT = "<vpdmf-not>";
	public static String GT = "<vpdmf-gt>";
	public static String GTEQ = "<vpdmf-gteq>";
	public static String LT = "<vpdmf-lt>";
	public static String LTEQ = "<vpdmf-lteq>";
	public static String LIKE = "<vpdmf-like>";
	
	private static Pattern codePatt = Pattern.compile(
			"(^<vpdmf-eq>|^<vpdmf-not>|^<vpdmf-gt>|^<vpdmf-gteq>|^<vpdmf-lt>|^<vpdmf-lteq>)"
			);

	private HashSet<AttributeInstance> connectedKeys = new HashSet<AttributeInstance>();

	public AttributeInstance() {
		super();
	}

	public AttributeInstance(UMLattribute attDef) {
		super();
		this.setDefinition(attDef);
		this.defName = attDef.getBaseName();

		/*
		 * UMLclass c = attDef.get_ParentClass(); try {
		 * this.definition.addToCollection(c.getPath(), attDef); } catch
		 * (Exception e) { e.printStackTrace(); }
		 */

	}

	public String getAddress() {
		return "]" + this.get_object().getPrimitive().getDefinition().getName()
				+ "|" + this.get_object().getDefinition().getBaseName() + "."
				+ this.getDefinition().getBaseName();
	}

	public void clearConditions() {

		this.setValue(null);

	}

	public void connectTo(AttributeInstance that) {

		this.connectedKeys.add(that);
		that.connectedKeys.add(this);
		
		if( this.value == null && that.value != null) 
			this.value = that.value;
		else if( that.value == null && this.value != null) 
			that.value = this.value;

	}

	public void destroy() {
		this.object = null;
		this.value = null;
		this.definition = null;
		this.connectedKeys = null;
	}

	public String dumpToXML() throws Exception {
		String xml = "<" + this.getDefinition().getBaseName() + ">";
		xml += this.readValueString();
		xml += "</" + this.getDefinition().getBaseName() + ">";
		return xml;

	}

	public String readDebugString() throws Exception {
		String debug = "";

		if (this.get_object().getPrimitive() != null) {
			debug += "]" + this.get_object().getPrimitive().getName();

		} else if (this.get_object().getPrimitiveLinkInstance() != null) {
			debug += "]"
					+ this.get_object().getPrimitiveLinkInstance()
							.getOutEdgeNode().getName();
			debug += "-"
					+ this.get_object().getPrimitiveLinkInstance()
							.getInEdgeNode().getName();

		} else {
			throw new Exception(this.get_object().getDefinition().getBaseName()
					+ "." + this.getDefinition().getBaseName() + " is isolated");
		}

		debug += "|" + this.get_object().getDefinition().getBaseName();
		debug += "." + this.getDefinition().getBaseName();
		debug += "=" + this.readValueString();
		return debug;

	}

	public void convertStreamsToImages() throws IOException,
			ClassNotFoundException {

		if (this.getDefinition().getType().getBaseName().equals("image")) {

			byte[] imgData = null;
			ByteArrayInputStream bis = null;
			ObjectInputStream ois = null;
			BufferedImage img = null;

			if (this.getValue() != null) {

				imgData = (byte[]) this.getValue();
				bis = new ByteArrayInputStream(imgData);
				img = ImageIO.read(bis);
				this.setValue(img);

			}
		}
	}

	public void convertImagesToStreams() throws IOException {

		if (this.getDefinition().getType().getBaseName().equals("image")) {
			if (this.getValue() != null && 
					this.getValue() instanceof BufferedImage) {

				BufferedImage img = (BufferedImage) this.getValue();
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(img, "png", baos);
				this.setValue(baos.toByteArray());

			}
		}
	}

	public void clearConnectedKeys() {
		this.connectedKeys = new HashSet();
	}

	public void removeDefinition() {

		this.definition = null;
		Iterator it = this.connectedKeys.iterator();
		while (it.hasNext()) {
			AttributeInstance ai = (AttributeInstance) it.next();
			ai.definition = null;
		}

	}

	public void instantiateDefinition(UMLclass cd) throws Exception {

		Iterator<UMLattribute> aIt = cd.getAttributes().iterator();
		while (aIt.hasNext()) {
			UMLattribute a = aIt.next();
			if (this.getDefinition().getBaseName().equals(a.getBaseName())) {
				this.setDefinition(a);
				return;
			}
		}

		throw new Exception("Attribute " + this.definition.getBaseName()
				+ "not found in class " + cd.getBaseName());

	}

	public InputStream readValueInputStream() throws Exception {
		InputStream is = null;
	    	
	    String baseType = this.getDefinition().getType().getBaseName();
		
		if (baseType.equalsIgnoreCase("image") ) {

			if( this.value instanceof BufferedImage ) {
			
				BufferedImage img = (BufferedImage) this.value;
	
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
	
				//
				// Write the buffer image into output stream in the
				// format of "PNG".
				//
				ImageIO.write(img, "png", baos);
				is = new ByteArrayInputStream(baos.toByteArray());

			} else {
			
				is = new ByteArrayInputStream((byte[]) this.value);
			
			}

		} else if (baseType.equalsIgnoreCase("blob")) {

			is = new ByteArrayInputStream((byte[]) this.value);

		} else {
			throw new Exception("Can not get byte array input stream for "
					+ baseType + " type.");
		}

		return is;

	}

	public boolean hasValue(Object o) {
		if ((o == null && this.value != null)
				|| (o != null && this.value == null))
			return false;
		else if (o == null && this.value == null)
			return true;
		else
			return this.value.equals(o);
	}

	public String readValueString() throws VPDMfException {
		
		String valueString = null;		
		
		//
		// TODO - must rewrite basic query engine. This encoding is horrible. 
		//
		if( this.queryCode.length > 1 || !this.queryCode[0].equals(EQ) ) {
			valueString = "";
			if( this.value instanceof String[]) {
				String[] valueArray = (String[]) this.value;
				for(int i=0; i<this.queryCode.length; i++) {
					valueString += this.queryCode[i] + valueArray[i]; 	
				}				
			} else {
				valueString += this.value; 	
			}
			return valueString;
		}
		
		try {
			valueString = UMLDataConverters.convertToString(
					this.definition,
					this.value);
		} catch (Exception e){
			throw new VPDMfException(e);
		}

		return valueString;
	}

	public Object getValue() {
		return this.value;
	}

	public HashSet getConnectedKeys() {
		return this.connectedKeys;
	}

	public UMLattribute getDefinition() {
		return this.definition;
	}

	public ClassInstance get_object() {
		return this.object;
	}

	public Boolean constainsBlobData() throws Exception {
		
		Boolean flag = new Boolean(false);
		String s = this.readValueString();
		if (this.value != null && s == null)
			flag = new Boolean(true);
		return flag;
	
	}

	public boolean isPrimaryKey() {
		boolean isPK = false;

		UMLclass cDef = this.get_object().getDefinition();
		UMLattribute aDef = this.getDefinition();

		Iterator<UMLattribute> pkIt = this.get_object().getDefinition()
				.getPkArray().iterator();
		while (pkIt.hasNext()) {
			UMLattribute pk = pkIt.next();
			if (aDef.equals(pk)) {
				return true;
			}
		}
		return false;

	}

	public void writeValueString(String value) throws Exception {
		
		if( value == null ) {
			return;
		}
		
		if( value.contains("<vpdmf-or>") ) {
		
			this.setAndOrCode( AttributeInstance.OR );
			String[] orValues = value.split("<vpdmf-or>");
			this.queryCode = new String[orValues.length];
			for( int i=0; i<orValues.length; i++) {
				String code = this.checkForConditions(orValues[i]);
				if( code.length() > 0 ) {
					orValues[i] = orValues[i].replaceAll(code, "");
					this.queryCode[i] = code;
				} else {
					this.queryCode[i] = AttributeInstance.EQ;
				}	
			}

			this.setValue(orValues);
		
		} else if( value.contains("<vpdmf-and>") ) {
		
			this.setAndOrCode( AttributeInstance.AND );
			String[] andValues = value.split("<vpdmf-and>");
			this.queryCode = new String[andValues.length];
			for( int i=0; i<andValues.length; i++) {
				String code = this.checkForConditions(andValues[i]);
				if( code.length() > 0 ) {
					andValues[i] = andValues[i].replaceAll(code, "");
					this.queryCode[i] = code;
				} else {
					this.queryCode[i] = AttributeInstance.EQ;
				}	
			}

			this.setValue(andValues);
		
		} else {

			String code = this.checkForConditions(value);
			if( code.length() > 0 ) {
				value = value.replaceAll(code, "");
				this.queryCode[0] = code;
			} else {
				this.queryCode[0] = AttributeInstance.EQ;
			}	
			Object data = UMLDataConverters.convertToType(this.definition, value);
			this.setValue(data);
			
		}
	
	}
	
	private String checkForConditions(String value) {
		String code = "";
		Matcher m = codePatt.matcher(value);
		if( m.find() ) {
			code = m.group(1);
		}
		return code;
	}
	
	public void setValue(Object value) {
		this.value = value;

		Iterator it = this.connectedKeys.iterator();
		while (it.hasNext()) {
			AttributeInstance ai = (AttributeInstance) it.next();
			if (!ai.equals(this)) {
				ai.value = value;
			}
		}

	}

	public void setConnectedKeys(HashSet connectedKeys) {
		this.connectedKeys = connectedKeys;
	}

	public void setNotNull(boolean isNotNull) {
		this.notNull = isNotNull;
	}

	public boolean getNotNull() {
		return this.notNull;
	}

	public void setDefinition(UMLattribute definition) {
		this.definition = definition;
	}

	public void setObject(ClassInstance object) {
		this.object = object;
	}
	
	public String toString() {
		
		if( this.value == null )
			return this.defName + "= null";
		else 
			return this.defName + "=" + this.value.toString();
	
	}

	public String getAndOrCode() {
		return andOrCode;
	}

	public void setAndOrCode(String andOrCode) {
		this.andOrCode = andOrCode;
	}

	public String[] getQueryCode() {
		return queryCode;
	}

	public void setQueryCode(String[] queryCode) {
		this.queryCode = queryCode;
	}
	
}
