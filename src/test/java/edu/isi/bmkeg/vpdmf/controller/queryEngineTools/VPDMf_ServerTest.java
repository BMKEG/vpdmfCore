package edu.isi.bmkeg.vpdmf.controller.queryEngineTools;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.sql.SQLException;
import java.util.List;

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
import edu.isi.bmkeg.vpdmf.model.instances.LightViewInstance;
import edu.isi.bmkeg.vpdmf.model.instances.ViewInstance;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/edu/isi/bmkeg/vpdmf/appCtx-VPDMfTest.xml" })
public class VPDMf_ServerTest {

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
		
	    buildFile = ctx.getResource("classpath:edu/isi/bmkeg/vpdmf/resource_excel_VPDMf.zip")
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
	public final void testExecuteListQuery() throws Exception {

		ViewDefinition vd = top.getViews().get("Article");

		ViewInstance vi = new ViewInstance(vd);

		AttributeInstance ai = vi.readAttributeInstance("]Scientist|Person.lastName", 0);
		ai.writeValueString("Hagg");
		
	    vhf.connectToDB(this.login, this.password, this.dbName);
		 
	    //vhf.local_state = VPDMfController.LIST;
	    //vhf.local_sourceViewInstance = null;
	    //vhf.local_doPagingInQuery = false;
	    //vhf.local_listOffset = 0;
	    
	    List<LightViewInstance> viewList = vhf.executeListQuery(vi);
	    
	    vhf.closeDbConnection();
	    
	    LightViewInstance lvi = viewList.get(0);
	    	    
	    assertTrue("Need to find bmkegOd=32106: ", lvi.getName().equals("vpdmfId=32106"));
			    
	}

	@Test @Ignore("Outdated")
	public final void testExecuteUIDQuery() throws Exception {
		
	    vhf.connectToDB(this.login, this.password, this.dbName);
		 	    
	    ViewInstance vi = vhf.executeUIDQuery("Article", 32106L);
		
	    vhf.closeDbConnection();
	  
	    assertTrue("Need to find id=32106: ", vi.getName().equals("vpdmfId=32106"));
	    	    
	}	

}
