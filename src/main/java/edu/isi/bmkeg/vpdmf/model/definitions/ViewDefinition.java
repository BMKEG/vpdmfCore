package edu.isi.bmkeg.vpdmf.model.definitions;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

import edu.isi.bmkeg.uml.model.UMLattribute;
import edu.isi.bmkeg.uml.model.UMLclass;
import edu.isi.bmkeg.uml.model.UMLmodel;
import edu.isi.bmkeg.uml.model.UMLpackage;
import edu.isi.bmkeg.uml.model.UMLrole;
import edu.isi.bmkeg.utils.superGraph.SuperGraphEdge;
import edu.isi.bmkeg.utils.superGraph.SuperGraphNode;
import edu.isi.bmkeg.vpdmf.exceptions.AttributeAddressException;
import edu.isi.bmkeg.vpdmf.exceptions.VPDMfException;
import edu.isi.bmkeg.vpdmf.model.definitions.specs.PrimitiveLinkSpec;
import edu.isi.bmkeg.vpdmf.model.definitions.specs.PrimitiveSpec;
import edu.isi.bmkeg.vpdmf.model.definitions.specs.ViewLinkSpec;
import edu.isi.bmkeg.vpdmf.model.definitions.specs.ViewSpec;

/**
 * 
 * <p>
 * Title: ViewDefinition
 * </p>
 * 
 * <p>
 * Description: This class encapsulates the design of a view within the system.
 * A view is a multi-object entity that has an individual identity as a
 * standalone thing. The view encapsulates data from all constituitive tables
 * and objects to represent that.
 * </p>
 * 
 * <p>
 * Copyright: Copyright (c) 2004
 * </p>
 * 
 * <p>
 * Company: University of Southern California
 * </p>
 * 
 * @author Gully APC Burns
 * @version 1.28
 */

//
// Change log:
// 06/23/05 - refactoring and documenting,
// (1) removed expanded/reduced views.
// (2) removed isEnclosedBy attribute
// (3) removed isIsolatedViewDefinition attribute
//

public class ViewDefinition extends SuperGraphNode {

	static final long serialVersionUID = 8047039304729208683L;

	/**
	 * The views type, could take any one value from DATA, SYSTEM, LOOKUP, LINK,
	 * COLLECTION or EXTERNAL (see below for explanation)
	 */
	private int type;

	/**
	 * A view of type DATA is a standard data view. This appears in the
	 * viewDefinitionGraph and may be manipulated directly by users.
	 */
	public static int DATA = 0;

	/**
	 * A view of type SYSTEM is a view generated and used by the system to keep
	 * track of user information and other details. These views are not directly
	 * accessible to users but are manipulated by the application.
	 */
	public static int SYSTEM = 1;

	/**
	 * A view of type LOOKUP is typically a lightweight view that permits DATA
	 * views to be retrieved by vpdmfLabel. This permits lightweight controls
	 * (such as lookup boxes) to access them
	 */
	public static int LOOKUP = 2;

	/**
	 * A view of type LINK has a set structure and is used to query the
	 * knowledge base for links between DATA views. The primary primitive of
	 * this view is always based around the the ViewLinkTable of the system.
	 * These views may also contain quite complex information.
	 */
	public static int LINK = 3;

	/**
	 * A view of type COLLECTION is treated by the system like a DATA view in
	 * every way, except that it typically contains DATA views. This is a
	 * semantic difference and may be represented explicitly in the system at a
	 * future time.
	 */
	public static int COLLECTION = 4;

	/**
	 * A view of type EXTERNAL is supplied by an external source or a reference
	 * ontology. It is not usually editable.
	 */
	public static int EXTERNAL = 5;

	/**
	 * Relations between this view and other views
	 */
	private HashSet<ViewDefinition> relations = new HashSet<ViewDefinition>();

	/**
	 * The index elements used to keep track of this view
	 */
	private Map<Integer, IndexElement> indexElements = new HashMap<Integer, IndexElement>();

	/**
	 * Dependencies between this view and other views
	 */
	private HashSet<ViewDefinition> dependencies = new HashSet<ViewDefinition>();

	/**
	 * A multiple inheritence mechanism for this view
	 */
	private Set<ViewDefinition> isa = new HashSet<ViewDefinition>();

	//
	// Types of ViewDefinition
	//
	private static int IN = 4;
	private static int OUT = 5;
	private static int LINKS = 6;
	private static int RELATIONS = 7;
	private static int BOTH = 8;

	/**
	 * XML specification string
	 */
	private String specification;
	
	/**
	 * XML-derived java specs
	 */
	private ViewSpec spec;

	/**
	 * format of the index string from the xml spec
	 */
	private String vpdmfLabelFormat = "";
	
	/**
	 * the attribute that acts as the vpdmfLabel for this view
	 */
	private UMLattribute vpdmfLabel;

	/**
	 * format of the uri for the view from the xml spec
	 */
	private String vpdmfUriFormat = "";

	/**
	 * the attribute that acts as the vpdmfLabel for this view
	 */
	private UMLattribute vpdmfUri;
	
	/**
	 * Can this view be edited?
	 */
	private boolean editable = true;

	/**
	 * The written documentation for this view
	 */
	private String documentation;

	/**
	 * The parent of this view
	 */
	private ViewDefinition parent;

	/**
	 * An alternate name (used in relations to indicate the name of the reverse
	 * relation)
	 */
	private String altName;

	/**
	 * The VPDM object that describes the whole model being used
	 */
	private VPDMf top;

	/**
	 * The primary primitive of this view
	 */
	private PrimitiveDefinition primaryPrimitive;

	public ViewDefinition() {
		super();
		PrimitiveDefinitionGraph pdg = new PrimitiveDefinitionGraph();
		this.setSubGraph(pdg);
		pdg.setSubGraphNode(this);
	}

