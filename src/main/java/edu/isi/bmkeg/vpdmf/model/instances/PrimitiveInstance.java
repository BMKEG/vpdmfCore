package edu.isi.bmkeg.vpdmf.model.instances;

/**
 * Timestamp: Thu_Jun_19_120936_2003;
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import edu.isi.bmkeg.uml.model.UMLattribute;
import edu.isi.bmkeg.uml.model.UMLclass;
import edu.isi.bmkeg.uml.model.UMLrole;
import edu.isi.bmkeg.uml.utils.UMLDataConverters;
import edu.isi.bmkeg.utils.superGraph.SuperGraphNode;
import edu.isi.bmkeg.vpdmf.exceptions.VPDMfException;
import edu.isi.bmkeg.vpdmf.model.definitions.ConditionElement;
import edu.isi.bmkeg.vpdmf.model.definitions.PrimitiveDefinition;
import edu.isi.bmkeg.vpdmf.model.definitions.ViewDefinition;

public class PrimitiveInstance extends SuperGraphNode {
	static final long serialVersionUID = 2593554832580697147L;

	private PrimitiveDefinition definition;
	
	private String defName;

	private HashMap<String, ClassInstance> objects = new HashMap<String, ClassInstance>();

	public PrimitiveInstance() {
		super();
	}
	
	public PrimitiveInstance(PrimitiveDefinition pvDef) {
		super();

		this.setDefName(pvDef.getName());

		this.setName(pvDef.getName() + "_0");
		this.setDefinition(pvDef);

		Iterator i = pvDef.getClasses().iterator();
		while (i.hasNext()) {

			UMLclass currentClass = (UMLclass) i.next();
			ClassInstance cI = new ClassInstance(currentClass);
			cI.setPrimitive(this);

			try {
				this.getObjects().put(currentClass.getBaseName(), cI);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	}

	public PrimitiveInstance(PrimitiveDefinition pvDef, int index) {
		super();
		this.setName(pvDef.getName() + "_" + index);

		this.setDefName(pvDef.getName());
		this.setDefinition(pvDef);

		Iterator i = pvDef.getClasses().iterator();
		while (i.hasNext()) {

			UMLclass currentClass = (UMLclass) i.next();
			ClassInstance cI = new ClassInstance(currentClass);
			cI.setPrimitive(this);

			try {
				this.getObjects().put(currentClass.getBaseName(), cI);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	}

	public AttributeInstance readAttribute(String attributeAddress)
			throws VPDMfException {

		ClassInstance classIns;
		AttributeInstance attrIns;

		//
		// Check the format of the attributeAddress
		//
		int dotPos = attributeAddress.lastIndexOf('.');
		int barPos = attributeAddress.lastIndexOf('|');

		if (dotPos == -1 || barPos == -1) {
			throw new VPDMfException(attributeAddress + " is badly formed\n");
		}

		String attributeName = attributeAddress.substring(dotPos + 1);
		String className = attributeAddress.substring(barPos + 1, dotPos);

		//
		// ii) Class
		if (this.getObjects().containsKey(className)) {
			classIns = (ClassInstance) this.getObjects().get(className);
		} else {
			throw new VPDMfException("Can't find " + attributeAddress
					+ ", no such class\n");
		}

		//
		// iii) Attribute
		if (classIns.attributes.containsKey(attributeName)) {
			attrIns = (AttributeInstance) classIns.attributes
					.get(attributeName);
		} else {
			throw new VPDMfException("Can't find " + attributeAddress
					+ ", no such attribute\n");
		}

		return attrIns;

	}

	/**
	 * Checks to see if the primitive instance is null (i.e., is it any
	 * different from a newly generated primitive instance within the same
	 * view?)
	 * @throws VPDMfException 
	 */
	public boolean isNull() throws VPDMfException  {

		PrimitiveInstance testPi = new PrimitiveInstance(this.getDefinition());
		testPi.fillInConditions();
		
		Vector addrVec = this.readAttributeAddresses();
		Iterator addrIt = addrVec.iterator();
		LOOP: while (addrIt.hasNext()) {
			String addr = (String) addrIt.next();

			AttributeInstance ai = this.readAttribute(addr);
			AttributeInstance testAi = testPi.readAttribute(addr);

			//
			// Do not use knowledge model, or any used in this primitive
			// to determine if a primitive is null
			//
			String a = addr.replaceAll("\\].*?\\|", "|");
			Iterator it = this.getDefinition().getConditionElements()
					.iterator();
			while (it.hasNext()) {
				ConditionElement ce = (ConditionElement) it.next();
				if (ai.getDefinition().getBaseName().equals("km_id")
						|| ce.toString().indexOf(a) != -1) {
					continue LOOP;
				}
			}

			if (ai.readValueString() != null) {
				if (!ai.readValueString().equals(testAi.readValueString())
						&& !ai.readValueString().equals("%")) {
					return false;
				}
			}

		}

		return true;

	}
	
	public boolean isNullExceptForFks() throws VPDMfException {

		PrimitiveInstance testPi = new PrimitiveInstance(this.getDefinition());
		testPi.fillInConditions();
		
		Vector addrVec = this.readAttributeAddresses();
		Iterator addrIt = addrVec.iterator();
		LOOP: while (addrIt.hasNext()) {
			String addr = (String) addrIt.next();

			AttributeInstance ai = this.readAttribute(addr);
			if( ai.getConnectedKeys().size() > 0 && 
					(ai.getDefinition().getStereotype() != null &&
					!ai.getDefinition().getStereotype().contains("PK")))
				continue;
			
			AttributeInstance testAi = testPi.readAttribute(addr);

			//
			// Do not use knowledge model, or any used in this primitive
			// to determine if a primitive is null
			//
			String a = addr.replaceAll("\\].*?\\|", "|");
			Iterator it = this.getDefinition().getConditionElements()
					.iterator();
			while (it.hasNext()) {
				ConditionElement ce = (ConditionElement) it.next();
				if (ai.getDefinition().getBaseName().equals("km_id")
						|| ce.toString().indexOf(a) != -1) {
					continue LOOP;
				}
			}

			if (ai.readValueString() != null) {
				if (!ai.readValueString().equals(testAi.readValueString())
						&& !ai.readValueString().equals("%")) {
					return false;
				}
			}

		}

		return true;
	
	}
	

	public boolean arePkSet() {
		boolean pkSet = true;
		try {

			String primaryName = this.getDefinition().getPrimaryClass()
					.getBaseName();

			ClassInstance obj = (ClassInstance) this.getObjects().get(
					primaryName);

			Iterator pkIt = obj.getDefinition().getPkArray().iterator();
			while (pkIt.hasNext()) {

				UMLattribute pk = (UMLattribute) pkIt.next();
				AttributeInstance pki = (AttributeInstance) obj.attributes
						.get(pk.getBaseName());
				if (pki.readValueString() == null) {
					pkSet = false;
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return pkSet;

	}

	public ClassInstance readPrimaryObject() {

		String primaryName = this.getDefinition().getPrimaryClass()
				.getBaseName();

		ClassInstance obj = (ClassInstance) this.getObjects().get(primaryName);

		return obj;

	}

	public void clearConditions() {
		Iterator ciIt = this.getObjects().values().iterator();
		while (ciIt.hasNext()) {
			ClassInstance ci = (ClassInstance) ciIt.next();
			ci.clearConditions();
		}
	}

	public void destroy() {
		this.setDefinition(null);
		this.setObjects(null);
		super.destroy();
	}

	public String dumpToXML() throws Exception {
		String xml = "<" + this.getDefinition().getName() + " id='"
				+ this.readIndex() + "'>\n";

		List<AttributeInstance> attVec = this.readAttributes();
		Iterator<AttributeInstance> attIt = attVec.iterator();
		while (attIt.hasNext()) {
			AttributeInstance att = attIt.next();
			xml += " " + att.dumpToXML() + "\n";
		}
		xml += "</" + this.getDefinition().getName() + ">\n";
		return xml;

	}

	public void fillInConditions() throws VPDMfException {

		if (this.getDefinition().getConditionElements().size() == 0) {
			return;
		}

		try {

			Iterator it = this.getDefinition().getConditionElements()
					.iterator();
			while (it.hasNext()) {
				ConditionElement ce = (ConditionElement) it.next();

				AttributeInstance ai = this.readAttribute("|"
						+ ce.getClassName() + "." + ce.getAttName());

				Object data = UMLDataConverters.convertToType(
						ai.getDefinition(), ce.getValue());

				ai.setValue(data);

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public Vector readAttributeAddresses() {
		Vector attrs = new Vector();

		Iterator classIt = this.getObjects().values().iterator();
		while (classIt.hasNext()) {
			ClassInstance ci = (ClassInstance) classIt.next();
			UMLclass cDef = ci.getDefinition();

			PrimitiveInstance pi = ci.getPrimitive();
			int pos = pi.getName().lastIndexOf('_');
			String piName = pi.getName().substring(0, pos);

			List<UMLattribute> attributes = cDef.getAttributes();
			Iterator attIt = attributes.iterator();
			while (attIt.hasNext()) {
				UMLattribute att = (UMLattribute) attIt.next();

				if (!att.getToImplement())
					continue;

				String addr = "]" + piName + "|" + cDef.getBaseName() + "."
						+ att.getBaseName();
				attrs.add(addr);
			}
		}

		return attrs;
	}

	public List<AttributeInstance> readAttributes() {
		List<AttributeInstance> attrs = new ArrayList<AttributeInstance>();

		Iterator<ClassInstance> classIt = this.getObjects().values().iterator();
		while (classIt.hasNext()) {
			ClassInstance ci = classIt.next();
			attrs.addAll(ci.attributes.values());
		}

		return attrs;

	}

	public String getDefName() {
		return this.defName;

	}

	public int readIndex() {
		String piName = this.getName();
		int pos = piName.lastIndexOf('_');
		String iString = piName.substring(pos + 1);
		Integer index = new Integer(iString);
		return index.intValue();

	}

	public Vector readOrderedObjects() throws VPDMfException {
		Vector classVec = new Vector(this.getDefinition().getClasses());
		Vector objVec = new Vector();

		CLASSLOOP: for (int i = 0; i < classVec.size(); i++) {
			UMLclass c = (UMLclass) classVec.get(i);

			Iterator objIt = this.getObjects().values().iterator();
			while (objIt.hasNext()) {
				ClassInstance obj = (ClassInstance) objIt.next();
				if (obj.getDefinition().equals(c)) {
					objVec.add(obj);
					continue CLASSLOOP;
				}
			}
			throw new VPDMfException(
					"Errors computing order of objects in primitive");
		}
		return objVec;

	}

	public PrimitiveDefinition getDefinition() {
		return this.definition;
	}

	public Vector readCardinalityOneConnectedPrimitives() {

		Vector linkedPis = new Vector();

		Vector theseClasses = new Vector(this.getDefinition().getClasses());

		Vector edges = new Vector(this.getOutgoingEdges().values());
		edges.addAll(this.getIncomingEdges().values());

		Iterator pliIt = edges.iterator();
		while (pliIt.hasNext()) {
			PrimitiveLinkInstance pli = (PrimitiveLinkInstance) pliIt.next();

			UMLrole r = pli.getPVLinkDef().getRole();
			UMLrole rr = r.otherRole();
			UMLclass c = r.getDirectClass();
			UMLclass cc = rr.getDirectClass();

			if (theseClasses.contains(c) && !theseClasses.contains(cc)
					&& (rr.getMult_upper() == 1)) {

				if (this.equals(pli.getInEdgeNode()))
					linkedPis.add(pli.getOutEdgeNode());
				else
					linkedPis.add(pli.getInEdgeNode());

			} else if (theseClasses.contains(cc) && !theseClasses.contains(c)
					&& (r.getMult_upper() == 1)) {

				if (this.equals(pli.getInEdgeNode()))
					linkedPis.add(pli.getOutEdgeNode());
				else
					linkedPis.add(pli.getInEdgeNode());

			}

		}

		return linkedPis;

	}

	/**
	 * We link the attributes in the model
	 */
	public void linkAttributeInstances() throws Exception {
		Hashtable lookup = new Hashtable();

		Vector pkfks = null;
		AttributeInstance att = null;
		UMLattribute def = null;

		ViewInstance vi = (ViewInstance) this.getGraph().getSubGraphNode();

		int index = this.readIndex();

		Vector defs = new Vector(this.getDefinition().readAttributes());
		List<AttributeInstance> atts = this.readAttributes();

		Iterator attsIt = atts.iterator();
		while (attsIt.hasNext()) {
			att = (AttributeInstance) attsIt.next();
			if (lookup.containsValue(att)) {
				throw new VPDMfException(
						"Primitive Instance Attributes already joined");
			}
			lookup.put(att.getDefinition(), att);
		}

		//
		// Link all intra-pv roles
		//
		Iterator rIt = this.getDefinition().getInternalRoles().iterator();
		while (rIt.hasNext()) {
			UMLrole r = (UMLrole) rIt.next();

			Iterator fIt = r.getFkArray().iterator();

			while (fIt.hasNext()) {
				UMLattribute f = (UMLattribute) fIt.next();
				UMLattribute p = f.getPk();
				AttributeInstance fi = (AttributeInstance) lookup.get(f);
				AttributeInstance pi = (AttributeInstance) lookup.get(p);

				if (fi != null && pi != null) {
					fi.connectTo(pi);
				} else {
					throw new Exception(
							"Can't find linking attributes, error in "
									+ "ViewDefinition: " + "["
									+ vi.getDefinition().getName() + "]"
									+ this.getDefinition().getName() + "|"
									+ r.getDirectClass().getBaseName() + "."
									+ r.getBaseName());
				}

			}

		}

		//
		// Link according to inheritence too.
		//
		Hashtable objLookup = new Hashtable();

		Iterator objsIt = this.getObjects().values().iterator();
		while (objsIt.hasNext()) {
			ClassInstance obj = (ClassInstance) objsIt.next();
			objLookup.put(obj.getDefinition(), obj);
		}

		objsIt = this.getObjects().values().iterator();
		while (objsIt.hasNext()) {
			ClassInstance obj = (ClassInstance) objsIt.next();

			if (obj.getDefinition().getParent() != null) {
				ClassInstance parentObj = (ClassInstance) objLookup.get(obj
						.getDefinition().getParent());

				//
				// Note:
				//
				// Interesting condition here, Previously, we required that
				// the complete inheritence hierarchy had to be present in
				// a primitives. This is, of course, unnecessary. But we
				// should put some safeguards to make sure that no
				// classInstance is 'disconnected', see below.
				//
				if (parentObj == null) {
					continue;
					// throw new
					// VPDMfException("Probably using a standard primitive instead of a lookup view");
				}

				Vector parentPkVec = new Vector(parentObj.getDefinition()
						.getPkArray());
				Vector childPkVec = new Vector(obj.getDefinition().getPkArray());
				for (int i = 0; i < parentPkVec.size(); i++) {
					UMLattribute childPk = (UMLattribute) childPkVec.get(i);
					AttributeInstance childAi = (AttributeInstance) lookup
							.get(childPk);

					UMLattribute parentPk = (UMLattribute) parentPkVec.get(i);
					AttributeInstance parentAi = (AttributeInstance) lookup
							.get(parentPk);

					parentAi.connectTo(childAi);

				}

			}
		}

		//
		// Note:
		//
		// Check for disconnected classInstances
		//
		objsIt = this.getObjects().values().iterator();
		while (this.getObjects().values().size() > 1 && objsIt.hasNext()) {
			ClassInstance obj = (ClassInstance) objsIt.next();

			boolean isDisconnected = true;

			Iterator aiIt = obj.getAttributes().values().iterator();
			while (aiIt.hasNext()) {
				AttributeInstance ai = (AttributeInstance) aiIt.next();
				if (ai.getConnectedKeys().size() > 0) {
					isDisconnected = false;
					break;
				}
			}
			if (isDisconnected)
				throw new Exception("]" + this.getDefinition().getName() + "|"
						+ obj.getDefinition().getBaseName()
						+ " is disconnected. Check the inheritence tree of " +
						this.getDefinition().getName() );

		}

	}

	public void removeDefinition() {

		this.setDefinition(null);

		Iterator<ClassInstance> ciIt = this.getObjects().values().iterator();
		while (ciIt.hasNext()) {
			ClassInstance ci = (ClassInstance) ciIt.next();
			ci.removeDefinition();
		}

	}

	public void instantiateDefinition(ViewDefinition vd) throws Exception {

		PrimitiveDefinition pd = (PrimitiveDefinition) vd.getSubGraph()
				.getNodes().get(this.getDefName());

		this.setDefinition(pd);

		Iterator<ClassInstance>  ciIt = this.getObjects().values().iterator();
		while (ciIt.hasNext()) {
			ClassInstance ci = (ClassInstance) ciIt.next();
			ci.instantiateDefinition(this.getDefinition());
		}

	}

	public void setDefinition(PrimitiveDefinition definition) {
		this.definition = definition;
	}

	public void suckInData(PrimitiveInstance pi) throws Exception {

		Iterator<ClassInstance>  objIt = pi.readOrderedObjects().iterator();
		while (objIt.hasNext()) {
			ClassInstance obj = (ClassInstance) objIt.next();
			ClassInstance thisObj = (ClassInstance) this.getObjects().get(
					obj.getDefinition().getBaseName());

			Iterator<AttributeInstance>  atIt = obj.attributes.values().iterator();
			while (atIt.hasNext()) {
				AttributeInstance at = (AttributeInstance) atIt.next();
				AttributeInstance thisAt = (AttributeInstance) thisObj.attributes
						.get(at.getDefinition().getBaseName());

				if (at.getValue() != null) {
					thisAt.setValue(at.getValue());
				}

			}

		}

	}

	public boolean equals(PrimitiveInstance pi) {

		try {

			Iterator objIt = pi.readOrderedObjects().iterator();
			TOPLOOP: while (objIt.hasNext()) {
				ClassInstance obj = (ClassInstance) objIt.next();
				ClassInstance thisObj = (ClassInstance) this.getObjects().get(
						obj.getDefinition().getBaseName());

				if (thisObj == null)
					return false;

				Iterator atIt = obj.attributes.values().iterator();
				while (atIt.hasNext()) {
					AttributeInstance at = (AttributeInstance) atIt.next();
					AttributeInstance thisAt = (AttributeInstance) thisObj.attributes
							.get(at.getDefinition().getBaseName());

					if ((at.getValue() == null && thisAt.getValue() != null)
							|| (at.getValue() != null && thisAt.getValue() == null)) {
						return false;
					}

					if (at.getValue() == null || thisAt.getValue() == null)
						continue;

					if (!at.getValue().equals(thisAt.getValue()))
						return false;

				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return true;

	}

	public void setDefName(String defName) {
		this.defName = defName;
	}

	public HashMap<String, ClassInstance> getObjects() {
		return objects;
	}

	public void setObjects(HashMap<String, ClassInstance> objects) {
		this.objects = objects;
	}

	public String toString() {
		String s = this.getDefName() + "\n";
		for( ClassInstance ci : this.objects.values() ) {
			s += ci.toString() + "\n~~~~~~~~~~~~~~~~~\n";
		}
		return s;
	}
	
};
