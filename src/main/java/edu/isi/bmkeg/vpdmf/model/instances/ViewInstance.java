package edu.isi.bmkeg.vpdmf.model.instances;

/**
 * Timestamp: Thu_Jun_19_120936_2003;
 */

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;

import cern.colt.matrix.ObjectMatrix1D;
import edu.isi.bmkeg.uml.model.UMLattribute;
import edu.isi.bmkeg.uml.model.UMLclass;
import edu.isi.bmkeg.uml.model.UMLmodel;
import edu.isi.bmkeg.utils.superGraph.SuperGraphEdge;
import edu.isi.bmkeg.utils.superGraph.SuperGraphNode;
import edu.isi.bmkeg.utils.superGraph.SuperGraphTraversal;
import edu.isi.bmkeg.vpdmf.exceptions.AttributeAddressException;
import edu.isi.bmkeg.vpdmf.exceptions.VPDMfException;
import edu.isi.bmkeg.vpdmf.model.definitions.IndexElement;
import edu.isi.bmkeg.vpdmf.model.definitions.PrimitiveDefinition;
import edu.isi.bmkeg.vpdmf.model.definitions.PrimitiveDefinitionGraph;
import edu.isi.bmkeg.vpdmf.model.definitions.PrimitiveLink;
import edu.isi.bmkeg.vpdmf.model.definitions.VPDMf;
import edu.isi.bmkeg.vpdmf.model.definitions.ViewDefinition;
import edu.isi.bmkeg.vpdmf.model.definitions.ViewLink;

public class ViewInstance extends LightViewInstance {

	private static final long serialVersionUID = 1L;

	private int kmID;

	private boolean visible;

	private boolean complete;

	private Map<String, Integer> piTotals = new HashMap<String, Integer>();

	private HashSet<ViewLinkInstance> linksToFillIn = new HashSet<ViewLinkInstance>();

	private PrimitiveInstance primaryPrimitive;

	private PrimitiveInstanceGraph subGraph;

	private viewGraphInstance graph;

	public ViewInstance() {
		super();
	}

	public ViewInstance(String defName) {
		super();

		// Checks
		if (defName == null || defName.length() == 0) {
			Exception argh = new Exception("Can't build an empty view instance");
			argh.printStackTrace();
		}

		this.setVpdmfLabel("");
		this.setUIDString("");
		this.setVpdmfUri("");

		this.setDefName(defName);
		
		this.complete = false;

	}

	public ViewInstance(ViewInstance vi) {

		super();

		// Checks
		if (vi.getDefinition() == null) {
			Exception argh = new Exception("Can't build an empty view instance");
			argh.printStackTrace();
		}

		this.init(vi.getDefinition());

		this.setUIDString(vi.getUIDString());
		this.setVpdmfId(vi.getVpdmfId());

		if (vi.getSubGraph() != null)
			this.setPiTotals(vi.getPiTotals());

	}

	public ViewInstance(ViewDefinition vd) {

		super();

		// Checks
		if (vd == null) {
			Exception argh = new Exception("Can't build an empty view instance");
			argh.printStackTrace();
		}

		this.init(vd);

	}

	public boolean isVisible() {
		return visible;
	}

	public HashSet<ViewLinkInstance> getLinksToFillIn() {
		return linksToFillIn;
	}

	public void setLinksToFillIn(HashSet<ViewLinkInstance> linksToFillIn) {
		this.linksToFillIn = linksToFillIn;
	}

	public Map<String, Integer> getPiTotals() {
		return piTotals;
	}

	public void setComplete(boolean complete) {
		this.complete = complete;
	}

	public void setPrimaryPrimitive(PrimitiveInstance primaryPrimitive) {
		this.primaryPrimitive = primaryPrimitive;
	}

	public void setPiTotals(Map<String, Integer> piTotals) {
		this.piTotals = piTotals;
	}

	/*
	 * public void set_thumbnail(ImageIcon thumbnail) { this.thumbnail =
	 * thumbnail; }
	 */

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	// ~ Overridden setters & getters ~~~~~~~~~~~~~~~~~~~~~~~~~

	public PrimitiveInstanceGraph getSubGraph() {
		return this.subGraph;
	}

	public void setSubGraph(PrimitiveInstanceGraph pig) {
		this.subGraph = pig;
		// super.setSubGraph(pig);
	}

	public viewGraphInstance getGraph() {
		return this.graph;
	}