	public boolean isDependentOn(ViewDefinition vd) {

		if (this.getDependencies().contains(vd))
			return true;

		Iterator parentIt = vd.readAllParents().iterator();
		while (parentIt.hasNext()) {
			ViewDefinition parent = (ViewDefinition) parentIt.next();
			if (this.getDependencies().contains(parent))
				return true;
		}

		return false;

	}

	public UMLattribute readAttributeDefinition(String attributeAddress)
			throws Exception {

		PrimitiveDefinition pd = null;
		UMLclass classDef = null;
		UMLattribute attr = null;

		//
		// Check the format of the attributeAddress
		//
		int dotPos = attributeAddress.lastIndexOf('.');
		int barPos = attributeAddress.lastIndexOf('|');
		int brackPos = attributeAddress.lastIndexOf(']');

		if (dotPos == -1 || barPos == -1 || brackPos == -1) {
			throw new AttributeAddressException(attributeAddress
					+ " is badly formed\n");
		}

		if (brackPos == -1) {

			String attributeName = attributeAddress.substring(dotPos + 1);
			String classPath = attributeAddress.substring(barPos + 1, dotPos);

			UMLmodel model = this.getTop().getUmlModel();

			//
			// ii) Class
			//
			if (model.listAllClasses().containsKey(classPath)) {
				classDef = model.listAllClasses().get(classPath);
			} else {
				throw new AttributeAddressException("Can't find "
						+ attributeAddress + ", no such class\n");
			}

			//
			// iii) Attribute
			Iterator<UMLattribute> atIt = classDef.getAttributes().iterator();
			while (atIt.hasNext()) {
				UMLattribute at = atIt.next();
				if (at.getBaseName().equals(attributeName))
					attr = at;
			}
			if (attr == null) {
				throw new AttributeAddressException("Can't find "
						+ attributeAddress + ", no such attribute\n");
			}

		} else {

			String attributeName = attributeAddress.substring(dotPos + 1);
			String className = attributeAddress.substring(barPos + 1, dotPos);
			String primitiveName = attributeAddress.substring(brackPos + 1,
					barPos);

			//
			// Check for the existence of the attributeAddress
			// i) Primitive Definition
			String pvIndex = primitiveName;
			if (this.getSubGraph().getNodes().containsKey(primitiveName)) {
				pd = (PrimitiveDefinition) this.getSubGraph().getNodes()
						.get(pvIndex);
			} else {
				throw new AttributeAddressException("Can't find "
						+ attributeAddress + ", no such primitive at index 0\n");
			}

			//
			// ii) Class
			classDef = pd.lookupClassByName(className);
			if (classDef == null) {
				throw new AttributeAddressException("Can't find "
						+ attributeAddress + ", no such class\n");
			}

			//
			// iii) Attribute
			Iterator<UMLattribute> atIt = classDef.getAttributes().iterator();
			while (atIt.hasNext()) {
				UMLattribute at = atIt.next();
				if (at.getBaseName().equals(attributeName))
					attr = at;
			}
			if (attr == null) {
				throw new AttributeAddressException("Can't find "
						+ attributeAddress + ", no such attribute\n");
			}

		}

		return attr;

	}

	public ArrayList<ViewDefinition> readChildren() {
		ArrayList<ViewDefinition> children = new ArrayList<ViewDefinition>();

		Iterator<ViewDefinition> vdIt = this.getTop().getViews().values()
				.iterator();
		while (vdIt.hasNext()) {
			ViewDefinition vd = vdIt.next();

			if (vd.checkIsAChildOf(this)) {
				children.add(vd);
			}

		}

		return children;

	}

	public Vector readAllClasses() {
		Vector classes = new Vector();
		Iterator pdIt = this.getSubGraph().getNodes().values().iterator();
		while (pdIt.hasNext()) {
			PrimitiveDefinition pd = (PrimitiveDefinition) pdIt.next();
			classes.addAll(pd.getClasses());
		}
		return classes;
	}

	public List<ViewDefinition> readAllParents() {
		List<ViewDefinition> parents = new ArrayList<ViewDefinition>();
		Iterator<ViewDefinition> vdIt = this.getTop().getViews().values().iterator();
		while (vdIt.hasNext()) {
			ViewDefinition vd = vdIt.next();
			if (this.checkIsAChildOf(vd)) {
				parents.add(vd);
			}
		}
		return parents;
	}

	public Vector<ViewDefinition> computeAllChildren() {
		Vector<ViewDefinition> children = new Vector<ViewDefinition>();
		Iterator vdIt = this.getTop().getViews().values().iterator();
		while (vdIt.hasNext()) {
			ViewDefinition vd = (ViewDefinition) vdIt.next();
			if (vd.checkIsAChildOf(this)) {
				children.add(vd);
			}
		}
		return children;
	}

	public List<ViewDefinition> computeAllSuperTypes() throws Exception {
		List<ViewDefinition> parents = new ArrayList<ViewDefinition>();
		Iterator<ViewDefinition> vdIt = this.getTop().getViews().values().iterator();
		while (vdIt.hasNext()) {
			ViewDefinition vd = vdIt.next();
			if (this.checkIsA(vd)) {
				parents.add(vd);
			}
		}
		return parents;
	}

	public List<ViewDefinition> computeAllSubTypes() throws Exception {
		List<ViewDefinition> children = new ArrayList<ViewDefinition>();
		Iterator<ViewDefinition> vdIt = this.getTop().getViews().values().iterator();
		while (vdIt.hasNext()) {
			ViewDefinition vd = (ViewDefinition) vdIt.next();
			if (vd.checkIsA(this)) {
				children.add(vd);
			}
		}
		return children;
	}

