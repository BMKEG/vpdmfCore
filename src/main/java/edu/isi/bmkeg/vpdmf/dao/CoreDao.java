package edu.isi.bmkeg.vpdmf.dao;

import java.util.List;
import java.util.Map;

import edu.isi.bmkeg.vpdmf.controller.queryEngineTools.VPDMfChangeEngineInterface;
import edu.isi.bmkeg.vpdmf.model.ViewTable;
import edu.isi.bmkeg.vpdmf.model.definitions.VPDMf;
import edu.isi.bmkeg.vpdmf.model.instances.LightViewInstance;
import edu.isi.bmkeg.vpdmf.model.instances.ViewBasedObjectGraph;
import edu.isi.bmkeg.vpdmf.model.instances.ViewInstance;
import edu.isi.bmkeg.vpdmf.model.qo.ViewTable_qo;

public interface CoreDao {

	public int countView(String viewTypeName) throws Exception;

	public VPDMfChangeEngineInterface getCe();
	
	public VPDMf getTop();
	
	public ClassLoader getCl();
	
	public void init() throws Exception;
	
	public void init(String login, String password, String uri) throws Exception;

	public Map<String, ViewBasedObjectGraph> generateVbogs() throws Exception;
	
	public List<LightViewInstance> goGetLightViewList(String viewName, String attrAddr, String attrVal) throws Exception;
	
	public List<ViewInstance> goGetHeavyViewList(String viewName, String attrAddr, String attrVal) throws Exception;
	
	public List<LightViewInstance> listAllViews(String viewName, boolean paging, int start, int pageSize) throws Exception;

	public List<LightViewInstance> listAllViews(String viewName) throws Exception;
	
	public <T> T loadViewBasedObject(Long vpdmfId, T viewType) throws Exception;
	
	public long insertVBOG(Object ov, String viewName) throws Exception;

	public Object findVBOGById(long id, String viewName) throws Exception;
	

	// ~~~~~~~~~~~~~~~~~~~~~~~
	// final simple operations
	// ~~~~~~~~~~~~~~~~~~~~~~~
	
	public <T extends ViewTable> T findById(long id, T obj, String viewTypeName) throws Exception;
	
	public boolean deleteById(long id, String viewTypeName) throws Exception;

	public <T extends ViewTable> long insert(T ov, String viewTypeName) throws Exception;

	public <T extends ViewTable> long update(T obj, String viewTypeName) throws Exception;
	
	public <T extends ViewTable> List<T> retrieve(T obj, String viewTypeName, int offset, int pageSize) throws Exception;
	
	public <T extends ViewTable> List<T> retrieve(T obj, String viewTypeName) throws Exception;
	
	public <T extends ViewTable_qo> List<LightViewInstance> list(T obj, String viewTypeName) throws Exception;
	
	public <T extends ViewTable_qo> List<LightViewInstance> list(T obj, String viewTypeName, int offset, int pageSize) throws Exception;

	public <T extends ViewTable_qo> int countView(T obj, String viewTypeName) throws Exception;
	
	// ~~~~~~~~~~~~~~~~~~~~
	// 'inTrans' operations
	// ~~~~~~~~~~~~~~~~~~~~
	
	public <T extends ViewTable> T findByIdInTrans(long id, T obj, String viewTypeName) throws Exception;

	public boolean deleteByIdInTrans(long id, String viewTypeName) throws Exception;
	
	public <T extends ViewTable> long insertInTrans(T obj, String viewTypeName) throws Exception;

	public <T extends ViewTable> long updateInTrans(T obj, String viewTypeName) throws Exception;
	
	public <T extends ViewTable> List<T> retrieveInTrans(T obj, String viewTypeName, int offset, int pageSize) throws Exception;
	
	public <T extends ViewTable> List<T> retrieveInTrans(T obj, String viewTypeName) throws Exception;
	
	public <T extends ViewTable_qo> int countViewInTrans(T obj, String viewTypeName) throws Exception;
	
	public <T extends ViewTable_qo> List<LightViewInstance> listInTrans(T obj, String viewTypeName) throws Exception;
	
	public <T extends ViewTable_qo> List<LightViewInstance> listInTrans(T obj, String viewTypeName, int offset, int pageSize) throws Exception;
	
	// ~~~~~~~~~~~~~~~~~~~~~~~
	// more complex operations
	// ~~~~~~~~~~~~~~~~~~~~~~~
	
	/**
	 * Retrieves view instances matching the condition:
	 * <viewName>]<primitiveName>|<className>.<attributeName> = <attributeValue> and
	 * converts them into a list of VBOGs
	 */
	public List<?> retrieveVBOGsByAttributeValue(String viewName,
			String primitiveName, String className, String attributeName, String attributeValue,
			int offset, int pageSize) throws Exception;

	/**
	 * Finds a view instances matching the condition:
	 * <viewName>]<primitiveName>|<className>.<attributeName> = <attributeValue> and
	 * converts them into a VBOG
	 */
	public Object findVBOGByAttributeValue(String viewName,
			String primitiveName, String className, String attributeName, String attributeValue) throws Exception;

}
