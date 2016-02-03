package edu.isi.bmkeg.utils.superGraph;

/**
 * Timestamp: Thu_Jun_19_120936_2003;
 */

import java.awt.Image;
import javax.swing.ImageIcon;
import java.io.InputStream;
import javax.imageio.ImageIO;

import java.io.Serializable;
import java.util.*;

public class SuperGraphNode implements Serializable {
	
	static final long serialVersionUID = -3211931052242144745L;

	private String name;
	private boolean displayable = true;
	private boolean deleteFlag = false;
	private String alias;
	private int tag;

	private Map<String, SuperGraphEdge> outgoingEdges = new HashMap<String, SuperGraphEdge>();
	private Map<String, SuperGraphEdge> incomingEdges = new HashMap<String, SuperGraphEdge>();
	private SuperGraph graph;
	private SuperGraph subGraph;

	public void setDisplayable(boolean isDisplayable) {
		this.displayable = isDisplayable;
	}

	public boolean isDisplayable() {
		return displayable;
	}

	public void setDeleteFlag(boolean deleteFlag) {
		this.deleteFlag = deleteFlag;
	}

	public boolean isDeleteFlag() {
		return deleteFlag;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public String getAlias() {
		return alias;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}
	
	public Map<String, SuperGraphEdge> getOutgoingEdges() {
		return outgoingEdges;
	}

	public Map<String, SuperGraphEdge> getIncomingEdges() {
		return incomingEdges;
	}

	public void setGraph(SuperGraph graph) {
		this.graph = graph;
	}

	public SuperGraph getGraph() {
		return this.graph;
	}

	public void setSubGraph(SuperGraph subGraph) {
		this.subGraph = subGraph;
	}

	public SuperGraph getSubGraph() {
		return this.subGraph;
	}

	public void destroy() {
		this.name = null;
		this.alias = null;
		this.outgoingEdges = null;
		this.incomingEdges = null;
		this.graph = null;
		this.subGraph = null;
	}

	public void setTag(int i) {
		this.tag = i;
	}

	public int getTag() {
		return this.tag;
	}

};