	public boolean checkIsAChildOf(ViewDefinition thatVd) {

		if (this.equals(thatVd))
			return false;

		ViewDefinition thisVd = this;

		while (thisVd != null) {
			if (thisVd.equals(thatVd))
				return true;
			thisVd = thisVd.getParent();
		}

		return false;

	}

	public boolean checkIsA(ViewDefinition thatVd) throws Exception {

		ViewDefinition thisVd = this;

		List<ViewDefinition> vec = new ArrayList<ViewDefinition>(thisVd.getIsa());
		HashSet<ViewDefinition> seen = new HashSet<ViewDefinition>();

		while (vec.size() > 0) {
			thisVd = vec.get(0);
			vec.remove(0);

			if (seen.contains(thisVd))
				continue;
			seen.add(thisVd);

			if (thisVd.equals(thatVd))
				return true;

			vec.addAll(thisVd.getIsa());

		}

		return false;

	}
	
	public Set<ViewLink> readAllLinkedViewLinksVector(boolean dependentsOK)
			throws Exception {
 
		return new HashSet<ViewLink>(this.readAllLinkedViewDefinitions(dependentsOK).values());
		
	}

	public Set<ViewDefinition> readAllLinkedViewDefinitionsVector(boolean dependentsOK)
			throws Exception {
		
		return this.readAllLinkedViewDefinitions(dependentsOK).keySet();

	}

	public Map<ViewDefinition, ViewLink> readInputViewDefinitions(boolean dependentsOK)
			throws Exception {

		return readLinkedAndRelatedViewDefinitions(IN, BOTH, dependentsOK);

	}

	public Map<ViewDefinition, ViewLink> readOutputViewDefinitions(boolean dependentsOK)
			throws Exception {

		return readLinkedAndRelatedViewDefinitions(OUT, BOTH, dependentsOK);

	}

	public Map<ViewDefinition, ViewLink> readAllLinkedViewDefinitions(boolean dependentsOK)
			throws Exception {

		return this.readLinkedAndRelatedViewDefinitions(BOTH, LINKS,
				dependentsOK);

	}

	public Map<ViewDefinition, ViewLink> readOutputLinkedViewDefinitions(boolean dependentsOK)
			throws Exception {

		return readLinkedAndRelatedViewDefinitions(OUT, LINKS, dependentsOK);

	}

	public Map<ViewDefinition, ViewLink> readInputLinkedViewDefinitions(boolean dependentsOK)
			throws Exception {

		return readLinkedAndRelatedViewDefinitions(IN, LINKS, dependentsOK);

	}

	public List<ViewLink> readAllRelatedViewLinksVector(boolean dependentsOK)
			throws Exception {

		return new ArrayList<ViewLink>(this.readAllRelatedViewDefinitions(dependentsOK).values());
	}

	public List<ViewDefinition> readAllRelatedViewDefinitionsVector(boolean dependentsOK)
			throws Exception {

		return new ArrayList<ViewDefinition>(this.readAllRelatedViewDefinitions(dependentsOK).keySet());

	}

	public Map<ViewDefinition, ViewLink> readAllRelatedViewDefinitions(boolean dependentsOK)
			throws Exception {

		return this.readLinkedAndRelatedViewDefinitions(BOTH, RELATIONS,
				dependentsOK);

	}

	public Map<ViewDefinition, ViewLink> readOutputRelatedViewDefinitions(boolean dependentsOK)
			throws Exception {

		return readLinkedAndRelatedViewDefinitions(OUT, RELATIONS, dependentsOK);

	}

	public Map<ViewDefinition, ViewLink> readInputRelatedViewDefinitions(boolean dependentsOK)
			throws Exception {

		return readLinkedAndRelatedViewDefinitions(IN, RELATIONS, dependentsOK);

	}

	private Map<ViewDefinition, ViewLink> readLinkedAndRelatedViewDefinitions(int inOutBothCode,
			int linksRelationBothCode, boolean dependentsOK) throws Exception {

		Map<ViewDefinition, ViewLink> linkedViews = new HashMap<ViewDefinition, ViewLink>();

		//
		// Get all the parents / supertypes of this node...
		//
		Set<ViewDefinition> vwSet = new HashSet<ViewDefinition>();
		vwSet.add(this);

		//
		// BugFix:
		//
		// Need to inherit all possible things when calculating this
		//
		if (linksRelationBothCode == LINKS) {
			vwSet.addAll(this.readAllParents());
		} else if (linksRelationBothCode == RELATIONS) {
			vwSet.addAll(this.computeAllSuperTypes());
			vwSet.addAll(this.readAllParents());
		} else if (linksRelationBothCode == BOTH) {
			vwSet.addAll(this.readAllParents());
			vwSet.addAll(this.computeAllSuperTypes());
		}

		//
		// For each parent / supertype...
		// - get appropriate view links
		//
		Iterator<ViewDefinition> it = vwSet.iterator();
		while (it.hasNext()) {
			ViewDefinition vd = it.next();
			Set<SuperGraphEdge> vlHs = new HashSet<SuperGraphEdge>();

			if (inOutBothCode == IN || inOutBothCode == BOTH) {
				vlHs.addAll(vd.getIncomingEdges().values());
			}
			if (inOutBothCode == OUT || inOutBothCode == BOTH) {
				vlHs.addAll(vd.getOutgoingEdges().values());
			}

			Iterator<SuperGraphEdge> vlIt = vlHs.iterator();
			while (vlIt.hasNext()) {
				ViewLink vl = (ViewLink) vlIt.next();

				ViewDefinition toEdgeNode = (ViewDefinition) vl.getInEdgeNode();
				ViewDefinition fromEdgeNode = (ViewDefinition) vl
						.getOutEdgeNode();

				ViewDefinition thatNode = null;
				boolean forwardFlag;
				if (vd.equals(toEdgeNode)) {
					thatNode = fromEdgeNode;
					forwardFlag = false;
				} else if (vd.equals(fromEdgeNode)) {
					thatNode = toEdgeNode;
					forwardFlag = true;
				} else {
					throw new VPDMfException("oops");
				}

				//
				// Filter the edges for links / relations
				// CHANGE: TRYING TO ADD THIS PERMITTING RELATIONS TO PASS HERE
				// FOR THE DETECTION OF RELATION VIEWS
				//
				int t = thatNode.getType();
				if ((linksRelationBothCode == RELATIONS && t != ViewDefinition.LINK)
						|| (linksRelationBothCode == LINKS && (t != ViewDefinition.DATA
								&& t != ViewDefinition.COLLECTION && t != ViewDefinition.EXTERNAL))) {
					continue;
				}

				linkedViews.put(thatNode, vl);

				HashSet<ViewDefinition> vw2Set = new HashSet<ViewDefinition>();
				if (linksRelationBothCode == LINKS) {
					vw2Set.addAll(thatNode.computeAllChildren());
				} else if (linksRelationBothCode == RELATIONS) {
					vw2Set.addAll(thatNode.computeAllSubTypes());
				} else if (linksRelationBothCode == BOTH) {
					vw2Set.addAll(thatNode.computeAllChildren());
					vw2Set.addAll(thatNode.computeAllSubTypes());
				}
				Iterator<ViewDefinition> vw2It = vw2Set.iterator();
				while (vw2It.hasNext()) {
					ViewDefinition thatChild = vw2It.next();
					linkedViews.put(thatChild, vl);
				}

			}

		}

		return linkedViews;

	}

