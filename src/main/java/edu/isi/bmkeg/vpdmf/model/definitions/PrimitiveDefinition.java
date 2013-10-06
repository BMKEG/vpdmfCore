package edu.isi.bmkeg.vpdmf.model.definitions;

/**
 * Timestamp: Thu_Jun_19_120936_2003;
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
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
import edu.isi.bmkeg.utils.superGraph.SuperGraphEdge;
import edu.isi.bmkeg.utils.superGraph.SuperGraphNode;
import edu.isi.bmkeg.utils.superGraph.SuperGraphTraversal;
import edu.isi.bmkeg.vpdmf.model.instances.PrimitiveInstance;
import edu.isi.bmkeg.vpdmf.model.instances.PrimitiveLinkInstance;

public class PrimitiveDefinition extends SuperGraphNode {
	static final long serialVersionUID = -6139817648173518724L;

	private String conditions;
	private boolean editable = true;
	private boolean nullable = false;
	private boolean unique = false;

	private UMLclass primaryClass;

	private ViewDefinition view;

	private ArrayList<UMLclass> classes = new ArrayList<UMLclass>();
	private ArrayList<ConditionElement> conditionElements = new ArrayList<ConditionElement>();
	private HashSet<UMLrole> internalRoles = new HashSet<UMLrole>();

	private ViewDefinition lookupView;
	private String lookupViewLookup;

	private boolean multiple;
	private boolean multipleSet;
	
	/**
	 * Calculate from the primitive graph whether this primitive is 1-n 
	 * with respect to the primary primitive in the view.
	 * @return
	 */
	public boolean isMultiple() throws Exception {
		
		if( multipleSet )
			return multiple;
				
		PrimitiveDefinitionGraph pdg = (PrimitiveDefinitionGraph) this.getGraph();
		ViewDefinition vd = (ViewDefinition) pdg.getSubGraphNode(); 
		PrimitiveDefinition primaryPv = vd.getPrimaryPrimitive();
		
		if( this.equals(primaryPv) ) {
			multiple = false;
			multipleSet = true;
			return false;
		}
		
		List<SuperGraphEdge> edges = pdg.readPath(primaryPv, this);
		Iterator<SuperGraphEdge> eIt = edges.iterator();
		while( eIt.hasNext() ) {
			PrimitiveLink pl = (PrimitiveLink) eIt.next();
			UMLrole r = pl.getRole();
			if( r.getMult_upper() == -1 ) {
				multiple = true;
				multipleSet = true;
				return true;				
			}
			
		}

		multiple = false;
		multipleSet = true;
		return false;
		
	}
	
	public UMLclass readIdentityClass() throws Exception {

		UMLclass c = this.getClasses().get(this.getClasses().size()-1);
		
		return c;

	}
	
	public UMLclass lookupClassByName(String name) throws Exception {

		UMLclass c = null;
		Iterator<UMLclass> cIt = this.getClasses().iterator();
		while (cIt.hasNext() && c == null) {
			UMLclass cc = cIt.next();
			if (cc.getBaseName().equals(name)) {
				if (c != null)
					throw new Exception("Multiple classes are called " + name);
				c = cc;
			}
		}
		return c;

	}

	public ArrayList<UMLattribute> readAttributes() {
		ArrayList<UMLattribute> attrs = new ArrayList<UMLattribute>();

		Iterator<UMLclass> classIt = this.getClasses().iterator();
		while (classIt.hasNext()) {
			UMLclass c = classIt.next();

			List<UMLattribute> attributes = c.getAttributes();
			Iterator<UMLattribute> attIt = attributes.iterator();
			while (attIt.hasNext()) {
				attrs.add(attIt.next());
			}
		}

		return attrs;

	}

	public ArrayList<String> readAttributeAddresses() {

		ArrayList<String> attrs = new ArrayList<String>();

		Iterator<UMLclass> classIt = this.getClasses().iterator();
		while (classIt.hasNext()) {
			UMLclass c = classIt.next();

			List<UMLattribute> attributes = c.getAttributes();
			Iterator<UMLattribute> attIt = attributes.iterator();
			while (attIt.hasNext()) {
				UMLattribute att = attIt.next();

				if( !att.getToImplement() ) 
					continue;
					
				String addr = "]" + this.getName() + "|" + c.getBaseName()
						+ "." + att.getBaseName();
				attrs.add(addr);
			}
		}

		return attrs;

	}

	/**
	 * Checks to see if the primitive has any conditions
	 */
	public boolean hasConditions() {

		if (this.getConditionElements().size() == 0) {
			return false;
		} else {
			return true;
		}

	}

	/**
	 * <p>
	 * Check to see if this primitive definition is a uniquely defined by it's
	 * conditions.
	 * </p>
	 * 
	 * @return boolean
	 */
	public boolean arePrimitiveConditionsIndexElements() {

		if (this.getLookupView() == null)
			return false;

		Iterator it = this.getLookupView().getIndexElements().values().iterator();
		while (it.hasNext()) {
			IndexElement ie = (IndexElement) it.next();

			boolean check = false;

			Iterator innerIt = this.getConditionElements().iterator();
			while (innerIt.hasNext()) {
				ConditionElement ce = (ConditionElement) innerIt.next();
				String attAddr = "]" + this.getName() + ce.readAddress();

				if (ie.getAttributeAddress().equals(attAddr)) {
					check = true;
					break;
				}
			}

			if (!check) {
				return false;
			}
		}

		return true;
	}

	public boolean hasConditionOn(String addr) {

		boolean check;

		String trim = addr.replaceAll("]" + this.getName(), "");

		String condString = this.getConditions();

		Iterator it = this.getConditionElements().iterator();
		while (it.hasNext()) {
			ConditionElement ce = (ConditionElement) it.next();
			if (ce.readAddress().equals(trim)) {
				return true;
			}
		}

		return false;

	}

	public ArrayList<UMLrole> readExtraPvRoles() throws Exception {
		ArrayList<UMLrole> roles = new ArrayList<UMLrole>();
		Collection classesInView = this.getView().getSubGraph().getNodes()
				.values();

		Iterator cIt = this.getClasses().iterator();
		while (cIt.hasNext()) {
			UMLclass c = (UMLclass) cIt.next();

			Iterator rIt = c.getAssociateRoles().values().iterator();
			while (rIt.hasNext()) {
				UMLrole r = (UMLrole) rIt.next();

				UMLclass luC = this.lookupClassByName(r.getDirectClass()
						.getBaseName());
				if (luC == null && !classesInView.contains(r.getDirectClass())) {
					roles.add(r);
				}

			}

		}

		return roles;

	}

	public boolean isPrimaryPrimitive() {
		boolean isPrimaryPv = false;

		if (this.getView().getPrimaryPrimitive() == this) {
			isPrimaryPv = true;
		}

		return isPrimaryPv;
	}

	public boolean containsIndexElements() throws Exception {

		ViewDefinition vd = (ViewDefinition) this.getGraph().getSubGraphNode();
		Iterator ieIt = vd.getIndexElements().values().iterator();
		while (ieIt.hasNext()) {
			IndexElement ie = (IndexElement) ieIt.next();
			if (ie.getAttributeAddress().indexOf("]" + this.getName() + "|") != -1)
				return true;
		}

		return false;

	}

	public int orderRelativeTo(PrimitiveDefinition pd) {
		int pos = 0;

		return pos;
	}

	@Deprecated
	public String readViewType() {

		String type = "";
		/*
		 * ViewDefinition vd1 = null;
		 * 
		 * if (this.getView().getPrimaryPrimitive().equals(this)) { vd1 =
		 * this.getView(); } else if
		 * (this.getView().getzop().Views.checkForExistence(this .getName())) {
		 * vd1 = (ViewDefinition) this.get_View().get_top().Views
		 * .readFromCollection(this.get_Name()); } else if
		 * (this.get_lookupView() != null) { vd1 = this.get_lookupView(); }
		 * 
		 * if (vd1 != null) { type = vd1.get_Name() + ".%"; Iterator vd2It =
		 * vd1.getAllParents().iterator(); while (vd2It.hasNext()) {
		 * ViewDefinition vd2 = (ViewDefinition) vd2It.next(); type =
		 * vd2.get_Name() + ".%" + type; } }
		 */
		return type;
	}

	public void setConditions(String conditions) {
		this.conditions = conditions;
	}

	public String getConditions() {
		return conditions;
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	public boolean isEditable() {
		return editable;
	}

	public void setNullable(boolean nullable) {
		this.nullable = nullable;
	}

	public boolean isNullable() {
		return nullable;
	}

	public void setUnique(boolean unique) {
		this.unique = unique;
	}

	public boolean isUnique() {
		return unique;
	}

	public void setPrimaryClass(UMLclass primaryClass) {
		this.primaryClass = primaryClass;
	}

	public UMLclass getPrimaryClass() {
		return primaryClass;
	}

	public void setView(ViewDefinition view) {
		this.view = view;
	}

	public ViewDefinition getView() {
		return view;
	}

	public void setClasses(ArrayList<UMLclass> classes) {
		this.classes = classes;
	}

	public ArrayList<UMLclass> getClasses() {
		return classes;
	}

	public void setConditionElements(
			ArrayList<ConditionElement> conditionElements) {
		this.conditionElements = conditionElements;
	}

	public ArrayList<ConditionElement> getConditionElements() {
		return conditionElements;
	}

	public void setInternalRoles(HashSet<UMLrole> internalRoles) {
		this.internalRoles = internalRoles;
	}

	public HashSet<UMLrole> getInternalRoles() {
		return internalRoles;
	}

	public void setLookupView(ViewDefinition lookupView) {
		this.lookupView = lookupView;
	}

	public ViewDefinition getLookupView() {
		return lookupView;
	}

	public void setLookupViewLookup(String lookupViewLookup) {
		this.lookupViewLookup = lookupViewLookup;
	}

	public String getLookupViewLookup() {
		return lookupViewLookup;
	}

	public void orderClasses() {

		HashSet<UMLrole> roles = this.getInternalRoles();

		ArrayList<UMLclass> order = new ArrayList();
		HashSet<UMLclass> lookup = new HashSet<UMLclass>();

		order.add(this.primaryClass);
		lookup.add(this.getPrimaryClass());
		/*
		 * NOT AT ALL CLEAR HOW THIS WORKS NEED TO CHECK. for(int i=0;
		 * i<order.size(); i++) {
		 * 
		 * UMLclass currentClass = order.get(i);
		 * 
		 * // Parents & children Iterator<UMLclass> chIt =
		 * currentClass.getChildren().iterator(); while( chiIt.hasNext() ) {
		 * UMLclass ch = chIt.next();
		 * 
		 * if( !lookup.contains(ch) &&
		 * this.getClasses().containsKey(ch.getName()) ) {
		 * 
		 * UMLclass parentClass = ch.getParent(); order.add(ch); subclasses, @{
		 * $j->{subClass}->get_elements() };
		 * 
		 * for $k (keys %lookup) { if( $lookup{ $k } > $lookup{
		 * $parentClass->{Name} } ) { $lookup{ $k }++; } } $lookup{$j->{Name}} =
		 * $lookup{ $parentClass->{Name} } + 1;
		 * 
		 * } }
		 * 
		 * # # Dependency (i.e. foreign/primary keys) # for $r ( @roles ) {
		 * 
		 * return if $r->{ForeignKeyArray}->calculate_size() == 0;
		 * 
		 * my $fkClass = $r->{ForeignKeyArray}->
		 * read_by_number(0)->{ParentClass};
		 * 
		 * my $pkClass = $r->{ForeignKeyArray}->
		 * read_by_number(0)->{PK}->{ParentClass};
		 * 
		 * if( $pkClass == $currentClass && !exists $lookup{ $fkClass->{Name} })
		 * {
		 * 
		 * push @order, $fkClass; for $k (keys %lookup) { if( $lookup{ $k } >
		 * $lookup{ $currentClass->{Name} } ) { $lookup{ $k }++; } }
		 * $lookup{$fkClass->{Name}} = $lookup{ $currentClass->{Name} } + 1;
		 * 
		 * } elsif( $fkClass == $currentClass && !exists $lookup{
		 * $pkClass->{Name} }) {
		 * 
		 * push @order, $pkClass; for $k (keys %lookup) { if( $lookup{ $k } >=
		 * $lookup{ $currentClass->{Name} } ) { $lookup{ $k }++; } }
		 * $lookup{$pkClass->{Name}} = $lookup{ $currentClass->{Name} } - 1;
		 * 
		 * }
		 * 
		 * }
		 * 
		 * }
		 * 
		 * if( scalar keys %lookup != scalar keys %{$self->{Classes}->{_hash}} )
		 * { confess
		 * "____________________________________________________________\n" .
		 * "mismatch in [$self->{graph}->{subGraphNode}->{Name}]$self->{Name}\n"
		 * . "Check if the inheritence hierarchy of " .
		 * "$self->{Name} is correct\n" .
		 * "____________________________________________________________\n"; }
		 * 
		 * $self->{Classes}->{_order} = \%lookup;
		 * 
		 * }
		 */
		// TODO Auto-generated method stub

	}

};
