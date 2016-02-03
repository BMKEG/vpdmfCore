package edu.isi.bmkeg.utils.superGraph;

/**
 * Timestamp: Thu_Jun_19_120936_2003;
 */

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

public class SuperGraph implements Serializable {

	static final long serialVersionUID = -6900886782434365837L;

	private SuperGraphNode subGraphNode;
	private Map<String, SuperGraphNode> nodes = new HashMap<String, SuperGraphNode>();
	private Set<SuperGraphEdge> edges = new HashSet<SuperGraphEdge>();

	private SuperGraphTraversal traversal;

	public boolean addEdge(String fromNode, String toNode) throws Exception {
		//
		// Check to make sure that both inNode & outNode are valid nodes in the
		// Graph
		//
		SuperGraphNode fromGraphNode = this.getNodes().get(fromNode);
		SuperGraphNode toGraphNode = this.getNodes().get(toNode);

		if (fromGraphNode == null || toGraphNode == null
				|| !fromGraphNode.getName().equals(fromNode)
				|| !toGraphNode.getName().equals(toNode)) {
			throw new Exception("Can't add an edge to the graph, "
					+ "none or both of the nodes don't exist");
		}

		if (!fromGraphNode.isDisplayable() || !toGraphNode.isDisplayable())
			return false;

		SuperGraphEdge edge = new SuperGraphEdge();
		edge.setGraph(this);
		this.getEdges().add(edge);

		edge.setName(fromNode + "->" + toNode);
		fromGraphNode.getOutgoingEdges().put(edge.getName(), edge);
		toGraphNode.getIncomingEdges().put(edge.getName(), edge);

		edge.setInEdgeNode(toGraphNode);
		edge.setOutEdgeNode(fromGraphNode);

		return true;

	}

	protected boolean addEdge(SuperGraphNode fromGraphNode,
			SuperGraphNode toGraphNode, 
			SuperGraphEdge graphEdge)
			throws Exception {

		if (this.checkExistEdge(fromGraphNode, graphEdge.getName()))
			throw new Exception("Edge already exists");

		if (!fromGraphNode.isDisplayable() || !toGraphNode.isDisplayable()
				|| !graphEdge.isDisplayable())
			throw new Exception("Problems with isDisplayable property");

		graphEdge.setGraph(this);
		this.getEdges().add(graphEdge);

		fromGraphNode.getOutgoingEdges().put(graphEdge.getName(), graphEdge);
		toGraphNode.getIncomingEdges().put(fromGraphNode.getName(), graphEdge);

		graphEdge.setInEdgeNode(toGraphNode);

		graphEdge.setOutEdgeNode(fromGraphNode);

		return false;

	}

	public boolean addNode(SuperGraphNode node) throws Exception {
		boolean newNodeCreated = false;

		if (node.getName() == null) {
			throw new Exception("Node's name is null, can't add to graph");
		}

		if (!node.isDisplayable())
			throw new Exception("Problems with isDisplayable property");

		//
		// Only add the node which do not already exist in the graph
		//
		if (!this.getNodes().containsKey(node.getName())) {

			this.getNodes().put(node.getName(), node);
			newNodeCreated = true;

		}

		return newNodeCreated;

	}

	public boolean checkExistEdge(SuperGraphNode fromGraphNode,
			String edgeName) {

		return fromGraphNode.getOutgoingEdges().containsKey(edgeName);

	}

	public void deleteEdgeSetFromGraph(Set<SuperGraphEdge> edgeSet) throws Exception {

		Iterator<SuperGraphEdge> it = this.getEdges().iterator();
		while (it.hasNext()) {
			SuperGraphEdge edge = it.next();

			if (edgeSet.contains(edge)) {
				SuperGraphNode outEdgeNode = edge.getOutEdgeNode();
				SuperGraphNode inEdgeNode = edge.getInEdgeNode();

				outEdgeNode.getOutgoingEdges().remove(inEdgeNode.getName());
				inEdgeNode.getIncomingEdges().remove(outEdgeNode.getName());

				this.getEdges().remove(edge);

			}

		}

	}

