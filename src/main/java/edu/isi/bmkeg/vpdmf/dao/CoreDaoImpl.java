package edu.isi.bmkeg.vpdmf.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.semanticweb.owlapi.model.OWLOntology;

import edu.isi.bmkeg.uml.model.UMLclass;
import edu.isi.bmkeg.uml.utils.OwlAPIUtility;
import edu.isi.bmkeg.vpdmf.controller.queryEngineTools.ChangeEngine;
import edu.isi.bmkeg.vpdmf.controller.queryEngineTools.VPDMfChangeEngineInterface;
import edu.isi.bmkeg.vpdmf.model.ViewTable;
import edu.isi.bmkeg.vpdmf.model.definitions.PrimitiveDefinition;
import edu.isi.bmkeg.vpdmf.model.definitions.VPDMf;
import edu.isi.bmkeg.vpdmf.model.definitions.ViewDefinition;
import edu.isi.bmkeg.vpdmf.model.instances.AttributeInstance;
import edu.isi.bmkeg.vpdmf.model.instances.ClassInstance;
import edu.isi.bmkeg.vpdmf.model.instances.LightViewInstance;
import edu.isi.bmkeg.vpdmf.model.instances.PrimitiveInstance;
import edu.isi.bmkeg.vpdmf.model.instances.ViewBasedObjectGraph;
import edu.isi.bmkeg.vpdmf.model.instances.ViewInstance;
import edu.isi.bmkeg.vpdmf.model.qo.ViewTable_qo;

/**
 * Base class for canonical factories used in KEfED. Sets up the persistence and
 * entity manager and provides useful utility routines for factories that
 * generate specific canonicalized results.
 * 
 * @author University of Southern California
 * @date $Date: 2011-07-06 17:57:37 -0700 (Wed, 06 Jul 2011) $
 * @version $Revision: 2554 $
 * 
 */
public class CoreDaoImpl implements CoreDao {

	private VPDMf top;
	private ClassLoader cl;

	private String login;
	private String password;
	private String uri;

	private VPDMfChangeEngineInterface ce;

	private OwlAPIUtility owlUtil;

	private Map<String, ViewBasedObjectGraph> vbogs;

	public CoreDaoImpl() throws Exception {
		this.owlUtil = new OwlAPIUtility();
		this.cl = this.getClass().getClassLoader();
	}

	public void init() throws Exception {
		this.init(login, password, uri);
	}

	public void init(String login, String password, String uri)
			throws Exception {

		this.ce = new ChangeEngine(login, password, uri);
		this.ce.connectToDB();
		this.top = this.ce.readTop();
		// this.cl = this.ce.provideClassLoaderForModel();
		this.ce.closeDbConnection();

	}

	public VPDMfChangeEngineInterface getCe() {
		return ce;
	}

	public void setCe(VPDMfChangeEngineInterface ce) {
		this.ce = ce;
	}

	public VPDMf getTop() {
		return top;
	}

	public void setTop(VPDMf top) {
		this.top = top;
	}

	public ClassLoader getCl() {
		return cl;
	}

	public void setCl(ClassLoader cl) {
		this.cl = cl;
	}

	public void saveViewInstanceToOntology(OWLOntology o, String uri,
			ViewInstance vi) throws Exception {

		Map<String, ViewBasedObjectGraph> vbogs = generateVbogs();

		PrimitiveInstance pi = vi.getPrimaryPrimitive();
		PrimitiveDefinition pd = pi.getDefinition();

		UMLclass umlClass = pd.getClasses().get(pd.getClasses().size() - 1);

		ClassInstance ci = pi.getObjects().get(umlClass.getBaseName());
		Iterator<AttributeInstance> aiIt = ci.getAttributes().values()
				.iterator();

		PrimitiveInstance tPi = (PrimitiveInstance) vi.getSubGraph().getNodes()
				.get("Term_0");
		if (tPi == null) {
			return;
		}

		String termValue = vi.readAttributeInstance("]Term|Term.termValue", 0)
				.readValueString();
		String shortTermId = vi.readAttributeInstance("]Term|Term.shortTermId",
				0).readValueString();
		String definition = vi
				.readAttributeInstance("]Term|Term.definition", 0)
				.readValueString();

		this.owlUtil.addIndividualToClass(umlClass.readClassAddress(),
				shortTermId, o);
		this.owlUtil.addNameComment(shortTermId, termValue, o);

		this.owlUtil.addExternalAnnotation(shortTermId, "definition",
				definition, o);

		// pIt = pigTraversal.nodeTraversal.iterator();
		// while (pIt.hasNext()) {
		// PrimitiveInstance pi = (PrimitiveInstance) pIt.next();

		// primitiveToObject(pi);

		// }

	}

