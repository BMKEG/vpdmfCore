package edu.isi.bmkeg.vpdmf.model.instances;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import edu.isi.bmkeg.uml.model.UMLattribute;
import edu.isi.bmkeg.uml.model.UMLclass;
import edu.isi.bmkeg.uml.model.UMLrole;
import edu.isi.bmkeg.utils.superGraph.SuperGraphEdge;
import edu.isi.bmkeg.utils.superGraph.SuperGraphNode;
import edu.isi.bmkeg.utils.superGraph.SuperGraphTraversal;
import edu.isi.bmkeg.vpdmf.model.definitions.ConditionElement;
import edu.isi.bmkeg.vpdmf.model.definitions.PrimitiveDefinition;
import edu.isi.bmkeg.vpdmf.model.definitions.PrimitiveDefinitionGraph;
import edu.isi.bmkeg.vpdmf.model.definitions.PrimitiveLink;
import edu.isi.bmkeg.vpdmf.model.definitions.VPDMf;
import edu.isi.bmkeg.vpdmf.model.definitions.ViewDefinition;

public class ViewBasedObjectGraph {

	private static Logger logger = Logger.getLogger(ViewBasedObjectGraph.class);

	private VPDMf top;

	private String viewName;

	private ViewInstance vi;

	private ClassLoader cl;

	private Map<String, Object> objMap;
	
	private Map<Integer,String> sortAddr = new HashMap<Integer,String>();
	
	private Set<SuperGraphNode> visitedView = new HashSet<SuperGraphNode>();
	private Set<SuperGraphEdge> visitedLinks = new HashSet<SuperGraphEdge>();

	private Map<Object, Integer> indexLookup = new HashMap<Object, Integer>();

	public ViewBasedObjectGraph(VPDMf top, ClassLoader cl, String viewName)
			throws Exception {

		this.top = top;
		this.viewName = viewName;
		this.cl = cl;

	}	
	
	public Map<String, Object> viewToObjectGraph(ViewInstance vi)
			throws Exception {
		
		if (!vi.getDefName().equals(this.viewName)) {

			ViewDefinition thisVd = vi.getDefinition().getTop().getViews().get(this.viewName);
			ViewDefinition thatVd = vi.getDefinition();

			if( !thatVd.checkIsAChildOf(thisVd) ) {
				throw new Exception("Can't build a " + this.viewName
						+ " ViewBasedObjectGraph " + "from a " + vi.getDefName()
						+ " ViewInstance");
			}
			
		}

		this.vi = vi;

		PrimitiveInstanceGraph pig = (PrimitiveInstanceGraph) vi.getSubGraph();
		SuperGraphTraversal pigTraversal = pig.readTraversal();

		this.objMap = new HashMap<String, Object>();
		this.visitedView = new HashSet<SuperGraphNode>();
		this.visitedLinks = new HashSet<SuperGraphEdge>();
		
		Iterator<SuperGraphNode> pIt = pigTraversal.nodeTraversal.iterator();
		while (pIt.hasNext()) {
			PrimitiveInstance pi = (PrimitiveInstance) pIt.next();
			PrimitiveDefinition pd = pi.getDefinition();

			UMLclass umlClass = pd.getClasses().get(pd.getClasses().size() - 1);
			Class c = cl.loadClass(umlClass.readClassAddress());
						
			Object o = c.newInstance();

			objMap.put(pi.getName(), o);
		}

		pIt = pigTraversal.nodeTraversal.iterator();
		while (pIt.hasNext()) {
			PrimitiveInstance pi = (PrimitiveInstance) pIt.next();

			Object o = this.objMap.get(pi.getName());
			
			primitiveToObject(pi, o, false);

		}

		return objMap;

	}
	
