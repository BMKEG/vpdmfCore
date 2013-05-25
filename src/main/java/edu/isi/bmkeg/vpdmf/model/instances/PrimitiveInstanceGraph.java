package edu.isi.bmkeg.vpdmf.model.instances;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DefaultEdge;

import edu.isi.bmkeg.utils.superGraph.SuperGraph;
import edu.isi.bmkeg.utils.superGraph.SuperGraphEdge;
import edu.isi.bmkeg.utils.superGraph.SuperGraphNode;
import edu.isi.bmkeg.utils.superGraph.SuperGraphTraversal;
import edu.isi.bmkeg.vpdmf.model.definitions.PrimitiveDefinition;
import edu.isi.bmkeg.vpdmf.model.definitions.PrimitiveDefinitionGraph;
import edu.isi.bmkeg.vpdmf.model.definitions.PrimitiveLink;
import edu.isi.bmkeg.vpdmf.model.definitions.ViewDefinition;

/**
 * Timestamp: Thu_Jun_19_120936_2003;
 */

public class PrimitiveInstanceGraph extends SuperGraph {
	static final long serialVersionUID = 2593554832580697147L;

	private PrimitiveDefinitionGraph definition;

	public void addPvInstance(PrimitiveInstance pi) throws Exception {

		this.getNodes().put(pi.getName(), pi);
		pi.setGraph(this);

		pi.linkAttributeInstances();

	}