	public Map<String, ViewBasedObjectGraph> regenerateVbogs() throws Exception {

		vbogs = null;
		return this.generateVbogs();

	}

	public Map<String, ViewBasedObjectGraph> generateVbogs() throws Exception {

		if (vbogs != null) {
			return vbogs;
		}

		Map<String, ViewBasedObjectGraph> vbogs = new HashMap<String, ViewBasedObjectGraph>();

		Iterator<String> keysIt = getTop().getViews().keySet().iterator();
		while (keysIt.hasNext()) {
			String key = keysIt.next();
			vbogs.put(key, new ViewBasedObjectGraph(getTop(), getCl(), key));
		}

		this.vbogs = vbogs;

		return vbogs;
	}

	public List<LightViewInstance> goGetLightViewList(String viewName,
			String attrAddr, String attrVal) throws Exception {

		Map<String, ViewBasedObjectGraph> vbogs = generateVbogs();

		ViewDefinition vd = getTop().getViews().get(viewName);
		ViewInstance qVi = new ViewInstance(vd);
		AttributeInstance ai = qVi.readAttributeInstance(attrAddr, 0);
		ai.writeValueString(attrVal);
		List<LightViewInstance> l = getCe().executeListQuery(qVi);

		return l;

	}

	public List<ViewInstance> goGetHeavyViewList(String viewName,
			String attrAddr, String attrVal) throws Exception {

		Map<String, ViewBasedObjectGraph> vbogs = generateVbogs();

		ViewDefinition vd = getTop().getViews().get(viewName);
		ViewInstance qVi = new ViewInstance(vd);
		AttributeInstance ai = qVi.readAttributeInstance(attrAddr, 0);
		ai.writeValueString(attrVal);
		List<ViewInstance> l = getCe().executeFullQuery(qVi);

		return l;

	}

	public List<LightViewInstance> listAllViews(String viewName)
			throws Exception {

		ViewDefinition vd = getTop().getViews().get(viewName);
		ViewInstance qVi = new ViewInstance(vd);

		getCe().connectToDB();

		List<LightViewInstance> viewList = getCe().executeListQuery(qVi);

		getCe().closeDbConnection();

		return viewList;

	}

	public List<LightViewInstance> listAllViews(String viewName,
			boolean paging, int start, int pageSize) throws Exception {

		ViewDefinition vd = getTop().getViews().get(viewName);
		ViewInstance qVi = new ViewInstance(vd);

		getCe().connectToDB();
		List<LightViewInstance> viewList = null;
		if (paging) {
			viewList = getCe().executeListQuery(qVi, paging, start, pageSize);
		} else {
			viewList = getCe().executeListQuery(qVi);
		}
		getCe().closeDbConnection();

		return viewList;

	}

	public long insertVBOG(Object ov, String viewName) throws Exception {

		long vpdmfId = 0;

		try {

			getCe().connectToDB();
			getCe().turnOffAutoCommit();

			ViewBasedObjectGraph vbog = new ViewBasedObjectGraph(getTop(),
					getCl(), viewName);

			ViewInstance vi = vbog.objectGraphToView(ov);
			vi.reconstructIndexStrings();
			Map<String, Object> objMap = vbog.getObjMap();

			vpdmfId = getCe().executeInsertQuery(vi);

			// TODO move the following recurring fragment to some Utils class
			Iterator<String> keyIt = objMap.keySet().iterator();
			while (keyIt.hasNext()) {
				String key = keyIt.next();
				PrimitiveInstance pi = (PrimitiveInstance) vi.getSubGraph()
						.getNodes().get(key);
				Object o = objMap.get(key);
				vbog.primitiveToObject(pi, o, true);
			}

			getCe().commitTransaction();

		} catch (Exception e) {

			getCe().rollbackTransaction();

			throw e;

		} finally {

			getCe().closeDbConnection();

		}

		return vpdmfId;
	}