	public void setGraph(viewGraphInstance vgi) {
		this.graph = vgi;
		// super.setGraph(vgi);
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private void init(ViewDefinition vd) {

		this.setDefName(vd.getName());
		// this.setIcon(defName);

		this.setVpdmfLabel("");
		this.setUIDString("");
		this.setVpdmfUri("");

		this.setDefinition(vd);

		Hashtable lookup = new Hashtable();

		PrimitiveInstanceGraph pig = new PrimitiveInstanceGraph();
		this.setSubGraph(pig);
		pig.setSubGraphNode(this);

		PrimitiveDefinitionGraph pdg = (PrimitiveDefinitionGraph) vd
				.getSubGraph();
		pig.setDefinition(pdg);

		/**
		 * Build a basic PrimitiveInstanceGraph from the
		 * PrimitiveDefinitionGraph
		 */
		Iterator pdIt = pdg.getNodes().values().iterator();
		while (pdIt.hasNext()) {
			PrimitiveDefinition pd = (PrimitiveDefinition) pdIt.next();
			PrimitiveInstance pI = new PrimitiveInstance(pd);

			if (pd.equals(vd.getPrimaryPrimitive())) {
				this.setPrimaryPrimitive(pI);
			}

			try {

				pig.addPvInstance(pI);
				pI.fillInConditions();

			} catch (Exception e) {

				e.printStackTrace();

			}

		}

		Iterator j = pdg.getEdges().iterator();
		while (j.hasNext()) {
			PrimitiveLink currentPvLink = (PrimitiveLink) j.next();

			PrimitiveDefinition fromPvDef = (PrimitiveDefinition) currentPvLink
					.getOutEdgeNode();

			PrimitiveDefinition toPvDef = (PrimitiveDefinition) currentPvLink
					.getInEdgeNode();

			try {
				pig.addPvInstanceLink(fromPvDef.getName() + "_0",
						toPvDef.getName() + "_0");

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	}

	public String toString() {

		String idx = this.readFormattedHumanIndex(true);

		return idx;
	}

	public int getKmID() {
		return this.kmID;
	}

	public boolean getComplete() {
		return this.complete;
	}

	public boolean isHeavy() throws VPDMfException {
		if (this.getSubGraph() != null)
			return true;
		else
			return false;
	}

	public ViewInstance convertLight2Heavy() throws VPDMfException {

		ViewInstance lVi = this;

		ViewInstance hVi = null;

		if (lVi.getSubGraph() == null) {

			String uid = lVi.getUIDString();

			hVi = new ViewInstance(lVi.getDefinition());
			hVi.setVpdmfLabel(lVi.getVpdmfLabel());
			hVi.setVpdmfUri(lVi.getVpdmfUri());

			hVi.setUIDString(uid);
			hVi.setVpdmfId(lVi.getVpdmfId());

			if (uid != null) {

				String uidName = uid.substring(0, uid.indexOf("="));
				String uidValue = uid.substring(uid.indexOf("=") + 1,
						uid.length());

				ViewDefinition vd = hVi.getDefinition();
				String addr = "]"
						+ vd.getPrimaryPrimitive().getName()
						+ "|"
						+ vd.getPrimaryPrimitive().getPrimaryClass()
								.getBaseName() + "." + uidName;
				try {
					AttributeInstance ai = hVi.readAttributeInstance(addr, 0);
					ai.writeValueString(uidValue);
				} catch (Exception e) {
				}
			}

		} else {

			hVi = lVi;

		}

		return hVi;

	}

	public boolean isNull() throws VPDMfException {
		Iterator it = this.getSubGraph().getNodes().values().iterator();
		while (it.hasNext()) {
			PrimitiveInstance pi = (PrimitiveInstance) it.next();
			if (!pi.isNull())
				return false;
		}
		return true;
	}

	public boolean isNullOutsideOfViewtable() throws VPDMfException {

		Iterator it = this.getSubGraph().getNodes().values().iterator();

		while (it.hasNext()) {

			PrimitiveInstance pi = (PrimitiveInstance) it.next();

			if (this.getPrimaryPrimitive().equals(pi)) {

				// continue;

				//
				// Bugfix:
				//
				// Added by Weicheng.
				//
				// Make sure to check those non-primary classes in
				// the primmary primitive.
				// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
				Iterator ciIt = pi.getObjects().values().iterator();
				while (ciIt.hasNext()) {
					ClassInstance ci = (ClassInstance) ciIt.next();

					if (ci.getDefinition().getBaseName().equals("ViewTable")) {
						continue;
					}

					if (!ci.isEmpty()) {
						return false;
					}
				}
				// End Bugfix
				// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			}

			if (!pi.isNull())

				return false;

		}

		return true;
	}

	public boolean isViewComplete() throws VPDMfException {
		Iterator it = this.getDefinition().getSubGraph().getNodes().values()
				.iterator();
		while (it.hasNext()) {
			PrimitiveDefinition pd = (PrimitiveDefinition) it.next();
			if (this.countPrimitives(pd) != this.readPrimitiveInstanceTotal(pd))
				return false;
		}
		return true;
	}

	public boolean arePrimitiveTotalsSet() throws VPDMfException {
		Iterator it = this.getDefinition().getSubGraph().getNodes().values()
				.iterator();
		while (it.hasNext()) {
			PrimitiveDefinition pd = (PrimitiveDefinition) it.next();
			if (this.getPiTotals().get(pd.getName()) == null)
				return false;
		}
		return true;
	}

	public int readPrimitiveInstanceTotal(PrimitiveDefinition pd)
			throws VPDMfException {

		if (!this.getDefinition().getSubGraph().getNodes()
				.containsKey(pd.getName()))
			throw new VPDMfException("Primitive not found " + pd.getName());

		Integer ii = (Integer) this.getPiTotals().get(pd.getName());

		if (ii == null)
			ii = new Integer(0);

		return ii.intValue();

	}

	public void computePrimitiveInstanceTotal(PrimitiveDefinition pd, int i)
			throws VPDMfException {

		if (!this.getDefinition().getSubGraph().getNodes()
				.containsKey(pd.getName()))
			throw new VPDMfException("Primitive not found " + pd.getName());

		Integer ii = new Integer(i);

		this.getPiTotals().put(pd.getName(), ii);

	}

	public void addNewPrimitiveRecursively(PrimitiveInstance sourcePI,
			PrimitiveDefinition pd, int j, boolean forwardFlag)
			throws Exception {

		PrimitiveInstanceGraph pig = (PrimitiveInstanceGraph) sourcePI
				.getGraph();

		//
		// If the target Pd is unique, link up the data and leave it at that.
		//
		if (pd.isUnique()) {
			if (forwardFlag) {
				pig.addPvInstanceLink(sourcePI.getName(), pd.getName() + "_0");
			} else {
				pig.addPvInstanceLink(pd.getName() + "_0", sourcePI.getName());
			}
			return;
		}

		//
		// ... Otherwise, add a new primitive and fill all the
		// links of that primitive
		//
		String newName = pd.getName() + "_" + j;
		PrimitiveInstance newPI = new PrimitiveInstance(pd, j);
		newPI.setName(newName);
		pig.addNode(newPI);
		newPI.setGraph(pig);
		newPI.fillInConditions();
		newPI.linkAttributeInstances();

		if (forwardFlag) {
			pig.addPvInstanceLink(sourcePI.getName(), newName);
		} else {
			pig.addPvInstanceLink(newName, sourcePI.getName());
		}

		//
		// Does the new Pi have any links to any primitives.
		// other than the source primtiive? We need to add those
		// too.
		//
		Iterator it = newPI.getDefinition().getOutgoingEdges().values()
				.iterator();
		while (it.hasNext()) {
			PrimitiveLink pl = (PrimitiveLink) it.next();
			PrimitiveDefinition otherPd = (PrimitiveDefinition) pl
					.getInEdgeNode();
			if (!otherPd.equals(sourcePI.getDefinition())) {

				this.addNewPrimitiveRecursively(newPI, otherPd, j, true);

			}
		}

		it = newPI.getDefinition().getIncomingEdges().values().iterator();
		while (it.hasNext()) {
			PrimitiveLink pl = (PrimitiveLink) it.next();
			PrimitiveDefinition otherPd = (PrimitiveDefinition) pl
					.getOutEdgeNode();
			if (!otherPd.equals(sourcePI.getDefinition())) {

				this.addNewPrimitiveRecursively(newPI, otherPd, j, false);

			}
		}

	}

	/**
	 * Adds a primitive instance as a new node in the primitive instance graph
	 * 
	 * (Note that there may be additional constraints that MUST be filled in, it
	 * is not currently clear how to accomplish this).
	 * 
	 * @throws Exception
	 */
	public void addNewPrimitiveInstance(String pdName, int j) throws Exception {

		PrimitiveInstanceGraph pig = (PrimitiveInstanceGraph) this
				.getSubGraph();

		PrimitiveDefinition pd = (PrimitiveDefinition) this.getDefinition()
				.getSubGraph().getNodes().get(pdName);
		PrimitiveInstance newPI = new PrimitiveInstance(pd);
		newPI.setName(pdName + "_" + j);
		pig.addNode(newPI);
		newPI.setGraph(pig);
		newPI.fillInConditions();
		newPI.linkAttributeInstances();

		// what's the path from the primary primitive to this node
		PrimitiveDefinition ppd = this.getDefinition().getPrimaryPrimitive();
		PrimitiveDefinitionGraph pdg = (PrimitiveDefinitionGraph) this
				.getDefinition().getSubGraph();

		Iterator<PrimitiveDefinition> pdIt = pdg.readPrimitivesToTarget(pd)
				.iterator();
		PrimitiveDefinition lastPd = pdIt.next();
		while (pdIt.hasNext()) {
			PrimitiveDefinition thisPd = (PrimitiveDefinition) pdIt.next();
			if (thisPd.equals(pd)) {
				pig.addPvInstanceLink(lastPd.getName() + "_" + 0,
						thisPd.getName() + "_" + j);
			}
			int i = 0;
			lastPd = thisPd;
		}

	}

	public void trimRepeatedPrimitives() throws Exception {

		PrimitiveDefinitionGraph pdg = (PrimitiveDefinitionGraph) this
				.getDefinition().getSubGraph();

		Iterator pdIt = pdg.getNodes().values().iterator();
		while (pdIt.hasNext()) {
			PrimitiveDefinition pd = (PrimitiveDefinition) pdIt.next();
			int n = this.countPrimitives(pd);

			HashSet ids = new HashSet();
			for (int i = 0; i < n; i++) {

				AttributeInstance ai = this.readAttributeInstance(
						"]"
								+ pd.getName()
								+ "|"
								+ pd.getPrimaryClass().getBaseName()
								+ "."
								+ ((UMLattribute) pd.getPrimaryClass()
										.getPkArray().get(0)).getBaseName(), i);

				if (!ids.contains(ai.getValue())) {
					ids.add(ai.getValue());
				} else {

				}

			}

		}

	}

	public void fillInExtraPrimitiveLinks() throws Exception {

		PrimitiveInstanceGraph pig = (PrimitiveInstanceGraph) this
				.getSubGraph();

		PrimitiveDefinitionGraph pdg = (PrimitiveDefinitionGraph) this
				.getDefinition().getSubGraph();

		Iterator plIt = pdg.getEdges().iterator();
		while (plIt.hasNext()) {
			PrimitiveLink pl = (PrimitiveLink) plIt.next();

			PrimitiveDefinition fPd = (PrimitiveDefinition) pl.getOutEdgeNode();
			PrimitiveDefinition tPd = (PrimitiveDefinition) pl.getInEdgeNode();

			String fAddr = "";
			String tAddr = "";

			ArrayList keys = pl.readFKKeys();
			for (int i = 0; i < keys.size(); i++) {
				UMLattribute fk = (UMLattribute) keys.get(i);
				UMLattribute pk = fk.getPk();

				if (fPd.getClasses().contains(pk.getParentClass())) {
					fAddr = "]" + fPd.getName() + "|"
							+ pk.getParentClass().getBaseName() + "."
							+ pk.getBaseName();
				}

				if (fPd.getClasses().contains(fk.getParentClass())) {
					fAddr = "]" + fPd.getName() + "|"
							+ fk.getParentClass().getBaseName() + "."
							+ fk.getBaseName();
				}

				if (tPd.getClasses().contains(pk.getParentClass())) {
					tAddr = "]" + tPd.getName() + "|"
							+ pk.getParentClass().getBaseName() + "."
							+ pk.getBaseName();
				}

				if (tPd.getClasses().contains(fk.getParentClass())) {
					tAddr = "]" + tPd.getName() + "|"
							+ fk.getParentClass().getBaseName() + "."
							+ fk.getBaseName();
				}
			}

			int nf = this.countPrimitives(fPd);
			int nt = this.countPrimitives(tPd);

			//
			// Look for matching keys
			//
			for (int j = 0; j < nf; j++) {
				AttributeInstance aif = this.readAttributeInstance(fAddr, j);

				for (int k = 0; k < nt; k++) {
					AttributeInstance ait = this
							.readAttributeInstance(tAddr, k);

					if (aif.getValue() == null || ait.getValue() == null)
						continue;

					if (aif.getValue().equals(ait.getValue())) {

						pig.addPvInstanceLink(fPd.getName() + "_" + j,
								tPd.getName() + "_" + k);

					}

				}

			}

		}

	}

	public void removeDefinition() {
		this.setDefinition(null);

		PrimitiveInstanceGraph pig = (PrimitiveInstanceGraph) this
				.getSubGraph();

		//
		// If there is no pig, then we just flip out and get away from here.
		//
		if (pig == null) {
			return;
		}

		pig.setDefinition(null);

		Iterator piIt = this.getSubGraph().getNodes().values().iterator();
		while (piIt.hasNext()) {
			PrimitiveInstance pi = (PrimitiveInstance) piIt.next();
			pi.removeDefinition();
		}

		Iterator pliIt = this.getSubGraph().getEdges().iterator();
		while (pliIt.hasNext()) {
			PrimitiveLinkInstance pli = (PrimitiveLinkInstance) pliIt.next();
			pli.removeDefinition();
		}
	}

	public void instantiateDefinition(VPDMf vpdm) throws Exception {

		ViewDefinition vd = (ViewDefinition) vpdm.getViews().get(
				this.getDefName());
		this.setDefinition(vd);

		if (this.getSubGraph() == null) {
			ViewInstance tempVi = new ViewInstance(vd);
			PrimitiveInstanceGraph tempPig = (PrimitiveInstanceGraph) tempVi
					.getSubGraph();
			tempPig.setSubGraphNode(tempVi);
			this.setSubGraph(tempPig);
			tempVi = null;
		}

		PrimitiveInstanceGraph pig = (PrimitiveInstanceGraph) this
				.getSubGraph();
		PrimitiveDefinitionGraph pdg = (PrimitiveDefinitionGraph) vd
				.getSubGraph();
		pig.setDefinition(pdg);

		Iterator piIt = this.getSubGraph().getNodes().values().iterator();
		while (piIt.hasNext()) {
			PrimitiveInstance pi = (PrimitiveInstance) piIt.next();
			pi.instantiateDefinition(vd);
		}

		Iterator pliIt = this.getSubGraph().getEdges().iterator();
		while (pliIt.hasNext()) {
			PrimitiveLinkInstance pli = (PrimitiveLinkInstance) pliIt.next();
			pli.instantiateDefinition(vd);
		}

	}

	public void convertImagesToStreams() throws IOException {

		if (this.getSubGraph() == null) {
			return;
		}

		Iterator aiIt = this.readAttributes().iterator();
		while (aiIt.hasNext()) {
			AttributeInstance ai = (AttributeInstance) aiIt.next();

			ai.convertImagesToStreams();

		}
	}

	public void convertStreamsToImages() throws IOException,
			ClassNotFoundException {

		Iterator aiIt = this.readAttributes().iterator();

		while (aiIt.hasNext()) {
			AttributeInstance ai = (AttributeInstance) aiIt.next();

			ai.convertStreamsToImages();

		}

	}

	public void clearConditions() {
		PrimitiveInstanceGraph pig = (PrimitiveInstanceGraph) this
				.getSubGraph();
		Iterator piIt = pig.getNodes().values().iterator();
		while (piIt.hasNext()) {
			PrimitiveInstance pi = (PrimitiveInstance) piIt.next();
			pi.clearConditions();
		}

		Iterator pliIt = pig.getEdges().iterator();
		while (pliIt.hasNext()) {
			PrimitiveLinkInstance pli = (PrimitiveLinkInstance) pliIt.next();
			pli.clearConditions();
		}

	}

	/**
	 * Counts the number of instances of pd in the current ViewGraph
	 * 
	 * @throws Exception
	 *             ß
	 */
	public int countPrimitives(String attrAddr) throws Exception {
		PrimitiveDefinition pd = this.readPrimitiveDefinition(attrAddr);
		return this.countPrimitives(pd);
	}

	public void confirmPrimitiveLinks(PrimitiveInstance thisPi)
			throws Exception {

		PrimitiveDefinition thisPd = thisPi.getDefinition();
		int thisCount = this.countPrimitives(thisPd);
		PrimitiveDefinition thatPd = null;

		PrimitiveInstanceGraph pig = (PrimitiveInstanceGraph) this
				.getSubGraph();

		//
		// Does an example of this primitiveLink exist within the view?
		//
		Collection<SuperGraphEdge> existingLinks = thisPi.getIncomingEdges()
				.values();
		existingLinks.addAll(thisPi.getOutgoingEdges().values());
		Iterator pliIt = existingLinks.iterator();
		while (pliIt.hasNext()) {
			PrimitiveLinkInstance pli = (PrimitiveLinkInstance) pliIt.next();
			PrimitiveLink plCheck = pli.getPVLinkDef();
			if (!(plCheck.getInEdgeNode().equals(thisPd) && plCheck
					.getOutEdgeNode().equals(thatPd))
					&& !(plCheck.getOutEdgeNode().equals(thisPd) && plCheck
							.getInEdgeNode().equals(thatPd)))
				existingLinks.add(plCheck);

		}

		Iterator plIt = existingLinks.iterator();
		while (plIt.hasNext()) {
			PrimitiveLink pl = (PrimitiveLink) plIt.next();

			boolean forwardFlag = true;

			if (existingLinks.contains(pl))
				continue;

			PrimitiveDefinition i = (PrimitiveDefinition) pl.getInEdgeNode();
			PrimitiveDefinition o = (PrimitiveDefinition) pl.getOutEdgeNode();

			if (i.equals(thisPd)) {
				thatPd = o;
			} else if (o.equals(thisPd)) {
				thatPd = i;
				forwardFlag = false;
			} else {
				throw new VPDMfException("Error in Primitive Links");
			}

			int thatCount = this.countPrimitives(thatPd);

			String f = "";
			String t = "";
			int c = 0;

			if (thatCount > 1 && thatCount >= thisCount) {
				c = thisCount - 1;
			}

			if (forwardFlag) {
				f = thisPi.getName();
				t = thatPd.getName() + "_" + c;
			} else {
				t = thisPi.getName();
				f = thatPd.getName() + "_" + c;
			}

			pig.addPvInstanceLink(f, t);

		}

	}

	/**
	 * Counts the number of instances of pd in the current ViewGraph
	 */
	public int countPrimitives(PrimitiveDefinition pd) {
		int count = 0;
		Vector keys = null;
		try {
			keys = new Vector(this.getSubGraph().getNodes().keySet());
		} catch (Exception e) {
			e.printStackTrace();
		}

		Pattern patt = Pattern.compile("^" + pd.getName() + "_[\\d]+$");

		Pattern luPatt = Pattern.compile("^" + pd.getName() + "LU_[\\d]+$");

		Iterator it = keys.iterator();
		while (it.hasNext()) {
			String pvName = (String) it.next();

			Matcher m1 = patt.matcher(pvName);
			Matcher m2 = luPatt.matcher(pvName);
			if (m1.find() || m2.find())
				count++;

		}
		return count;

	}

	public ViewInstance cloneToParent() throws Exception {
		ViewDefinition vd = this.getDefinition().getParent();

		if (vd == null) {
			throw new Exception("Cannot clone a parent view instance because "
					+ "the current view (" + this.getDefinition().getName()
					+ ") doesn't have a parent..");
		}

		ViewInstance parentVi = new ViewInstance(vd);
		PrimitiveInstance parentPpi = parentVi.getPrimaryPrimitive();
		PrimitiveInstance thisPpi = this.getPrimaryPrimitive();

		Iterator objIt = parentPpi.readOrderedObjects().iterator();
		while (objIt.hasNext()) {
			ClassInstance thatObj = (ClassInstance) objIt.next();

			ClassInstance thisObj = (ClassInstance) thisPpi.getObjects().get(
					thatObj.getDefinition().getBaseName());

			Iterator atIt = thatObj.attributes.values().iterator();
			while (atIt.hasNext()) {
				AttributeInstance thatAi = (AttributeInstance) atIt.next();
				AttributeInstance thisAi = (AttributeInstance) thisObj.attributes
						.get(thatAi.getDefinition().getBaseName());

				thatAi.setValue(thisAi.getValue());
			}

		}

		return parentVi;
	}

	public ViewInstance deepCopy() {

		ViewInstance copyVi = new ViewInstance(this.getDefinition());

		try {

			copyVi.setVpdmfLabel(this.getVpdmfLabel());
			copyVi.setUIDString(this.getUIDString());
			copyVi.setVpdmfId(this.getVpdmfId());
			copyVi.setPiTotals(this.getPiTotals());
			// copyVi.thumbnail = this.thumbnail;
			copyVi.setAlias(this.getAlias());
			copyVi.setName(this.getName());

			PrimitiveInstanceGraph cPig = (PrimitiveInstanceGraph) copyVi
					.getSubGraph();

			if (this.getSubGraph() == null)
				return copyVi;

			Iterator nodesIt = this.getSubGraph().getNodes().values()
					.iterator();
			while (nodesIt.hasNext()) {
				PrimitiveInstance pi = (PrimitiveInstance) nodesIt.next();
				int index = pi.readIndex();
				if (!copyVi.getSubGraph().getNodes().containsKey(pi.getName())) {
					PrimitiveInstance cPi = new PrimitiveInstance(
							pi.getDefinition(), index);
					cPig.addPvInstance(cPi);
					cPi.fillInConditions();
				}
			}

			Iterator edgesIt = this.getSubGraph().getEdges().iterator();
			while (edgesIt.hasNext()) {
				PrimitiveLinkInstance pli = (PrimitiveLinkInstance) edgesIt
						.next();
				PrimitiveInstance fromPi = (PrimitiveInstance) pli
						.getOutEdgeNode();
				PrimitiveInstance toPi = (PrimitiveInstance) pli
						.getInEdgeNode();

				if (!cPig.checkForLinkInstanceExistence(fromPi.getName(),
						toPi.getName())) {
					cPig.addPvInstanceLink(fromPi.getName(), toPi.getName());
				}

			}

			Collection<SuperGraphNode> primitives = this.getSubGraph()
					.getNodes().values();
			Iterator pvIt = primitives.iterator();
			while (pvIt.hasNext()) {
				PrimitiveInstance pi = (PrimitiveInstance) pvIt.next();
				PrimitiveInstance cPi = (PrimitiveInstance) copyVi
						.getSubGraph().getNodes().get(pi.getName());

				Iterator objIt = pi.getObjects().values().iterator();
				while (objIt.hasNext()) {

					ClassInstance obj = (ClassInstance) objIt.next();
					ClassInstance cObj = (ClassInstance) cPi.getObjects().get(
							obj.getDefinition().getBaseName());

					Iterator attIt = obj.attributes.values().iterator();
					while (attIt.hasNext()) {
						AttributeInstance att = (AttributeInstance) attIt
								.next();
						AttributeInstance cAtt = (AttributeInstance) cObj.attributes
								.get(att.getDefinition().getBaseName());

						cAtt.setValue(att.getValue());

					}

				}

			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return copyVi;

	}

	public void nullify(PrimitiveDefinition pd) throws VPDMfException {

		Collection<SuperGraphNode> primitives = this.getSubGraph().getNodes()
				.values();
		Iterator<SuperGraphNode> pvIt = primitives.iterator();
		while (pvIt.hasNext()) {
			PrimitiveInstance pi = (PrimitiveInstance) pvIt.next();
			if (pi.getDefinition() != pd)
				continue;

			Iterator<ClassInstance> objIt = pi.getObjects().values().iterator();
			while (objIt.hasNext()) {

				ClassInstance obj = (ClassInstance) objIt.next();

				Iterator<AttributeInstance> attIt = obj.attributes.values()
						.iterator();
				while (attIt.hasNext()) {
					AttributeInstance att = (AttributeInstance) attIt.next();
					att.setValue(null);
				}
			}

			pi.fillInConditions();

		}

	}

	public void destroy() {

		if (this.getSubGraph() != null) {
			Vector primitives = (Vector) this.getSubGraph().getNodes().values();
			Iterator pvIt = primitives.iterator();
			while (pvIt.hasNext()) {
				PrimitiveInstance pi = (PrimitiveInstance) pvIt.next();
				Iterator objIt = pi.getObjects().values().iterator();
				while (objIt.hasNext()) {
					ClassInstance obj = (ClassInstance) objIt.next();
					Vector attributes = new Vector(obj.attributes.values());
					Iterator attIt = attributes.iterator();
					while (attIt.hasNext()) {
						AttributeInstance att = (AttributeInstance) attIt
								.next();
						att.destroy();
					}
					obj.destroy();
				}
				pi.destroy();
			}

			Iterator lnkIt = this.getSubGraph().getEdges().iterator();
			while (lnkIt.hasNext()) {
				PrimitiveLinkInstance lnk = (PrimitiveLinkInstance) lnkIt
						.next();
				lnk.destroy();
			}

			this.getSubGraph().destroy();
		}

		this.setPrimaryPrimitive(null);
		this.setDefinition(null);

		//
		// Added by Weicheng.
		//
		// ===================================
		this.setLinksToFillIn(null);
		// this.thumbnail = null;
		this.setVpdmfLabel(null);
		this.setUIDString(null);
		this.setDefName(null);
		// ===================================

		super.destroy();

	}

	public String dumpToXML() throws Exception {
		String xml = "<" + this.getDefinition().getName() + ">\n";

		String temp = ((PrimitiveInstanceGraph) this.getSubGraph()).dumpToXML();

		Pattern patt = Pattern.compile("\n");
		Matcher matcher = patt.matcher(temp);
		temp = matcher.replaceAll("\n  ");

		xml += temp;
		xml += "</" + this.getDefinition().getName() + ">\n";
		return xml;

	}

	/**
	 * Reading the attribute definition can occur in one of two ways:
	 * 
	 * 1) via a Primitive, i.e., ']pvName|className.attributeName'
	 * 
	 * 2) via the model, i.e., '|classPath.attributeName'
	 * 
	 */
	public UMLattribute readAttributeDefinition(String attributeAddress)
			throws Exception {

		PrimitiveInstance pvIns;
		ClassInstance classIns;
		UMLclass classDef;
		UMLattribute attr = null;

		//
		// Check the format of the attributeAddress
		//
		int dotPos = attributeAddress.lastIndexOf('.');
		int barPos = attributeAddress.lastIndexOf('|');
		int brackPos = attributeAddress.lastIndexOf(']');

		if (dotPos == -1 || barPos == -1 || brackPos == -1) {
			throw new Exception(attributeAddress + " is badly formed\n");
		}

		if (brackPos == -1) {

			String attributeName = attributeAddress.substring(dotPos + 1);
			String classPath = attributeAddress.substring(barPos + 1, dotPos);

			UMLmodel model = this.getDefinition().getTop().getUmlModel();

			//
			// ii) Class
			//
			Map m = model.listClasses();
			// TODO: CHECK THIS!
			if (m.containsKey("|." + classPath)) {
				classDef = (UMLclass) m.get("|." + classPath);
			} else {
				throw new Exception("Can't find " + attributeAddress
						+ ", no such class\n");
			}

			//
			// iii) Attribute
			UMLattribute a = null;
			Iterator<UMLattribute> l = classDef.getAttributes().iterator();
			while (l.hasNext()) {
				a = l.next();
				if (a.getBaseName().equals(attributeName)) {
					attr = a;
				}
			}

			if (a == null) {
				throw new Exception("Can't find " + attributeAddress
						+ ", no such attribute\n");
			}

		} else {

			String attributeName = attributeAddress.substring(dotPos + 1);
			String className = attributeAddress.substring(barPos + 1, dotPos);
			String primitiveName = attributeAddress.substring(brackPos + 1,
					barPos);

			//
			// Check for the existence of the attributeAddress
			// i) Primitive Definition
			String pvIndex = primitiveName + "_0";
			if (this.getSubGraph().getNodes().containsKey(primitiveName + "_0")) {
				pvIns = (PrimitiveInstance) this.getSubGraph().getNodes()
						.get(pvIndex);
			} else {
				throw new Exception("Can't find " + attributeAddress
						+ ", no such primitive at index 0\n");
			}

			//
			// ii) Class
			if (pvIns.getObjects().containsKey(className)) {
				classIns = (ClassInstance) pvIns.getObjects().get(className);
				classDef = classIns.getDefinition();
			} else {
				throw new Exception("Can't find " + attributeAddress
						+ ", no such class\n");
			}

			//
			// iii) Attribute
			UMLattribute a = null;
			Iterator<UMLattribute> l = classDef.getAttributes().iterator();
			while (l.hasNext()) {
				a = l.next();
				if (a.getBaseName().equals(attributeName)) {
					attr = a;
				}
			}

			if (a == null) {
				throw new Exception("Can't find " + attributeAddress
						+ ", no such attribute\n");
			}

		}

		return attr;

	}

	public AttributeInstance readAttributeInstance(String attributeAddress,
			int index) throws AttributeAddressException {
		PrimitiveInstance pvIns;
		ClassInstance classIns;
		AttributeInstance attrIns;

		//
		// Check the format of the attributeAddress
		//
		if (attributeAddress == null)
			return null;

		int dotPos = attributeAddress.lastIndexOf('.');
		int barPos = attributeAddress.lastIndexOf('|');
		int brackPos = attributeAddress.lastIndexOf(']');

		if (dotPos == -1 || barPos == -1 || brackPos == -1) {
			throw new AttributeAddressException(attributeAddress
					+ " is badly formed\n");
		}

		String attributeName = attributeAddress.substring(dotPos + 1);
		String className = attributeAddress.substring(barPos + 1, dotPos);
		String primitiveName = attributeAddress.substring(brackPos + 1, barPos);

		return this.readAttributeInstance(primitiveName, className,
				attributeName, index);

	}

	public AttributeInstance readAttributeInstance(String primitiveName,
			String className, String attributeName, int index)
			throws AttributeAddressException {

		PrimitiveInstance pvIns;
		ClassInstance classIns;
		AttributeInstance attrIns;

		//
		// Check for the existence of the attributeAddress
		// i) Primitive Definition
		String pvIndex = primitiveName + "_" + index;
		if (this.getSubGraph().getNodes()
				.containsKey(primitiveName + "_" + index)) {
			pvIns = (PrimitiveInstance) this.getSubGraph().getNodes()
					.get(pvIndex);
		} else {
			throw new AttributeAddressException("Can't find ]" + primitiveName
					+ "|" + className + "." + attributeName
					+ ", no such primitive at index " + index + "\n");
		}

		//
		// ii) Class
		if (pvIns.getObjects().containsKey(className)) {
			classIns = (ClassInstance) pvIns.getObjects().get(className);
		} else {
			throw new AttributeAddressException("Can't find ]" + primitiveName
					+ "|" + className + "." + attributeName
					+ ", no such class\n");
		}

		//
		// iii) Attribute
		if (classIns.attributes.containsKey(attributeName)) {
			attrIns = (AttributeInstance) classIns.attributes
					.get(attributeName);
		} else {
			throw new AttributeAddressException("Can't find ]" + primitiveName
					+ "|" + className + "." + attributeName
					+ ", no such attribute\n");
		}

		return attrIns;

	}

	public List<String> readAttributeAddresses() {
		Vector addresses = new Vector();
		Object[] primitives = this.getSubGraph().getNodes().values().toArray();

		for (int i = 0; i < primitives.length; i++) {
			PrimitiveInstance pi = (PrimitiveInstance) primitives[i];
			Object[] classes = pi.getObjects().values().toArray();
			for (int j = 0; j < classes.length; j++) {
				ClassInstance ci = (ClassInstance) classes[j];
				Object[] attributes = ci.attributes.values().toArray();
				for (int k = 0; k < attributes.length; k++) {
					AttributeInstance ai = (AttributeInstance) attributes[k];
					addresses.add("]" + pi.getDefinition().getName() + "|"
							+ ci.getDefinition().getBaseName() + "."
							+ ai.getDefinition().getBaseName());
				}
			}
		}

		return addresses;

	}

	public Vector readAttributeAddresses_cardinality1() {

		Vector addresses = new Vector();

		Vector pVec = new Vector();
		pVec.add(this.getPrimaryPrimitive());
		pVec.addAll(this.getPrimaryPrimitive()
				.readCardinalityOneConnectedPrimitives());

		Iterator pIt = pVec.iterator();
		while (pIt.hasNext()) {
			PrimitiveInstance p = (PrimitiveInstance) pIt.next();
			addresses.addAll(p.readAttributeAddresses());
		}

		return addresses;

	}

	public Vector readAttributes() {
		Vector attrs = new Vector();
		Object[] primitives = this.getSubGraph().getNodes().values().toArray();

		for (int i = 0; i < primitives.length; i++) {
			PrimitiveInstance pi = (PrimitiveInstance) primitives[i];
			Object[] classes = pi.getObjects().values().toArray();
			for (int j = 0; j < classes.length; j++) {
				ClassInstance ci = (ClassInstance) classes[j];
				Object[] attributes = ci.attributes.values().toArray();
				for (int k = 0; k < attributes.length; k++) {
					attrs.add((AttributeInstance) attributes[k]);
				}
			}
		}
		return attrs;

	}

	public String readQueryString() {
		String qs = "/viewType=" + this.getDefinition().getName() + "/";

		PrimitiveInstanceGraph pig = (PrimitiveInstanceGraph) this
				.getSubGraph();
		SuperGraphTraversal pigTraversal = pig.readTraversal();

		ArrayList<SuperGraphNode> piVec = pigTraversal.nodeTraversal;
		Iterator piIt = piVec.iterator();
		while (piIt.hasNext()) {
			PrimitiveInstance pi = (PrimitiveInstance) piIt.next();

			Iterator attIt = pi.readAttributes().iterator();
			while (attIt.hasNext()) {
				AttributeInstance ai = (AttributeInstance) attIt.next();

				if (pi.getDefinition().hasConditionOn(ai.getAddress())
						|| ai.getValue() == null)
					continue;

				if (!qs.endsWith("/"))
					qs += "&";

				try {
					qs += ai.readDebugString();
				} catch (Exception e) {
					e.printStackTrace();
				}

			}

		}
		return qs;

	}

	public String readDebugString() throws Exception {
		String debug = this.getDefinition().getName() + ":"
				+ this.getVpdmfLabel() + " \n";

		PrimitiveInstanceGraph pig = (PrimitiveInstanceGraph) this
				.getSubGraph();
		SuperGraphTraversal pigTraversal = pig.readTraversal();

		ArrayList<SuperGraphNode> piVec = pigTraversal.nodeTraversal;
		Iterator piIt = piVec.iterator();
		while (piIt.hasNext()) {
			PrimitiveInstance pi = (PrimitiveInstance) piIt.next();

			Iterator attIt = pi.readAttributes().iterator();
			while (attIt.hasNext()) {
				AttributeInstance ai = (AttributeInstance) attIt.next();
				debug += ai.readDebugString() + "\n";
			}

			Iterator pliIt = pi.getIncomingEdges().values().iterator();
			while (pliIt.hasNext()) {
				PrimitiveLinkInstance pli = (PrimitiveLinkInstance) pliIt
						.next();
				if (pli.getLinkClass() != null) {
					attIt = pli.getLinkClass().attributes.values().iterator();
					while (attIt.hasNext()) {
						AttributeInstance ai = (AttributeInstance) attIt.next();
						debug += ai.readDebugString() + "\n";
					}
				}
			}
		}

		return debug;

	}

	private String buildComplexLabel(String currentLabel) throws Exception {
		//
		// LABEL FUNCTIONS:
		//
		// - CHECKSUM
		//
		Pattern p = Pattern.compile("checksum\\[(.*?)\\]");
		Matcher m = p.matcher(currentLabel);
		if (m.find()) {
			String checksumLabel = m.group(1);
			checksumLabel = this.buildSimpleIndex(checksumLabel);

			if (checksumLabel == null) {
				throw new Exception("Can't build vpdmfLabel");
			}

			checksumLabel = this.runChecksumOnIndex(checksumLabel);
			currentLabel = m.replaceAll(checksumLabel);
		}

		if (currentLabel == null) {
			throw new Exception("Can't build vpdmfLabel");
		}

		//
		// vpdmfLabel FUNCTIONS:
		//
		// - TEXT REPLACEMENT
		//
		p = Pattern.compile("REGEX\\[(.*?)\\,\\/(.*?)\\/\\]");
		m = p.matcher(currentLabel);
		if (m.find()) {
			String regexLabel = m.group(1);
			regexLabel = this.buildSimpleIndex(regexLabel);

			String regex = m.group(2);
			// String regex = "^(.*?\\), \\`\\S+\\s+\\S+)";
			try {
				Pattern pp = Pattern.compile(regex);
				Matcher mm = pp.matcher(regexLabel);
				String repl = "";
				if (mm.find()) {
					repl = mm.group(1);
				}
				currentLabel = m.replaceAll(repl);

			} catch (Exception ee) {

				throw new Exception("error in regex vpdmfLabel: "
						+ ee.getMessage());

			}
			if (regexLabel == null) {
				throw new Exception("Can't build vpdmfLabel");
			}

		}

		//
		// vpdmfLabel FUNCTIONS:
		//
		// - TEXT TRUNCATION
		// TRUNCATE[$1$,10]
		// (The space will be taken out before running the truncate function)
		//
		p = Pattern.compile("TRUNCATE\\[(.*?)\\,(.*?)]");
		m = p.matcher(currentLabel);
		if (m.find()) {
			String truncLabel = m.group(1);
			truncLabel = this.buildSimpleIndex(truncLabel);
			truncLabel = truncLabel.replaceAll(" ", "");

			int length = (new Integer(m.group(2))).intValue();

			if (length > truncLabel.length()) {
				length = truncLabel.length();
			}

			truncLabel = truncLabel.substring(0, length);
			currentLabel = m.replaceAll(truncLabel);
		}

		//
		// vpdmfLabel FUNCTIONS:
		//
		// - PAD TO RIGHT
		// PAD[$1$,10]
		// (The space will be taken out before running the truncate function)
		//
		/*
		 * p = Pattern.compile("PAD\\[(.*?)\\,(.*?)]"); m =
		 * p.matcher(currentIndexString);
		 * 
		 * if (m.find()) {
		 * 
		 * String spaces =
		 * "                                                       "; String
		 * truncIndexstring = m.group(1); truncIndexstring =
		 * this.buildIndexString(truncIndexstring); int actualLength =
		 * truncIndexstring.length(); int desiredLength = (new
		 * Integer(m.group(2))).intValue();
		 * 
		 * if( desiredLength > actualLength ) { truncIndexstring =
		 * spaces.substring(0, desiredLength - actualLength) + truncIndexstring;
		 * }
		 * 
		 * currentIndexString = m.replaceAll(truncIndexstring);
		 * 
		 * }
		 */

		//
		// INDEXSTRING FUNCTIONS:
		//
		// - USE DEFAULT VALUE IF NULL
		// DEFAULT_IF_NULL[$1$,String]
		//
		p = Pattern.compile("DEFAULT_IF_NULL\\[(.*?)\\,(.*?)]");
		m = p.matcher(currentLabel);
		if (m.find()) {
			String testIdx = m.group(1);
			testIdx = this.buildSimpleIndex(testIdx);
			testIdx = testIdx.replaceAll(" ", "");

			if (testIdx.equals("-")) {
				testIdx = m.group(2);
				;
			}

			currentLabel = m.replaceAll(testIdx);

		}

		if (currentLabel == null) {
			throw new Exception("Can't build label");
		}

		currentLabel = this.buildSimpleIndex(currentLabel);
		return currentLabel;
	}

	/**
	 * Generates and index given a specific indexFormat string performing
	 * appropriate substitutions from a view's contents into the string.
	 * 
	 * Note (7/24/2004, Gully)
	 * 
	 * We permit individual indexElements to be defined as 'Nullable'. This
	 * means that if their value is null, then we set the substituted string as
	 * '-' rather than returning an error or an overall null value for the
	 * returned String as we would previously have done.
	 * 
	 * @param currentIndex
	 *            String
	 * @throws VpdmfLabelException
	 * @return String
	 */
	private String buildSimpleIndex(String currentIndex) throws Exception {
		int i, j, k;

		//
		// Indicate if running checksum on vpdmfLabel
		//
		boolean runChecksum = false;

		if (currentIndex == null) {
			throw new Exception("Index is invalid, passed to function as null");
		}

		Pattern simplePatt = Pattern.compile("\\$([\\d\\s\\,]+)\\$");
		Pattern arrayPatt = Pattern.compile("\\@([\\d\\s\\,]+)\\@");
		Pattern nPatt = Pattern.compile("\\b(\\d+)\\b");

		Matcher simpleMatcher = simplePatt.matcher(currentIndex);

		StringBuffer sb = new StringBuffer();

		boolean topLoopFlag = simpleMatcher.find();
		TOPLOOP: while (topLoopFlag) {

			AttributeInstance ai = null;
			String indexItemString = null;

			StringBuffer nSB = new StringBuffer();
			String numbers = simpleMatcher.group(1);
			Matcher nMatcher = nPatt.matcher(numbers);

			boolean fieldsFlag = nMatcher.find();
			FIELDSLOOP: while (fieldsFlag) {
				String ii = nMatcher.group(1);

				IndexElement ie = this.getDefinition().getIndexElements()
						.get(new Integer(ii));

				ai = this.readAttributeInstance(ie.getAttributeAddress(), 0);

				indexItemString = ai.readValueString();

				if (indexItemString == null || indexItemString.length() == 0) {

					if (ie.getNullable()) {

						indexItemString = "-";

					} else {

						throw new Exception("Can't build index, field ]"
								+ ai.get_object().getPrimitive().getDefName()
								+ "|"
								+ ai.get_object().getDefinition().getBaseName()
								+ "." + ai.getDefinition().getBaseName()
								+ " is null");

					}

				}

				indexItemString = indexItemString.replaceAll("\\$", "&#36;");

				try {
					nMatcher.appendReplacement(nSB, indexItemString);
					fieldsFlag = nMatcher.find();
				} catch (IndexOutOfBoundsException e) {
					// Ignore this, due to the presence of '$' signs in the
					// indexString
					fieldsFlag = false;
				} catch (Exception e2) {
					throw e2;
				}

			}

			simpleMatcher.appendReplacement(sb, nSB.toString());

			topLoopFlag = simpleMatcher.find();

		}

		simpleMatcher.appendTail(sb);
		currentIndex = sb.toString();

		sb = new StringBuffer();
		Matcher arrayMatcher = arrayPatt.matcher(currentIndex);
		topLoopFlag = arrayMatcher.find();
		TOPLOOP: while (topLoopFlag) {

			AttributeInstance ai = null;
			String indexItemString = null;
			String data = null;

			int cardinality = 0;
			String numbers = arrayMatcher.group(1);
			Matcher nMatcher = nPatt.matcher(numbers);
			boolean fieldsFlag = nMatcher.find();
			FIELDSLOOP: while (fieldsFlag) {
				String ii = nMatcher.group(1);
				PrimitiveInstance pi = null;

				IndexElement ie = (IndexElement) this.getDefinition()
						.getIndexElements().get(new Integer(ii));

				pi = this.readPrimitiveInstance(ie.getAttributeAddress(), 0);
				cardinality = this.countPrimitives(pi.getDefinition());
				break FIELDSLOOP;
			}

			for (j = 0; j < cardinality; j++) {

				StringBuffer nSB = new StringBuffer();

				numbers = arrayMatcher.group(1);
				nMatcher = nPatt.matcher(numbers);

				fieldsFlag = nMatcher.find();
				FIELDSLOOP: while (fieldsFlag) {
					String ii = nMatcher.group(1);

					IndexElement ie = (IndexElement) this.getDefinition()
							.getIndexElements().get(new Integer(ii));

					ai = this
							.readAttributeInstance(ie.getAttributeAddress(), j);

					data = ai.readValueString();

					if (data == null || data.length() == 0) {
						if (ie.getNullable()) {
							data = "-";
						} else {
							return null;
						}
					}

					nMatcher.appendReplacement(nSB, data);
					fieldsFlag = nMatcher.find();

				}

				if (j == 0) {
					indexItemString = nSB.toString();
				} else {
					indexItemString += ", " + nSB.toString();
				}

			}

			arrayMatcher.appendReplacement(sb, indexItemString);
			topLoopFlag = arrayMatcher.find();

		}

		arrayMatcher.appendTail(sb);

		currentIndex = sb.toString();

		return currentIndex;

	}

	public String readLabel() {
		return this.readFormattedHumanIndex(true);
	}

	public PrimitiveDefinition readPrimitiveDefinition(String attributeAddress)
			throws Exception {
		PrimitiveDefinition pd = null;

		//
		// Check the format of the attributeAddress
		//
		int barPos = attributeAddress.lastIndexOf('|');
		int brackPos = attributeAddress.lastIndexOf(']');

		if (brackPos == -1) {
			throw new Exception(attributeAddress + " is badly formed\n");
		}

		String primitiveName = null;
		if (barPos == -1) {
			primitiveName = attributeAddress.substring(brackPos + 1);
		} else {
			primitiveName = attributeAddress.substring(brackPos + 1, barPos);
		}

		pd = (PrimitiveDefinition) this.getDefinition().getSubGraph()
				.getNodes().get(primitiveName);

		if (pd == null) {
			throw new Exception("Primitive " + primitiveName + " not found");
		}

		return pd;

	}

	public PrimitiveInstance readPrimitiveInstance(String attributeAddress,
			int index) throws Exception {
		PrimitiveInstance pvIns;

		//
		// Check the format of the attributeAddress
		//
		int barPos = attributeAddress.lastIndexOf('|');
		int brackPos = attributeAddress.lastIndexOf(']');

		if (brackPos == -1) {
			throw new Exception(attributeAddress + " is badly formed\n");
		}

		String primitiveName = null;
		if (barPos == -1) {
			primitiveName = attributeAddress.substring(brackPos + 1);
		} else {
			primitiveName = attributeAddress.substring(brackPos + 1, barPos);
		}

		//
		// Check for the existence of the attributeAddress
		// i) Primitive Definition
		String pvIndex = primitiveName + "_" + index;
		if (this.getSubGraph().getNodes()
				.containsKey(primitiveName + "_" + index)) {
			pvIns = (PrimitiveInstance) this.getSubGraph().getNodes()
					.get(pvIndex);
		} else {
			throw new Exception("Can't find " + attributeAddress
					+ ", no such primitive at index" + index + "\n");
		}

		return pvIns;

	}

	/*
	 * public Vector getPrimitiveInstancesFromSource(PrimitiveInstance sourcePI,
	 * PrimitiveDefinition targetPD) { Vector edges = null; try { edges =
	 * sourcePI.getOutEdges().getFilteredElements(targetPD.getName()); } catch
	 * (Exception ex) { ex.printStackTrace(); } return edges;
	 * 
	 * }
	 */

	public String readFormattedHumanIndex(boolean tidyFlag) {
		String idx = this.getVpdmfLabel();

		if (idx != null)
			idx = runSubsOnIndex(idx, tidyFlag);

		if (idx.indexOf(" ... to ... ") != -1) {

			String[] s = idx.split(" \\.\\.\\. to \\.\\.\\. ");
			int l = Math.min(s[0].length(), s[1].length());
			int d = 0;

			for (int i = 0; i < l; i++) {
				if (!s[0].substring(0, i).equals(s[1].substring(0, i))) {
					d = i;
					break;
				}
			}
			idx = "\'" + s[0].substring(0, d + 3) + "...\' - \'"
					+ s[1].substring(0, d + 3) + "...\'";
		}

		return idx;

	}

	public static String[][] getTidyGroups(String input) {

		input = input.replaceAll("hide\\[(.*?)\\]", "");

		String tidyRegex = "tidy\\[(.*?)\\]";
		String toRegex = " \\.\\.\\.\\ to \\.\\.\\. ";

		Pattern pTo = Pattern.compile(toRegex);
		Matcher mTo = pTo.matcher(input);

		Pattern pTidy = Pattern.compile(tidyRegex);
		Matcher mTidy = pTidy.matcher(input);
		String repl = "";
		ArrayList list = new ArrayList();
		while (mTidy.find()) {
			repl = mTidy.group(1);
			input = input.replaceFirst(tidyRegex, repl);
			mTidy = pTidy.matcher(input);
			list.add(repl);
		}

		String[][] output;
		if (mTo.find()) {
			int nLevels = list.size() / 2;
			output = new String[2][nLevels];
			for (int i = 0; i < 2; i++) {
				for (int j = 0; j < nLevels; j++) {
					output[i][j] = (String) list.get(i * nLevels + j);
				}
			}
		} else {
			int nLevels = list.size();
			output = new String[1][nLevels];
			for (int j = 0; j < list.size(); j++) {
				output[0][j] = (String) list.get(j);
			}
		}

		return output;

	}

	public static String runSubsOnIndex(String input, boolean tidyFlag) {

		String output = input.replaceAll("hide\\[(.*?)\\]", "");

		String tidyRegex = "tidy\\[(.*?)\\]";

		if (tidyFlag) {

			output = output.replaceAll(tidyRegex, "");

		} else {

			Pattern pp = Pattern.compile(tidyRegex);
			Matcher mm = pp.matcher(output);
			String repl = "";
			while (mm.find()) {
				repl = mm.group(1);
				output = output.replaceFirst(tidyRegex, repl);
				mm = pp.matcher(output);
			}

		}

		return output;

	}

	public ArrayList readGroupsFromIndex() {

		String tidyRegex = "tidy\\[(.*?)\\]";

		Pattern pp = Pattern.compile(tidyRegex);
		String idx = this.getVpdmfLabel();
		Matcher mm = pp.matcher(idx);
		String repl = "";
		ArrayList groups = new ArrayList();
		while (mm.find()) {
			repl = mm.group(1);
			idx = idx.replaceFirst(tidyRegex, repl);
			groups.add(repl);
			mm = pp.matcher(idx);
		}
		return groups;

	}

	public PrimitiveInstance getPrimaryPrimitive() {
		return this.primaryPrimitive;
	}

	/*
	 * public ImageIcon get_thumbnail() { return this.thumbnail; }
	 */

	public boolean getVisible() {
		return this.isVisible();
	}

	public ViewInstance makeLightViewInstance() {
		ViewInstance lvi = new ViewInstance(this.getDefName());
		// lvi.thumbnail = this.thumbnail;
		lvi.setVpdmfLabel(this.getVpdmfLabel());
		lvi.setUIDString(this.getUIDString());
		lvi.setVpdmfId(this.getVpdmfId());
		lvi.setDefinition(this.getDefinition());

		return lvi;

	}

	private String runChecksumOnIndex(String currentVpdmfLabel) {
		//
		// Get the checksum value for indexString
		//
		long vpdmfLabelChecksum = 0;
		try {
			//
			// Convert indexString into byteArrayInputStream
			//
			ByteArrayInputStream bis = new ByteArrayInputStream(
					currentVpdmfLabel.getBytes());

			CheckedInputStream checkStream = new CheckedInputStream(bis,
					new Adler32());

			byte[] tempBuf = new byte[128];
			while (checkStream.read(tempBuf) >= 0) {
			}

			vpdmfLabelChecksum = checkStream.getChecksum().getValue();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return String.valueOf(vpdmfLabelChecksum);

	}

	public ClassInstance readPrimaryClass() {

		PrimitiveInstance pi = this.getPrimaryPrimitive();

		ClassInstance ci = (ClassInstance) pi.getObjects().get(
				pi.getDefinition().getPrimaryClass().getBaseName());

		return ci;

	}

	public Long readVpdmfIdFromData() {

		ClassInstance ci = this.readPrimaryClass();

		Iterator<UMLattribute> pkdIt = ci.getDefinition().getPkArray()
				.iterator();
		UMLattribute pkd = (UMLattribute) pkdIt.next();
		AttributeInstance pki = (AttributeInstance) ci.attributes.get(pkd
				.getBaseName());

		Long vpdmfId = (Long) pki.getValue();

		return vpdmfId;

	}

	public boolean isUIDSet() throws Exception {

		PrimitiveDefinition pPd = this.getPrimaryPrimitive().getDefinition();
		UMLclass pCd = pPd.getPrimaryClass();
		boolean isSet = true;

		Iterator pkIt = pCd.getPkArray().iterator();
		while (pkIt.hasNext()) {
			UMLattribute pk = (UMLattribute) pkIt.next();

			String pkAddr = "]" + pPd.getName() + "|" + pCd.getBaseName() + "."
					+ pk.getBaseName();
			AttributeInstance ai = this.readAttributeInstance(pkAddr, 0);
			if (ai.getValue() instanceof ObjectMatrix1D) {
				ObjectMatrix1D om = (ObjectMatrix1D) ai.getValue();
				if (om.get(0) == null) {
					return false;
				}
			} else if (ai.readValueString() == null
					|| ai.readValueString().length() == 0) {
				return false;
			}
		}
		return true;

	}

	public void writeVpdmfId(Long vpdmfId) {

		this.setVpdmfId(vpdmfId);

		PrimitiveInstance pPv = this.getPrimaryPrimitive();
		UMLclass pCd = pPv.getDefinition().getPrimaryClass();
		ClassInstance pCI = (ClassInstance) pPv.getObjects().get(
				pCd.getBaseName());
		AttributeInstance ai = pCI.getAttributes().get("vpdmfId");
		ai.setValue(vpdmfId);

	}

	public void setKmID(int kmID) {
		this.kmID = kmID;
	}

	public void computeCompletionOfNeighbors(boolean complete) {

		Iterator it = this.getIncomingEdges().values().iterator();
		while (it.hasNext()) {
			ViewLinkInstance vli = (ViewLinkInstance) it.next();
			ViewInstance vi = (ViewInstance) vli.getOutEdgeNode();
			vi.setComplete(complete);
		}

		it = this.getOutgoingEdges().values().iterator();
		while (it.hasNext()) {
			ViewLinkInstance vli = (ViewLinkInstance) it.next();
			ViewInstance vi = (ViewInstance) vli.getInEdgeNode();
			vi.setComplete(complete);
		}

	}

	public Vector spawnLightViewInstance(ViewDefinition thatVd)
			throws Exception {
		Vector lightViewInsVec = new Vector();

		PrimitiveInstanceGraph thisPig = (PrimitiveInstanceGraph) this
				.getSubGraph();

		// PrimitiveDefinition thatPrimaryPd = thatVd.get_primaryPrimitive();
		// int thatPrimaryPvCount = this.countPrimitives(thatPrimaryPd);

		//
		// Bugfix:
		//
		// Modified by Weicheng.
		//
		// ViewLink error?
		// (The commented section above is the original code)
		//
		// It is not always the case to use the name of the primary
		// primitive of 'thatVd' to count the PI(s) in the current
		// view instance??
		//
		// Check the bug case: BibliographicFragment -> Publication
		// The name of the primary primitive in Publication is 'Resource'.
		// We cannot find a primitive named 'Resource' in the
		// BibliographicFragment
		// view??
		//
		// We don't want the naming constraint when defining the view spec.
		//
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		boolean attLinkFromThis = false;
		ViewLink vl = null;

		Map<ViewDefinition, ViewLink> edges = null;

		try {
			edges = this.getDefinition().readInputLinkedViewDefinitions(true);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		if (edges.containsKey(thatVd)) {
			vl = (ViewLink) edges.get(thatVd);
		}

		try {
			edges = this.getDefinition().readOutputLinkedViewDefinitions(true);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		if (edges.containsKey(thatVd)) {
			vl = (ViewLink) edges.get(thatVd);
			attLinkFromThis = true;
		}

		String thatPdAttAddr = null;
		/*
		 * String attLink = vl.getAttributeLinkage();
		 * 
		 * // If attribute linkage contains multiple entries, use the first one.
		 * int index = attLink.indexOf(","); if (index != -1) { attLink =
		 * attLink.substring(0, index); }
		 * 
		 * String[] attArray = attLink.split("=");
		 * 
		 * if (attLinkFromThis) { thatPdAttAddr = attArray[0]; } else {
		 * thatPdAttAddr = attArray[1]; }
		 * 
		 * // If the 'thatPd' is not available in the current view instance, //
		 * then cannot spawn to the target view and return null. try {
		 * this.readPrimitiveDefinition(thatPdAttAddr); } catch (Exception ex) {
		 * return null; }
		 * 
		 * int thatPrimaryPvCount = this.countPrimitives(thatPdAttAddr);
		 * 
		 * // End Bugfix //
		 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		 * 
		 * for (int i = 0; i < thatPrimaryPvCount; i++) {
		 * 
		 * PrimitiveInstance thisPi = null;
		 * 
		 * // // Bugfix: // // According to the commented code fragment above,
		 * we are trying // to identify the 'thisPi' in order to put the data
		 * from // 'thisPi' into the primary primitive instance of the 'thatVi'.
		 * // // This method is not working when trying to spawn the view
		 * instance // from a 'BookChapter' to a 'Book' view. // Iterator nIt =
		 * thisPig.getNodes().values().iterator(); while (nIt.hasNext()) {
		 * PrimitiveInstance pi = (PrimitiveInstance) nIt.next(); ViewDefinition
		 * lookupVd = (ViewDefinition) pi.getDefinition() .getLookupView();
		 * 
		 * if (pi.readIndex() != i) { continue; }
		 * 
		 * String vName = thatVd.getName(); if (lookupVd == null) {
		 * 
		 * String pName = pi.getDefinition().getName(); if
		 * (!pName.equals(vName)) { continue; }
		 * 
		 * } else {
		 * 
		 * String lvName = lookupVd.getName(); if (!lvName.equals(vName + "LU")
		 * && !lvName.equals(vName)) { continue; }
		 * 
		 * }
		 * 
		 * thisPi = pi; break; }
		 * 
		 * if (thisPi == null) { return null; }
		 * 
		 * ViewInstance thatVi = new ViewInstance(thatVd);
		 * thatVi.getPrimaryPrimitive().suckInData(thisPi);
		 * 
		 * // // 'LookupSpec' view instance will not have machine index and //
		 * thumbnail. // Therefore, only update its humanIndex. // try {
		 * thatVi.updateUIDString();
		 * thatVi.updateVpdmfLabelsFromPrimaryPrimitive();
		 * thatVi.setName(thatVi.getUIDString()); lightViewInsVec.add(thatVi); }
		 * catch (Exception ex) { ex.printStackTrace(); } }
		 */

		return lightViewInsVec;

	}

	public Vector spawnLookupLightViewInstance(ViewDefinition thatVd)
			throws Exception {

		Vector lightViewInsVec = new Vector();

		PrimitiveInstanceGraph thisPig = (PrimitiveInstanceGraph) this
				.getSubGraph();

		PrimitiveDefinition thatPrimaryPd = thatVd.getPrimaryPrimitive();
		int thatPrimaryPvCount = this.countPrimitives(thatPrimaryPd);
		for (int i = 0; i < thatPrimaryPvCount; i++) {
			PrimitiveInstance thisPi = (PrimitiveInstance) thisPig.getNodes()
					.get(thatPrimaryPd.getName() + "_" + i);
			ViewInstance thatVi = new ViewInstance(thatVd);
			thatVi.getPrimaryPrimitive().suckInData(thisPi);

			//
			// 'LookupSpec' view instance will not have machine index and
			// thumbnail.
			// Therefore, only update its hamanIndex.
			//
			try {
				thatVi.updateUIDString();
				thatVi.updateLabel();
				thatVi.updateIndexTuple();
				thatVi.setName(thatVi.getUIDString());
				lightViewInsVec.add(thatVi);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		return lightViewInsVec;

	}

	public ViewInstance spawnViewInstance(ViewDefinition thatVd)
			throws Exception {
		return this.spawnViewInstance(thatVd, 0, 0);
	}

	public ViewInstance spawnViewInstance(ViewDefinition thatVd, int thisIndex,
			int thatIndex) throws Exception {

		ViewInstance thatVi = null;
		boolean changesMade = false;

		ViewDefinition thisVd = this.getDefinition();
		PrimitiveInstanceGraph thisPig = (PrimitiveInstanceGraph) this
				.getSubGraph();

		thatVi = new ViewInstance(thatVd);
		PrimitiveInstanceGraph thatPig = (PrimitiveInstanceGraph) thatVi
				.getSubGraph();

		Map<ViewDefinition, ViewLink> linkHt = thisVd
				.readAllLinkedViewDefinitions(true);
		Map<ViewDefinition, ViewLink> rlnHt = thisVd
				.readAllRelatedViewDefinitions(true);

		ViewLink vl = (ViewLink) linkHt.get((Object) thatVd);
		ViewLink rln = (ViewLink) rlnHt.get((Object) thatVd);

		if (vl != null) {
			thatVi = this.spawnViewInstanceFromLink(vl, thatVd, thisIndex,
					thatIndex);
		}
		//
		// If we can't find a link, then maybe it's a relation.
		//
		else if (rln != null) {
			thatVi = this.spawnViewInstanceFromLink(rln, thatVd, thisIndex,
					thatIndex);
		} else {

			thatVi = null;

		}

		return thatVi;

	}

	private ViewInstance spawnViewInstanceFromLink(ViewLink vl,
			ViewDefinition thatVd, int thisIndex, int thatIndex)
			throws Exception {
		ViewInstance thatVi = null;
		boolean changesMade = false;

		ViewDefinition thisVd = this.getDefinition();

		PrimitiveInstanceGraph thisPig = (PrimitiveInstanceGraph) this
				.getSubGraph();

		thatVi = new ViewInstance(thatVd);
		PrimitiveInstanceGraph thatPig = (PrimitiveInstanceGraph) thatVi
				.getSubGraph();

		boolean thisIn = true;

		/*
		 * String sr = vl.getSetRelation(); String attLink =
		 * vl.getAttributeLinkage();
		 * 
		 * if (attLink == null) { return null; }
		 * 
		 * // // Bugfix: // // Added by Weicheng. // (NEED TO EXPLAIN THIS TO
		 * GULLY TO MAKE SURE IT'S OKAY?) // // How to use the attribute linkage
		 * string?? // // BUG CASE: // // When trying to insert a bibliographic
		 * fragment from an // article, the system will use this method to spawn
		 * a // bibliographic fragment view instance from the selected //
		 * article view instance. // From the current view design, the 'attLink'
		 * variable we // will get is
		 * ']PublicationLU|Resource=]Resource|Resource'. // //
		 * ']PublicationLU|Resource' => LeftPart // ']Resource|Resource' =>
		 * RightPart // // Note that, without the following bugfix, the system
		 * would // always use 'LeftPart' string to find out 'thisPd' and //
		 * 'RightPart' string to find out 'thatPd'. IT WOULD CAUSE // ERROR! //
		 * // Please remove the following bugfix section to re-produce // the
		 * bug if necessary. // // WHY IT WORKS BEFORE? The reason might come
		 * from the way we // name the Publication primitive in the Publication
		 * and // BibliographicFragment views. //
		 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ if
		 * (vl.getOutEdgeNode() == thatVd) { thisIn = false; } // End Bugfix //
		 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		 * 
		 * String[] connxArray = attLink.split(","); for (int i = 0; i <
		 * connxArray.length; i++) {
		 * 
		 * String addr = connxArray[i];
		 * 
		 * Pattern pattPv1 = null; Pattern pattPv2 = null; Pattern patt3 =
		 * Pattern.compile("\\:(.*?)\\=");
		 * 
		 * if (thisIn) { pattPv1 = Pattern.compile("^](.*?)\\|.*="); pattPv2 =
		 * Pattern.compile("=](.*?)\\|"); } else { pattPv2 =
		 * Pattern.compile("^](.*?)\\|.*="); pattPv1 =
		 * Pattern.compile("=](.*?)\\|"); }
		 * 
		 * Matcher matcher1 = pattPv1.matcher(addr); Matcher matcher2 =
		 * pattPv2.matcher(addr); Matcher matcher3 = patt3.matcher(addr);
		 * 
		 * if (matcher1.find() && matcher2.find()) {
		 * 
		 * String thisPv = matcher1.group(1); String thatPv = matcher2.group(1);
		 * 
		 * PrimitiveDefinition thisPd = null; PrimitiveDefinition thatPd = null;
		 * 
		 * if (((PrimitiveDefinitionGraph) thisVd.getSubGraph())
		 * .getNodes().containsKey(thisPv)) {
		 * 
		 * thisPd = (PrimitiveDefinition) ((PrimitiveDefinitionGraph) thisVd
		 * .getSubGraph()).getNodes().get(thisPv);
		 * 
		 * } else if (((PrimitiveDefinitionGraph) thisVd.getSubGraph())
		 * .getNodes().containsKey(thisPv + "LU")) {
		 * 
		 * thisPd = (PrimitiveDefinition) ((PrimitiveDefinitionGraph) thisVd
		 * .getSubGraph()).getNodes().get(thisPv + "LU");
		 * 
		 * }
		 * 
		 * if (((PrimitiveDefinitionGraph) thatVd.getSubGraph())
		 * .getNodes().containsKey(thatPv)) {
		 * 
		 * thatPd = (PrimitiveDefinition) ((PrimitiveDefinitionGraph) thatVd
		 * .getSubGraph()).getNodes().get(thatPv);
		 * 
		 * } else if (((PrimitiveDefinitionGraph) thatVd.getSubGraph())
		 * .getNodes().containsKey(thatPv + "LU")) {
		 * 
		 * thatPd = (PrimitiveDefinition) ((PrimitiveDefinitionGraph) thatVd
		 * .getSubGraph()).getNodes().get(thatPv + "LU");
		 * 
		 * }
		 * 
		 * PrimitiveInstance thisPi = null; if (thisPig.getNodes().containsKey(
		 * thisPd.getName() + "_" + thisIndex)) { thisPi = (PrimitiveInstance)
		 * thisPig.getNodes().get( thisPd.getName() + "_" + thisIndex); } else {
		 * thisPi = (PrimitiveInstance) thisPig.getNodes().get( thisPd.getName()
		 * + "_0"); }
		 * 
		 * PrimitiveInstance thatPi = null; if (thatPig.getNodes().containsKey(
		 * thatPd.getName() + "_" + thisIndex)) { thatPi = (PrimitiveInstance)
		 * thatPig.getNodes().get( thatPd.getName() + "_" + thatIndex); } else {
		 * thatPi = (PrimitiveInstance) thatPig.getNodes().get( thatPd.getName()
		 * + "_0"); }
		 * 
		 * boolean anyChange = false;
		 * 
		 * // // The primtitives are equivalent, just copy them if
		 * (sr.equals("Identical") || sr.equals("Superset") ||
		 * sr.equals("Subset") || sr.equals("Overlap") ||
		 * sr.equals("ForwardDependent") || sr.equals("ReverseDependent") ||
		 * sr.equals("Interdependent")) {
		 * 
		 * Object[] theseClasses = thisPi.getObjects().values() .toArray();
		 * Object[] thoseClasses = thatPi.getObjects().values() .toArray();
		 * 
		 * // // Note: // // Need to identify the target 'thatCi' in
		 * 'thoseClasses' // array. // Sometimes the organization of the source
		 * and target PI // might // be different! // for (int j = 0; j <
		 * theseClasses.length; j++) { ClassInstance thisCi = (ClassInstance)
		 * theseClasses[j]; ClassInstance thatCi = null;
		 * 
		 * for (int c = 0; c < thoseClasses.length; c++) { ClassInstance tempCi
		 * = (ClassInstance) thoseClasses[c]; if (thisCi.getDefinition().equals(
		 * tempCi.getDefinition())) { thatCi = tempCi; } }
		 * 
		 * if (thatCi != null) { Object[] theseAtts = thisCi.attributes.values()
		 * .toArray(); Object[] thoseAtts = thatCi.attributes.values()
		 * .toArray();
		 * 
		 * for (int k = 0; k < theseAtts.length; k++) { AttributeInstance thisAi
		 * = (AttributeInstance) theseAtts[k]; AttributeInstance thatAi =
		 * (AttributeInstance) thoseAtts[k];
		 * 
		 * thatAi.setValue(thisAi.getValue()); changesMade = true;
		 * 
		 * } } }
		 * 
		 * } // // Need to locate the remote key and fill it in else if
		 * (sr.equals("Connected") || sr.equals("Interconnected")) {
		 * 
		 * if (!matcher3.find()) { throw new Exception(
		 * "attributeLinkage error in viewlink"); }
		 * 
		 * // // Locate correct role object // String roleName =
		 * matcher3.group(1); UMLrole evr = null; ArrayList<UMLrole>
		 * extraViewRoles = thisPi.getDefinition() .readExtraPvRoles(); Iterator
		 * evrIt = extraViewRoles.iterator(); while (evrIt.hasNext()) { evr =
		 * (UMLrole) evrIt.next(); if (evr.getBaseName().equals(roleName)) {
		 * break; } } if (!evr.getBaseName().equals(roleName)) { throw new
		 * Exception("Can't find correct linking role"); }
		 * 
		 * // // Is the target class a linking class? // UMLclass targetClass =
		 * evr.getDirectClass(); boolean linkingTable = false; if
		 * (targetClass.getLinkAssociation() != null &&
		 * targetClass.getAssociateRoles().containsKey( evr.getBaseName())) {
		 * targetClass = ((UMLrole) targetClass
		 * .getAssociateRoles().get(evr.getBaseName())) .getDirectClass();
		 * linkingTable = true; }
		 * 
		 * // // If the linking role has a linking table, // then generate a
		 * data holder and attach it to // thatVi.linksToFillIn //
		 * ViewLinkInstance vli = null; if (linkingTable) { vli = new
		 * ViewLinkInstance(vl); try { thatVi.getLinksToFillIn().add(vli); }
		 * catch (Exception e) { throw new Exception(e.getMessage()); } }
		 * 
		 * // // The target class exists in our output data! // Fill in the
		 * data... if (thatPi.getObjects().containsKey(targetClass)) {
		 * 
		 * Iterator fkIt = evr.getFkArray().iterator(); while (fkIt.hasNext()) {
		 * 
		 * UMLattribute fk = (UMLattribute) fkIt.next(); UMLattribute pk =
		 * (UMLattribute) fk.getPk();
		 * 
		 * UMLattribute thisAd = null; UMLattribute thatAd = null;
		 * 
		 * if (thisPi.getObjects().containsKey(
		 * pk.getParentClass().getBaseName()) &&
		 * thatPi.getObjects().containsKey( fk.getParentClass().getBaseName()))
		 * {
		 * 
		 * thisAd = pk; thatAd = fk;
		 * 
		 * } else if (thisPi.getObjects().containsKey(
		 * fk.getParentClass().getBaseName()) &&
		 * thatPi.getObjects().containsKey( pk.getParentClass().getBaseName()))
		 * {
		 * 
		 * thisAd = fk; thatAd = pk;
		 * 
		 * }
		 * 
		 * ClassInstance thisCi = (ClassInstance) thisPi .getObjects().get(
		 * thisAd.getParentClass() .getBaseName());
		 * 
		 * ClassInstance thatCi = (ClassInstance) thatPi .getObjects().get(
		 * thatAd.getParentClass() .getBaseName());
		 * 
		 * AttributeInstance thisAi = (AttributeInstance) thisCi.attributes
		 * .get(thisAd.getBaseName()); AttributeInstance thatAi =
		 * (AttributeInstance) thatCi.attributes .get(thatAd.getBaseName());
		 * 
		 * String data = thisAi.readValueString();
		 * thatAi.writeValueString(data);
		 * 
		 * changesMade = true;
		 * 
		 * }
		 * 
		 * }
		 * 
		 * }
		 * 
		 * }
		 * 
		 * }
		 * 
		 * if (!changesMade) { return null; }
		 */

		return thatVi;

	}

	/**
	 * Sets the value of the AttributeInstance specified in the address string
	 * to the value set in the object.
	 * <P>
	 * The algorithm is as follows:
	 * <Q>
	 * 1) Run checks on the formatting of the attribute string & make sure the
	 * attribute instance exists.
	 * <P>
	 * 2) Check the object's class is appropriate
	 * <P>
	 * 3) Check the target string is 'Condition', 'Client' or 'Server'
	 * <P>
	 * 4) Locate the attribute instance
	 * <P>
	 * 5) Set the appropriate value
	 * <P>
	 * 
	 */
	public void updateAttributeInstance(String attributeAddress, Object value,
			int index) throws Exception {

		PrimitiveInstance pvIns;
		ClassInstance classIns;
		AttributeInstance attrIns;

		//
		// Check the format of the attributeAddress
		//
		int dotPos = attributeAddress.lastIndexOf('.');
		int barPos = attributeAddress.lastIndexOf('|');
		int brackPos = attributeAddress.lastIndexOf(']');

		if (dotPos == -1 || barPos == -1 || brackPos == -1) {
			throw new Exception(attributeAddress + " is badly formed\n");
		}

		String attributeName = attributeAddress.substring(dotPos + 1);
		String className = attributeAddress.substring(barPos + 1, dotPos);
		String primitiveName = attributeAddress.substring(brackPos + 1, barPos);

		//
		// Check for the existence of the attributeAddress
		// i) Primitive Definition
		String pvIndex = primitiveName + "_" + index;
		if (this.getSubGraph().getNodes()
				.containsKey(primitiveName + "_" + index)) {
			pvIns = (PrimitiveInstance) this.getSubGraph().getNodes()
					.get(pvIndex);
		} else {
			throw new Exception("Can't find " + attributeAddress
					+ ", no such primitive at index" + index + "\n");
		}

		//
		// ii) Class
		if (pvIns.getObjects().containsKey(className)) {

			classIns = (ClassInstance) pvIns.getObjects().get(className);

		} else {

			throw new Exception("Can't find " + attributeAddress
					+ ", no such class\n");

		}

		//
		// iii) Attribute
		if (classIns.attributes.containsKey(attributeName)) {

			attrIns = (AttributeInstance) classIns.attributes
					.get(attributeName);

		} else {

			throw new Exception("Can't find " + attributeAddress
					+ ", no such class\n");

		}

		//
		// Is this object the right type?
		//
		String type = attrIns.getDefinition().getType().getBaseName();
		if (type.equals("int")) {
			type = "Integer";
		} else if (type.equals("long")) {
			type = "Long";
		} else if (type.equals("float")) {
			type = "Float";
		} else if (type.equals("double")) {
			type = "Double";
		} else if (type.equals("boolean")) {
			type = "Boolean";
		} else if (type.equals("short")) {
			type = "Short";
		} else if (type.equals("char")) {
			type = "Char";
		}

		if (!value.getClass().getName().endsWith(type)) {
			throw new Exception(value.getClass().getName()
					+ " Object is not of the correct type: "
					+ attrIns.getDefinition().getType().getBaseName() + "\n");
		}

		//
		// Set the value
		//
		attrIns.setValue(value);

	}

	public void updateIndexes() throws Exception {
		this.updateUIDString();
		this.updateLabel();
		this.updateIndexTuple();
		this.updateUri();
		this.updateVpdmfId();
		this.setName(this.getUIDString());
	}

	public void updateVpdmfLabelsFromPrimaryPrimitive() throws Exception {
		String vpdmfLabel = "";

		ViewDefinition vd = this.getDefinition();

		String a = "]" + vd.getPrimaryPrimitive().getName() + "|"
				+ vd.getPrimaryPrimitive().getPrimaryClass().getBaseName();

		AttributeInstance ai = this.readAttributeInstance(a + ".vpdmfLabel", 0);
		this.setVpdmfLabel(ai.readValueString());

	}

	/**
	 * Generates the index tuple for this view instance.
	 * 
	 * @throws VPDMfException
	 */
	public String generateIndexTuple() throws VPDMfException {

		String indexTuple = "";

		int ieCount = this.getDefinition().getIndexElements().size();
		for (int i = 0; i < ieCount; i++) {
			Integer ii = new Integer(i + 1);
			IndexElement ie = this.getDefinition().getIndexElements().get(ii);

			int j = 0;
			String pvKey = ie.getP() + "_0";
			while (this.getSubGraph().getNodes().containsKey(pvKey)) {
				AttributeInstance ai = this.readAttributeInstance(ie.getP(),
						ie.getC(), ie.getA(), j);
				String idx = ai.readValueString();
				if (idx == null || idx.length() == 0)
					idx = "null";

				if (j > 0)
					indexTuple += ",";
				indexTuple += idx;

				j++;
				pvKey = ie.getP() + "_" + j;
			}

			if (i < ieCount - 1)
				indexTuple += ViewInstance.INDEX_TUPLE_SEPARATOR;

		}

		return indexTuple;

	}

	public void updateLabel() throws Exception {

		String vpdmfLabel = this.getDefinition().getVpdmfLabelFormat();
		vpdmfLabel = buildComplexLabel(vpdmfLabel);

		ViewDefinition vd = this.getDefinition();
		String addrStub = "]" + vd.getPrimaryPrimitive().getName() + "|"
				+ vd.getPrimaryPrimitive().getPrimaryClass().getBaseName();

		AttributeInstance hi = this.readAttributeInstance(addrStub
				+ ".vpdmfLabel", 0);
		hi.writeValueString(vpdmfLabel);

		this.setVpdmfLabel(vpdmfLabel);

	}

	public void updateIndexTuple() throws Exception {

		String indexTuple = this.generateIndexTuple();

		ViewDefinition vd = this.getDefinition();
		String addrStub = "]" + vd.getPrimaryPrimitive().getName() + "|"
				+ vd.getPrimaryPrimitive().getPrimaryClass().getBaseName();

		AttributeInstance idxTup = this.readAttributeInstance(addrStub
				+ ".indexTuple", 0);

		idxTup.writeValueString(indexTuple);
		this.setIndexTuple(indexTuple);

	}

	public void updateUri() throws Exception {

		String vpdmfUri = this.getDefinition().getVpdmfUriFormat();
		vpdmfUri = buildComplexLabel(vpdmfUri);

		ViewDefinition vd = this.getDefinition();
		String addrStub = "]" + vd.getPrimaryPrimitive().getName() + "|"
				+ vd.getPrimaryPrimitive().getPrimaryClass().getBaseName();

		AttributeInstance hi = this.readAttributeInstance(addrStub
				+ ".vpdmfUri", 0);
		hi.writeValueString(vpdmfUri);

		this.setVpdmfUri(vpdmfUri);

	}

	/**
	 * A new method created by Weicheng.
	 * 
	 * Reconstruct the data for those primitive instances which are depended by
	 * the primary primitive instance.
	 * 
	 * @param sourceVi
	 *            The sourceVi from which the dependent primitive instances will
	 *            be copied from.
	 */
	public void reconstructDependentPrimitives(ViewInstance sourceVi)
			throws Exception {
		if (sourceVi == null
				|| sourceVi.getDefinition() != this.getDefinition()) {
			throw new Exception("Invalid source view instance!");
		}

		Iterator<ViewDefinition> depIt = this.getDefinition().getDependencies()
				.iterator();
		while (depIt.hasNext()) {
			ViewDefinition dependVd = depIt.next();
			String pIAddress = "]" + dependVd.getName() + "LU";

			PrimitiveInstance oldPi = sourceVi.readPrimitiveInstance(pIAddress,
					0);
			PrimitiveInstance newPi = this.readPrimitiveInstance(pIAddress, 0);

			newPi.suckInData(oldPi);
		}

	}

	public Hashtable readLightEnclosedViewInstances() throws Exception {

		Hashtable ht = new Hashtable();

		VPDMf top = this.getDefinition().getTop();
		Iterator it = this.getSubGraph().getNodes().values().iterator();
		MAINLOOP: while (it.hasNext()) {
			PrimitiveInstance pi = (PrimitiveInstance) it.next();

			//
			// Bugfix:
			//
			// No need to spawn the target VI from the current PI
			// if the current PI is null.
			//
			if (pi.isNull() || pi.readIndex() != 0) {
				continue;
			}

			String pvName = pi.getDefinition().getName();
			PrimitiveDefinition pd = pi.getDefinition();
			ViewDefinition tempVd = null;

			boolean goFlag = false;
			try {
				Set<ViewDefinition> vdVec = this.getDefinition()
						.readAllLinkedViewDefinitionsVector(true);

				//
				// NEED TO TEST THIS CHANGE WITH EDITS
				//
				// - Is pvName the name of a linked ViewSpec Definition?
				// AND
				// - is it not the parent of the current view?
				//
				Iterator vdit = vdVec.iterator();
				while (vdit.hasNext()) {
					ViewDefinition vd = (ViewDefinition) vdit.next();
					String vdName = vd.getName();

					if ((pvName.equals(vdName) || pvName.equals(vdName + "LU"))
							&& !this.getDefinition().checkIsAChildOf(vd)) {
						goFlag = true;
						tempVd = vd;
						break;
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			if (!goFlag)
				continue MAINLOOP;

			try {
				int i = 0;
				Iterator vIt = this.spawnLightViewInstance(tempVd).iterator();
				while (vIt.hasNext()) {
					ViewInstance tempVi = (ViewInstance) vIt.next();
					ht.put(pvName + "_" + i, tempVi);
					i++;
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new Exception(e.getMessage());
			}
		}

		return ht;

	}

	/**
	 * 
	 * @throws SpawnViewException
	 * @return Hashtable
	 */
	public Map<String, ViewInstance> readEnclosedViewInstances()
			throws Exception {

		Hashtable ht = new Hashtable();

		VPDMf top = this.getDefinition().getTop();

		Iterator it = this.getSubGraph().getNodes().values().iterator();
		while (it.hasNext()) {
			PrimitiveInstance pi = (PrimitiveInstance) it.next();

			//
			// Bugfix:
			//
			// No need to spawn the target VI from the current PI
			// if the current PI is null.
			//
			if (pi.isNull()) {
				continue;
			}

			String pvName = pi.getDefinition().getName();
			int i = pi.readIndex();

			ViewDefinition tempVd = null;

			try {

				String temp = pvName;
				temp = temp.replaceAll("LU$", "");
				ViewDefinition lVd = (ViewDefinition) top.getViews().get(temp);

				Map<ViewDefinition, ViewLink> allLinkedViews = this
						.getDefinition().readAllLinkedViewDefinitions(true);

				if (lVd != null && allLinkedViews.containsKey(lVd)) {

					tempVd = (ViewDefinition) top.getViews().get(pvName);

					if (this.getDefinition().checkIsAChildOf(tempVd)) {
						continue;
					}

				} else {

					continue;

				}
				tempVd = lVd;

				ViewInstance tempVi = this.spawnViewInstance(tempVd, i, 0);

				if (tempVi == null) {
					continue;
				}

				ht.put(pi.getName(), tempVi);

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		return ht;

	}

	/**
	 * If a primitive contains a ViewTable primitive and is named the same as a
	 * view. Then copy the primitive into a new view and regenerate the index
	 * strings from the primitive.
	 */
	public void reconstructIndexStrings() throws Exception {

		Set<String> viewLookup = this.getDefinition().getTop().getViews()
				.keySet();
		for (SuperGraphNode n : this.getDefinition().getSubGraph().getNodes()
				.values()) {
			PrimitiveDefinition pd = (PrimitiveDefinition) n;

			if (!pd.isEditable()
					|| pd == this.getDefinition().getPrimaryPrimitive())
				continue;

			boolean skip = true;
			for (UMLclass c : pd.getClasses()) {
				if (c.getBaseName().equals("ViewTable")
						&& viewLookup.contains(pd.getName()))
					skip = false;
			}

			if (skip)
				continue;

			ViewDefinition vd = this.getDefinition().getTop().getViews()
					.get(pd.getName());

			int piCount = this.readPrimitiveInstanceTotal(pd);
			for (int i = 0; i < piCount; i++) {
				PrimitiveInstance pi = (PrimitiveInstance) this.getSubGraph()
						.getNodes().get(pd.getName() + "_" + i);

				if (pi.isNull())
					continue;

				ViewInstance vi = new ViewInstance(vd);
				vi.getPrimaryPrimitive().suckInData(pi);
				vi.updateLabel();
				vi.updateIndexTuple();
				pi.suckInData(vi.getPrimaryPrimitive());

			}

		}

	}

	/**
	 * Update the UIDString for the view instance.
	 * 
	 * - Create the UIDString. - Set up the UIDString into the current view
	 * instance.
	 * 
	 */
	@Deprecated
	public void updateUIDString() {
		String uidString = "";

		List<UMLattribute> pkArray = this.getDefinition().getPrimaryPrimitive()
				.getPrimaryClass().getPkArray();
		String pVName = this.getDefinition().getPrimaryPrimitive().getName();
		String className = this.getDefinition().getPrimaryPrimitive()
				.getPrimaryClass().getBaseName();

		for (int i = 0; i < pkArray.size(); i++) {
			UMLattribute pkAtt = (UMLattribute) pkArray.get(i);
			String attAddress = "]" + pVName + "|" + className + "."
					+ pkAtt.getBaseName();

			String pkValue = "";
			try {
				pkValue = this.readAttributeInstance(attAddress, 0)
						.readValueString();
			} catch (Exception e) {
				e.printStackTrace();
			}

			//
			// Modified by Weicheng: 'pkValue' might be null!
			//
			// if( pkValue.length() == 0 ) {
			if (pkValue == null || pkValue.length() == 0) {
				uidString = "null";
			} else if (i == 0) {
				uidString = pkAtt.getBaseName() + "=" + pkValue;
			} else {
				uidString += "&" + pkAtt.getBaseName() + "=" + pkValue;
			}
		}

		this.setUIDString(uidString);

	}

	/**
	 * @throws AttributeAddressException
	 * 
	 */
	public void updateVpdmfId() throws AttributeAddressException {

		List<UMLattribute> pkArray = this.getDefinition().getPrimaryPrimitive()
				.getPrimaryClass().getPkArray();
		UMLattribute pk = pkArray.get(0);

		String pVName = this.getDefinition().getPrimaryPrimitive().getName();
		String className = this.getDefinition().getPrimaryPrimitive()
				.getPrimaryClass().getBaseName();

		String attAddress = "]" + pVName + "|" + className + "."
				+ pk.getBaseName();

		AttributeInstance pkAi = this.readAttributeInstance(attAddress, 0);

		this.setVpdmfId((Long) pkAi.getValue());

	}

	public void reconstructPiTotals() throws Exception {
		Iterator<SuperGraphNode> pdIt = this.getDefinition().getSubGraph()
				.getNodes().values().iterator();

		while (pdIt.hasNext()) {
			PrimitiveDefinition pd = (PrimitiveDefinition) pdIt.next();
			this.computePrimitiveInstanceTotal(pd, this.countPrimitives(pd));
		}
	}

	public void unlockViewTableInstances() throws Exception {
		Iterator it = this.getSubGraph().getNodes().values().iterator();
		while (it.hasNext()) {
			PrimitiveInstance pi = (PrimitiveInstance) it.next();
			PrimitiveDefinition pd = pi.getDefinition();
			if (pd.getName().endsWith("_ViewTable")) {
				String addr = "]" + pd.getName() + "|ViewTable.locked";
				AttributeInstance ai = this.readAttributeInstance(addr,
						pi.readIndex());
				ai.writeValueString("false");
			}
		}
	}

	public ArrayList readAllLinkedViews() {
		ArrayList nodes = new ArrayList();
		Iterator vliIt = this.getOutgoingEdges().values().iterator();
		while (vliIt.hasNext()) {
			ViewLinkInstance vli = (ViewLinkInstance) vliIt.next();
			nodes.add(vli.getInEdgeNode());
		}
		vliIt = this.getIncomingEdges().values().iterator();
		while (vliIt.hasNext()) {
			ViewLinkInstance vli = (ViewLinkInstance) vliIt.next();
			nodes.add(vli.getOutEdgeNode());
		}
		return nodes;
	}

};
