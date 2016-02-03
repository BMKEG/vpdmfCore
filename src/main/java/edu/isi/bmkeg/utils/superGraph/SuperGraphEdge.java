package edu.isi.bmkeg.utils.superGraph;

import java.io.Serializable;

public class SuperGraphEdge implements Serializable {

	static final long serialVersionUID = -3211931052242144787L;

	private String id;
	private String name;

	private boolean displayable = true;
	private boolean directed;
	private boolean deleteFlag = false;

	private SuperGraphNode outEdgeNode;
	private SuperGraph graph;
	private SuperGraphNode inEdgeNode;

	public void setOutEdgeNode(SuperGraphNode outEdgeNode) {
		this.outEdgeNode = outEdgeNode;
	}

	public SuperGraphNode getOutEdgeNode() {
		return this.outEdgeNode;
	}

	public void setGraph(SuperGraph graph) {
		this.graph = graph;
	}

	public SuperGraph getGraph() {
		return this.graph;
	}

	public void setInEdgeNode(SuperGraphNode inEdgeNode) {
		this.inEdgeNode = inEdgeNode;
	}

	public SuperGraphNode getInEdgeNode() {
		return this.inEdgeNode;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setDisplayable(boolean displayable) {
		this.displayable = displayable;
	}

	public boolean isDisplayable() {
		return displayable;
	}

	public void setDirected(boolean directed) {
		this.directed = directed;
	}

	public boolean isDirected() {
		return directed;
	}

	public void setDeleteFlag(boolean deleteFlag) {
		this.deleteFlag = deleteFlag;
	}

	public boolean isDeleteFlag() {
		return deleteFlag;
	}

	public void destroy() {
		id = null;
		name = null;

		outEdgeNode.destroy();
		graph.destroy();
		inEdgeNode.destroy();

		outEdgeNode = null;
		graph = null;
		inEdgeNode = null;
	}

};