	public Object findVBOGById(long id, String viewName) throws Exception {

		try {

			getCe().connectToDB();
			getCe().turnOffAutoCommit();

			ViewInstance vi = getCe().executeUIDQuery(viewName, id);

			ViewBasedObjectGraph vbog = generateVbogs().get(viewName);
			vbog.viewToObjectGraph(vi);
			Object ov = vbog.readPrimaryObject();

			return ov;

		} finally {
			getCe().closeDbConnection();
		}

	}

	/**
	 * Finds a view instances matching the condition:
	 * <viewName>]<primitiveName>|<className>.<attributeName> = <attributeValue>
	 * and converts them into a VBOG
	 */
	public Object findVBOGByAttributeValue(String viewName,
			String primitiveName, String className, String attributeName,
			String attributeValue) throws Exception {

		try {

			getCe().connectToDB();
			getCe().turnOffAutoCommit();

			ViewDefinition vd = getTop().getViews().get(viewName);
			ViewBasedObjectGraph vbog = generateVbogs().get(viewName);

			ViewInstance qvi = new ViewInstance(vd);

			AttributeInstance ai = qvi.readAttributeInstance("]" + primitiveName
					+ "|" + className + "." + attributeName, 0);
			ai.writeValueString(attributeValue);

			Object o = null;

			List<LightViewInstance> l = getCe().executeListQuery(qvi, true, 0, 1);
			if (l.size() == 1) {
				LightViewInstance lvi = l.get(0);
				ViewInstance vi = getCe().executeUIDQuery(lvi);
				vbog.viewToObjectGraph(vi);
				o = vbog.readPrimaryObject();
			}
			
			return o;

		} finally {
			getCe().closeDbConnection();
		}

	}

	/**
	 * Retrieves view instances matching the condition:
	 * <viewName>]<primitiveName>|<className>.<attributeName> = <attributeValue>
	 * and converts them into a list of VBOGs
	 */
	public List<?> retrieveVBOGsByAttributeValue(String viewName,
			String primitiveName, String className, String attributeName,
			String attributeValue, int offset, int pageSize) throws Exception {
		try {

			getCe().connectToDB();
			getCe().turnOffAutoCommit();

			ViewDefinition vd = getTop().getViews().get(viewName);
			ViewBasedObjectGraph vbog = generateVbogs().get(viewName);

			ViewInstance vi = new ViewInstance(vd);

			AttributeInstance ai = vi.readAttributeInstance("]" + primitiveName
					+ "|" + className + "." + attributeName, 0);
			ai.writeValueString(attributeValue);

			List<Object> l = new ArrayList<Object>();

			Iterator<ViewInstance> it = getCe().executeFullQuery(vi, true,
					offset, pageSize).iterator();
			while (it.hasNext()) {
				ViewInstance lvi = it.next();

				vbog.viewToObjectGraph(lvi);
				Object o = vbog.readPrimaryObject();
				l.add(o);

			}

			return l;

		} catch (Exception e) {
			e.printStackTrace();
			return null;

		} finally {
			getCe().closeDbConnection();
		}
	}

	/**
	 * Returns the complete object graph of any view given a vpdmfId string.
	 * 
	 * @param vpdmfId
	 *            Must be formatted as a string: "bmkeg=1234"
	 * @param viewType
	 *            An instance of the object of the primary primitive of the view
	 * @return The complete object graph of that particular view.
	 * @throws Exception
	 */
	public <T> T loadViewBasedObject(Long vpdmfId, T viewType) throws Exception {

		String viewTypeName = viewType.getClass().getSimpleName();

		ViewBasedObjectGraph vbog = new ViewBasedObjectGraph(this.getTop(),
				this.getCl(), viewTypeName);

		this.getCe().connectToDB();
		ViewInstance vi = this.getCe().executeUIDQuery(viewTypeName, vpdmfId);
		this.getCe().closeDbConnection();

		Map<String, Object> objMap = vbog.viewToObjectGraph(vi);
		Iterator<String> keyIt = objMap.keySet().iterator();
		while (keyIt.hasNext()) {
			String key = keyIt.next();
			PrimitiveInstance pi = (PrimitiveInstance) vi.getSubGraph()
					.getNodes().get(key);
			Object o = objMap.get(key);
			vbog.primitiveToObject(pi, o, true);
		}

		T o = (T) vbog.readPrimaryObject();

		return o;

	}

	// ~~~~~~~~~~~~~~~~~~
	// final operations
	// ~~~~~~~~~~~~~~~~~~

