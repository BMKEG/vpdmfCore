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

	public void addPvInstanceLink(PrimitiveLink pl, String pi1Name, String pi2Name)
			throws Exception {

		PrimitiveInstance pi1 = (PrimitiveInstance) this.getNodes().get(
				pi1Name);
		PrimitiveInstance pi2 = (PrimitiveInstance) this.getNodes().get(
				pi2Name);

		if (pi1 == null || pi2 == null) {
			throw new Exception("Can't add an edge to the graph, none "
					+ "or both of the nodes don't exist");
		}

		PrimitiveDefinition pd1 = (PrimitiveDefinition) pi1
				.getDefinition();

		PrimitiveDefinition pd2 = (PrimitiveDefinition) pi2
				.getDefinition();		
		
		PrimitiveInstance fromPi = pi1;
		PrimitiveInstance toPi = pi2;
		
		PrimitiveLinkInstance pli = new PrimitiveLinkInstance(pl);
		pli.setGraph(this);
		this.getEdges().add(pli);
		
		String key = fromPi.getName() + "." + pl.getRole().getBaseName() + "->" + toPi.getName();
		pli.setName(key);
		fromPi.getOutgoingEdges().put(key, pli);
		toPi.getIncomingEdges().put(key, pli);

		pli.setOutEdgeNode(fromPi);
		pli.setInEdgeNode(toPi);
		
		// check to see if the pli has a link class with a vpdmfOrder attribute.
		if( pli.getLinkClass() != null  && 
				pli.getLinkClass().getAttributes().containsKey("vpdmfOrder")) {
			
			String toNodeIndex = toPi.getName().substring(pi2Name.lastIndexOf("_")+1, toPi.getName().length());
			String fromNodeIndex = fromPi.getName().substring(pi1Name.lastIndexOf("_")+1, fromPi.getName().length());

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

	public boolean checkForLinkInstanceExistence(PrimitiveLink pl, 
			String fromNodeName, String toNodeName) throws Exception {

		PrimitiveInstance fromPvIns = (PrimitiveInstance) this.getNodes().get(
				fromNodeName);

		PrimitiveInstance toPvIns = (PrimitiveInstance) this.getNodes().get(
				toNodeName);

		if (fromPvIns == null || toPvIns == null) {
			return false;
		}

		String key = fromNodeName + "." + pl.getRole().getBaseName() + "->" + toNodeName;
		if( fromPvIns.getOutgoingEdges().containsKey(key) || 
				fromPvIns.getIncomingEdges().containsKey(key) ) {
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
	 * @throws Exception 
	 */
	public void linkAttributeInstances() throws Exception {
		//
		// First, remove all existing attribute links
		//
		ViewInstance vi = (ViewInstance) this.getSubGraphNode();
		Iterator<AttributeInstance> aiIt = vi.readAttributes().iterator();
		while (aiIt.hasNext()) {
			AttributeInstance ai = (AttributeInstance) aiIt.next();
			ai.clearConnectedKeys();
		}

		//
		// Run through all the links in the pig and link the underlying
		// attributes
		Iterator it = this.getEdges().iterator();
		while (it.hasNext()) {
			PrimitiveLinkInstance pli = (PrimitiveLinkInstance) it.next();
			pli.linkAttributeInstances();
		}

		//
		// Run through all Primitives and link intraPrimitive attributes.
		it = this.getNodes().values().iterator();
		while (it.hasNext()) {
			PrimitiveInstance pi = (PrimitiveInstance) it.next();
			pi.linkAttributeInstances();
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
