package edu.isi.bmkeg.vpdmf.dao;

import java.util.List;
import java.util.Map;

import org.semanticweb.owlapi.model.OWLOntology;

import edu.isi.bmkeg.vpdmf.controller.queryEngineTools.VPDMfChangeEngineInterface;
import edu.isi.bmkeg.vpdmf.model.ViewTable;
import edu.isi.bmkeg.vpdmf.model.definitions.VPDMf;
import edu.isi.bmkeg.vpdmf.model.instances.LightViewInstance;
import edu.isi.bmkeg.vpdmf.model.instances.ViewBasedObjectGraph;
import edu.isi.bmkeg.vpdmf.model.instances.ViewInstance;
import edu.isi.bmkeg.vpdmf.model.instances.VpdmfObject;
import edu.isi.bmkeg.vpdmf.model.instances.VpdmfQueryObject;
import edu.isi.bmkeg.vpdmf.model.qo.ViewTable_qo;

public interface CoreDao {

	public VPDMfChangeEngineInterface getCe();
	
	public VPDMf getTop();
	
	public ClassLoader getCl();
	
	public void init() throws Exception;
	
	public void init(String login, String password, String uri) throws Exception;	

	// ~~~~~~~~~~~~~~~~~~
	// getters 'n setters
	// ~~~~~~~~~~~~~~~~~~

	public String getLogin();

	public void setLogin(String login);

	public String getPassword();

	public void setPassword(String password);

	public String getUri();

	public void setUri(String uri);
	
	// ~~~~~~~~~~~~~~~~~~~~~~
	// Transaction management
	// ~~~~~~~~~~~~~~~~~~~~~~
	public void connectToDb() throws Exception;

	public void commitTransaction() throws Exception;

	public void rollbackTransaction() throws Exception;
	
	public void closeDbConnection() throws Exception;
	
	// ~~~~~~~~~~~~~~~~~~~~~~~
	// final simple operations
	// ~~~~~~~~~~~~~~~~~~~~~~~
	
	public <T extends VpdmfObject> T findById(long id, T obj, String viewTypeName) throws Exception;
	
	public boolean deleteById(long id, String viewTypeName) throws Exception;

	public <T extends VpdmfObject> long insert(T ov, String viewTypeName) throws Exception;

	public <T extends VpdmfObject> long update(T obj, String viewTypeName) throws Exception;
	
	public <T extends VpdmfObject> List<T> retrieve(T obj, String viewTypeName, int offset, int pageSize) throws Exception;
	
	public <T extends VpdmfObject> List<T> retrieve(T obj, String viewTypeName) throws Exception;
	
	public <T extends VpdmfQueryObject> List<LightViewInstance> list(T obj, String viewTypeName) throws Exception;
	
	public <T extends VpdmfQueryObject> List<LightViewInstance> list(T obj, String viewTypeName, int offset, int pageSize) throws Exception;

	public <T extends VpdmfQueryObject> int countView(T obj, String viewTypeName) throws Exception;
	
	// ~~~~~~~~~~~~~~~~~~~~
	// 'inTrans' operations
	// ~~~~~~~~~~~~~~~~~~~~
	
	public <T extends VpdmfObject> T findByIdInTrans(long id, T obj, String viewTypeName) throws Exception;

	public boolean deleteByIdInTrans(long id, String viewTypeName) throws Exception;
	
	public <T extends VpdmfObject> long insertInTrans(T obj, String viewTypeName) throws Exception;

	public <T extends VpdmfObject> long updateInTrans(T obj, String viewTypeName) throws Exception;
	
	public <T extends VpdmfObject> List<T> retrieveInTrans(T obj, String viewTypeName, int offset, int pageSize) throws Exception;
	
	public <T extends VpdmfObject> List<T> retrieveInTrans(T obj, String viewTypeName) throws Exception;
	
	public <T extends VpdmfQueryObject> int countViewInTrans(T obj, String viewTypeName) throws Exception;
	
	public <T extends VpdmfQueryObject> List<LightViewInstance> listInTrans(T obj, String viewTypeName) throws Exception;
	
	public <T extends VpdmfQueryObject> List<LightViewInstance> listInTrans(T obj, String viewTypeName, int offset, int pageSize) throws Exception;


	// ~~~~~~~~~~~~~~~~~~~~~~
	// Class level operations
	// ~~~~~~~~~~~~~~~~~~~~~~
	
	public <T1 extends VpdmfQueryObject, T2> List<T2> listClassInTrans(T1 qObj, T2 rObj) 
			throws Exception;
	
	
	// ~~~~~~~~~~~~~~~~~~~~
	// 'additional' operations
	// ~~~~~~~~~~~~~~~~~~~~	
	/*public void saveViewInstanceToOntology(OWLOntology o, String uri,
			ViewInstance vi) throws Exception */
	
	
}
