package edu.isi.bmkeg.vpdmf.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.isi.bmkeg.uml.model.UMLattribute;
import edu.isi.bmkeg.uml.model.UMLclass;
import edu.isi.bmkeg.uml.model.UMLmodel;
import edu.isi.bmkeg.uml.model.UMLrole;
import edu.isi.bmkeg.uml.sources.UMLModelSimpleParser;
import edu.isi.bmkeg.utils.TextUtils;
import edu.isi.bmkeg.utils.superGraph.SuperGraphNode;
import edu.isi.bmkeg.utils.xml.XmlBindingTools;
import edu.isi.bmkeg.vpdmf.model.definitions.ConditionElement;
import edu.isi.bmkeg.vpdmf.model.definitions.IndexElement;
import edu.isi.bmkeg.vpdmf.model.definitions.PrimitiveDefinition;
import edu.isi.bmkeg.vpdmf.model.definitions.VPDMf;
import edu.isi.bmkeg.vpdmf.model.definitions.ViewDefinition;
import edu.isi.bmkeg.vpdmf.model.definitions.ViewGraphDefinition;
import edu.isi.bmkeg.vpdmf.model.definitions.specs.ClassSpec;
import edu.isi.bmkeg.vpdmf.model.definitions.specs.ConditionSpec;
import edu.isi.bmkeg.vpdmf.model.definitions.specs.IndexElementSpec;
import edu.isi.bmkeg.vpdmf.model.definitions.specs.PrimitiveLinkSpec;
import edu.isi.bmkeg.vpdmf.model.definitions.specs.PrimitiveSpec;
import edu.isi.bmkeg.vpdmf.model.definitions.specs.ViewSpec;
import edu.isi.bmkeg.vpdmf.model.definitions.specs.VpdmfSpec;

/**
 * <p>
 * Title: VPDMf Parser.
 * </p>
 * 
 * <p>
 * Description: Imports view specification files and builds a VPDMf model for a
 * whole system.
 * </p>
 * 
 * <p>
 * Copyright: Copyright (c) 2004
 * </p>
 * 
 * <p>
 * Company:
 * </p>
 * 
 * @author not attributable
 * @version 1.0
 */
public class VPDMfParser {

	Logger log = Logger.getLogger("edu.isi.bmkeg.vpdmf.utils.VPDMfParser");

	private UMLmodel model;
	private HashMap<String, ViewDefinition> views = new HashMap<String, ViewDefinition>();

