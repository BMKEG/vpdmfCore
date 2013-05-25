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
import java.util.List;

import javax.imageio.ImageIO;

import edu.isi.bmkeg.uml.model.UMLattribute;
import edu.isi.bmkeg.uml.model.UMLclass;
import edu.isi.bmkeg.uml.utils.UMLDataConverters;

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
	
	private String queryCode = EQ;
	public static String EQ = "equalTo";
	public static String OR = "or";
	public static String AND = "and";
	public static String NOT = "not";
	public static String GT = "greaterThan";
	public static String GTEQ = "greaterThanOrEqualTo";
	public static String LT = "lessThan";
	public static String LTEQ = "lessThanOrEqualTo";
			
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
		return "]"
				+ this.get_object().getPrimitive().getDefinition().getName()
				+ "|" + this.get_object().getDefinition().getBaseName() + "."
				+ this.getDefinition().getBaseName();
	}

	public void clearConditions() {

		this.setValue(null);

	}

	public void connectTo(AttributeInstance that) {
		
		this.connectedKeys.add(that);
		that.connectedKeys.add(this);

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
			if (this.getValue() != null) {

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
		while(aIt.hasNext()) {
			UMLattribute a = aIt.next();
			if(this.getDefinition().getBaseName().equals(a.getBaseName()) ) {
				this.setDefinition(a);				
				return;
			}
		}
		
		throw new Exception("Attribute "+ this.definition.getBaseName() +"not found in class " + cd.getBaseName());

	}

	public InputStream readValueInputStream() throws Exception {
		InputStream is = null;
	    	
	    String baseType = this.getDefinition().getType().getBaseName();
		
		if (baseType.equalsIgnoreCase("image")) {

			BufferedImage img = null;

			img = (BufferedImage) this.value;

			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			//
			// Write the buffer image into output stream in the
			// format of "PNG".
			//
			ImageIO.write(img, "png", baos);
			is = new ByteArrayInputStream(baos.toByteArray());

		} else if (baseType.equalsIgnoreCase("blob")) {

			BufferedImage img = null;

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

	public String readValueString() {
		String value = null;
		try {
			value = UMLDataConverters.convertToString(this.definition, this.value);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return value;
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

	public Boolean constainsBlobData() {
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

		Iterator<UMLattribute> pkIt = this.get_object().getDefinition().getPkArray().iterator();
		while(pkIt.hasNext()) {
			UMLattribute pk = pkIt.next();
			if(aDef.equals(pk)) {
				return true;
			}
		}
		return false;

	}

	public void writeValueString(String value) {
		try {

			Object data = UMLDataConverters.convertToType(this.definition, value);
			this.setValue(data);

		} catch (Exception e) {
			e.printStackTrace();
		}
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

	public String getQueryCode() {
		return queryCode;
	}

	public void setQueryCode(String queryCode) {
		this.queryCode = queryCode;
	}
	
};
