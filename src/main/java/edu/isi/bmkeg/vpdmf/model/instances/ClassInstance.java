package edu.isi.bmkeg.vpdmf.model.instances;

/**
 * Timestamp: Thu_Jun_19_120936_2003;
 */

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.isi.bmkeg.uml.model.UMLattribute;
import edu.isi.bmkeg.uml.model.UMLclass;
import edu.isi.bmkeg.vpdmf.model.definitions.IndexElement;
import edu.isi.bmkeg.vpdmf.model.definitions.PrimitiveDefinition;
import edu.isi.bmkeg.vpdmf.model.definitions.PrimitiveLink;
import edu.isi.bmkeg.vpdmf.model.definitions.ViewDefinition;

public class ClassInstance implements Serializable {
	static final long serialVersionUID = 2593554832580697147L;

	private PrimitiveLinkInstance primitiveLinkInstance;
	public HashMap<String, AttributeInstance> attributes = new HashMap<String, AttributeInstance>();
	private PrimitiveInstance primitive;
	private UMLclass definition;
	private String defName;

	public ClassInstance() {
		super();
	}

	public ClassInstance(UMLclass classDef) {
		super();
		this.definition = classDef;

		this.setDefName(classDef.getBaseName());

		Iterator<UMLattribute> i = classDef.getAttributes().iterator();

		while (i.hasNext()) {
			UMLattribute currentAttribute = (UMLattribute) i.next();

			// TODO: CHECK
			// ADDED BY GULLY 08/01/2011
			if (!currentAttribute.getToImplement())
				continue;

			AttributeInstance aI = new AttributeInstance(currentAttribute);

			try {
				this.attributes.put(currentAttribute.getBaseName(), aI);
				aI.setObject(this);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	}

	public ClassInstance(String defName) {
		super();
		this.setDefName(defName);
	}

	/**
	 * Returns a vector of all the index items in a classInstance - If the class
	 * contains a column called 'vpdmfLabel' we'll return that. - otherwise
	 * build the vector from all the attributes that make up the ViewSpec's
	 * vpdmfLabel columns.
	 * 
	 */
	public List<UMLattribute> buildIndexVector() {
		List<UMLattribute> idxVec = new ArrayList<UMLattribute>();

		PrimitiveInstance pi = (PrimitiveInstance) this.getPrimitive();

		if (pi == null) {
			return idxVec;
		}

		// If the class has a non-null vpdmfLabel already set, we use that.
		if (this.attributes.containsKey("vpdmfLabel")) {

			AttributeInstance aIidxs = (AttributeInstance) this.attributes
					.get("vpdmfLabel");
			if (aIidxs.getValue() != null
					&& aIidxs.readValueString().length() > 0) {
				idxVec.add(aIidxs.getDefinition());
			}

			if (idxVec.size() > 0) {
				return idxVec;
			}

		}

		int index = pi.readIndex();

		PrimitiveInstanceGraph pig = (PrimitiveInstanceGraph) pi.getGraph();
		ViewInstance vi = (ViewInstance) pig.getSubGraphNode();

		PrimitiveDefinition pd = pi.getDefinition();
		ViewDefinition vd = vi.getDefinition();

		/*
		 * WHAT IS THIS? HORRIBLE HACK.
		 * 
		 * VPDMf top = vi.getDefinition().getTop(); if
		 * (top.getViews().containsKey(pd.getName())) { vd = (ViewDefinition)
		 * top.getViews().get(pd.getName()); }
		 */

		Iterator adIt = vd.getIndexElements().values().iterator();

		while (adIt.hasNext()) {
			String addr = ((IndexElement) adIt.next()).getAttributeAddress();
			AttributeInstance aI = null;

			try {
				aI = vi.readAttributeInstance(addr, index);
			} catch (Exception e) {
				//
				// Can't find that index,
				// so we skip to the next attribute...
				// throw no errors here.
				//
				continue;
			}

			UMLattribute aD = (UMLattribute) aI.getDefinition();

			if (aD.getParentClass() == this.getDefinition()) {
				idxVec.add(aD);
			}

		}

		return idxVec;

	}

	public void clearConditions() {
		Iterator aiIt = this.attributes.values().iterator();
		while (aiIt.hasNext()) {
			AttributeInstance ai = (AttributeInstance) aiIt.next();
			ai.clearConditions();
		}

	}

	public void destroy() {
		this.primitiveLinkInstance = null;
		this.primitive = null;
		this.definition = null;
		this.attributes = null;
	}

	public String dumpToXML() throws Exception {
		String xml = "<" + this.getDefinition().getBaseName() + ">\n";
		String temp = "";
		Iterator atIt = this.attributes.values().iterator();
		while (atIt.hasNext()) {
			AttributeInstance ai = (AttributeInstance) atIt.next();
			temp += ai.dumpToXML();
		}
		Pattern patt = Pattern.compile("\n");
		Matcher matcher = patt.matcher(temp);
		temp = matcher.replaceAll("\n  ");

		return xml;

	}

	public Map<String, AttributeInstance> getAttributes() {
		return this.attributes;
	}

	public UMLclass getDefinition() {
		return this.definition;
	}

	public PrimitiveInstance getPrimitive() {
		return this.primitive;
	}

	public PrimitiveLinkInstance getPrimitiveLinkInstance() {
		return this.primitiveLinkInstance;
	}

	public void removeDefinition() {
		this.setDefinition(null);

		Iterator aiIt = this.attributes.values().iterator();
		while (aiIt.hasNext()) {
			AttributeInstance ai = (AttributeInstance) aiIt.next();
			ai.removeDefinition();
		}
	}

	public void instantiateDefinition(PrimitiveDefinition pd) throws Exception {

		Iterator<UMLclass> cIt = pd.getClasses().iterator();
		while (cIt.hasNext()) {
			UMLclass c = cIt.next();
			if (this.getDefName().equals(c.getBaseName())) {
				this.instantiateDefinition(c);
				this.setDefinition(c);
				return;
			}
		}

	}

	public void instantiateDefinition(UMLclass cd) throws Exception {
		this.setDefinition(cd);

		Iterator<String> aiIt = this.attributes.keySet().iterator();
		while (aiIt.hasNext()) {
			String key = aiIt.next();

			AttributeInstance ai = this.attributes.get(key);

			Iterator<UMLattribute> aIt = cd.getAttributes().iterator();
			AILOOP: while (aIt.hasNext()) {
				UMLattribute a = aIt.next();
				if (key.equals(a.getBaseName())) {
					ai.setDefinition(a);
					break AILOOP;
				}
			}

		}

	}

	public void instantiateDefinition(PrimitiveLink pl) throws Exception {

		UMLclass cd = pl.readLinkClass();

		if (cd != null) {
			this.instantiateDefinition(cd);
		}

	}

	public void convertStreamsToImages() throws IOException,
			ClassNotFoundException {

		Iterator aiIt = this.attributes.values().iterator();
		while (aiIt.hasNext()) {
			AttributeInstance ai = (AttributeInstance) aiIt.next();

			ai.convertStreamsToImages();

		}

	}

	public void convertImagesToStreams() throws IOException {

		Iterator aiIt = this.attributes.values().iterator();
		while (aiIt.hasNext()) {
			AttributeInstance ai = (AttributeInstance) aiIt.next();

			ai.convertImagesToStreams();

		}

	}

	public boolean isEmpty() {
		UMLclass def = this.getDefinition();
		Object value = null;

		try {
			Iterator aiIt = this.attributes.values().iterator();
			while (aiIt.hasNext()) {
				AttributeInstance ai = (AttributeInstance) aiIt.next();

				value = ai.getValue();

				if (value != null) {
					return false;
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return true;
	}

	public boolean isInPrimaryPrimitive() {
		PrimitiveInstanceGraph pig = (PrimitiveInstanceGraph) this
				.getPrimitive().getGraph();
		ViewInstance vi = (ViewInstance) pig.getSubGraphNode();

		if (vi.getPrimaryPrimitive().equals(this.getPrimitive())) {
			return true;
		} else {
			return false;
		}
	}

	public boolean checkIsPkNull() {
		UMLclass def = this.getDefinition();
		Object value = null;

		try {
			Iterator aiIt = this.attributes.values().iterator();
			while (aiIt.hasNext()) {
				AttributeInstance ai = (AttributeInstance) aiIt.next();

				value = ai.getValue();

				if (def.getPkArray().contains(ai.getDefinition())
						&& value == null) {
					return true;
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	public boolean checkIsPrimaryClass() {
		UMLclass def = this.getDefinition();
		PrimitiveDefinition pd = this.getPrimitive().getDefinition();
		if (pd.getPrimaryClass().equals(def)) {
			return true;
		} else {
			return false;
		}
	}

	public void setDefinition(UMLclass definition) {
		this.definition = definition;
	}

	public void setPrimitive(PrimitiveInstance primitive) {
		this.primitive = primitive;
	}

	public void setPrimitiveLinkInstance(
			PrimitiveLinkInstance primitiveLinkInstance) {
		this.primitiveLinkInstance = primitiveLinkInstance;
	}

	public String getDefName() {
		return defName;
	}

	public void setDefName(String defName) {
		this.defName = defName;
	}

	public String toString() {
		String s = this.getDefName() + "\n";
		for( AttributeInstance ai : this.attributes.values() ) {
			s += ai.toString() + "\n";
		}
		return s;
	}
	
};
