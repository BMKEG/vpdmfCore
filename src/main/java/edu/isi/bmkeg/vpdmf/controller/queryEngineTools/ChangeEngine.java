package edu.isi.bmkeg.vpdmf.controller.queryEngineTools;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import edu.isi.bmkeg.vpdmf.exceptions.VPDMfException;
import edu.isi.bmkeg.vpdmf.model.instances.ClassInstance;
import edu.isi.bmkeg.vpdmf.model.instances.LightViewInstance;
import edu.isi.bmkeg.vpdmf.model.instances.ViewInstance;

public interface ChangeEngine extends QueryEngine {

	/**
	 * Before updating a view, store a local copy of the view being changed so that 
	 * we can perform 'garbage collection' on the views' unused primitives. 
	 * @param vi
	 * @throws Exception
	 */
	public void storeViewInstanceForUpdate(ViewInstance vi) throws Exception;

	/**
	 * Commit changes to the specified view within the database. 
	 * 
	 * @param vi
	 * @return 
	 * @throws Exception
	 */
	public long executeUpdateQuery(ViewInstance vi) throws Exception;

	/**
	 * Insert a brand new view into the database
	 * @param vi
	 * @throws Exception
	 */
	public long executeInsertQuery(ViewInstance vi) throws Exception;
	
	/**
	 * Deletes a view from the database based on the ViewSpec's type and id value 
	 * @param vi
	 * @return 
	 * @throws Exception
	 */
	public boolean executeDeleteQuery(String viewType, Long id) throws Exception;
	
	/**
	 * Deletes a view from the database
	 * @param vi
	 * @return true if deletion was successful
	 * @throws Exception
	 */
	public boolean executeDeleteQuery(ViewInstance vi) throws Exception;

	public void deleteObject(ClassInstance ci) throws VPDMfException, Exception;
	
	public void turnOffAutoCommit() throws Exception;

	public void turnOnAutoCommit() throws Exception;
	
	public void commitTransaction() throws Exception;
	
	public void rollbackTransaction() throws Exception;

	public boolean insertObjectIntoDB(ClassInstance ci) throws Exception;
	
	/**
	 * Executes a raw UPDATE query on the underlying database. Optimized for speed.
	 * Use with Caution
	 * @param sql
	 * @return number of rows changed. 
	 * @throws SQLException 
	 */
	public int executeRawUpdateQuery(String sql) throws SQLException;

	
}
