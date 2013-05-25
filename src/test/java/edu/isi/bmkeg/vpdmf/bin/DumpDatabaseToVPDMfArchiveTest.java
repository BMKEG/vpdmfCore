package edu.isi.bmkeg.vpdmf.bin;

import java.io.File;
import java.sql.SQLException;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.isi.bmkeg.utils.springContext.AppContext;
import edu.isi.bmkeg.utils.springContext.BmkegProperties;
import edu.isi.bmkeg.vpdmf.controller.VPDMfKnowledgeBaseBuilder;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={ "/edu/isi/bmkeg/vpdmf/appCtx-VPDMfTest.xml"})
public class DumpDatabaseToVPDMfArchiveTest {
	
	ApplicationContext ctx;

	DumpDatabaseToVpdmfArchive p;
	
	String login, password, dbUrl;
	File archiveFile, outputFile;
	VPDMfKnowledgeBaseBuilder builder;
	
	@Before
	public void setUp() throws Exception {
		
		ctx = AppContext.getApplicationContext();
		BmkegProperties prop = (BmkegProperties) ctx.getBean("bmkegProperties");

		login = prop.getDbUser();
		password = prop.getDbPassword();
		dbUrl = prop.getDbUrl();
		
		int l = dbUrl.lastIndexOf("/");
		if (l != -1)
			dbUrl = dbUrl.substring(l + 1, dbUrl.length());
	
		archiveFile = ctx.getResource(
				"classpath:edu/isi/bmkeg/vpdmf/resource_excel_VPDMf.zip").getFile();
		outputFile = new File( archiveFile.getParent() + "/tempOutVPDMf.zip");
		builder = new VPDMfKnowledgeBaseBuilder(archiveFile, 
				login, password, dbUrl); 
		
		try {
			builder.destroyDatabase(dbUrl);
		} catch (SQLException sqlE) {		
			
			// Gully: Make sure that this runs, avoid silly issues.
			if( !sqlE.getMessage().contains("database doesn't exist") ) {
				sqlE.printStackTrace();
			}
			
		}
		
	}

	@After
	public void tearDown() throws Exception {
		
		builder.destroyDatabase(dbUrl);
		
	}
	
	@Test @Ignore("Outdated")
	public final void testRunExecWithFullPaths() throws Exception {
				
		String[] args = new String[] { 
				archiveFile.getPath(), dbUrl, login, password
				};

		BuildDatabaseFromVpdmfArchive.main(args);

		args = new String[] { 
				archiveFile.getPath(), dbUrl, login, password, outputFile.getPath()
				};
		
		DumpDatabaseToVpdmfArchive.main(args);
				
	}
		
}

