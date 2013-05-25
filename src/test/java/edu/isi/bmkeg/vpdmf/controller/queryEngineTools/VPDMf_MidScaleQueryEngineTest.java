package edu.isi.bmkeg.vpdmf.controller.queryEngineTools;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;
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
import edu.isi.bmkeg.vpdmf.model.definitions.ViewDefinition;
import edu.isi.bmkeg.vpdmf.model.instances.AttributeInstance;
import edu.isi.bmkeg.vpdmf.model.instances.ViewInstance;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/edu/isi/bmkeg/vpdmf/appCtx-VPDMfTest.xml" })
public class VPDMf_MidScaleQueryEngineTest {

	Logger log = Logger.getLogger("edu.isi.bmkeg.vpdmf.controller.queryEngineTools.VPDMf_QueryEngineTest");

	ApplicationContext ctx;

	VPDMf top;
	UMLmodel m;
	UMLModelSimpleParser p;

	String dbName;
	String login;
	String password;

	VPDMfQueryEngineInterface vhf;

	VPDMfKnowledgeBaseBuilder builder;

	File buildFile;
	
	String sql;

	@Before
	public void setUp() throws Exception {

		ctx = AppContext.getApplicationContext();

		BmkegProperties prop = (BmkegProperties) ctx.getBean("bmkegProperties");
		
		login = prop.getDbUser();
		password =  prop.getDbPassword();
		dbName = "resource_vpdmf_test";
		
	    buildFile = ctx.getResource("classpath:edu/isi/bmkeg/vpdmf/resource_sheets_VPDMf.zip")
				.getFile();
	    
	    builder = new VPDMfKnowledgeBaseBuilder(buildFile, login, password, dbName);
		
		try {
			builder.destroyDatabase(dbName);
		} catch (SQLException sqlE) {
			// Gully: Make sure that this runs, avoid silly issues.
			if( !sqlE.getMessage().contains("database doesn't exist") ) {
				sqlE.printStackTrace();
//				throw sqlE;
			}
		}

		builder.buildDatabaseFromArchive();
		
		vhf = new QueryEngine(this.login, this.password, dbName);
		vhf.connectToDB(this.login, this.password, this.dbName);
	 
	    top = vhf.readTop();
		
	    vhf.closeDbConnection();

	}

	@After
	public void tearDown() throws Exception {

		builder.destroyDatabase(dbName);		
		
	}
	
	@Test @Ignore("Outdated")
	public final void testExecuteFullQuery() throws Exception {

		ViewDefinition vd = top.getViews().get("Article");

		ViewInstance vi = new ViewInstance(vd);
		
	    vhf.connectToDB(this.login, this.password, this.dbName);
		 	    
	    List<ViewInstance> viewList = vhf.executeFullQuery(vi, true, 0, 50);
	    
	    vhf.closeDbConnection();
	    	    	    
	    assertTrue("Should return all 50 complete articles: ", viewList.size() == 50);
			    
	}
		
}
