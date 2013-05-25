package edu.isi.bmkeg.vpdmf.model.definitions;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.isi.bmkeg.uml.model.UMLclass;
import edu.isi.bmkeg.utils.superGraph.SuperGraph;
import edu.isi.bmkeg.utils.superGraph.SuperGraphNode;

/**
 * Timestamp: Thu_Jun_19_120936_2003;
 */

public class ViewGraphDefinition extends SuperGraph {

	static final long serialVersionUID = 6971285177790961159L;
	
	private static Logger logger = Logger.getLogger(ViewGraphDefinition.class);

	private VPDMf top;

	public VPDMf getTop() {
		return this.top;
	}

	public void setTop(VPDMf top) {
		this.top = top;
	}

	/**
	 * Identical - Every pv in $fromView can be found in $toView - Every pv in $toView can
	 * be found in $fromView - Both Views have the same primary primitive
	 * 
	 * Disconnected - None of the pvs in $fromView has any relationship with any
	 * of the pvs in $toView
	 * 
	 * Connected - The primary primitive of $fromView has a one-way connection
	 * with the primary primitive of $toView - Note that lookup Views may not be
	 * involved in 'Connected' ViewSpec Links
	 * 
	 * Interconnected - The primary primitive of $fromView has a reciprocal
	 * connection with the primary primitive of $toView
	 * 
	 * Superset - The pvs of $fromView have a superset or identical relationship
	 * with all of the pvs of $toView
	 * 
	 * Subset - All of the pvs of $fromView have a subset or identical
	 * relationship with all of the pvs of $fromView
	 * 
	 * Overlap - Some of the pvs of $fromView have a superset or identical
	 * relationship with all of the pvs of $toView
	 * 
	 * Interdependent ForwardDependent ReverseDependent
	 * 
	 * @throws Exception
	 */
	public void buildViewGraphDefinition() throws Exception {

		VPDMf top = this.top;
		
		Set<ViewDefinition> views = new HashSet<ViewDefinition>();
		Set<ViewDefinition> lookups = new HashSet<ViewDefinition>();
		
		//
		// Link the Views externally
		//
		Iterator<ViewDefinition> vdIt = this.getTop().getViews().values()
				.iterator();
		while (vdIt.hasNext()) {
			ViewDefinition vd = vdIt.next();

			if( vd.getType() == ViewDefinition.LOOKUP || 
					vd.getType() == ViewDefinition.SYSTEM ) {
				continue;
			} 
			
			this.addNode(vd);
			
			for( SuperGraphNode n : vd.getSubGraph().getNodes().values() ) {
				PrimitiveDefinition pd = (PrimitiveDefinition) n;
				UMLclass idClass = pd.readIdentityClass();
				
				if( vd.getPrimaryPrimitive().equals( pd )) 
					continue;
				
				//
				// Criteria for a primitive to be representative of a 
				// ViewDefinition is only that it contains the ViewTable  
				// class. 
				//
				// This indicates that there should be a link between views.
				//
				if( pd.getPrimaryClass().getBaseName().equals("ViewTable") ) {
					
					ViewDefinition linkedVd = null; 
					if( top.getViews().containsKey(idClass.getBaseName()) ) {
						linkedVd = top.getViews().get(idClass.getBaseName());
					} else {
						throw new Exception("Can't find " + idClass.getBaseName()  
								+ " as linked view in " + vd.getName() 
								+ " in the overall model");
					}
					
					ViewLink vl = new ViewLink(pd, linkedVd);
					this.addEdge(vd, linkedVd, vl);
					
					logger.debug( "Adding viewlink " + vl.toString() );
					
				}
				
			}		
				
		}
			
	}
	
}