	public void primitiveToObject(PrimitiveInstance pi, Object o, boolean keysOnly) throws Exception {

		// ____________________________________________________________________________________
		// Makes sure we don't trace back over the graph and re-examine
		// primitive definitions
		this.visitedView.add(pi);

		if( pi.getDefinition().isNullable() && pi.isNull() ) {
			return;
		} else if(pi.isNull()) {
			throw new Exception(pi.getName() + " is null and not nullable.");
		}
		
		PrimitiveInstanceGraph pig = (PrimitiveInstanceGraph) pi.getGraph();
		ViewInstance vi = (ViewInstance) pig.getSubGraphNode();

		Map<String, Method> methods = this.getMethodsLookup(o.getClass());

		Iterator<ClassInstance> ciIt = pi.getObjects().values().iterator();
		while (ciIt.hasNext()) {
			ClassInstance ci = ciIt.next();
			UMLclass cd = ci.getDefinition();

			Iterator<String> attrNameIt = ci.attributes.keySet().iterator();
			ATTLOOP: while (attrNameIt.hasNext()) {
				String attrName = attrNameIt.next();

				AttributeInstance ai = ci.attributes.get(attrName);
				UMLattribute ad = ai.getDefinition();

				// __________________________
				// add all the available data
				String stem = attrName.substring(0, 1).toUpperCase()
						+ attrName.substring(1, attrName.length());
				String setterName = "set" + stem;
			
				Method m = methods.get(setterName);
				Object value = ai.getValue();
				
				if( keysOnly && m != null && value != null && 
						ai.getConnectedKeys().size() > 0) {					
					m.invoke(o, value);
				} else if (m != null && value != null) {
					m.invoke(o, value);
				}

			}

		}
		
		if( keysOnly )
			return;

		Set<SuperGraphEdge> pls = new HashSet<SuperGraphEdge>();
		pls.addAll(pi.getOutgoingEdges().values());
		pls.addAll(pi.getIncomingEdges().values());
		Iterator<SuperGraphEdge> plIt = pls.iterator();

		while (plIt.hasNext()) {

			PrimitiveLinkInstance pli = (PrimitiveLinkInstance) plIt.next();
			PrimitiveInstance remotePi = (PrimitiveInstance) pli
					.getInEdgeNode();

			if (remotePi == pi) {
				remotePi = (PrimitiveInstance) pli.getOutEdgeNode();
			}

			UMLrole r = pli.getPVLinkDef().getRole();
			String s1 = r.getDirectClass().getClassAddress().substring(2);

			// Hack to make sure the backwards role
			// can be detected even if it involves a 
			// superclass of the appropriate object. 
			Set<String> oClassNames = new HashSet<String>();
			oClassNames.add(o.getClass().getName());
			Class cc = o.getClass();
			while( cc.getSuperclass() != null ) {
				oClassNames.add(cc.getSuperclass().getName());
				cc = cc.getSuperclass();	
			}
			oClassNames.remove("java.lang.Object");
			oClassNames.remove("edu.isi.bmkeg.vpdmf.model.ViewTable");
			
				
			if (oClassNames.contains(s1)) {
				r = r.otherRole();
			}

			if (!r.getNavigable()) {
				continue;
			}

			String roleName = r.getBaseName();

			String setterName = "set" + roleName.substring(0, 1).toUpperCase()
					+ roleName.substring(1, roleName.length());
			String getterName = "get" + roleName.substring(0, 1).toUpperCase()
					+ roleName.substring(1, roleName.length());

			Method getter = methods.get(getterName);
			Method setter = methods.get(setterName);

			Object remoteObj = this.objMap.get(remotePi.getName());

			String s = remotePi.getName();
			String piIndex = s.substring(s.lastIndexOf("_") + 1, s.length());
			Integer ii = new Integer(piIndex);

			if (r.getMult_upper() == -1) {

				List l = (List) getter.invoke(o);

				while (l.size() < ii + 1) {
					l.add(new Object());
				}

				// Need to set the values to make sure the primitive order is
				// preserved.
				l.set(ii, remoteObj);

			} else {

				setter.invoke(o, remoteObj);

			}

		}

	}

	
	public ViewInstance objectGraphToView(Object primaryPrimitiveObject)
			throws Exception {

		return this.objectGraphToView(primaryPrimitiveObject, true);
	
	}
	
