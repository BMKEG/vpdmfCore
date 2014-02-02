package edu.isi.bmkeg.vpdmf.controller.queryEngineTools;

import java.io.File;
import java.util.List;

import edu.isi.bmkeg.vpdmf.model.definitions.VPDMf;
import edu.isi.bmkeg.vpdmf.model.instances.ClassInstance;

/** 
 * Interface describing API for database specific functions within VPDMf
 * @author burns
 *
 */
public interface DatabaseEngine {
	
	public ClassLoader readClassLoader(File jarLocation) throws Exception;

	public VPDMf readTop() throws Exception;
	
	public boolean connectToDB(String login, String password, String uri) throws Exception;

	public boolean connectToDB() throws Exception;
		 
	public void closeDbConnection() throws Exception;
	
	public void setMaxReturnedInQuery(int maxReturnedInQuery);
	
	public List<ClassInstance> queryClass(ClassInstance obj) throws Exception;
	
	public void prettyPrintSQL(String sql);
	
}
