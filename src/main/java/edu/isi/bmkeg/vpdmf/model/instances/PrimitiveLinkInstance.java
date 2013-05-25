package edu.isi.bmkeg.vpdmf.model.instances;

/**
 * Timestamp: Thu_Jun_19_120936_2003;
 */

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.isi.bmkeg.uml.model.UMLattribute;
import edu.isi.bmkeg.uml.model.UMLclass;
import edu.isi.bmkeg.uml.model.UMLrole;
import edu.isi.bmkeg.utils.superGraph.SuperGraphEdge;
import edu.isi.bmkeg.vpdmf.model.definitions.PrimitiveDefinition;
import edu.isi.bmkeg.vpdmf.model.definitions.PrimitiveLink;
import edu.isi.bmkeg.vpdmf.model.definitions.ViewDefinition;

public class PrimitiveLinkInstance extends SuperGraphEdge {

	static final long serialVersionUID = 2593554832580697147L;

	private ClassInstance linkClass;
	private PrimitiveLink pVLinkDef;
	private String defName;

	public PrimitiveLinkInstance() {
		super();
	}
	
	public PrimitiveLinkInstance(PrimitiveLink pL) {
		super();
		this.setPVLinkDef(pL);

		this.defName = pL.getOutEdgeNode().getName() + " "
				+ pL.getInEdgeNode().getName();

		if (pL.getRole() != null && pL.getRole().getImplementedBy().size() > 0) {

			UMLclass linkClass = pL.getRole().getAss().getLinkClass();
			ClassInstance linkClassInstance = new ClassInstance(linkClass);
			this.setLinkClass(linkClassInstance);
			linkClassInstance.setPrimitiveLinkInstance(this);

		}

	}

	public void clearConditions() {
		if (this.linkClass != null) {
			this.linkClass.clearConditions();
		}

	}

	public void destroy() {
		this.linkClass = null;
		this.pVLinkDef = null;

		super.destroy();
	}

	public String dumpToXML() throws Exception {
		String xml = "<PrimitiveLink>\n";

		PrimitiveInstance fromPi = (PrimitiveInstance) this.getOutEdgeNode();
		PrimitiveInstance toPi = (PrimitiveInstance) this.getInEdgeNode();
		xml += "  <start type='" + fromPi.getDefinition().getName()
				+ "' index='" + fromPi.readIndex() + "'/>\n";
		xml += "  <end type='" + toPi.getDefinition().getName() + "' index='"
				+ toPi.readIndex() + "'/>\n";

		if (this.linkClass != null) {
			String temp = this.getLinkClass().dumpToXML();
			Pattern patt = Pattern.compile("\n");
			Matcher matcher = patt.matcher(temp);
			temp = matcher.replaceAll("\n  ");
			xml += temp;
		}
		xml += "</PrimitiveLink>\n";

		return xml;

	}

