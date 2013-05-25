package edu.isi.bmkeg.vpdmf.model.definitions;

import java.util.ArrayList;
import java.util.Iterator;

import edu.isi.bmkeg.uml.model.UMLassociation;
import edu.isi.bmkeg.uml.model.UMLattribute;
import edu.isi.bmkeg.uml.model.UMLclass;
import edu.isi.bmkeg.uml.model.UMLitem;
import edu.isi.bmkeg.uml.model.UMLrole;
import edu.isi.bmkeg.utils.superGraph.SuperGraphEdge;

/**
 * Timestamp: Thu_Jun_19_120936_2003;
 */

public class PrimitiveLink extends SuperGraphEdge {

	static final long serialVersionUID = -7919696491611703007L;

	private UMLrole role;

	private boolean paged;
	
	/**
	 * returns a vector of umlAttributes for the foreign keys for this link
	 * 
	 */
	public ArrayList<UMLattribute> readFKKeys() {
		ArrayList<UMLattribute> v = new ArrayList<UMLattribute>();
		UMLrole r = this.getRole();

		//
		// Note:
		//
		// If the role is set to null, then this pvlink is Parent->Child
		//
		if (r == null) {

			PrimitiveDefinition childPv = (PrimitiveDefinition) this
					.getInEdgeNode();
			UMLclass primaryClass = childPv.getPrimaryClass();
			v.addAll(primaryClass.getPkArray());

		} else if (r.getImplementedBy().size() > 0) {

			Iterator<UMLrole> it = r.getImplementedBy().iterator();
			while (it.hasNext()) {
				UMLrole tempR = (UMLrole) it.next();
				v.addAll(tempR.getFkArray());
			}

		} else {
			v.addAll(this.getRole().getFkArray());
		}

		return v;

	}

	/**
	 * Returns the LinkClass of the association (null if there is no LinkClass).
	 * 
	 */
	public UMLclass readLinkClass() {
		if (this.getRole() == null)
			return null;

		UMLassociation ass = this.getRole().getAss();
		return ass.getLinkClass();

	}

	/**
	 * returns a vector of umlAttributes for the primary keys for this link
	 * 
	 */
	public ArrayList readPKKeys() {
		ArrayList fks = this.readFKKeys();
		Iterator it = fks.iterator();

		ArrayList pks = new ArrayList();
		while (it.hasNext()) {
			UMLattribute fk = (UMLattribute) it.next();
			pks.add(fk.getPk());
		}

		return pks;

	}

	public void setRole(UMLrole role) {
		this.role = role;
	}

	public UMLrole getRole() {
		return role;
	}

	public boolean isPaged() {
		return paged;
	}

	public void setPaged(boolean paged) {
		this.paged = paged;
	}

};