	public String readDocsHTML() throws VPDMfException {

		//
		// Set up an output stream we can print the table to.
		// This is easier than concatenating strings all the time.
		//
		StringWriter sout = new StringWriter();
		PrintWriter out = new PrintWriter(sout);

		// Print the directory name as the page title
		out.print("    <HTML>\n"
				+ "    <HEAD>\n"
				+ "    <!-- Generated by vpdmf on "
				+ " -->\n"
				+ "    <TITLE>\n"
				+ "    Autogenerated VPDMf documentation [VPDMF VERSION NUMBERS]: ViewDefinition "
				+ this.getName()
				+ "\n"
				+ "    </TITLE>\n"
				+ "    <LINK REL =\"stylesheet\" TYPE=\"text/css\" HREF=\"../../../../stylesheet.css\" TITLE=\"Style\">\n"
				+ "    </HEAD>\n" + "    <BODY BGCOLOR=\"white\">\n"
				+ "        <TABLE BORDER=\"0\" WIDTH=\"600\">\n"
				+ "        <TR>\n" + "        <TD>\n");

		// Print the directory name as the page title
		out.print("\n<!-- ======== START OF VIEW DATA ======== -->" + "<H2>\n"
				+ this.readCategoryPath() + "</FONT>\n</H2>\n");

		out.print("<TABLE BORDER=\"1\" CELLPADDING=\"3\" CELLSPACING=\"0\" WIDTH=\"100%\">\n"
				+ "<TR BGCOLOR=\"#CCCCFF\" CLASS=\"TableHeadingColor\">\n"
				+ "<TD COLSPAN=1><FONT SIZE=\"+2\">\n" + "<B>");
		if (this.getType() == ViewDefinition.DATA
				|| this.getType() == ViewDefinition.COLLECTION
				|| this.getType() == ViewDefinition.EXTERNAL)
			out.print(this.getName());
		else
			out.print("<I>" + this.getName() + "</I>");
		out.print("</B></FONT></TD>\n" + "</TR>\n" + "</TABLE>");

		//
		// Loop over parents for current view
		//
		Vector<ViewDefinition> parents = new Vector<ViewDefinition>();
		ViewDefinition temp = this;
		parents.add(temp);
		while (temp.getParent() != null) {
			parents.add(temp.getParent());
			temp = temp.getParent();
		}

		if (this.getParent() != null) {

			temp = parents.get(parents.size() - 1);
			out.print("        <PRE>\n<A href=\"" + temp.getName() + "\">"
					+ temp.getName() + "</A>\n");

			int nTab = 1;
			for (int i = parents.size() - 2; i >= 0; i--) {
				temp = parents.get(i);

				String tabString = "";
				for (int j = 0; j < nTab; j++)
					tabString += "   ";
				nTab++;

				out.print(tabString + "|\n" + tabString + "+-- ");
				if (this.equals(temp)) {
					out.print(" <B>" + temp.getName() + "</B>\n");
				} else {
					out.print("<A href=\"" + temp.getName() + "\">"
							+ temp.getName() + "</A>\n");
				}

			}
			out.print("</PRE>\n");

		}

		try {

			if (this.getType() == ViewDefinition.DATA
					|| this.getType() == ViewDefinition.COLLECTION
					|| this.getType() == ViewDefinition.EXTERNAL) {

				out.print("<DL>\n");

				if (this.getDependencies().size() > 0) {
					out.print("<DT>Dependencies: ");
					Iterator it = this.getDependencies().iterator();
					String s = "";
					while (it.hasNext()) {
						ViewDefinition target = (ViewDefinition) it.next();
						if (s.length() > 0)
							s += ", ";
						s += this.readDocAddress(target, false);
					}
					out.print(s);
				}

				ArrayList<ViewDefinition> al = this.readChildren();
				if (al.size() > 0) {
					out.print("<DT>Children: ");
					for (int i = 0; i < al.size(); i++) {
						ViewDefinition target = al.get(i);
						if (i > 0)
							out.print(", ");
						out.print(this.readDocAddress(target, false));
					}
				}


			} else if (this.getType() == ViewDefinition.LINK) {

				out.print("<DL>\n");

			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		/*
		 * if (this.getDocumentation() != null) { out.print("<P>\n" +
		 * this.getDocumentation() + "<P>\n" + "<HR>\n"); } else if
		 * (this.getMostAppropriateClass().getDocumentation() != null) {
		 * out.print("<P>\n" + this.getMostAppropriateClass().getDocumentation()
		 * + "<P>\n" + "<HR>\n"); }
		 */

		out.print("    </TD>\n" + "  </TR>\n" + "</TABLE>\n");

		out.close();

		// Get the string of HTML from the StringWriter and return it.
		String data = sout.toString();

		return data;

	}

	private String readViewDefinitionType() {
		String type = "";

		if (this.getType() == ViewDefinition.DATA)
			type = "Data";
		else if (this.getType() == ViewDefinition.LINK)
			type = "Link";
		else if (this.getType() == ViewDefinition.LOOKUP)
			type = "LookupSpec";
		else if (this.getType() == ViewDefinition.SYSTEM)
			type = "System";
		else if (this.getType() == ViewDefinition.COLLECTION)
			type = "Collection";
		else if (this.getType() == ViewDefinition.EXTERNAL)
			type = "External";

		return type;

	}

	private String readCategoryPath() {
		UMLclass nspc = this.readMostAppropriateClass();
		return nspc.getPkg().getPkgAddress();
	}

	@Deprecated
	// THIS LOOKS HORRIBLE
	private UMLclass readMostAppropriateClass() {

		PrimitiveDefinition pd = this.getPrimaryPrimitive();

		Iterator<UMLclass> it = pd.getClasses().iterator();
		UMLclass c = null;
		while (it.hasNext()) {
			UMLclass cc = it.next();
			if (cc.getBaseName().equals(this.getName())) {
				c = cc;
				break;
			}

		}
		return c;

	}

	private String readDocAddress(ViewDefinition target, boolean pathFlag) {

		String link = "<A HREF=\"" + target.getName() + "\">";
		if (pathFlag)
			link += target.readCategoryPath() + ".";
		link += target.getName() + "</A>";

		return link;

	}

	private String readDocAddress(PrimitiveDefinition target, boolean pathFlag) {

		String link = "<A HREF=\"\">";
		link += target.getName() + "</A>";
		return link;

	}

	public String readDocsHTML(UMLattribute ad, PrimitiveDefinition pd)
			throws VPDMfException {

		if (!pd.readAttributes().contains(ad))
			throw new VPDMfException("Attribute not found in Primitive");

		String out = "";

		out += "<A NAME=\"" + ad.getBaseName() + "\"><!-- --></A>\n";
		out += "<H3>" + ad.getBaseName() + "</H3>\n";
		out += "<PRE>\n";
		out += ad.getBaseName() + " : " + ad.getType();

		//
		// Search for conditions
		//
		if (pd.getConditions() != null) {

			String adConditionSearch = "]" + pd.getName() + "|"
					+ ad.getParentClass().getBaseName() + "."
					+ ad.getBaseName() + "='(.*?)'";

			String q = "'"; // ad.getQuote();
			Pattern patt = Pattern.compile(adConditionSearch);
			Matcher matcher = patt.matcher(pd.getConditions());
			if (matcher.find()) {
				String valueString = matcher.group(1);
				out += " = " + q + valueString + q;
			}
		}

		out += "</PRE>\n";
		/*
		 * if (ad.getDocumentation() != null) { out += "<DL>\n<DD>\n"; out +=
		 * ad.get_Documentation(); out += "</DL>\n"; }
		 */

		return out;

	}

	/**
	 * Returns the numerical code of the ViewSpec Definition from a descriptive
	 * string
	 * 
	 * @param type
	 *            String, the type as a string, e.g. 'data'
	 * @return int, the type as an integer
	 */
	public int readType(String type) {
		int t;

		if (type.equalsIgnoreCase("data"))
			t = ViewDefinition.DATA;
		else if (type.equalsIgnoreCase("system"))
			t = ViewDefinition.SYSTEM;
		else if (type.equalsIgnoreCase("lookup"))
			t = ViewDefinition.LOOKUP;
		else if (type.equalsIgnoreCase("link"))
			t = ViewDefinition.LINK;
		else if (type.equalsIgnoreCase("collection"))
			t = ViewDefinition.COLLECTION;
		else if (type.equalsIgnoreCase("external"))
			t = ViewDefinition.EXTERNAL;
		else
			t = -1;

		return t;

	}

	public String readTree() {
		ViewDefinition vd = this;
		String parents = vd.getName();
		while (vd.getParent() != null) {
			parents = "." + parents;
			String p = vd.getParent().getName();
			parents = p + parents;
			vd = vd.getParent();
		}
		return parents;
	}

	public UMLpackage readCategory() {
		UMLpackage cat = null;
		if (this.getType() == ViewDefinition.LINK) {

			Iterator it1 = this.getIncomingEdges().values().iterator();
			ViewLink fromVl = (ViewLink) it1.next();
			ViewDefinition fromVd = (ViewDefinition) fromVl.getOutEdgeNode();
			UMLpackage fromCat = fromVd.readCategory();
			String fromCatAddr = fromCat.getPkgAddress();

			Iterator it2 = this.getOutgoingEdges().values().iterator();
			ViewLink toVl = (ViewLink) it2.next();
			ViewDefinition toVd = (ViewDefinition) toVl.getInEdgeNode();
			UMLpackage toCat = toVd.readCategory();
			String toCatAddr = toCat.getPkgAddress();

			if (fromCatAddr.compareTo(toCatAddr) <= 0) {
				cat = toCat;
			} else if (fromCatAddr.compareTo(toCatAddr) > 0) {
				cat = fromCat;
			}

		} else {

			cat = this.getPrimaryPrimitive().getPrimaryClass().getPkg();

			/*
			 * THIS MAY BE MORE COMPLICATED THAN THIS INVOLVING THE ORDER OF
			 * CLASSES IN THE PRIMITIVE Iterator it =
			 * this.getPrimaryPrimitive().getPrimaryClass(); String topKey =
			 * (String) it.next(); cat = cl.getPkg();
			 */

		}
		return cat;
	}

	public void setSpecification(String specification) {
		this.specification = specification;
	}

	public String getSpecification() {
		return this.specification;
	}

	public ViewSpec getSpec() {
		return spec;
	}

	public void setSpec(ViewSpec spec) {
		this.spec = spec;
	}
	
	public void setRelations(HashSet<ViewDefinition> relations) {
		this.relations = relations;
	}

	public HashSet<ViewDefinition> getRelations() {
		return this.relations;
	}

	public void setIndexElements(Map<Integer, IndexElement> indexElements) {
		this.indexElements = indexElements;
	}

	public Map<Integer, IndexElement> getIndexElements() {
		return indexElements;
	}

	public void setDependencies(HashSet<ViewDefinition> dependencies) {
		this.dependencies = dependencies;
	}

	public HashSet<ViewDefinition> getDependencies() {
		return this.dependencies;
	}

	public void setIsa(HashSet<ViewDefinition> isa) {
		this.isa = isa;
	}

	public Set<ViewDefinition> getIsa() {
		return this.isa;
	}

	public void setVpdmfLabelFormat(String vpdmfLabelFormat) {
		this.vpdmfLabelFormat = vpdmfLabelFormat;
	}

	public String getVpdmfLabelFormat() {
		return this.vpdmfLabelFormat;
	}

	public void setVpdmfLabel(UMLattribute vpdmfLabel) {
		this.vpdmfLabel = vpdmfLabel;
	}

	public UMLattribute getVpdmfLabel() {
		return this.vpdmfLabel;
	}

	public String getVpdmfUriFormat() {
		return vpdmfUriFormat;
	}

	public void setVpdmfUriFormat(String vpdmfUriFormat) {
		this.vpdmfUriFormat = vpdmfUriFormat;
	}

	public void setEditable(boolean editable) {
		editable = editable;
	}

	public boolean isEditable() {
		return editable;
	}

	public UMLattribute getVpdmfUri() {
		return vpdmfUri;
	}

	public void setVpdmfUri(UMLattribute vpdmfUri) {
		this.vpdmfUri = vpdmfUri;
	}

	public void setDocumentation(String documentation) {
		this.documentation = documentation;
	}

	public String getDocumentation() {
		return documentation;
	}

	public void setParent(ViewDefinition parent) {
		this.parent = parent;
	}

	public ViewDefinition getParent() {
		return parent;
	}

	public void setAltName(String altName) {
		this.altName = altName;
	}

	public String getAltName() {
		return altName;
	}

	public void setPrimaryPrimitive(PrimitiveDefinition primaryPrimitive) {
		this.primaryPrimitive = primaryPrimitive;
	}

	public PrimitiveDefinition getPrimaryPrimitive() {
		return primaryPrimitive;
	}

	public void setTop(VPDMf top) {
		this.top = top;
	}

	public VPDMf getTop() {
		return top;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getType() {
		return this.type;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// CODE TO BUILD VIEWS FROM XML SPECS
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	
	public void buildViewsSystemViews() throws Exception {

		// Add the ViewTable class to the view's primary primitive
		UMLclass vt = this.getTop().getUmlModel().listClasses()
				.get("|.edu.isi.bmkeg.vpdmf.model.ViewTable");
		// # my $km =
		// $self->{top}->{umlModel}->lookup_class("$rootCatAddress.KnowledgeModel");

		if (this.getPrimaryPrimitive() == null) {
			throw new Exception("ViewSpec " + this.getName() + " is not defined!");
		}

		this.getPrimaryPrimitive().getClasses().add(0, vt);

		// The primary class of the view must inherit from the ViewTable class
		UMLclass primaryClass = this.getPrimaryPrimitive().getPrimaryClass();

		if (primaryClass.getBaseName().equals("ViewTable")) {
			throw new Exception(
					"You have an error in your ViewSpec specification for "
							+ this.getName()
							+ "in the inheritence mechanism. Please check this view.");
		}

		UMLclass c = vt.lookupChildByName(primaryClass.getBaseName());
		if (c == null) {
			vt.getChildren().add(primaryClass);
			primaryClass.setParent(vt);
		}

		this.getPrimaryPrimitive().setPrimaryClass(vt);

	}

	public void buildViewsSystemLinkViews() throws Exception {

		UMLclass vlt = this.getTop().getUmlModel().listClasses()
				.get("|.ViewLinkTable");

		// Add the ViewLinkTable class to the view's primary primitive
		UMLclass luc = this.getPrimaryPrimitive().lookupClassByName(
				"ViewLinkTable");
		if (luc == null) {

			this.getPrimaryPrimitive().getClasses().add(0, vlt);

			// The primary class of the view must inherit from the ViewTable
			// class
			UMLclass primaryClass = this.getPrimaryPrimitive()
					.getPrimaryClass();

			// Note that we permit some ViewLinks to not have a primary class
			// defined.
			// In these cases, we do not try to link up the VLT & the primary
			// class
			if (primaryClass != null) {
				if (vlt.lookupChildByName(primaryClass.getBaseName()) != null) {
					vlt.getChildren().add(primaryClass);
					primaryClass.setParent(vlt);
				}
			}
			this.primaryPrimitive.setPrimaryClass(vlt);

			ConditionElement ce = new ConditionElement();
			ce.setClassName("ViewLinkTable");
			ce.setAttName("linkType");
			ce.setValue(this.getName());

			this.getPrimaryPrimitive().getConditionElements().add(ce);

			String condition = "|ViewLinkTable.linkType='$self->{Name}'";

			/*
			 * if(this.getPprimaryPrimitive}->{Conditions} > 0) {
			 * 
			 * my $temp = $self->{primaryPrimitive}->{Conditions};
			 * $self->{primaryPrimitive}->Conditions( $temp . "&" . $condition);
			 * 
			 * } else {
			 * 
			 * $self->{primaryPrimitive}->Conditions($condition);
			 * 
			 * } }
			 */
		}

	}
	
	public String readViewtypeString() {

		ViewDefinition tempVd = this;
		//
	  	// need to be careful with lookup views since they don't 
	  	// preserve the inheritence hieararchy of their data view
	  	// counterparts
	  	//
	  	if( this.getType() == ViewDefinition.LOOKUP) {
		  
	  		String viewName = this.getName();
		  	viewName = viewName.replaceAll("LU$", "");
  			return "." + viewName + ".%";

	  	}
	  
	  	String tempCond = "." + this.getName() + ".%";
	  	while( tempVd.getParent() != null ) {
		  	tempCond = "." + tempVd.getParent().getName() + ".%" + tempCond;
		  	tempVd = tempVd.getParent();
	  	}
	  	
		return tempCond;

	}

	public void buildViewTypeConditions() throws Exception {

		if ( !(this.getType() == ViewDefinition.DATA
				//|| this.getType() == ViewDefinition.LOOKUP
				|| this.getType() == ViewDefinition.COLLECTION
				|| this.getType() == ViewDefinition.EXTERNAL) ) {
			return;
		}
		
		String type = this.readViewtypeString();

		ConditionElement ce = new ConditionElement();
		ce.setClassName("ViewTable");
		ce.setAttName("viewType");
		ce.setValue(type);
		this.getPrimaryPrimitive().getConditionElements().add(ce);
		
		for( SuperGraphEdge e : this.getOutgoingEdges().values() ) {
			ViewLink vl = (ViewLink) e;
			PrimitiveDefinition pd = vl.getSource();
			ViewDefinition vd = (ViewDefinition) vl.getInEdgeNode();
			String type2 = vd.readViewtypeString();
			
			ConditionElement ce2 = new ConditionElement();
			ce2.setClassName("ViewTable");
			ce2.setAttName("viewType");
			ce2.setValue(type2);
			pd.getConditionElements().add(ce2);
		}
		

	}
	
	public void linkViewTypeConditions() {

		// Connect all the PrimitiveDefinitions that use lookups,
		// and copy their primary primitives where appropriate
		Iterator<SuperGraphNode> it = this.getSubGraph().getNodes().values()
				.iterator();
		while (it.hasNext()) {
			PrimitiveDefinition pd = (PrimitiveDefinition) it.next();

			if (pd.getLookupView() != null) {

				// Copy the lookup view's conditions into the target primitive
				PrimitiveDefinition temp = pd.getLookupView()
						.getPrimaryPrimitive();

				Iterator<ConditionElement> ceIt = temp.getConditionElements()
						.iterator();
				while (ceIt.hasNext()) {
					ConditionElement ce = ceIt.next();
					pd.getConditionElements().add(ce);
				}

			}

		}

	}
	
	/**
	 *  CODE TO READ NEW FORMAT VPDMf DEFINITIONS
	 */
	
	
	
	public void buildViewsSystemViews_specs() throws Exception {

		// Add the ViewTable class to the view's primary primitive
		UMLclass vt = this.getTop().getUmlModel().listClasses()
				.get("|.edu.isi.bmkeg.vpdmf.model.ViewTable");
		// # my $km =
		// $self->{top}->{umlModel}->lookup_class("$rootCatAddress.KnowledgeModel");

		if (this.getPrimaryPrimitive() == null) {
			throw new Exception("ViewSpec " + this.getName() + " is not defined!");
		}

		this.getPrimaryPrimitive().getClasses().add(0, vt);

		// The primary class of the view must inherit from the ViewTable class
		UMLclass primaryClass = this.getPrimaryPrimitive().getPrimaryClass();

		if (primaryClass.getBaseName().equals("ViewTable")) {
			throw new Exception(
					"You have an error in your ViewSpec specification for "
							+ this.getName()
							+ "in the inheritence mechanism. Please check this view.");
		}

		UMLclass c = vt.lookupChildByName(primaryClass.getBaseName());
		if (c == null) {
			vt.getChildren().add(primaryClass);
			primaryClass.setParent(vt);
		}

		this.getPrimaryPrimitive().setPrimaryClass(vt);

	}

	public void buildViewsSystemLinkViews_specs() throws Exception {

		UMLclass vlt = this.getTop().getUmlModel().listClasses()
				.get("|.ViewLinkTable");

		// Add the ViewLinkTable class to the view's primary primitive
		UMLclass luc = this.getPrimaryPrimitive().lookupClassByName(
				"ViewLinkTable");
		if (luc == null) {

			this.getPrimaryPrimitive().getClasses().add(0, vlt);

			// The primary class of the view must inherit from the ViewTable
			// class
			UMLclass primaryClass = this.getPrimaryPrimitive()
					.getPrimaryClass();

			// Note that we permit some ViewLinks to not have a primary class
			// defined.
			// In these cases, we do not try to link up the VLT & the primary
			// class
			if (primaryClass != null) {
				if (vlt.lookupChildByName(primaryClass.getBaseName()) != null) {
					vlt.getChildren().add(primaryClass);
					primaryClass.setParent(vlt);
				}
			}
			this.primaryPrimitive.setPrimaryClass(vlt);

			ConditionElement ce = new ConditionElement();
			ce.setClassName("ViewLinkTable");
			ce.setAttName("linkType");
			ce.setValue(this.getName());

			this.getPrimaryPrimitive().getConditionElements().add(ce);

			String condition = "|ViewLinkTable.linkType='$self->{Name}'";

			/*
			 * if(this.getPprimaryPrimitive}->{Conditions} > 0) {
			 * 
			 * my $temp = $self->{primaryPrimitive}->{Conditions};
			 * $self->{primaryPrimitive}->Conditions( $temp . "&" . $condition);
			 * 
			 * } else {
			 * 
			 * $self->{primaryPrimitive}->Conditions($condition);
			 * 
			 * } }
			 */
		}

	}

	
	public void buildPIGLinks_specs() throws Exception {

		// Check to avoid building composition of same view twice
		if (this.getSubGraph().getEdges().size() > 0 )
			return;
		
		PrimitiveDefinition currentPrimitive = null;
		UMLclass currentClass = null;

		Iterator<PrimitiveSpec> pvIt = this.spec.getPrimitives().iterator();
		while( pvIt.hasNext() ) {
			PrimitiveSpec pvSpec = pvIt.next();

			Iterator<PrimitiveLinkSpec> plIt = pvSpec.getPvLinks().iterator();
			while( plIt.hasNext() ) {
				PrimitiveLinkSpec plSpec = plIt.next();
			
				String p1 = pvSpec.getName();
				String c1 = plSpec.getC1();
				String roleName = plSpec.getRole();
				String p2 = plSpec.getPv2();
				String c2 = plSpec.getC2();
				
				boolean paged = plSpec.isPaged();
		
				UMLrole r = null;
				
				HashSet<UMLclass> class1hits = this.getTop().getUmlModel()
						.lookupClass(c1);
				HashSet<UMLclass> class2hits = this.getTop().getUmlModel()
						.lookupClass(c2);

				if (class1hits.size() == 0)
					throw new Exception(
							"~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
									+ "You have an error in your ViewSpec specification for "
									+ this.getName()
									+ " in the triplet: \n\n "
									+ plSpec.toString()
									+ "\n\n"
									+ "Class:" + c1+ " doees not exist\n"
									+ "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");
				
				if (class2hits.size() == 0)
					throw new Exception(
							"~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
									+ "You have an error in your ViewSpec specification for "
									+ this.getName()
									+ " in the triplet: \n\n "
									+ plSpec.toString()
									+ "\n\n"
									+ "Class:" + c2 + " doees not exist\n"
									+ "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");

				UMLclass class1 = class1hits.iterator().next();
				UMLclass class2 = class2hits.iterator().next();

				if (!this.getSubGraph().getNodes().containsKey(p1)) {
					throw new Exception(
							"~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
									+ "You have an error in your ViewSpec specification for "
									+ this.getName()
									+ " in the triplet: \n\n "
									+ plSpec.toString()
									+ ", \n\n"
									+ "Make sure that the view ["
									+ this.getName()
									+ "] has a PrimitiveDefinition "
									+ "called '"
									+ p1
									+ "'\n"
									+ "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");
				}

				PrimitiveDefinition pv1 = (PrimitiveDefinition) this
						.getSubGraph().getNodes().get(p1);

				if (!this.getSubGraph().getNodes().containsKey(p2)) {
					throw new Exception(
							"~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
									+ "You have an error in your ViewSpec specification for "
									+ this.getName()
									+ " in the triplet: \n\n "
									+ plSpec.toString()
									+ ", \n\n"
									+ "Make sure that the view ["
									+ this.getName()
									+ "] has a PrimitiveDefinition "
									+ "called '"
									+ p2
									+ "'\n"
									+ "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");
				}

				PrimitiveDefinition pv2 = (PrimitiveDefinition) this
						.getSubGraph().getNodes().get(p2);

				// Check for substitutions (i.e. for a set backing table etc).
				if (class1.getAssociateRoles().containsKey(
						roleName + "__design")) {

					r = class1.getAssociateRoles().get(roleName + "__design");

				} else if (class1.getAssociateRoles().containsKey(roleName)) {

					r = class1.getAssociateRoles().get(roleName);

				} else if (!roleName.equals("vpdmfParentClass")) {

					String expl = "";
					UMLclass luc = pv2.lookupClassByName("ViewLinkTable");

					if (luc == null) {
						expl = "[" + this.getName() + "]" + pv1.getName() + "|"
								+ c1 + " is not defined."
								+ " Check the composition of ["
								+ this.getName() + "]" + pv1.getName() + "\n";
					} else if (!class1.getAssociateRoles()
							.containsKey(roleName)) {
						expl = "The class " + c1
								+ " in the UML model does not have "
								+ "a role called " + roleName + ".\n";
					}
					throw new Exception(
							"~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
									+ "You have an error in your ViewSpec specification for "
									+ this.getName()
									+ " in the triplet: \n\n" 
									+ plSpec
									+ " \n\n"
									+ expl
									+ "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");

				} else {
					r = null;
				}

				if (!pv1.equals(pv2)) {
					PrimitiveDefinitionGraph pdg = (PrimitiveDefinitionGraph) this
							.getSubGraph();
					pdg.addPrimitiveLink(pv1, pv2, r, paged);
				} else {
					pv1.getInternalRoles().add(r);
				}

			}

		}

		// This sets the order of the nodes in the Primitive Definition graph to
		// a depth-first-s earch (DFS) ordering.
		// #
		// $self->{subGraph}->set_pvDefGraph_traversal();

		/*
		 * Check to see if there are any disconnected nodes in the graph if(
		 * !$self->{subGraph}->{graph}->is_weakly_connected() ) { my @wcc =
		 * $self->{subGraph}->{graph}->weakly_connected_components(); print join
		 * "\n____________________________________\n", map {join ",", @{$_}}
		 * 
		 * @wcc; print "\n\n\n"; print $self->{subGraph}->{graph}; confess
		 * "$self->{Name}'s primitive graph is not connected\n"; }
		 */


	}
	
}