	public <T extends ViewTable> long update(T obj, String viewTypeName)
			throws Exception {

		long vpdmfId = 0;

		try {

			getCe().connectToDB();
			getCe().turnOffAutoCommit();

			vpdmfId = this.updateInTrans(obj, viewTypeName);

			getCe().commitTransaction();

		} catch (Exception e) {

			getCe().rollbackTransaction();

			throw e;

		} finally {

			getCe().closeDbConnection();

		}

		return vpdmfId;

	}

	public <T extends ViewTable> long insert(T obj, String viewTypeName)
			throws Exception {

		long vpdmfId = 0;

		try {

			getCe().connectToDB();
			getCe().turnOffAutoCommit();

			vpdmfId = this.insertInTrans(obj, viewTypeName);

			getCe().commitTransaction();

		} catch (Exception e) {

			getCe().rollbackTransaction();

			throw e;

		} finally {

			getCe().closeDbConnection();

		}

		return vpdmfId;
	}

	public <T extends ViewTable> List<T> retrieve(T obj, String viewTypeName,
			int offset, int pageSize) throws Exception {

		try {

			getCe().connectToDB();
			getCe().turnOffAutoCommit();

			List<T> l = this.retrieveInTrans(obj, viewTypeName, offset,
					pageSize);

			return l;

		} finally {

			getCe().closeDbConnection();

		}

	}

	public <T extends ViewTable> List<T> retrieve(T obj, String viewTypeName)
			throws Exception {

		try {

			getCe().connectToDB();
			getCe().turnOffAutoCommit();

			List<T> l = this.retrieveInTrans(obj, viewTypeName);

			return l;

		} finally {

			getCe().closeDbConnection();

		}

	}

	public <T extends ViewTable_qo> List<LightViewInstance> list(T obj,
			String viewTypeName, int offset, int pageSize) throws Exception {

		if (!this.getTop().getViews().containsKey(viewTypeName)) {
			throw new Exception(viewTypeName + " view not found!");
		}

		try {

			getCe().connectToDB();
			getCe().turnOffAutoCommit();

			List<LightViewInstance> l = this.listInTrans(obj, viewTypeName,
					offset, pageSize);

			return l;

		} finally {

			getCe().closeDbConnection();

		}

	}

	public <T extends ViewTable_qo> List<LightViewInstance> list(T obj,
			String viewTypeName) throws Exception {

		if (!this.getTop().getViews().containsKey(viewTypeName)) {
			throw new Exception(viewTypeName + " view not found!");
		}

		try {

			getCe().connectToDB();
			getCe().turnOffAutoCommit();

			List<LightViewInstance> l = this.listInTrans(obj, viewTypeName);

			return l;

		} finally {

			getCe().closeDbConnection();

		}

	}
	

	public <T extends ViewTable> T findById(long id, T obj, String viewTypeName)
			throws Exception {

		try {

			getCe().connectToDB();
			getCe().turnOffAutoCommit();

			T ov = this.findByIdInTrans(id, obj, viewTypeName);

			return ov;

		} finally {

			getCe().closeDbConnection();

		}

	}

	public int countView(String viewTypeName) throws Exception {

		if (!this.getTop().getViews().containsKey(viewTypeName)) {
			throw new Exception(viewTypeName + " view not found!");
		}

		this.getCe().connectToDB();
		int count = this.countViewInTrans(viewTypeName);
		this.getCe().closeDbConnection();

		return count;

	}

