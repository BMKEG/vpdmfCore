package edu.isi.bmkeg.vpdmf.dao;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.isi.bmkeg.uml.model.UMLattribute;
import edu.isi.bmkeg.uml.model.UMLclass;
import edu.isi.bmkeg.utils.Converters;
import edu.isi.bmkeg.vpdmf.controller.queryEngineTools.ChangeEngineImpl;
import edu.isi.bmkeg.vpdmf.controller.queryEngineTools.ChangeEngine;
import edu.isi.bmkeg.vpdmf.model.definitions.VPDMf;
import edu.isi.bmkeg.vpdmf.model.instances.AttributeInstance;
import edu.isi.bmkeg.vpdmf.model.instances.ClassInstance;
import edu.isi.bmkeg.vpdmf.model.instances.LightViewInstance;
import edu.isi.bmkeg.vpdmf.model.instances.PrimitiveInstance;
import edu.isi.bmkeg.vpdmf.model.instances.ViewBasedObjectGraph;
import edu.isi.bmkeg.vpdmf.model.instances.ViewInstance;
import edu.isi.bmkeg.vpdmf.model.instances.VpdmfObject;
import edu.isi.bmkeg.vpdmf.model.instances.VpdmfQueryObject;

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
public class CoreDaoElasticSearchImpl implements CoreDao {

	private VPDMf top;
	private ClassLoader cl;

	private String login;
	private String password;
	private String uri;
	private String workingDirectory;

	private ChangeEngine ce;

	public CoreDaoElasticSearchImpl() throws Exception {
		this.cl = this.getClass().getClassLoader();
	}

	public void init() throws Exception {
		this.init(login, password, uri, workingDirectory);
	}

	public void init(String login, String password, String uri, String workingDirectory)
			throws Exception {

		this.ce = new ChangeEngineImpl(login, password, uri);
		this.ce.connectToDB();
		this.top = this.ce.readTop();
		this.workingDirectory = workingDirectory;
		
		//this.cl = this.ce.provideClassLoaderForModel();
		this.ce.closeDbConnection();

	}

	public ChangeEngine getCe() {
		return ce;
	}

