package edu.isi.bmkeg.vpdmf.controller.queryEngineTools;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import edu.isi.bmkeg.vpdmf.model.instances.LightViewInstance;
import edu.isi.bmkeg.vpdmf.model.instances.ViewInstance;

public interface VPDMfQueryEngineInterface extends VPDMfDatabaseEngineInterface {

	/**
	 * Runs a list query based on criteria specified within a blank view instance
	 * @param vi - the view instance used to specify the query
	 * @return a count of views in the database satisfying the query
	 * @throws Exception
	 */
	public int executeCountQuery(ViewInstance vi) throws Exception;
	
	/**
	 * Runs a list query based on criteria specified within a blank view instance
	 * @param vi - the view instance used to specify the query
	 * @return a List<ViewInstance> collection with only light view instances satisfying the query
	 * @throws Exception
	 */
	public List<LightViewInstance> executeListQuery(ViewInstance vi) throws Exception;

	/**
	 * Runs a list query based on criteria specified within a blank view instance
	 * @param vi - the view instance used to specify the query
	 * @param paging - shall we perform paging in this query?
	 * @param listOffset - where shall we start the page?
	 * @param pageSize - how large should each page be?
	 * @return a List<ViewInstance> collection with only light view instances satisfying the query
	 * @throws Exception
	 */
	public List<LightViewInstance> executeListQuery(ViewInstance vi, boolean paging, int listOffset, int pageSize) throws Exception;
	
	/**
	 * Runs a query for a specific view based on a specific id string
	 * @param viewType
	 * @param id
	 * @return
	 * @throws Exception
	 */
	public ViewInstance executeUIDQuery(String viewType, Long id) throws Exception;

	public ViewInstance executeUIDQuery(LightViewInstance lvi) throws Exception;
	
	/**
	 * Runs a query for every full view in the database based on a query view.
	 * @param qVi - the view instance used to specify the query
	 * @return a List<ViewInstance> collection with only light view instances satisfying the query
	 * @throws Exception
	 */
	public List<ViewInstance> executeFullQuery(ViewInstance qVi) throws Exception;
	
	/**
	 * Runs a query for every full view in the database based on a query view.
	 * @param qVi - the view instance used to specify the query
	 * @param paging - shall we perform paging in this query?
	 * @param listOffset - where shall we start the page?
	 * @param pageSize - how large should each page be?
	 * @return a List<ViewInstance> collection with only light view instances satisfying the query
	 * @throws Exception
	 */
	public List<ViewInstance> executeFullQuery(ViewInstance qVi, boolean paging, int listOffset, int pageSize) throws Exception;

	/**
	 * Runs a list query that returns data from a single attribute based on criteria 
	 * specified within a view instance
	 * @param vi - the view instance used to specify the query
	 * @param pvName - Name of the primitive to query
	 * @param cName - Name of the class to query
	 * @param aName - Name of the attribute to query
	 * @param paging - shall we perform paging in this query?
	 * @param listOffset - where shall we start the page?
	 * @param pageSize - how large should each page be?
	 * @return a List<ViewInstance> collection with only light view instances satisfying the query
	 * @throws Exception
	 */
	public Map<Long, Object> executeAttributeQuery(ViewInstance vi, 
			String pName, String cName, String aName, 
			boolean paging, int listOffset, int pageSize) throws Exception;

	/**
	 * Executes a raw SQL query on the underlying database. Optimized for speed.
	 * @param sql
	 * @return
	 * @throws SQLException 
	 */
	public ResultSet executeRawSqlQuery(String sql) throws SQLException;	
	
}