	public <T extends ViewTable_qo> int countView(T obj, String viewTypeName)
			throws Exception {

		if (!this.getTop().getViews().containsKey(viewTypeName)) {
			throw new Exception(viewTypeName + " view not found!");
		}

		this.getCe().connectToDB();
		int count = this.countViewInTrans(obj, viewTypeName);
		this.getCe().closeDbConnection();

		return count;

	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// operations occurring within an external transaction
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public <T extends ViewTable> long updateInTrans(T obj, String viewTypeName)
			throws Exception {

		ViewInstance vi0;
		try {
			vi0 = getCe().executeUIDQuery(viewTypeName, obj.getVpdmfId());
		} catch (Exception e) {
			throw new Exception("No " + viewTypeName + " with id: "
					+ obj.getVpdmfId()
					+ " was found for updating. You might want to use an "
					+ " insert function instead.");
		}

		getCe().storeViewInstanceForUpdate(vi0);

		ViewBasedObjectGraph vbog = new ViewBasedObjectGraph(this.getTop(),
				this.getCl(), viewTypeName);

		ViewInstance vi1 = vbog.objectGraphToView(obj);

		Map<String, Object> objMap = vbog.getObjMap();

		long vpdmfId = getCe().executeUpdateQuery(vi1);

		Iterator<String> keyIt = objMap.keySet().iterator();
		while (keyIt.hasNext()) {
			String key = keyIt.next();
			PrimitiveInstance pi = (PrimitiveInstance) vi1.getSubGraph()
					.getNodes().get(key);
			Object o = objMap.get(key);
			vbog.primitiveToObject(pi, o, true);
		}

		return vpdmfId;

	}

	public <T extends ViewTable> long insertInTrans(T obj, String viewTypeName)
			throws Exception {

		ViewBasedObjectGraph vbog = new ViewBasedObjectGraph(getTop(), getCl(),
				viewTypeName);

		ViewInstance vi = vbog.objectGraphToView(obj);

		vi.reconstructIndexStrings();
		
		Map<String, Object> objMap = vbog.getObjMap();

		long vpdmfId = getCe().executeInsertQuery(vi);

		// TODO move the following recurring fragment to some Utils class
		Iterator<String> keyIt = objMap.keySet().iterator();
		while (keyIt.hasNext()) {
			String key = keyIt.next();
			PrimitiveInstance pi = (PrimitiveInstance) vi.getSubGraph()
					.getNodes().get(key);
			Object o = objMap.get(key);
			vbog.primitiveToObject(pi, o, true);
		}

		return vpdmfId;

	}

	public <T extends ViewTable> List<T> retrieveInTrans(T obj,
			String viewTypeName, int offset, int pageSize) throws Exception {

		ViewBasedObjectGraph vbog = new ViewBasedObjectGraph(this.getTop(),
				this.getCl(), viewTypeName);
		ViewInstance vi = vbog.objectGraphToView(obj, false);

		List<T> l = new ArrayList<T>();
		Iterator<ViewInstance> it = getCe().executeFullQuery(vi, true, offset,
				pageSize).iterator();
		while (it.hasNext()) {
			ViewInstance lvi = it.next();

			vbog.viewToObjectGraph(lvi);
			T a = (T) vbog.readPrimaryObject();

			l.add(a);

		}

		return l;

	}

	public <T extends ViewTable> List<T> retrieveInTrans(T obj, String viewTypeName) throws Exception {

		ViewBasedObjectGraph vbog = new ViewBasedObjectGraph(this.getTop(),
				this.getCl(), viewTypeName);
		ViewInstance vi = vbog.objectGraphToView(obj, false);

		List<T> l = new ArrayList<T>();
		Iterator<ViewInstance> it = getCe().executeFullQuery(vi).iterator();
		while (it.hasNext()) {
			ViewInstance lvi = it.next();

			vbog.viewToObjectGraph(lvi);
			T a = (T) vbog.readPrimaryObject();

			l.add(a);

		}

		return l;

	}

	public <T extends ViewTable_qo> List<LightViewInstance> listInTrans(T obj,
			String viewTypeName, int offset, int pageSize) throws Exception {

		ViewBasedObjectGraph vbog = new ViewBasedObjectGraph(this.getTop(),
				this.getCl(), viewTypeName);
		ViewInstance vi = vbog.objectGraphToView(obj, false);

		List<LightViewInstance> l = new ArrayList<LightViewInstance>();
		Iterator<LightViewInstance> it = getCe().executeListQuery(vi, true,
				offset, pageSize).iterator();
		while (it.hasNext()) {
			LightViewInstance lvi = it.next();
			lvi.setDefinition(null);
			l.add(lvi);
		}

		return l;

	}

	public <T extends ViewTable_qo> List<LightViewInstance> listInTrans(T obj,
			String viewTypeName) throws Exception {

		ViewBasedObjectGraph vbog = new ViewBasedObjectGraph(this.getTop(),
				this.getCl(), viewTypeName);
		ViewInstance vi = vbog.objectGraphToView(obj, false);

		List<LightViewInstance> l = new ArrayList<LightViewInstance>();
		Iterator<LightViewInstance> it = getCe().executeListQuery(vi)
				.iterator();
		while (it.hasNext()) {
			LightViewInstance lvi = it.next();
			lvi.setDefinition(null);
			l.add(lvi);
		}

		return l;

	}

	public <T extends ViewTable> T findByIdInTrans(long id, T obj,
			String viewTypeName) throws Exception {

		ViewInstance vi = getCe().executeUIDQuery(viewTypeName, id);

		if( vi == null) 
			return null;
		
		vi.convertImagesToStreams();
		
		ViewBasedObjectGraph vbog = generateVbogs().get(viewTypeName);
		vbog.viewToObjectGraph(vi);
		T ov = (T) vbog.readPrimaryObject();

		return ov;

	}

	public int countViewInTrans(String viewTypeName) throws Exception {

		ViewDefinition vd = this.getTop().getViews().get(viewTypeName);
		ViewInstance vi = new ViewInstance(vd);

		int count = this.getCe().executeCountQuery(vi);

		return count;

	}

	public <T extends ViewTable_qo> int countViewInTrans(T obj, String viewTypeName)
			throws Exception {

		ViewBasedObjectGraph vbog = new ViewBasedObjectGraph(this.getTop(),
				this.getCl(), viewTypeName);

		ViewInstance vi = vbog.objectGraphToView(obj, false);

		int count = this.getCe().executeCountQuery(vi);

		return count;

	}

	/*
	 * public Map<PrimitiveInstance, Term>
	 * buildTermLookupForViewInstance(ViewInstance vi) throws Exception {
	 * 
	 * Map<String, ViewBasedObjectGraph> vbogs = generateVbogs();
	 * ViewBasedObjectGraph vbog = vbogs.get(vi.getDefName());
	 * vbog.viewToObjectGraph(vi);
	 * 
	 * Map<PrimitiveInstance, Term> tLookup = new HashMap<PrimitiveInstance,
	 * Term>(); PrimitiveInstanceGraph pig = (PrimitiveInstanceGraph)
	 * vi.getSubGraph(); kmrgGraphTraversal pigTraversal = pig.readTraversal();
	 * Iterator<kmrgGraphEdge> pliIt = pigTraversal.edgeTraversal.iterator();
	 * while (pliIt.hasNext()) { PrimitiveLinkInstance pli =
	 * (PrimitiveLinkInstance) pliIt.next();
	 * 
	 * UMLrole r = pli.getPVLinkDef().getRole(); String dClassName =
	 * r.getDirectClass().getBaseName();
	 * 
	 * if( dClassName.equals("Ontology") || dClassName.equals("TermMapping") ||
	 * dClassName.equals("Person")) continue;
	 * 
	 * if (dClassName.equals("Term") && (r.getBaseName().equals("ontology") ||
	 * r.getBaseName() .equals("definitionEditor"))) continue;
	 * 
	 * PrimitiveInstance pi1 = (PrimitiveInstance) pli.getOutEdgeNode();
	 * PrimitiveInstance pi2 = (PrimitiveInstance) pli.getInEdgeNode();
	 * 
	 * PrimitiveInstance sPi = null, tPi = null; if(
	 * pi1.getDefinition().getPrimaryClass().getBaseName().equals("Term") ) {
	 * sPi = pi2; tPi = pi1; } else if(
	 * pi2.getDefinition().getPrimaryClass().getBaseName().equals("Term") ) {
	 * sPi = pi1; tPi = pi2; } else { continue; }
	 * 
	 * Map<String, Object> objMap = vbog.getObjMap(); Object o =
	 * objMap.get(tPi.getName()); if(o == null) throw new
	 * Exception("Can't find primitive " + tPi.getDefinition().getName() );
	 * 
	 * Term t = (Term) o;
	 * 
	 * if(r.getBaseName().equals("term")) { tLookup.put(sPi, t); } else {
	 * tLookup.put(tPi, t); }
	 * 
	 * } return tLookup; }
	 */

	// ~~~~~~~~~~~~~~~~~~
	// getters 'n setters
	// ~~~~~~~~~~~~~~~~~~

	public Map<String, ViewBasedObjectGraph> getVbogs() {
		return vbogs;
	}

	public void setVbogs(Map<String, ViewBasedObjectGraph> vbogs) {
		this.vbogs = vbogs;
	}

	public OwlAPIUtility getOwlUtil() {
		return owlUtil;
	}

	public void setOwlUtil(OwlAPIUtility owlUtil) {
		this.owlUtil = owlUtil;
	}

	public String getLogin() {
		return login;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

}