	/**
	 * Builds an ViewInstance from an object denoting a primary primitive in a
	 * view
	 */
	public ViewInstance objectGraphToView(Object primaryPrimitiveObject, 
			boolean buildIndexes)
			throws Exception {

		this.objMap = new HashMap<String, Object>();
		this.visitedView = new HashSet<SuperGraphNode>();
		this.visitedLinks = new HashSet<SuperGraphEdge>();
		
		ViewDefinition vd = top.getViews().get(this.viewName);
		ViewInstance vi = new ViewInstance(vd);
		this.indexLookup = new HashMap<Object, Integer>();

		PrimitiveDefinition ppd = vd.getPrimaryPrimitive();
		UMLclass cd = ppd.readIdentityClass();
		String stem = "]" + ppd.getName() + "|" + cd.getBaseName() + ".";

		String cdPath = cd.getClassAddress();
		cdPath = cdPath.substring(2);

		String cPath = primaryPrimitiveObject.getClass().getName();

		if (!cdPath.equals(cPath)) {
			logger.debug("Mismatch between primary ViewSpec-Primitive-Class name:"
							+ cdPath + " and object of type: " + cPath);
		}

		PrimitiveInstance ppi = vi.getPrimaryPrimitive();

		this.visitedLinks = new HashSet<SuperGraphEdge>();

		this.indexLookup.put(primaryPrimitiveObject, 0);

		objectToPrimitive(primaryPrimitiveObject, ppi);

		vi.fillInExtraPrimitiveLinks();

		vi.reconstructPiTotals();
		
		if( buildIndexes ) {
			vi.updateIndexes();
		}
		
		return vi;

	}

