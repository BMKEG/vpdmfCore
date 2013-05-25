package edu.isi.bmkeg.vpdmf.test;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;

import junit.framework.TestCase;
import edu.isi.bmkeg.utils.Converters;
import edu.isi.bmkeg.utils.springContext.BmkegProperties;
import edu.isi.bmkeg.vpdmf.controller.VPDMfKnowledgeBaseBuilder;

public abstract class VPDMfTestCase extends TestCase {

	private String login;
	private String password;
	private String dbUrl;
	File archiveFile;
	VPDMfKnowledgeBaseBuilder builder;
	boolean buildDestroyFlag = false;
	
	protected void setUp(String archiveFilePath, boolean buildDestroyFlag) throws Exception
	{ 
		super.setUp();
				
		this.buildDestroyFlag = buildDestroyFlag;
		
		BmkegProperties prop = new BmkegProperties(true);
				
		login = prop.getDbUser();
		password = prop.getDbPassword();
		dbUrl = prop.getDbUrl();
		
		int l = dbUrl.lastIndexOf("/");
		if (l != -1)
			dbUrl = dbUrl.substring(l + 1, dbUrl.length());
	
		URL u = this.getClass().getClassLoader().getResource(archiveFilePath);
		
		if( u == null ) {
			u = new URL("file:"+archiveFilePath);
			if( u == null ) 
				throw new Exception(archiveFilePath + " does not exist.");
		}
		
		archiveFile = new File( u.getPath() );
		builder = new VPDMfKnowledgeBaseBuilder(archiveFile, 
				getLogin(), password, dbUrl); 
		
		if( this.buildDestroyFlag ) {
			
			try {
				builder.destroyDatabase(dbUrl);
			} catch (SQLException sqlE) {			
				// Gully: Make sure that this runs, avoid silly issues.
				if( !sqlE.getMessage().contains("database doesn't exist") ) {
					sqlE.printStackTrace();
				}
				
			}			
			builder.buildDatabaseFromArchive();
		}
	
				
	}

	protected void tearDown() throws Exception
	{
		super.tearDown();
		if( this.buildDestroyFlag ) {
			builder.destroyDatabase(dbUrl);
		}
	}

	public String getLogin() {
		return login;
	}

	public String getPassword() {
		return password;
	}

	public String getDbUrl() {
		return dbUrl;
	}
	
}
