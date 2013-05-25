package edu.isi.bmkeg.vpdmf.model.instances;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.isi.bmkeg.utils.superGraph.SuperGraph;
import edu.isi.bmkeg.utils.superGraph.SuperGraphEdge;
import edu.isi.bmkeg.utils.superGraph.SuperGraphNode;
import edu.isi.bmkeg.vpdmf.exceptions.VPDMfException;
import edu.isi.bmkeg.vpdmf.model.definitions.ViewDefinition;
import edu.isi.bmkeg.vpdmf.model.definitions.ViewLink;
import edu.isi.bmkeg.vpdmf.model.definitions.ViewGraphDefinition;

/**
 * Timestamp: Thu_Jun_19_120936_2003;
 */

public class viewGraphInstance extends SuperGraph {
	static final long serialVersionUID = 2593554832580697147L;

	private ViewGraphDefinition definition;

	public boolean addLink(String fromVIName, String toVIName, String label)
			throws Exception, VPDMfException {

		boolean success = false;

		//
		// Check to make sure that both inNode & outNode are valid nodes in the
		// Graph
		//
		ViewInstance fromVI = (ViewInstance) this.getNodes().get(fromVIName);

		ViewInstance toVI = (ViewInstance) this.getNodes().get(toVIName);

		if (fromVI == null || toVI == null) {
			return false;
		}

		if (this.checkExistEdge(fromVI, toVI.getName() )) {
			return false;
		}

		ViewDefinition fromVD = fromVI.getDefinition();
		ViewDefinition toVD = toVI.getDefinition();

		/*Hashtable ht = fromVD.readOutputViewDefinitions(true);

		if (ht.containsKey(toVD)) {
			ViewLink vl = (ViewLink) ht.get((Object) toVD);
			ViewLinkInstance vli = new ViewLinkInstance(vl);
			vli.setLabel(label);
			super.addEdge(fromVI, toVI, vli);
			success = true;
		}*/

		return success;

	}

	public void destroy() {
		this.definition = null;

		super.destroy();
	}

	public ViewGraphDefinition getDefinition() {
		return this.definition;
	}

	public void setDefinition(ViewGraphDefinition definition) {
		this.definition = definition;
	}

	public viewGraphInstance(ViewGraphDefinition definition) {

		this.setDefinition(definition);

	}

	public HashSet readActiveLinks(ViewInstance vi) throws Exception {

		HashSet activeSet = new HashSet();

		ViewInstance thisVi = (ViewInstance) this.getNodes().get(vi.getName());

		if (thisVi == null) {
			throw new Exception("No node found in graph for count: "
					+ vi.getName());
		}

		Iterator viewIt = thisVi.readAllLinkedViews().iterator();
		while (viewIt.hasNext()) {
			ViewInstance view = (ViewInstance) viewIt.next();
			ViewDefinition vd = view.getDefinition();
			activeSet.add(vd);
		}

		return activeSet;

	}

	// TODO: Horrible code. Need to make this typesafe. Fix this.
	public void modifyVGI(LinkedHashSet<ViewInstance> viewsToAdd,
			HashSet linksToAdd) throws Exception {

		this.addNodesToVGI(viewsToAdd);
		
	    Iterator linkIt  = linksToAdd.iterator();
	    while( linkIt.hasNext() ) {

	      Object link = linkIt.next();

	      if( link instanceof ClassInstance )
	        this.addLinkToVGI( (ClassInstance) link);
	      else if( link instanceof ViewLinkInstance )
	        this.addLinkToVGI( (ViewLinkInstance) link);

	    }


	}

	public void addNodesToVGI(LinkedHashSet<ViewInstance> viewsToAdd)
			throws Exception {

		//
		// Adding nodes
		//
		Iterator<ViewInstance> newNodeIt = viewsToAdd.iterator();
		while (newNodeIt.hasNext()) {

			ViewInstance lvi = (ViewInstance) newNodeIt.next();
			// lvi.setIcon(lvi.get_defName());

			if (!this.getNodes().containsKey(lvi.getName())) {

				this.addNode(lvi);

			} else {

				System.out.println(lvi.getName());

			}

		}

	}

	public void addLinkToVGI(ViewLinkInstance link) throws Exception {

		ViewInstance outVi = (ViewInstance) link.getOutEdgeNode();
		String fromUid = outVi.getName();

		ViewInstance inVi = (ViewInstance) link.getInEdgeNode();
		String toUid = inVi.getName();

		this.addLink(fromUid, toUid, "");

	}
	
	
	//
	// TODO : not sure what this is exactly... Need to check.
	//
	private void addLinkToVGI(ClassInstance link) throws Exception {

		AttributeInstance ai = (AttributeInstance) link.attributes
				.get("machineIndex");
		String mIdx = ai.readValueString();

		ai = (AttributeInstance) link.attributes.get("vpdmfLabel");
		String hIdx = ai.readValueString();

		ai = (AttributeInstance) link.attributes.get("vpdmfId");
		String linkUid = ai.readValueString();
		linkUid = "ViewLinkTable_id=" + linkUid;

		ai = (AttributeInstance) link.attributes.get("linkType");
		String edgeType = ai.readValueString();
		String edgeValue = "";

		//
		// Use regexes to get data from vpdmfLabel
		// index1 >>> value >>> index2
		//
		Pattern p = Pattern.compile(".*>>>(.*)>>>.*");
		Matcher m = p.matcher(mIdx);

		if (m.find()) {
			edgeValue = m.group(1);
		}

		String label = "";
		if (edgeValue.length() > 0) {
			label = edgeValue;
		} else {
			label = edgeType;
		}

		AttributeInstance fromAi = (AttributeInstance) link.attributes
				.get("from_id");
		String fromUid = fromAi.readValueString();
		fromUid = "id=" + fromUid;

		AttributeInstance toAi = (AttributeInstance) link.attributes
				.get("to_id");
		String toUid = toAi.readValueString();
		toUid = "id=" + toUid;

		if (edgeType.equals("link") || edgeType.equals("d")) {

			this.addLink(fromUid, toUid, "");

		} else {

			ViewDefinition rlnDef = (ViewDefinition) this.getNodes().get(
					edgeType);

			ViewInstance rln = new ViewInstance(edgeType);
			rln.setDefinition(rlnDef);
			rln.setVpdmfLabel(hIdx);
			rln.setUIDString(linkUid);

			// TODO NEED TO FIND A WAY OF GETTING THIS FROM THE CURRENT VIEWS IN
			// MEMORY...;
			ViewInstance lvi = rln; // /vsm.viewInstanceInMemory(rln);

			p = Pattern.compile("(id=\\d+) .* (id=\\d+)");
			m = p.matcher(mIdx);
			String proxyFrom = null;
			String proxyTo = null;
			if (m.find()) {
				proxyFrom = m.group(1);
				proxyTo = m.group(2);
			} else
				throw new Exception("Error in proxy path check");

			if (!this.getNodes().containsKey(linkUid)) {

				this.addNode(lvi);

			}

			this.addLink(fromUid, linkUid, "");
			this.addLink(linkUid, toUid, "");

		}

	}
	
	public void removeDefinitions() {

		this.setDefinition(null);

		Iterator<SuperGraphNode> vIt = this.getNodes().values().iterator();
		while(vIt.hasNext()) {
			ViewInstance vi = (ViewInstance) vIt.next();
			
			vi.removeDefinition();

		}

	}

}