	public void objectToPrimitive(Object o, PrimitiveInstance pi)
			throws Exception {
		this.objectToPrimitive(o, pi, 0);
	}
	
	
	public void objectToPrimitive(Object o, PrimitiveInstance pi, int order)
			throws Exception {

		if (o == null)
			return;
		
		if( this.objMap.containsKey(pi.getName()))
			return;
		
		this.objMap.put(pi.getName(), o);

		// ____________________________________________________________________________________
		// Makes sure we don't trace back over the graph and re-examine
		// primitive definitions
		//
		PrimitiveDefinition pd = pi.getDefinition();

		// ____________________________________________________________________________________
		// Some values are set by default in java objects that screws up the
		// views,
		// so skip 'em
		// - condition elements
		// - primary keys
		//
		Set<UMLattribute> attributesToSkip = new HashSet<UMLattribute>();
		ViewDefinition vd = pd.getView();

		Iterator<ConditionElement> cdIt = pd.getConditionElements().iterator();
		while (cdIt.hasNext()) {
			ConditionElement ce = cdIt.next();
			String addr = "]" + pd.getName() + "|" + ce.getClassName() + "."
					+ ce.getAttName();
			UMLattribute at = vd.readAttributeDefinition(addr);
			attributesToSkip.add(at);
		}

		PrimitiveInstanceGraph pig = (PrimitiveInstanceGraph) pi.getGraph();
		List<PrimitiveInstance> piList = pig.readPrimitivesToTarget(pi);
		PrimitiveDefinitionGraph pdg = pig.getDefinition();
		ViewInstance vi = (ViewInstance) pig.getSubGraphNode();

		Map<String, Method> methods = this.getMethodsLookup(o.getClass());

		String sortRegex = "<vpdmf-sort-(\\d+)>";
		Pattern sortPatt = Pattern.compile(sortRegex);
		String revsortRegex = "<vpdmf-rev-sort-(\\d+)>";
		Pattern revsortPatt = Pattern.compile(revsortRegex);

		Iterator<ClassInstance> ciIt = pi.getObjects().values().iterator();
		while (ciIt.hasNext()) {
			ClassInstance ci = ciIt.next();
			UMLclass cd = ci.getDefinition();

			Iterator<String> attrNameIt = ci.attributes.keySet().iterator();
			while (attrNameIt.hasNext()) {
				String attrName = attrNameIt.next();

				AttributeInstance ai = ci.attributes.get(attrName);
				UMLattribute ad = ai.getDefinition();
					
				if( ad.getBaseName().equals("vpdmfOrder") ) {
					ai.setValue(order);
				}
				
				if( !ad.getDesigned() ) 
					continue;
				
				// __________________________
				// add all the available data
				String getterName = "get"
						+ attrName.substring(0, 1).toUpperCase()
						+ attrName.substring(1, attrName.length());
				Method m = methods.get(getterName);
				if (m == null) {
					logger.debug("No method " + o.getClass().getSimpleName() +
						"." + getterName);
					continue;
				}
				
				Object value = m.invoke(o);

				// Don't overwrite conditions
				// (these do not get instantiated within the object model)
				if ( attributesToSkip.contains(ai.getDefinition()) ) {
					continue;
				}

				// checks for key values
				// - if any PK or FK is set to zero, leave them alone
				// otherwise, update them
				if( ai.getConnectedKeys().size() > 0 ) {
					if( value instanceof Long ) {
						Long l = (Long) value;
						if(l==0) {
							continue;
						}
					}
				}
				
				if( ad.getType().isDataType() && value != null) {
					
					//
					// provide a mechanism to perform -or- & -and- 
					// queries for a single variable from a view.
					//
					if( value instanceof String ) {
						String strValue = (String) value; 
						
						if( strValue.contains("<vpdmf-sort") ) {
					
							// Are any attributes set with <vpdmf-sort-XX> values,
							// where XX is set to a number. This is the sort mechanism
							// for list queries.
							Matcher matcher = sortPatt.matcher(strValue); 
							if( matcher.find() ) {
								Integer key = new Integer(matcher.group(1));
								getSortAddr().put(key, ai.getAddress() );
								strValue = strValue.replaceAll(sortRegex, "");
								ai.writeValueString(strValue+"");		
							}
					
						} else if( strValue.contains("<vpdmf-rev-sort") ) {
					
							// Are any attributes set with <vpdmf-sort-XX> values,
							// where XX is set to a number. This is the sort mechanism
							// for list queries.
							Matcher matcher = revsortPatt.matcher(strValue); 
							if( matcher.find() ) {
								Integer key = new Integer(matcher.group(1));
								getSortAddr().put(key, "-" + ai.getAddress() );
								strValue = strValue.replaceAll(revsortRegex, "");
								ai.writeValueString(strValue+"");		
							}
					
						} else {
							
							ai.writeValueString(value+"");													
						
						}
						
					} else if( ad.getType().getBaseName().equals("blob") ||
							ad.getType().getBaseName().equals("image")) { 
					
						ai.setValue(value);												

					} else {

						ai.writeValueString(value+"");						
					
					}
					
				} 
								 
			}

		}

		Set<SuperGraphEdge> pls = new HashSet<SuperGraphEdge>();
		pls.addAll(pi.getDefinition().getOutgoingEdges().values());
		pls.addAll(pi.getDefinition().getIncomingEdges().values());
		Iterator<SuperGraphEdge> plIt = pls.iterator();

		LINKS: while (plIt.hasNext()) {

			PrimitiveLink pl = (PrimitiveLink) plIt.next();
			PrimitiveDefinition remotePv = (PrimitiveDefinition) pl
					.getInEdgeNode();
			boolean forwardFlag = true;

			if (remotePv == pi.getDefinition()) {
				remotePv = (PrimitiveDefinition) pl.getOutEdgeNode();
				forwardFlag = false;
			}

			// Roles in PvLinks are always in the direction 
			// specified in the ViewSpec Definition Document
			UMLrole r = pl.getRole();
			
			String roleName = r.getBaseName();
			String getterName = "get" + roleName.substring(0, 1).toUpperCase()
					+ roleName.substring(1, roleName.length());
			Method m = methods.get(getterName);

			// Role names for PVLinks always are directed 
			// according to the specs. Thus the specs dictate the 
			// traversal over the graph and reverse edges
			// will have no corresponding link (unless they 
			// have the same name at each end of the same relation).
			if(m==null) {
				continue;
			}
			
			Object value = m.invoke(o);

			int remoteCount = vi.countPrimitives(remotePv);
			
			if (value instanceof List) {
				List l = (List) value;
				for (int i = 0; i < l.size(); i++) {
					Object remoteObject = l.get(i);

					if( remoteObject == null)
						continue LINKS;
					
					String cName = remoteObject.getClass().getSimpleName();
					String rTargetName = r.getDirectClass().getBaseName();
					//if( !cName.equals(rTargetName) ) {
					//	continue LINKS;
					//}
					
					if (remoteCount == 1 && i == 0) {
						PrimitiveInstance remotePi = (PrimitiveInstance) pig
								.getNodes().get(remotePv.getName() + "_0");
						this.objectToPrimitive(remoteObject, remotePi, i);
						
					}
					// spawn a new primitive and use that...
					else {

						int newI = remoteCount + i - 1;
						vi.addNewPrimitiveRecursively(pl, pi, remotePv, newI,
								forwardFlag);
						PrimitiveInstance remotePi = (PrimitiveInstance) vi
								.readPrimitiveInstance("]" + remotePv.getName(), newI);
						this.objectToPrimitive(remoteObject, remotePi, i);

					}
					

				}
			} else {

				// Need to check to see what cardinality
				// of the primitive link is. This is based on
				// a traversal of the PDG from the primary
				// primitive to the target.

				int pIndex = pi.readIndex();

				Object remoteObject = value;

				if( remoteObject == null)
					continue LINKS;
				
//				String cName = remoteObject.getClass().getSimpleName();
//				String rTargetName = r.getDirectClass().getBaseName();
//				if( !cName.equals(rTargetName) ) {
//					continue LINKS;
//				}

				//
				// BUG: the forward-spawning of new primitives does not work when 
				// the links go back to itself. Not sure how to fix this in the long term.
				//
				if( !pig.getNodes().containsKey(remotePv.getName() + "_" + pIndex) ) {
					vi.addNewPrimitiveRecursively(pl, pi, remotePv, pIndex, false);
				}

				PrimitiveInstance remotePi = (PrimitiveInstance) pig.getNodes()
						.get(remotePv.getName() + "_" + pIndex);
				
				
				this.objectToPrimitive(remoteObject, remotePi);

			}

		}

	}

	private Map<String, Method> getMethodsLookup(Class c) {
		Map<String, Method> methods = new HashMap<String, Method>();
		Method mArray[] = c.getMethods();
		for (int i = 0; i < mArray.length; i++) {
			Method m = mArray[i];
			String mName = m.getName();
			methods.put(mName, m);
		}
		return methods;
	}

	public Object readPrimaryObject() throws Exception {

		if (this.objMap.size() == 0) {
			if (this.vi == null)
				throw new Exception("ViewBasedObjectGraph is empty");
			else
				this.objMap = this.viewToObjectGraph(this.vi);
		}

		ViewDefinition vd = top.getViews().get(this.viewName);
		PrimitiveDefinition pd = vd.getPrimaryPrimitive();

		Object o = this.objMap.get(pd.getName() + "_0");

		return o;

	}

	public Map<String, Object> getObjMap() {
		return objMap;
	}

	public void setObjMap(Map<String, Object> objMap) {
		this.objMap = objMap;
	}

	public Map<Integer,String> getSortAddr() {
		return sortAddr;
	}

	public void setSortAddr(Map<Integer,String> sortAddr) {
		this.sortAddr = sortAddr;
	}

}
