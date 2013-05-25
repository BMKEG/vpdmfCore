package edu.isi.bmkeg.vpdmf.controller.queryEngineTools;

import static org.junit.Assert.assertTrue;

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

import edu.isi.bmkeg.uml.model.UMLmodel;
import edu.isi.bmkeg.uml.sources.UMLModelSimpleParser;
import edu.isi.bmkeg.utils.springContext.AppContext;
import edu.isi.bmkeg.utils.springContext.BmkegProperties;
import edu.isi.bmkeg.vpdmf.controller.VPDMfKnowledgeBaseBuilder;
import edu.isi.bmkeg.vpdmf.model.definitions.VPDMf;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/edu/isi/bmkeg/vpdmf/appCtx-VPDMfTest.xml" })
public class VPDMf_DBBuilderTest {

	ApplicationContext ctx;

	VPDMfKnowledgeBaseBuilder builderSheets;
	VPDMfKnowledgeBaseBuilder builderExcel;

	VPDMf top;
	UMLmodel m;
	UMLModelSimpleParser p;

	File buildFileSheets, buildFileExcel, rebuiltZip;
	
	String sql;

	String dbName;
	String login;
	String password;
	
	@Before
	public void setUp() throws Exception {
		
		ctx = AppContext.getApplicationContext();
		BmkegProperties prop = (BmkegProperties) ctx.getBean("bmkegProperties");
				
		login = prop.getDbUser();
		password =  prop.getDbPassword();
		dbName = "resource_vpdmf_test";
		
		buildFileSheets = ctx.getResource("classpath:edu/isi/bmkeg/vpdmf/resource_sheets_VPDMf.zip")
				.getFile();

		buildFileExcel = ctx.getResource("classpath:edu/isi/bmkeg/vpdmf/resource_excel_VPDMf.zip")
				.getFile();
		rebuiltZip = new File(buildFileExcel.getParent() + "/rebuiltResourceExcel.zip");
		
		builderSheets = new VPDMfKnowledgeBaseBuilder(buildFileSheets, login, password, dbName);
		builderExcel = new VPDMfKnowledgeBaseBuilder(buildFileExcel, login, password, dbName);

		try {
			builderSheets.destroyDatabase(dbName);
			builderExcel.destroyDatabase(dbName);
		} catch (SQLException sqlE) {		
			
			// Gully: Make sure that this runs, avoid silly issues.
			if( !sqlE.getMessage().contains("database doesn't exist") ) {
				sqlE.printStackTrace();
			}
			
		}
		
	}

	@After
	public void tearDown() throws Exception {
		
		try {
			
			builderSheets.destroyDatabase(dbName);			
			builderExcel.destroyDatabase(dbName);
		
		} catch (SQLException sqlE) {		
			
			// Gully: Make sure that this runs, avoid silly issues.
			if( !sqlE.getMessage().contains("database doesn't exist") ) {
				sqlE.printStackTrace();
			}
			
		}
		
	}

	@Test @Ignore("Outdated")
	public final void build_VPDMf_DB_from_sheets_archive_test() throws Exception {

		builderSheets.buildDatabaseFromArchive();		

	}

	@Test @Ignore("Outdated")
	public final void build_VPDMf_DB_from_excel_archive_test() throws Exception {

		builderExcel.buildDatabaseFromArchive();
	
	}

	@Test @Ignore("Outdated")
	public final void retreive_top_from_archive_test() throws Exception {
		
		VPDMf top = builderExcel.readTop();

		assertTrue("top is not null", (top != null) );
	
	}
	
	@Test @Ignore("Outdated")
	public final void dumpDataToArchiveFile() throws Exception {

		builderExcel.buildDatabaseFromArchive();
		builderExcel.refreshDataToNewArchive(rebuiltZip);

		builderExcel.destroyDatabase(dbName);
		
		VPDMfKnowledgeBaseBuilder rebuiltBuilder = 
				new VPDMfKnowledgeBaseBuilder(rebuiltZip, login, password, dbName);
		rebuiltBuilder.buildDatabaseFromArchive();
		
	}
		
	/** 
	 * Build the vpdmfserver database example as a unit test.
	 * @throws Exception
	 *
	@Test
	public final void constructVPDMfServer() throws Exception {

		try {
			builderSheets.destroyDatabase( "vpdmfserver");
		} catch (SQLException sqlE) {					
			// Gully: Make sure that this runs, avoid silly issues.
			if( !sqlE.getMessage().contains("database doesn't exist") ) {
				sqlE.printStackTrace();
			}
		}
		
		builderSheets = new VPDMfKnowledgeBaseBuilder(buildFileSheets, login, password, "vpdmfserver");
		builderSheets.buildDatabaseFromArchive();
		
		this.dbName = "temp";
	
	}*/
	
}