	public PrimitiveInstance addPvInstance(String pVDefName, int index) {
		PrimitiveDefinition pVDef = (PrimitiveDefinition) this.getDefinition()
				.getNodes().get(pVDefName);

		PrimitiveInstance newPVIns = new PrimitiveInstance(pVDef, index);
		try {
			this.addNode(newPVIns);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		Vector toNodes = new Vector();
		Vector fromNodes = new Vector();

		Iterator<SuperGraphEdge> edgeIt = pVDef.getOutgoingEdges().values()
				.iterator();
		while (edgeIt.hasNext()) {
			SuperGraphEdge edge = edgeIt.next();
			toNodes.add(edge.getInEdgeNode());
		}

		edgeIt = pVDef.getIncomingEdges().values().iterator();
		while (edgeIt.hasNext()) {
			SuperGraphEdge edge = edgeIt.next();
			fromNodes.add(edge.getOutEdgeNode());
		}

		Iterator<SuperGraphNode> nodeIt = this.getNodes().values().iterator();
		while (nodeIt.hasNext()) {
			SuperGraphNode node = nodeIt.next();
			PrimitiveInstance currentPVIns = (PrimitiveInstance) node;

			try {
				if (toNodes.contains(currentPVIns.getDefinition())) {
					this.addPvInstanceLink(newPVIns.getName(),
							currentPVIns.getName());

				} else if (fromNodes.contains(currentPVIns.getDefinition())) {
					this.addPvInstanceLink(currentPVIns.getName(),
							newPVIns.getName());
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		try {
			newPVIns.setGraph(this);
			this.linkAttributeInstances();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return newPVIns;
	}

	public void addPvInstanceLink(String fromNodeName, String toNodeName)
			throws Exception {

		PrimitiveInstance fromPvIns = (PrimitiveInstance) this.getNodes().get(
				fromNodeName);
		PrimitiveInstance toPvIns = (PrimitiveInstance) this.getNodes().get(
				toNodeName);

		if (fromPvIns == null || toPvIns == null) {
			throw new Exception("Can't add an edge to the graph, none "
					+ "or both of the nodes don't exist");
		}

		PrimitiveDefinition fromPvDef = (PrimitiveDefinition) fromPvIns
				.getDefinition();

		PrimitiveDefinition toPvDef = (PrimitiveDefinition) toPvIns
				.getDefinition();

		PrimitiveLink pl = (PrimitiveLink) fromPvDef.getOutgoingEdges().get(
				toPvDef.getName());

		if (pl == null)
			throw new Exception("Can't add pvLink to graph\n"
					+ toPvDef.getName() + " not found in "
					+ fromPvDef.getName());

		PrimitiveLinkInstance pli = new PrimitiveLinkInstance(pl);
		pli.setGraph(this);
		this.getEdges().add(pli);

		pli.setName(toNodeName);
		fromPvIns.getOutgoingEdges().put(toNodeName, pli);
		toPvIns.getIncomingEdges().put(fromNodeName, pli);

		pli.setOutEdgeNode(fromPvIns);
		pli.setInEdgeNode(toPvIns);
		
		// check to see if the pli has a link class with a vpdmfOrder attribute.
		if( pli.getLinkClass() != null  && 
				pli.getLinkClass().getAttributes().containsKey("vpdmfOrder")) {
			
			String toNodeIndex = toNodeName.substring(toNodeName.lastIndexOf("_")+1, toNodeName.length());
			String fromNodeIndex = fromNodeName.substring(fromNodeName.lastIndexOf("_")+1, fromNodeName.length());

			Integer to = new Integer(toNodeIndex);
			Integer from = new Integer(fromNodeIndex);
			
			Integer idx = to;
			if(from > to)
				idx = from;

			AttributeInstance ai = pli.getLinkClass().getAttributes().get("vpdmfOrder");
			ai.setValue(idx);
				
		}

		pli.linkAttributeInstances();

	}

	public boolean checkExistPvInstance(String attAddress, int index) {
		boolean isExist = false;

		String pVName = attAddress.substring(attAddress.indexOf("]") + 1,
				attAddress.indexOf("|"));
		pVName += "_" + index;

		isExist = this.getNodes().containsKey(pVName);

		return isExist;

	}

	public boolean checkForLinkInstanceExistence(String fromNodeName,
			String toNodeName) throws Exception {
		PrimitiveInstance fromPvIns = (PrimitiveInstance) this.getNodes().get(
				fromNodeName);

		PrimitiveInstance toPvIns = (PrimitiveInstance) this.getNodes().get(
				toNodeName);

		if (fromPvIns == null || toPvIns == null) {
			return false;
		}

		if (fromPvIns.getOutgoingEdges().containsKey(toNodeName)) {
			return true;
		} else {
			return false;
		}

	}

	public int countPrimitives(PrimitiveDefinition pd) {
		int i = 0;
		Iterator it = this.getNodes().values().iterator();
		while (it.hasNext()) {
			PrimitiveInstance pi = (PrimitiveInstance) it.next();
			if (pi.getDefinition().equals(pd)) {
				i++;
			}
		}
		return i;
	}

	public void deleteNode(PrimitiveInstance target) throws Exception {
		super.deleteNodeFromGraph(target);
		target.destroy();
	}

	public void destroy() {
		this.definition = null;
		super.destroy();
	}

	public String dumpToXML() throws Exception {
		String xml = "<Primitives>\n";
		String temp = "";
		Iterator nodeIt = this.getNodes().values().iterator();

		while (nodeIt.hasNext()) {
			PrimitiveInstance pi = (PrimitiveInstance) nodeIt.next();
			temp += pi.dumpToXML();
		}

		Pattern patt = Pattern.compile("\n");
		Matcher matcher = patt.matcher(temp);
		temp = matcher.replaceAll("\n  ");

		xml += temp + "\n</Primitives>\n<PrimitiveLinks>\n";

		temp = "";
		Iterator edgeIt = this.getEdges().iterator();
		while (edgeIt.hasNext()) {
			PrimitiveLinkInstance pli = (PrimitiveLinkInstance) edgeIt.next();
			temp += pli.dumpToXML();
		}
		patt = Pattern.compile("\n");
		matcher = patt.matcher(temp);
		temp = matcher.replaceAll("\n  ");

		xml += temp + "\n</PrimitiveLinks>\n";
		return xml;

	}



	public PrimitiveDefinitionGraph getDefinition() {
		return this.definition;
	}

	/**
	 * Link all attribute instances that require linking in the PIG
	 */
	public void linkAttributeInstances() {
		//
		// First, remove all existing attribute links
		//
		ViewInstance vi = (ViewInstance) this.getSubGraphNode();
		Iterator aiIt = vi.readAttributes().iterator();
		while (aiIt.hasNext()) {
			AttributeInstance ai = (AttributeInstance) aiIt.next();
			ai.clearConnectedKeys();
		}

		//
		// Run through all the links in the pig and link the underlying
		// attributes
		try {
			Iterator it = this.getEdges().iterator();
			while (it.hasNext()) {
				PrimitiveLinkInstance pli = (PrimitiveLinkInstance) it.next();
				pli.linkAttributeInstances();
			}

		} catch (Exception e) {

			e.printStackTrace();

		}

		//
		// Run through all Primitives and link intraPrimitive attributes.
		try {

			Iterator it = this.getNodes().values().iterator();
			while (it.hasNext()) {
				PrimitiveInstance pi = (PrimitiveInstance) it.next();
				pi.linkAttributeInstances();
			}

		} catch (Exception e) {

			e.printStackTrace();

		}

	}

	public void removePvInstance(String alias) throws Exception {
		PrimitiveInstance pi = (PrimitiveInstance) this.getNodes().get(alias);

		this.deleteNode(pi);
		this.linkAttributeInstances();

	}

	public void setDefinition(PrimitiveDefinitionGraph definition) {
		this.definition = definition;
	}

	public List<PrimitiveInstance> readPrimitivesToTarget(
			PrimitiveInstance pi) {

		List<PrimitiveInstance> piList = new ArrayList<PrimitiveInstance>();

		ViewInstance vi = (ViewInstance) this.getSubGraphNode();
		UndirectedGraph<SuperGraphNode, DefaultEdge> gg = this.dumpToJGraphT();

		DijkstraShortestPath<SuperGraphNode, DefaultEdge> dij = new DijkstraShortestPath<SuperGraphNode, DefaultEdge>(
				gg, vi.getPrimaryPrimitive(), pi);

		GraphPath<SuperGraphNode, DefaultEdge> path = dij.getPath();

		if (path == null) {
			return piList;
		}

		Iterator<SuperGraphNode> piIt = Graphs.getPathVertexList(dij.getPath())
				.iterator();
		while (piIt.hasNext()) {
			piList.add((PrimitiveInstance) piIt.next());
		}

		return piList;

	}

};
