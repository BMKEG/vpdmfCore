package edu.isi.bmkeg.vpdmf.model.definitions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DefaultEdge;

import edu.isi.bmkeg.uml.model.UMLattribute;
import edu.isi.bmkeg.uml.model.UMLclass;
import edu.isi.bmkeg.uml.model.UMLrole;
import edu.isi.bmkeg.utils.superGraph.SuperGraph;
import edu.isi.bmkeg.utils.superGraph.SuperGraphEdge;
import edu.isi.bmkeg.utils.superGraph.SuperGraphNode;

/**
 * Timestamp: Thu_Jun_19_120936_2003;
 */

public class PrimitiveDefinitionGraph extends SuperGraph {

	static final long serialVersionUID = 6192677979673230342L;
	
	public void addPrimitiveLink(PrimitiveDefinition pv1,
			PrimitiveDefinition pv2, UMLrole role, boolean paged, boolean crossLink) {

		PrimitiveDefinition fromPv = null;
		PrimitiveDefinition toPv = null;

		// If the role is not defined, then this is a child->parent connection
		// between primitives. The PK is in the parent, so the order is reversed
		if (role == null) {
			fromPv = pv2;
			toPv = pv1;
		}
		// if the role is implemented by other roles then we simply preserve the 
		// original order, this is an n-to-n relationship and could be constructed 
		// either way. 
		else if( role.getImplementedBy().size() > 0 ) {
			
			fromPv = pv1;
			toPv = pv2;
			
	 	} 
		// we need to sort out the dependency based on key constraints
		// from the cardinality of the association
		else {

			List fks = role.getFkArray();
			UMLclass pkClass = ((UMLattribute) fks.get(0)).getPk()
					.getParentClass();

			boolean go1 = false;
			Iterator it = pv1.getClasses().iterator();
			while (it.hasNext()) {
				UMLclass c = (UMLclass) it.next();
				if (c.equals(pkClass)) {
					go1 = true;
					break;
				}
			}

			boolean go2 = false;
			it = pv2.getClasses().iterator();
			while (it.hasNext()) {
				UMLclass c = (UMLclass) it.next();
				if (c.equals(pkClass)) {
					go2 = true;
					break;
				}
			}

			if (go1) {
				fromPv = pv1;
				toPv = pv2;
			} else if (go2) {
				fromPv = pv2;
				toPv = pv1;
			} else {
				fromPv = pv1;
				toPv = pv2;
			}

		}

		PrimitiveLink edge = null;
		if (role != null) {
			edge = new PrimitiveLink();
			edge.setPaged(paged);
			edge.setCrossLink(crossLink);
			edge.setDirected(true);
			edge.setRole(role);
		} else {
			edge = new PrimitiveLink();
			edge.setPaged(paged);
			edge.setCrossLink(crossLink);
			edge.setDirected(true);
		}

		this.getEdges().add(edge);
			
		edge.setName(fromPv.getName() + "." + role.getBaseName() + "->" + toPv.getName());
		fromPv.getOutgoingEdges().put(edge.getName(), edge);
		toPv.getIncomingEdges().put(edge.getName(), edge);

		edge.setInEdgeNode(toPv);
		edge.setOutEdgeNode(fromPv);

	}
	
	public int countNCardinalitiesToTarget(PrimitiveDefinition pd) throws Exception {

		int cardinalityCount = 0;
		
		Iterator<PrimitiveLink> plIt = this.readPrimitiveLinksToTarget(pd).iterator();
		while( plIt.hasNext() ) {
			PrimitiveLink pl = (PrimitiveLink) plIt.next();
				
			UMLrole r = pl.getRole();
			if( r.getMult_upper() == -1 )
				cardinalityCount++;
			
		}	
	
		return cardinalityCount;		
	
	}
	
	public List<PrimitiveLink> readPrimitiveLinksToTarget(PrimitiveDefinition pd) throws Exception {

		List<PrimitiveLink> plList = new ArrayList<PrimitiveLink>();
		
		ViewDefinition vd = pd.getView();
		UndirectedGraph<SuperGraphNode, DefaultEdge> gg = vd.getSubGraph()
				.dumpToJGraphT();
		
		DijkstraShortestPath<SuperGraphNode, DefaultEdge> dij = new DijkstraShortestPath<SuperGraphNode, DefaultEdge>(
				gg, vd.getPrimaryPrimitive(), pd);

		GraphPath<SuperGraphNode, DefaultEdge> path = dij.getPath();

		if (path == null) {
			return plList;
		}

		List<SuperGraphNode> list = Graphs.getPathVertexList(dij.getPath());

		PrimitiveDefinition source = (PrimitiveDefinition) list.get(0);
		for(int i=1; i<list.size(); i++) {
			PrimitiveDefinition target = (PrimitiveDefinition) list.get(i);

			PrimitiveLink pl = null;
			for( SuperGraphEdge e : source.getOutgoingEdges().values() ) {
				if( e.getOutEdgeNode() == source && e.getInEdgeNode() == target) {
					pl = (PrimitiveLink) e;
					break;
				}
			}
			if( pl == null ) {
				for( SuperGraphEdge e : source.getIncomingEdges().values() ) {
					if( e.getInEdgeNode() == source && e.getOutEdgeNode() == target) {
						pl = (PrimitiveLink) e;
						break;
					}
				}
				if( pl == null ) 
					throw new Exception("Error: can't link" + source.getName() + " to " + target.getName());
			}			
			plList.add(pl);
						
			source = target;
		}		

		return plList;
		
	}

	public List<PrimitiveDefinition> readPrimitivesToTarget(PrimitiveDefinition pd) {

		List<PrimitiveDefinition> pdList = new ArrayList<PrimitiveDefinition>();
		
		ViewDefinition vd = pd.getView();
		UndirectedGraph<SuperGraphNode, DefaultEdge> gg = vd.getSubGraph()
				.dumpToJGraphT();
				
		DijkstraShortestPath<SuperGraphNode, DefaultEdge> dij = new DijkstraShortestPath<SuperGraphNode, DefaultEdge>(
				gg, vd.getPrimaryPrimitive(), pd);

		GraphPath<SuperGraphNode, DefaultEdge> path = dij.getPath();

		if (path == null) {
			return pdList;
		}

		Iterator<SuperGraphNode> pdIt = Graphs.getPathVertexList(dij.getPath()).iterator();
		while( pdIt.hasNext() ) {
			pdList.add((PrimitiveDefinition) pdIt.next());
		}		

		return pdList;
		
	}

	
	
};