	public void deleteNodeFromGraph(SuperGraphNode node) throws Exception {
		//
		// Make a Vector of all the edges involving this node.
		HashSet<SuperGraphEdge> edgeSet = new HashSet<SuperGraphEdge>();
		edgeSet.addAll(node.getOutgoingEdges().values());
		edgeSet.addAll(node.getIncomingEdges().values());

		this.deleteEdgeSetFromGraph(edgeSet);
		this.getNodes().remove(node.getName());

	}

	public void setNodes(Map<String, SuperGraphNode> nodes) {
		this.nodes = nodes;
	}

	public Map<String, SuperGraphNode> getNodes() {
		return nodes;
	}

	public void setEdges(Set<SuperGraphEdge> edges) {
		this.edges = edges;
	}

	public Set<SuperGraphEdge> getEdges() {
		return edges;
	}

	public void setSubGraphNode(SuperGraphNode subGraphNode) {
		this.subGraphNode = subGraphNode;
	}

	public SuperGraphNode getSubGraphNode() {
		return subGraphNode;
	}

	/**
	 * Note that we are using a very old version of JGraphT to do this. There
	 * may well be a better library.
	 * 
	 * @return
	 */
	public UndirectedGraph<SuperGraphNode, DefaultEdge> dumpToJGraphT() {

		UndirectedGraph<SuperGraphNode, DefaultEdge> g =
	            new SimpleGraph<SuperGraphNode, DefaultEdge>
	            (DefaultEdge.class);

		Iterator<SuperGraphNode> nIt = this.nodes.values().iterator();
		while (nIt.hasNext()) {
			SuperGraphNode n = nIt.next();
			g.addVertex(n);
		}

		Iterator<SuperGraphEdge> eIt = this.edges.iterator();
		while (eIt.hasNext()) {
			SuperGraphEdge e = eIt.next();
			
			g.addEdge(e.getOutEdgeNode(), e.getInEdgeNode());
		}

		return g;

	}
	
	public List<SuperGraphEdge> readPath(SuperGraphNode s, SuperGraphNode t) throws Exception {

		List<SuperGraphEdge> edges = new ArrayList<SuperGraphEdge>();
		
		UndirectedGraph<SuperGraphNode, DefaultEdge> gg = this.dumpToJGraphT();

		DijkstraShortestPath<SuperGraphNode, DefaultEdge> dij = 
			new DijkstraShortestPath<SuperGraphNode, DefaultEdge>(
			gg, s, t);

		GraphPath<SuperGraphNode, DefaultEdge> path = dij.getPath();

		List<SuperGraphNode> ll = Graphs.getPathVertexList(dij.getPath());
		for(int i = 1; i<ll.size(); i++) {

			SuperGraphNode ss = ll.get(i-1);
			SuperGraphNode tt = ll.get(i);

			if(s==null)
				continue;
		
			SuperGraphEdge e = null;
			for( SuperGraphEdge e2 : s.getOutgoingEdges().values() ) {
				if( e2.getOutEdgeNode() == s && e2.getInEdgeNode() == t) {
					e = e2;
					break;
				}
			}
			if( e == null ) {
				for( SuperGraphEdge e2 : s.getIncomingEdges().values() ) {
					if( e2.getInEdgeNode() == s && e2.getOutEdgeNode() == t) {
						e = e2;
						break;
					}
				}
				if( e == null ) 
					return new ArrayList<SuperGraphEdge>();
			}
			edges.add(e);
		}

		return edges;
		
	}
	
	public SuperGraphTraversal generateTraversal() {
		this.traversal = new SuperGraphTraversal(this);
		traversal.traverseDependency();
		traversal.buildEdgeTraversal();

		return traversal;
	}
	
	public SuperGraphTraversal readTraversal() {
		
		if( this.traversal == null) 
			return this.generateTraversal();
		else 
			return traversal;
	
	}

	
	public void destroy() {
		subGraphNode.destroy();
		subGraphNode = null;
		nodes = null;
		edges = null;
	}

};