	private List<String> solrViews = new ArrayList<String>();

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public static List<File> getAllSpecFiles(File dir) {
		List<File> al = new ArrayList<File>();
		File[] fArray = dir.listFiles();

		if (fArray == null)
			return al;

		for (int i = 0; i < fArray.length; i++) {
			File f = fArray[i];
			if (f.isDirectory()) {
				al.addAll(VPDMfParser.getAllSpecFiles(f));
			} else if ( f.exists() && f.getName().endsWith(".xml")) {
				al.add(f);
			}
		}
		return al;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public VPDMf buildAllViewsAsRelationalDatabaseModel(VpdmfSpec vpdmfSpec,
			UMLmodel model, List<File> allSpecFiles) throws Exception {

		return this.buildAllViewsAsRelationalDatabaseModel(vpdmfSpec, model,
				allSpecFiles, null);

	}

	public VPDMf buildAllViewsAsRelationalDatabaseModel(VpdmfSpec vpdmfSpec,
			UMLmodel model, List<File> allSpecFiles, List<String> solrViews)
			throws Exception {

		this.model = model;

		String groupId = vpdmfSpec.getGroupId();
		String artifactId = vpdmfSpec.getArtifactId();
		String version = vpdmfSpec.getVersion();

		VPDMf top = new VPDMf(groupId, artifactId, version);

		this.constructViewShellsForSystem(top);

		// ~~~~~~~~~~~~~~~~~~~~~~~~
		// Add Solr specifications
		// ~~~~~~~~~~~~~~~~~~~~~~~~
		if (solrViews != null)
			top.setViewsToIndex(solrViews);

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Create the basic View Graph Definition.
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		ViewGraphDefinition vgd = new ViewGraphDefinition();
		top.setvGraphDef(vgd);
		vgd.setTop(top);

		this.model.mergeModel(top.getVpdmfModel());

		top.setUmlModel(this.model);

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// link additional views patterns into the VPDMf model
		// from patterns (i.e. Term / Ontology) as required
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		List<String> ontPkgPatterns = vpdmfSpec.getOntpkgs();
		if (ontPkgPatterns != null && ontPkgPatterns.size() > 0) {

			Iterator<String> ontPkgPattIt = ontPkgPatterns.iterator();
			while (ontPkgPattIt.hasNext()) {
				String ontPkgPatt = ontPkgPattIt.next();

				addClassesToModelForTerminology(ontPkgPatt);

				constructViewShellsForTerminology(top);

			}

		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Build the view shells from ViewSpecs.
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		Iterator<File> sfIt = allSpecFiles.iterator();
		while (sfIt.hasNext()) {
			File sf = (File) sfIt.next();

			log.debug("Parsing " + sf.getName());

			ViewDefinition vd = new ViewDefinition();
			vd.setTop(top);

			String spec = TextUtils.readFileToString(sf);

			spec = spec.replaceAll("[\\t\\n]", "");
			StringReader reader = new StringReader(spec);
			ViewSpec viewSpec = XmlBindingTools
					.parseXML(reader, ViewSpec.class);

			vd.setSpecification(spec);
			vd.setSpec(viewSpec);

			this.constructViewShell(vd, this.model);

			top.getViews().put(vd.getName(), vd);

		}

		Iterator<ViewDefinition> vdIt = top.getViews().values().iterator();
		while (vdIt.hasNext()) {
			ViewDefinition vd = vdIt.next();
			ViewSpec viewSpec = vd.getSpec();

			if (viewSpec.getParent() != null
					&& viewSpec.getParent().length() > 0) {

				if (!top.getViews().containsKey(viewSpec.getParent()))
					throw new Exception("Parent " + viewSpec.getParent()
							+ " of view " + vd.getName() + " not found");

				ViewDefinition vdParent = top.getViews().get(
						viewSpec.getParent());

				if (vd == vdParent)
					throw new Exception("A view cannot be it's own parent: "
							+ vd.getName());

				vd.setParent(vdParent);

			}

		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// link the system classes into the underlying UML model.
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		top.linkSystemClasses();

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// check for unmerged proxies.
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		Iterator<UMLclass> cIt = top.getUmlModel().listClasses().values()
				.iterator();
		while (cIt.hasNext()) {
			UMLclass c = cIt.next();
			if (c.getStereotype() != null && c.getStereotype().equals("proxy")) {
				throw new Exception("Unmerged proxy class found in model:"
						+ c.getClassAddress());
			}
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Annotate UMLattributes with idx flags if they occur
		// within an IndexElement
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		vdIt = top.getViews().values().iterator();
		while (vdIt.hasNext()) {
			ViewDefinition vd = vdIt.next();

			addAttributesToIndexes(vd);

		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Convert the finalized UML model, with all it's changes
		// complete into a RDBMS model.
		//
		// We can only run this step now to get all the different
		// linkages between keys and primitives correct.
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		this.model.convertToRelationalImplementation("\\.model\\.");

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Convert all classes found in 'events' packages to
		//
		// toImplement = false;
		//
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		Map<String, UMLclass> cMap = this.model.listClasses("\\.events\\.");
		Iterator<UMLclass> it = cMap.values().iterator();
		while (it.hasNext()) {
			UMLclass c = it.next();
			c.setToImplement(false);
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Construct the internal structure of the view from its shell.
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		this.buildViewsFromShells(top);

		// ~~~~~~~~~~~~~~~~~~~~~~
		// Build the view graph.
		// ~~~~~~~~~~~~~~~~~~~~~~
		top.getvGraphDef().buildViewGraphDefinition();

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Add viewType conditions to each view definition
		// and primitive involved in a viewLink
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		for (ViewDefinition vd : top.getViews().values()) {

			vd.buildViewTypeConditions();

		}

		return top;

	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public VPDMf buildAllViewsAsClassModel(VpdmfSpec vpdmfSpec, UMLmodel model,
			List<File> allSpecFiles) throws Exception {

		return this.buildAllViewsAsClassModel(vpdmfSpec, model, allSpecFiles,
				null);

	}

	public VPDMf buildAllViewsAsClassModel(VpdmfSpec vpdmfSpec, UMLmodel model,
			List<File> allSpecFiles, List<String> solrViews) throws Exception {

		this.model = model;
		model.convertToObjectOrientedImplementation();
		
		String groupId = vpdmfSpec.getGroupId();
		String artifactId = vpdmfSpec.getArtifactId();
		String version = vpdmfSpec.getVersion();

		VPDMf top = new VPDMf(groupId, artifactId, version);

		// ~~~~~~~~~~~~~~~~~~~~~~~~
		// Add Solr specifications
		// ~~~~~~~~~~~~~~~~~~~~~~~~
		if (solrViews != null)
			top.setViewsToIndex(solrViews);

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Create the basic View Graph Definition.
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		ViewGraphDefinition vgd = new ViewGraphDefinition();
		top.setvGraphDef(vgd);
		vgd.setTop(top);

		top.setUmlModel(this.model);

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Build the view shells from ViewSpecs.
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		for( File sf: allSpecFiles ) {

			log.debug("Parsing " + sf.getName());

			ViewDefinition vd = new ViewDefinition();
			vd.setTop(top);

			String spec = TextUtils.readFileToString(sf);

			spec = spec.replaceAll("[\\t\\n]", "");
			StringReader reader = new StringReader(spec);
			ViewSpec viewSpec = XmlBindingTools
					.parseXML(reader, ViewSpec.class);

			vd.setSpecification(spec);
			vd.setSpec(viewSpec);

			this.constructViewShell(vd, this.model);

			top.getViews().put(vd.getName(), vd);

		}

		Iterator<ViewDefinition> vdIt = top.getViews().values().iterator();
		while (vdIt.hasNext()) {
			ViewDefinition vd = vdIt.next();
			ViewSpec viewSpec = vd.getSpec();

			if (viewSpec.getParent() != null
					&& viewSpec.getParent().length() > 0) {

				if (!top.getViews().containsKey(viewSpec.getParent()))
					throw new Exception("Parent " + viewSpec.getParent()
							+ " of view " + vd.getName() + " not found");

				ViewDefinition vdParent = top.getViews().get(
						viewSpec.getParent());

				if (vd == vdParent)
					throw new Exception("A view cannot be it's own parent: "
							+ vd.getName());

				vd.setParent(vdParent);

			}

		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// check for unmerged proxies.
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		Iterator<UMLclass> cIt = top.getUmlModel().listClasses().values()
				.iterator();
		while (cIt.hasNext()) {
			UMLclass c = cIt.next();
			if (c.getStereotype() != null && c.getStereotype().equals("proxy")) {
				throw new Exception("Unmerged proxy class found in model:"
						+ c.getClassAddress());
			}
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Annotate UMLattributes with idx flags if they occur
		// within an IndexElement
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		vdIt = top.getViews().values().iterator();
		while (vdIt.hasNext()) {
			ViewDefinition vd = vdIt.next();
			addAttributesToIndexes(vd);
		}


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Convert all classes found in 'events' packages to
		//
		// toImplement = false;
		//
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		Map<String, UMLclass> cMap = this.model.listClasses("\\.events\\.");
		Iterator<UMLclass> it = cMap.values().iterator();
		while (it.hasNext()) {
			UMLclass c = it.next();
			c.setToImplement(false);
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Construct the internal structure of the view from its shell.
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		this.buildViewsFromShells(top);

		// ~~~~~~~~~~~~~~~~~~~~~~
		// Build the view graph.
		// ~~~~~~~~~~~~~~~~~~~~~~
		top.getvGraphDef().buildViewGraphDefinition();

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Add viewType conditions to each view definition
		// and primitive involved in a viewLink
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		for (ViewDefinition vd : top.getViews().values()) {
			vd.buildViewTypeConditions();
		}

		return top;

	}

	private void addAttributesToIndexes(ViewDefinition vd) throws Exception {
		Iterator<IndexElement> idxIt = vd.getIndexElements().values()
				.iterator();
		IDXLOOP: while (idxIt.hasNext()) {
			IndexElement idx = idxIt.next();

			PrimitiveDefinition pd = (PrimitiveDefinition) vd.getSubGraph()
					.getNodes().get(idx.getP());

			if (pd == null) {
				throw new Exception("Badly specified index for " + vd.getName()
						+ ": " + idx.getAttributeAddress());
			}
			Iterator<UMLclass> ccIt = pd.getClasses().iterator();
			while (ccIt.hasNext()) {
				UMLclass cc = ccIt.next();
				if (cc.getBaseName().equals(idx.getC())) {
					Iterator<UMLattribute> aaIt = cc.getAttributes().iterator();
					while (aaIt.hasNext()) {
						UMLattribute aa = aaIt.next();
						if (aa.getBaseName().equals(idx.getA())) {
							aa.setIdx(true);
							if (idx.isUniqueKey()) {
								aa.setUnique(true);
							}
							continue IDXLOOP;
						}
					}
				}
			}

		}
	}

	private void constructViewShellsForSystem(VPDMf top) throws Exception {

		InputStream is = ClassLoader
				.getSystemResourceAsStream("edu/isi/bmkeg/vpdmf/vpdmfSystem/vpdmfSystem.xml");

		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);

		UMLModelSimpleParser p = new UMLModelSimpleParser(
				UMLmodel.XMI_MAGICDRAW);
		p.parseUMLModelFile(br, "vpdmfSystem");
		top.setVpdmfModel(p.getUmlModels().get(0));

		ArrayList<String> viewPaths = new ArrayList<String>();
		viewPaths.add("edu/isi/bmkeg/vpdmf/vpdmfSystem/specs/vpdmfUser-vw.xml");
		viewPaths.add("edu/isi/bmkeg/vpdmf/vpdmfSystem/specs/fromView-vw.xml");
		viewPaths.add("edu/isi/bmkeg/vpdmf/vpdmfSystem/specs/toView-vw.xml");
		viewPaths.add("edu/isi/bmkeg/vpdmf/vpdmfSystem/specs/linkView-vw.xml");

		Iterator<String> pathIt = viewPaths.iterator();
		while (pathIt.hasNext()) {
			String path = pathIt.next();
			this.constructViewShellsFromResource(top, path);
		}

	}

	private void constructViewShellsFromResource(VPDMf top, String path)
			throws Exception {

		InputStream is = ClassLoader.getSystemResourceAsStream(path);
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);

		String spec = "", buffer = "";
		while ((buffer = br.readLine()) != null) {
			spec += buffer;
		}

		ViewDefinition vd = new ViewDefinition();
		vd.setTop(top);

		StringReader reader = new StringReader(spec);
		ViewSpec viewSpec = XmlBindingTools.parseXML(reader, ViewSpec.class);

		vd.setSpecification(spec);
		vd.setSpec(viewSpec);
		vd.setName(viewSpec.getName());

		this.constructViewShell(vd, top.getVpdmfModel());

		top.getViews().put(vd.getName(), vd);

	}

	/**
	 * Builds a 'view shell'. This consists of a basic construction of the
	 * primitives
	 * 
	 * @param m
	 * @throws Exception
	 */
	private void constructViewShell(ViewDefinition vd, UMLmodel m)
			throws Exception {

		//
		// Check to avoid building shell of same view twice
		//
		if (vd.getPrimaryPrimitive() != null)
			return;

		Map<String, UMLclass> umlClassMap = m.listClasses();

		vd.setName(vd.getSpec().getName());

		//
		// Assign the view type
		//
		String t = vd.getSpec().getType();
		if (t.toLowerCase().equals("data")) {

			vd.setType(ViewDefinition.DATA);

		} else if (t.toLowerCase().equals("system")) {

			vd.setType(ViewDefinition.SYSTEM);

		} else if (t.toLowerCase().equals("lookup")) {

			vd.setType(ViewDefinition.LOOKUP);

		} else if (t.toLowerCase().equals("link")) {

			vd.setType(ViewDefinition.LINK);

		} else if (t.toLowerCase().equals("collection")) {

			vd.setType(ViewDefinition.COLLECTION);

		} else if (t.toLowerCase().equals("external")) {

			vd.setType(ViewDefinition.EXTERNAL);

		} else {

			throw new Exception("ViewSpec Definition type: " + t
					+ ", not recognized");

		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// VpdmfLabel & VpdmfUri formats.
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		vd.setVpdmfLabelFormat(vd.getSpec().getVpdmfLabel().getFormat());
		if (vd.getSpec().getVpdmfUri() != null)
			vd.setVpdmfUriFormat(vd.getSpec().getVpdmfUri().getFormat());

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// IndexElements definition.
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		Iterator<IndexElementSpec> ieIt = vd.getSpec().getIndexElements()
				.iterator();
		while (ieIt.hasNext()) {
			IndexElementSpec ieSpec = ieIt.next();

			IndexElement ie = new IndexElement();
			ie.setPosition(ieSpec.getPos());
			ie.setP(ieSpec.getP());
			ie.setC(ieSpec.getC());
			ie.setA(ieSpec.getA());

			if (ieSpec.getNullable() != null) {
				ie.setNullable(ieSpec.getNullable());
			}

			if (ieSpec.getUniqueKey() != null) {
				ie.setUniqueKey(ieSpec.getUniqueKey());
			}

			vd.getIndexElements().put(ie.getPosition(), ie);

		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Simple primitive definition.
		// - we will need additional annotations
		// * unique
		// * editable
		// * nullable
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		Iterator<PrimitiveSpec> pvIt = vd.getSpec().getPrimitives().iterator();
		while (pvIt.hasNext()) {
			PrimitiveSpec pvSpec = pvIt.next();

			PrimitiveDefinition pv = new PrimitiveDefinition();
			pv.setName(pvSpec.getName());

			pv.setEditable(pvSpec.isEditable());
			pv.setNullable(pvSpec.isNullable());

			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// LookupSpec primitive definition.
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			if (pvSpec.getLookupView() != null
					&& pvSpec.getLookupView().length() > 0) {
				pv.setLookupViewLookup(pvSpec.getLookupView());
				if (pv.getName() == null || pv.getName().length() == 0)
					pv.setName(pvSpec.getLookupView());
			}

			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// always define the primary primitive first.
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			if (vd.getPrimaryPrimitive() == null) {
				vd.setPrimaryPrimitive(pv);
			}

			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// link the view to it's primitive graph.
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			vd.getSubGraph().addNode(pv);
			pv.setGraph(vd.getSubGraph());
			pv.setView(vd);

			// ~~~~~~~~~~~~~~~~~~~~~~~~~
			// Simple class definition.
			// ~~~~~~~~~~~~~~~~~~~~~~~~~
			Iterator<ClassSpec> cIt = pvSpec.getClasses().iterator();
			while (cIt.hasNext()) {
				ClassSpec cSpec = cIt.next();

				String s = cSpec.getName();

				UMLclass c = null;
				if (umlClassMap.containsKey(s)) {
					c = umlClassMap.get(s);
				} else {
					Iterator<UMLclass> it = umlClassMap.values().iterator();
					while (it.hasNext() && c == null) {
						UMLclass cc = it.next();
						if (cc.getBaseName().equals(s)) {
							c = cc;
						}
					}
					if (c == null) {
						throw new Exception(
								"~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
										+ "You have an error in your ViewSpec specification for "
										+ vd.getName()
										+ " in the specification of the primitive "
										+ pv.getName()
										+ ".\n\n"
										+ "We cannot locate the class "
										+ s
										+ " in the model. \n"
										+ "Please check the spelling of the class in the ViewSpec or in the Model.\n"
										+ "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");
					}
				}
				pv.getClasses().add(c);
				if (pv.getPrimaryClass() == null) {
					pv.setPrimaryClass(c);
				}
			}

			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// Condition definition.
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			Iterator<ConditionSpec> condIt = pvSpec.getConditions().iterator();
			while (condIt.hasNext()) {
				ConditionSpec condSpec = condIt.next();

				String cName = condSpec.getC();
				Set<UMLclass> sc = vd.getTop().getUmlModel().lookupClass(cName);
				if (sc.size() != 1) {
					throw new Exception(
							"~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
									+ "You have an error in your ViewSpec specification for "
									+ vd.getName()
									+ " in the specification of the primitive "
									+ pv.getName()
									+ ".\n\n"
									+ "The class name "
									+ cName
									+ " in the condition "
									+ condSpec.toString()
									+ " is incorrect.\n"
									+ "Please check the spelling of the class in the ViewSpec or in the Model.\n"
									+ "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");
				}
				UMLclass c = sc.iterator().next();

				String aName = condSpec.getA();
				UMLattribute a = c.lookupAttributeByName(aName);

				String aValue = condSpec.getV();

				ConditionElement conditionElement = new ConditionElement();
				conditionElement.setAttName(aName);
				conditionElement.setClassName(c.getBaseName());
				conditionElement.setValue(aValue);

				pv.getConditionElements().add(conditionElement);

			}

		}

		/*
		 * TODO: Add Documentation field into view spec.
		 */

	}

	private void buildViewsFromShells(VPDMf top) throws Exception {

		// Connect all the PrimitiveDefinitions that use lookups,
		// and copy their primary primitives where appropriate
		Iterator<ViewDefinition> vdIt = top.getViews().values().iterator();
		while (vdIt.hasNext()) {
			ViewDefinition vd = vdIt.next();

			Iterator<SuperGraphNode> pdIt = vd.getSubGraph().getNodes()
					.values().iterator();
			while (pdIt.hasNext()) {
				PrimitiveDefinition pd = (PrimitiveDefinition) pdIt.next();

				if (pd.getLookupViewLookup() != null
						&& pd.getLookupViewLookup().length() > 0) {

					if (!top.getViews().containsKey(pd.getLookupViewLookup())) {
						throw new Exception(
								"\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
										+ "You have an error in your ViewSpec Specification for "
										+ vd.getName()
										+ " within the primitive: "
										+ pd.getName()
										+ "\n"
										+ "You are trying to refer to a lookup view: "
										+ pd.getLookupViewLookup()
										+ " but this view does not exist in the model. "
										+ "\nPlease check to see if this is a typo\n"
										+ "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");
					}
					pd.setLookupView(top.getViews().get(
							pd.getLookupViewLookup()));

					// Copy the whole lookup view into the target primitive
					PrimitiveDefinition temp = pd.getLookupView()
							.getPrimaryPrimitive();

					pd.setPrimaryClass(temp.getPrimaryClass());
					Iterator<UMLclass> cIt = temp.getClasses().iterator();
					while (cIt.hasNext()) {
						UMLclass c = cIt.next();
						pd.getClasses().add(c);
					}

				}

			}

		}

		// reorder the classes within the primitives
		vdIt = top.getViews().values().iterator();
		while (vdIt.hasNext()) {
			ViewDefinition vd = vdIt.next();

			Iterator<SuperGraphNode> pdIt = vd.getSubGraph().getNodes()
					.values().iterator();
			while (pdIt.hasNext()) {
				PrimitiveDefinition pd = (PrimitiveDefinition) pdIt.next();

				pd.orderClasses();

			}

		}

		/* Link the views internally */
		vdIt = top.getViews().values().iterator();
		while (vdIt.hasNext()) {
			ViewDefinition vd = vdIt.next();
			vd.buildPIGLinks_specs();
		}

		// Copy internal roles from the lookupViews into their target primitives
		vdIt = top.getViews().values().iterator();
		while (vdIt.hasNext()) {
			ViewDefinition vd = vdIt.next();

			Iterator<SuperGraphNode> pdIt = vd.getSubGraph().getNodes()
					.values().iterator();
			while (pdIt.hasNext()) {
				PrimitiveDefinition pd = (PrimitiveDefinition) pdIt.next();

				if (pd.getLookupView() != null) {
					PrimitiveDefinition temp = pd.getLookupView()
							.getPrimaryPrimitive();

					Iterator<UMLrole> rIt = temp.getInternalRoles().iterator();
					while (rIt.hasNext()) {
						UMLrole r = rIt.next();
						pd.getInternalRoles().add(r);
					}
				}

			}

		}

		/*
		 * # # Load the formDesign # for $vw (@{ $self->{views}->get_elements()
		 * }) { for $pv (@{ $vw->{subGraph}->{nodes}->get_elements() }) {
		 * $pv->build_formdesign(); } }
		 */

	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// PLUGINS FOR TERMINOLOGY
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Adds the classes from the terminology package into the model.
	 * 
	 * @param pkgPattern
	 * @throws Exception
	 */
	private void addClassesToModelForTerminology(String pkgPattern)
			throws Exception {

		UMLmodel m = this.model;

		if (!m.listClasses().containsKey(
				"|.edu.isi.bmkeg.terminology.model.Term")) {

			InputStream is = ClassLoader
					.getSystemResourceAsStream("edu/isi/bmkeg/terminology/terminology.xml");

			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);

			UMLModelSimpleParser p = new UMLModelSimpleParser(
					UMLmodel.XMI_MAGICDRAW);
			p.parseUMLModelFile(br, "term");
			UMLmodel m2 = p.getUmlModels().get(0);

			m.mergeModel(m2);

		}

		UMLclass termClass = m.listClasses().get(
				"|.edu.isi.bmkeg.terminology.model.Term");

		// add a 'term -> Term' attribute to each top-level class in the model.
		Iterator<UMLclass> cIt = m.listClasses(pkgPattern).values().iterator();
		while (cIt.hasNext()) {
			UMLclass c = cIt.next();

			if (c.getParent() != null || c.getBaseName().equals("Term")
					|| c.getBaseName().equals("Ontology")
					|| c.getBaseName().equals("TermMapping"))
				continue;

			UMLattribute t = new UMLattribute();
			t.setType(termClass);
			t.setBaseName("term");
			t.setImplName("term");
			t.setParentClass(c);
			c.getAttributes().add(t);
		}

	}

	private void constructViewShellsForTerminology(VPDMf top) throws Exception {

		ArrayList<String> viewPaths = new ArrayList<String>();
		viewPaths.add("edu/isi/bmkeg/terminology/specs/term-vw.xml");
		viewPaths.add("edu/isi/bmkeg/terminology/specs/termLU-vw.xml");
		viewPaths.add("edu/isi/bmkeg/terminology/specs/ontology-vw.xml");

		Iterator<String> pathIt = viewPaths.iterator();
		while (pathIt.hasNext()) {
			String path = pathIt.next();

			this.constructViewShellsFromResource(top, path);

		}

	}

	private void addTermElementsToSpecs(VPDMf top, String pkgPattern)
			throws Exception {

		UMLmodel m = this.model;
		UMLclass termClass = m.listClasses().get(
				"|.edu.isi.bmkeg.terminology.model.Term");
		UMLclass ontClass = m.listClasses().get(
				"|.edu.isi.bmkeg.terminology.model.Ontology");
		UMLclass personClass = m.listClasses().get(
				"|.edu.isi.bmkeg.person.model.Person");

		Map<String, UMLclass> classesToInclude = top.getUmlModel().listClasses(
				pkgPattern);

		boolean go = false;

		Iterator<ViewDefinition> vdIt = top.getViews().values().iterator();
		while (vdIt.hasNext()) {
			ViewDefinition vd = vdIt.next();
			PrimitiveDefinition pd = vd.getPrimaryPrimitive();

			UMLclass tc = null;
			Iterator<UMLclass> cIt = pd.getClasses().iterator();
			while (cIt.hasNext()) {
				UMLclass c = cIt.next();
				Iterator<UMLattribute> aIt = c.getAttributes().iterator();
				while (aIt.hasNext()) {
					UMLattribute a = aIt.next();
					if (a.getBaseName().equals("term")
							&& a.getType().equals(termClass)) {
						tc = c;
						break;
					}
				}
			}

			if (tc != null) {
				ViewSpec vSpec = vd.getSpec();
				PrimitiveSpec pSpec = vSpec.getPrimitives().get(0);

				PrimitiveLinkSpec plSpec = new PrimitiveLinkSpec(
						tc.getBaseName(), "term", "Term", "Term");
				pSpec.getPvLinks().add(plSpec);

				pSpec = new PrimitiveSpec();
				pSpec.setName("Ontology");
				ClassSpec cSpec = new ClassSpec();
				cSpec.setName("Ontology");
				pSpec.getClasses().add(cSpec);
				vSpec.getPrimitives().add(pSpec);

				pSpec = new PrimitiveSpec();
				pSpec.setName("Term");
				cSpec = new ClassSpec();
				cSpec.setName("Term");
				pSpec.getClasses().add(cSpec);
				plSpec = new PrimitiveLinkSpec("Term", "ontology", "Ontology",
						"Ontology");
				pSpec.getPvLinks().add(plSpec);
				vSpec.getPrimitives().add(pSpec);

			}

		}

	}

}
