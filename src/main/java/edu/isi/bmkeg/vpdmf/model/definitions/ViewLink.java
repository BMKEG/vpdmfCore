package edu.isi.bmkeg.vpdmf.model.definitions;

import java.util.Map;

import edu.isi.bmkeg.utils.superGraph.SuperGraphEdge;
import edu.isi.bmkeg.utils.superGraph.SuperGraphNode;

/**
 * Timestamp: Thu_Jun_19_120936_2003;
 */

public class ViewLink extends SuperGraphEdge {

	static final long serialVersionUID = 2719685622825995846L;
	
	private PrimitiveDefinition source;

	/**
	 * Constructing ViewLinks are based on the names of overlapping primitives,
	 * if the names of the primitives are the same then we create a link
	 * 
	 * @param vdSource
	 * @param vdTarget
	 * @throws Exception 
	 */
	public ViewLink(PrimitiveDefinition pd, ViewDefinition target) throws Exception {

		this.source = pd;
		this.setOutEdgeNode(pd.getView());
		this.setInEdgeNode(target);
		this.setName( pd.getName() );
		
	}
	
	public PrimitiveDefinition getSource() {
		return source;
	}

	public void setSource(PrimitiveDefinition source) {
		this.source = source;
	}

	/**
	 * Identical
	 * 
	 * - Every pv in $fromView can be found in $toView - Every pv in $toView can
	 * be found in $fromView - Both Views have the same primary primitive
	 */
	public static String IDENTICAL = "Identical";

	/**
	 * Disconnected - None of the pvs in $fromView has any relationship with any
	 * of the pvs in $toView
	 */
	public static String DISCONNECTED = "Disconnected";

	/**
	 * Connected - The primary primitive of $fromView has a one-way connection
	 * with the primary primitive of $toView - Note that lookup Views may not be
	 * involved in 'Connected' ViewSpec Links
	 */
	public static String CONNECTED = "Connected";

	/**
	 * Interconnected - The primary primitive of $fromView has a reciprocal
	 * connection with the primary primitive of $toView
	 */
	public static String INTERCONNECTED = "Interconnected";

	/**
	 * Superset - The pvs of $fromView have a superset or identical relationship
	 * with all of the pvs of $toView
	 */
	public static String SUPERSET = "Superset";

	/**
	 * Subset - All of the pvs of $fromView have a subset or identical
	 * relationship with all of the pvs of $fromView
	 */
	public static String SUBSET = "Subset";

	/**
	 * Overlap - Some of the pvs of $fromView have a superset or identical
	 * relationship with all of the pvs of $toView
	 */
	public static String OVERLAP = "Overlap";

	public static String INTERDEPENDENT = "Interdependent";
	public static String FORWARDDEPENDENT = "ForwardDependent";
	public static String REVERSEDEPENDENT = "ReverseDependent";
	
	public String toString() {
		return this.getOutEdgeNode().getName() + " ---" + this.getName() + "---> "
				+ this.getInEdgeNode().getName();
	}


	
};