	/**
	 * Get the addresses of the linking attributes from each primitive.
	 * 
	 * Note that this does NOT include the attributes of the linking class
	 */
	public ArrayList readAttributeAddresses() {
		ArrayList attrs = new ArrayList();
		PrimitiveLink pl = this.getPVLinkDef();
		PrimitiveDefinition pv1 = (PrimitiveDefinition) pl.getOutEdgeNode();
		PrimitiveDefinition pv2 = (PrimitiveDefinition) pl.getInEdgeNode();

		ArrayList attDefLookup_pv1 = new ArrayList(pv1.readAttributes());
		ArrayList attDefLookup_pv2 = new ArrayList(pv2.readAttributes());

		ArrayList LinkClassLookup = null;
		if (pl.readLinkClass() != null) {
			LinkClassLookup = new ArrayList(pl.readLinkClass().getAttributes());
		} else {
			LinkClassLookup = new ArrayList();
		}

		//
		// Note:
		//
		// If the role isn't null, then the inter-Pv connection is
		// conventional... role mediated...
		//
		UMLrole role = pl.getRole();
		if (role != null) {

			// Run through the implemented roles to find out 
			// where they terminate in their
			// respective primitives.
			Set<UMLrole> roles = new HashSet<UMLrole>();
			if( role.getImplementedBy().size() > 0 ) {
				roles = role.getImplementedBy();
			} else {
				roles.add(role);
			}

			Iterator roleIt = roles.iterator();
			while (roleIt.hasNext()) {
				role = (UMLrole) roleIt.next();

				Iterator fksIt = role.getFkArray().iterator();
				while (fksIt.hasNext()) {

					UMLattribute fk = (UMLattribute) fksIt.next();
					UMLattribute pk = fk.getPk();
					String fkPv = null;
					String pkPv = null;

					if (attDefLookup_pv1.contains(pk)
							&& attDefLookup_pv2.contains(fk)) {
						pkPv = pv1.getName();
						fkPv = pv2.getName();
					} else if (attDefLookup_pv1.contains(fk)
							&& attDefLookup_pv2.contains(pk)) {
						fkPv = pv1.getName();
						pkPv = pv2.getName();
					} else if (attDefLookup_pv1.contains(pk)
							&& LinkClassLookup.contains(fk)) {
						pkPv = pv1.getName();
					} else if (attDefLookup_pv2.contains(pk)
							&& LinkClassLookup.contains(fk)) {
						pkPv = pv2.getName();
					}

					if (fkPv != null) {
						attrs.add("]" + fkPv + "|"
								+ fk.getParentClass().getBaseName() + "."
								+ fk.getBaseName());
					}
					if (pkPv != null) {
						attrs.add("]" + pkPv + "|"
								+ pk.getParentClass().getBaseName() + "."
								+ pk.getBaseName());
					}

				}

			}

		}
		//
		// Note:
		//
		// If the role is not null, then the inter-Pv connection is
		// unconventional... and is a parent -> child relationship
		//
		else {

			Iterator fksIt = pv2.getPrimaryClass().getPkArray().iterator();
			while (fksIt.hasNext()) {

				UMLattribute fk = (UMLattribute) fksIt.next();
				UMLattribute pk = fk.getPk();
				String fkPv = pv2.getName();
				String pkPv = pv1.getName();

				attrs.add("]" + fkPv + "|" + fk.getParentClass().getBaseName()
						+ "." + fk.getBaseName());

				attrs.add("]" + pkPv + "|" + pk.getParentClass().getBaseName()
						+ "." + pk.getBaseName());
			}

		}

		return attrs;

	}

	public ArrayList readAttributes() {
		ArrayList attrs = new ArrayList();

		if (this.linkClass != null) {
			ClassInstance ci = this.linkClass;
			List<AttributeInstance> attributes = 
					new ArrayList<AttributeInstance>(ci.getAttributes().values());
			Iterator attIt = attributes.iterator();
			while (attIt.hasNext()) {
				attrs.add((AttributeInstance) attIt.next());
			}

		}

		return attrs;

	}

	public void removeDefinition() {

		this.setPVLinkDef(null);

		if (this.getLinkClass() != null) {
			this.getLinkClass().removeDefinition();
		}

	}

	public void instantiateDefinition(ViewDefinition vd) throws Exception {

		String[] pdNames = this.defName.split(" ");

		PrimitiveDefinition pd = (PrimitiveDefinition) vd.getSubGraph()
				.getNodes().get(pdNames[0]);

		PrimitiveLink pl = (PrimitiveLink) pd.getOutgoingEdges().get(pdNames[1]);

		this.setPVLinkDef(pl);

		if (this.getLinkClass() != null) {
			this.getLinkClass().instantiateDefinition(pl);
		}

	}

	public PrimitiveInstance getIncompletePI() {
		PrimitiveInstance toPi = (PrimitiveInstance) this.getInEdgeNode();
		boolean toSetFlag = toPi.arePkSet();

		PrimitiveInstance fromPi = (PrimitiveInstance) this.getOutEdgeNode();
		boolean fromSetFlag = fromPi.arePkSet();

		PrimitiveInstance targetPi = null;
		if (toSetFlag && !fromSetFlag) {
			targetPi = fromPi;
		} else if (!toSetFlag && fromSetFlag) {
			targetPi = toPi;
		} else {
			targetPi = null;
		}

		return targetPi;
	}

	public ClassInstance getLinkClass() {
		return this.linkClass;
	}

