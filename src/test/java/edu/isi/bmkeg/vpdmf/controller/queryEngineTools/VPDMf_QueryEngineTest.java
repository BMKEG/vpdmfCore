package edu.isi.bmkeg.vpdmf.controller.queryEngineTools;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

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
import edu.isi.bmkeg.vpdmf.model.instances.LightViewInstance;
import edu.isi.bmkeg.vpdmf.model.instances.ViewBasedObjectGraph;
import edu.isi.bmkeg.vpdmf.model.instances.ViewInstance;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/edu/isi/bmkeg/vpdmf/appCtx-VPDMfTest.xml" })
public class VPDMf_QueryEngineTest {

	Logger log = Logger.getLogger("edu.isi.bmkeg.vpdmf.controller.queryEngineTools.VPDMf_QueryEngineTest");

	ApplicationContext ctx;

	VPDMf top;
	ClassLoader cl;
	UMLmodel m;
	UMLModelSimpleParser p;

	String dbName;
	String login;
	String password;

	ChangeEngine ce;

	VPDMfKnowledgeBaseBuilder builder;

	File buildFile;
	
	String sql;
	
	// DO WE NEED TO REBUILD THE DATABASE FROM SCRATCH AFTER EVERY TEST?
	boolean rebuildDb = true;

	@Before
	public void setUp() throws Exception {

		ctx = AppContext.getApplicationContext();

		BmkegProperties prop = (BmkegProperties) ctx.getBean("bmkegProperties");
		
		login = prop.getDbUser();
		password =  prop.getDbPassword();
		dbName = "vpdmfCore_test";
		
	    buildFile = ctx.getResource("classpath:edu/isi/bmkeg/vpdmf/digitalLibrary/digitalLibrary-mysql-neurosciData.zip")
				.getFile();
	    
	    builder = new VPDMfKnowledgeBaseBuilder(buildFile, login, password, dbName);
		
	    if( rebuildDb || !builder.checkIfKbExists(dbName) ) {

	    	try {
				builder.destroyDatabase(dbName);
			} catch (SQLException sqlE) {
				// Gully: Make sure that this runs, avoid silly issues.
				if( !sqlE.getMessage().contains("database doesn't exist") ) {
					sqlE.printStackTrace();
					throw sqlE;
				}
			}
			builder.buildDatabaseFromArchive();
			
		}
	    
	    File jarLocation = new File(buildFile.getParent() + "/digitalLibrary-jpa-1.1.3-SNAPSHOT.jar" );
		
		ce = new ChangeEngine(this.login, this.password, dbName);
		ce.connectToDB(this.login, this.password, this.dbName);
	 
	    top = ce.readTop();
	    
		URL url = jarLocation.toURI().toURL();
		URL[] urls = new URL[]{url};
		cl = new URLClassLoader(urls);
	    
	    ce.closeDbConnection();

	}

	@After
	public void tearDown() throws Exception {

		if( rebuildDb ) {
			builder.destroyDatabase(dbName);
		}
		
	}

	@Test // Reinstated these tests  
	public final void testExecuteListQuery() throws Exception {

		ViewDefinition vd = top.getViews().get("ArticleCitation");

		ViewInstance vi = new ViewInstance(vd);

		AttributeInstance ai = vi.readAttributeInstance("]Author|Person.surname", 0);
		ai.writeValueString("Uhernik");
		
	    ce.connectToDB(this.login, this.password, this.dbName);
	    
	    List<LightViewInstance> viewList = ce.executeListQuery(vi);
	    
	    ce.closeDbConnection();
	    
	    LightViewInstance lvi = viewList.get(0);
	    	    
	    assertTrue("Need to find vpdmfId=26768: ", lvi.getName().equals("vpdmfId=26768"));
	    
	    rebuildDb = false;
			    
	}
	
	@Test
	public final void testExecuteUIDQuery() throws Exception {
		
	    ce.connectToDB(this.login, this.password, this.dbName);
		 	    
	    ViewInstance vi = ce.executeUIDQuery("ArticleCitation", 26768L);
		
	    ce.closeDbConnection();
	  
	    assertTrue("Need to find id=26768: ", vi.getName().equals("vpdmfId=26768"));
	    	    
	} 
		
	@Test 
	public final void testViewInstanceToObjectGraph() throws Exception {

	    ce.connectToDB(this.login, this.password, this.dbName);
		 	    
	    ViewInstance vi = ce.executeUIDQuery("ArticleCitation", 26768L);
	    
	    String viewName = "ArticleCitation";
	    
	    ViewBasedObjectGraph og = new ViewBasedObjectGraph(top, cl, viewName);
	    
	    Map<String, Object> map = og.viewToObjectGraph(vi);
		
	    ce.closeDbConnection();
	  
	    assertTrue("Should have 11 mapped objects here: ", (map.size() == 11) );

	} 

	@Test 
	public final void testPagingListQuery() throws Exception {

		ViewDefinition vd = top.getViews().get("ArticleCitation");

		ViewInstance vi = new ViewInstance(vd);
		
	    ce.connectToDB(this.login, this.password, this.dbName);
		 
	    List<LightViewInstance> viewList = ce.executeListQuery(vi, true, 2, 2);
	    
	    ce.closeDbConnection();
	    	    	    
	    assertTrue("Viewlist needs to have 5 views: ", viewList.size() == 2);
	    
	}	
	
}