	public void setCe(ChangeEngine ce) {
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
	
	@Override
	public void connectToDb() throws Exception {

		this.ce.connectToDB();
		this.ce.turnOffAutoCommit();
	
	}

	@Override
	public void commitTransaction() throws Exception {
	
		this.ce.commitTransaction();
	
	}

	@Override
	public void rollbackTransaction() throws Exception {
	
		this.ce.rollbackTransaction();
		
	}

	@Override
	public void closeDbConnection() throws Exception {
		
		this.ce.closeDbConnection();
		
	}

	// ~~~~~~~~~~~~~~~~~~
	// final operations
	// ~~~~~~~~~~~~~~~~~~
	@Override
	public <T extends VpdmfObject> long update(T obj, String viewTypeName)
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

	@Override
	public <T extends VpdmfObject> long insert(T obj, String viewTypeName)
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
	
	@Override
	public <T extends VpdmfObject> List<T> retrieve(T obj, String viewTypeName,
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
	
	@Override
	public <T extends VpdmfObject> List<T> retrieve(T obj, String viewTypeName)
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
	
	@Override
	public <T extends VpdmfQueryObject> List<LightViewInstance> list(T obj,
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

	@Override
	public <T extends VpdmfQueryObject> List<LightViewInstance> list(T obj,
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
	
	@Override
	public <T extends VpdmfObject> T findById(long id, T obj, String viewTypeName)
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
	

	@Override
	public <T extends VpdmfQueryObject> int countView(T obj, String viewTypeName)
			throws Exception {

		if (!this.getTop().getViews().containsKey(viewTypeName)) {
			throw new Exception(viewTypeName + " view not found!");
		}

		this.getCe().connectToDB();
		int count = this.countViewInTrans(obj, viewTypeName);
		this.getCe().closeDbConnection();

		return count;

	}
	
	@Override
	public boolean deleteById(long id, String viewTypeName)
			throws Exception {

		boolean complete = false;
		
		try {

			getCe().connectToDB();
			getCe().turnOffAutoCommit();

			complete = this.deleteByIdInTrans(id, viewTypeName);

			getCe().commitTransaction();

		} catch (Exception e) {

			getCe().rollbackTransaction();

			complete = false;
			
			throw e;
		
		} finally {

			getCe().closeDbConnection();
			
		}
		
		return complete;

	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// operations occurring within an external transaction
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public <T extends VpdmfObject> long updateInTrans(T obj, String viewTypeName)
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

		ViewInstance vi1 = vbog.objectGraphToView(obj, false);
				
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

	@Override
	public <T extends VpdmfObject> long insertInTrans(T obj, String viewTypeName)
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

	@Override
	public <T extends VpdmfObject> List<T> retrieveInTrans(T obj,
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

	@Override
	public <T extends VpdmfObject> List<T> retrieveInTrans(T obj, String viewTypeName) throws Exception {

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

	@Override
	public <T extends VpdmfQueryObject> List<LightViewInstance> listInTrans(T obj,
			String viewTypeName, int offset, int pageSize) throws Exception {

		ViewBasedObjectGraph vbog = new ViewBasedObjectGraph(this.getTop(),
				this.getCl(), viewTypeName);
		ViewInstance vi = vbog.objectGraphToView(obj, false);

		List<String> sortAddr = new ArrayList<String>();
		for(Integer ii : Converters.asSortedList(vbog.getSortAddr().keySet()) ) {
			sortAddr.add(vbog.getSortAddr().get(ii));
		}

		List<LightViewInstance> l = new ArrayList<LightViewInstance>();
		Iterator<LightViewInstance> it = getCe().executeListQuery(vi, 
				sortAddr, true,
				offset, pageSize).iterator();
		while (it.hasNext()) {
			LightViewInstance lvi = it.next();
			lvi.setDefinition(null);
			l.add(lvi);
		}

		return l;

	}
	
	@Override
	public <T extends VpdmfQueryObject> List<LightViewInstance> listInTrans(T obj,
			String viewTypeName) throws Exception {

		ViewBasedObjectGraph vbog = new ViewBasedObjectGraph(this.getTop(),
				this.getCl(), viewTypeName);
		ViewInstance vi = vbog.objectGraphToView(obj, false);

		List<LightViewInstance> l = new ArrayList<LightViewInstance>();
		
		List<String> sortAddr = new ArrayList<String>();
		for(Integer ii : Converters.asSortedList(vbog.getSortAddr().keySet()) ) {
			sortAddr.add(vbog.getSortAddr().get(ii));
		}
		
		Iterator<LightViewInstance> it = getCe().executeListQuery(vi, sortAddr)
				.iterator();
		while (it.hasNext()) {
			LightViewInstance lvi = it.next();
			lvi.setDefinition(null);
			l.add(lvi);
		}

		return l;

	}

	@Override
	public <T extends VpdmfObject> T findByIdInTrans(long id, T obj,
			String viewTypeName) throws Exception {

		ViewInstance vi = getCe().executeUIDQuery(viewTypeName, id);

		if( vi == null) 
			return null;
		
		vi.convertImagesToStreams();
		
		ViewBasedObjectGraph vbog = new ViewBasedObjectGraph(getTop(), 
				getCl(), 
				viewTypeName);
		vbog.viewToObjectGraph(vi);
		T ov = (T) vbog.readPrimaryObject();

		return ov;

	}

	@Override
	public <T extends VpdmfQueryObject> int countViewInTrans(T obj, String viewTypeName)
			throws Exception {

		ViewBasedObjectGraph vbog = new ViewBasedObjectGraph(this.getTop(),
				this.getCl(), viewTypeName);

		ViewInstance vi = vbog.objectGraphToView(obj, false);

		int count = this.getCe().executeCountQuery(vi);

		return count;

	}
	
	@Override
	public boolean deleteByIdInTrans(long id, String viewTypeName) throws Exception {

		return getCe().executeDeleteQuery(viewTypeName, id);
		
	}
	
	// ~~~~~~~~~~~~~~~~~~~~~~
	// class level operations 
	// ~~~~~~~~~~~~~~~~~~~~~~
	@Override
	public <T1 extends VpdmfQueryObject, T2> List<T2> listClassInTrans(T1 qObj, T2 rObj) 
			throws Exception {
		
		VPDMf top = getCe().readTop();
		
		String qoName = qObj.getClass().getSimpleName();
		Map<String, Method> qMethods = new HashMap<String, Method>();
		Method m1Array[] = qObj.getClass().getMethods();
		for (int i = 0; i < m1Array.length; i++) {
			Method m = m1Array[i];
			String mName = m.getName();
			qMethods.put(mName, m);
		}

		String roName = rObj.getClass().getSimpleName();
		Map<String, Method> rMethods = new HashMap<String, Method>();
		Method m2Array[] = rObj.getClass().getMethods();
		for (int i = 0; i < m2Array.length; i++) {
			Method m = m2Array[i];
			String mName = m.getName();
			rMethods.put(mName, m);
		}
		
		String cName = qoName.replaceAll("_qo", "");
		if( !cName.equals(rObj.getClass().getSimpleName() ) ) {
			throw new Exception("Classnames for query object (" + 
						qObj.getClass().getSimpleName() + 
						") and returned object (" + 
						rObj.getClass().getSimpleName() + 
						")do not match");
		}
		
		Set<UMLclass> cSet = top.getUmlModel().lookupClass(cName);
		
		if( cSet.size() != 1 ) {
			throw new Exception("Can't find unique reference to class: " + qoName );
		}
		
		UMLclass c = cSet.iterator().next();
		ClassInstance ci = new ClassInstance(c);

		for( String attName : ci.getAttributes().keySet() ) {
			AttributeInstance ai = ci.getAttributes().get(attName);
			
			String getterName = "get"
					+ attName.substring(0, 1).toUpperCase()
					+ attName.substring(1, attName.length());
			Method m = qMethods.get(getterName);
			if (m == null) {
				continue;
			}			
			Object value = m.invoke(qObj);
			ai.writeValueString((String)value);

		}

		List<ClassInstance> lci = getCe().queryClass(ci);
		List<T2> rList = new ArrayList<T2>();
		
		for( ClassInstance rCi : lci ) {
			T2 o2 = (T2) rObj.getClass().newInstance();
			
			for( String attName : rCi.getAttributes().keySet() ) {
				AttributeInstance ai = rCi.getAttributes().get(attName);
				
				if(ai.getDefinition().getPk() != null) 
					continue;
				
				String setterName = "set"
						+ attName.substring(0, 1).toUpperCase()
						+ attName.substring(1, attName.length());
				Method m = rMethods.get(setterName);
				if (m == null) {
					throw new Exception("No method " + roName + "." + setterName);
				}			
				m.invoke(o2, ai.getValue());
				
			}
			
			rList.add(o2);
			
		}

		return rList;
		
	}


	// ~~~~~~~~~~~~~~~~~~
	// getters 'n setters
	// ~~~~~~~~~~~~~~~~~~

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

	public String getWorkingDirectory() {
		return workingDirectory;
	}

	public void setWorkingDirectory(String workingDirectory) {
		this.workingDirectory = workingDirectory;
	}

}