	public PrimitiveLink getPVLinkDef() {
		return this.pVLinkDef;
	}

	public boolean hasData() {
		Iterator aiIt = this.getLinkClass().attributes.values().iterator();

		while (aiIt.hasNext()) {
			AttributeInstance ai = (AttributeInstance) aiIt.next();
			String s = ai.getDefinition().getStereotype();
			if (!s.equals("PK") && !s.equals("FK")) {
				return true;
			}
		}

		return false;

	}

	/**
	 * Create a light copy of the current primitive link instance
	 * 
	 */
	public PrimitiveLinkInstance lightCopy() {
		PrimitiveInstanceGraph pig = (PrimitiveInstanceGraph) this.getGraph();
		ViewInstance vi = (ViewInstance) pig.getSubGraphNode();
		ViewInstance viCopy = vi.deepCopy();

		PrimitiveLinkInstance lightPli = new PrimitiveLinkInstance(
				this.getPVLinkDef());

		lightPli.suckInData(this.getLinkClass());

		return lightPli;

	}

	public void linkAttributeInstances() throws Exception {
		PrimitiveLink pl = this.getPVLinkDef();
		PrimitiveInstanceGraph pig = (PrimitiveInstanceGraph) this.getGraph();
		ViewInstance vi = (ViewInstance) pig.getSubGraphNode();

		ArrayList FKs = pl.readFKKeys();
		ArrayList addresses = this.readAttributeAddresses();

		PrimitiveInstance pi_out = (PrimitiveInstance) this.getOutEdgeNode();
		int pi_out_index = pi_out.readIndex();
		PrimitiveInstance pi_in = (PrimitiveInstance) this.getInEdgeNode();
		int pi_in_index = pi_in.readIndex();

		Iterator attDefIt = FKs.iterator();
		while (attDefIt.hasNext()) {
			UMLattribute fk = (UMLattribute) attDefIt.next();
			UMLattribute pk = fk.getPk();

			AttributeInstance pkIns = null;
			AttributeInstance fkIns = null;
			AttributeInstance att = null;
			UMLattribute def = null;

			Iterator addressesIt = addresses.iterator();
			while (addressesIt.hasNext()) {
				String address = (String) addressesIt.next();

				if (address.startsWith("]" + pi_out.getDefinition().getName()
						+ "|")) {
					att = vi.readAttributeInstance(address, pi_out_index);
				} else {
					att = vi.readAttributeInstance(address, pi_in_index);
				}
				def = vi.readAttributeDefinition(address);

				if (fk.equals(def)) {
					fkIns = att;
				} else if (pk.equals(def)) {
					pkIns = att;
				}

			}

			if (fkIns == null && this.getLinkClass() != null) {
				Iterator attsIt = this.getLinkClass().getAttributes()
						.values().iterator();
				LOOP: while (attsIt.hasNext()) {
					att = (AttributeInstance) attsIt.next();
					if (att.getDefinition().equals(fk)) {
						fkIns = att;
						break LOOP;
					}
				}
			}

			if (fkIns == null || pkIns == null) {
				throw new Exception("Cannot join attributes between "
						+ pi_out.getDefName() + " & " + pi_in.getDefName());
			}

			fkIns.connectTo(pkIns);

		}

	}

	public void setLinkClass(ClassInstance linkClass) {
		this.linkClass = linkClass;
	}

	public void setPVLinkDef(PrimitiveLink pVLinkDef) {
		this.pVLinkDef = pVLinkDef;
	}

	public void suckInData(ClassInstance obj) {
		if (this.getLinkClass() == null) {
			return;
		}

		try {

			ClassInstance thisObj = this.getLinkClass();

			Iterator atIt = obj.attributes.values().iterator();
			while (atIt.hasNext()) {
				AttributeInstance at = (AttributeInstance) atIt.next();
				AttributeInstance thisAt = (AttributeInstance) thisObj.attributes
						.get(at.getDefinition().getBaseName());

				if (at.getValue() != null) {
					thisAt.setValue(at.getValue());
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getDefName() {
		return defName;
	}

	public void setDefName(String defName) {
		this.defName = defName;
	}

};
