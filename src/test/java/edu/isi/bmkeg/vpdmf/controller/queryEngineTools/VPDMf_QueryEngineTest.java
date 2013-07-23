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

	VPDMfQueryEngineInterface vhf;

	VPDMfKnowledgeBaseBuilder builder;

	File buildFile;
	
	String sql;
	
	// DO WE NEED TO REBUILD THE DATABASE FROM SCRATCH AFTER EVERY TEST?
	static boolean REBUILD_DB = true;

	@Before
	public void setUp() throws Exception {

		ctx = AppContext.getApplicationContext();

		BmkegProperties prop = (BmkegProperties) ctx.getBean("bmkegProperties");
		
		login = prop.getDbUser();
		password =  prop.getDbPassword();
		dbName = "triage_vpdmf_test";
		
	    buildFile = ctx.getResource("classpath:edu/isi/bmkeg/vpdmf/triage/triage-mysql-1-1-3-testData.zip")
				.getFile();
	    
	    builder = new VPDMfKnowledgeBaseBuilder(buildFile, login, password, dbName);
		
	    if( REBUILD_DB || !builder.checkIfKbExists(dbName) ) {

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
	    
	    File jarLocation = new File(buildFile.getParent() + "/triage-jpa-1.1.3-SNAPSHOT.jar" );
		
		vhf = new QueryEngine(this.login, this.password, dbName);
		vhf.connectToDB(this.login, this.password, this.dbName);
	 
	    top = vhf.readTop();
	    
		URL url = jarLocation.toURI().toURL();
		URL[] urls = new URL[]{url};
		cl = new URLClassLoader(urls);
	    
	    vhf.closeDbConnection();

	}

	@After
	public void tearDown() throws Exception {

		if( REBUILD_DB ) {
			builder.destroyDatabase(dbName);
		}
		
	}

	@Test // Reinstated these tests  
	public final void testExecuteListQuery() throws Exception {

		ViewDefinition vd = top.getViews().get("ArticleCitation");

		ViewInstance vi = new ViewInstance(vd);

		AttributeInstance ai = vi.readAttributeInstance("]Author|Person.fullName", 0);
		ai.writeValueString("A Kawano");
		
	    vhf.connectToDB(this.login, this.password, this.dbName);
	    
	    List<LightViewInstance> viewList = vhf.executeListQuery(vi);
	    
	    vhf.closeDbConnection();
	    
	    LightViewInstance lvi = viewList.get(0);
	    	    
	    assertTrue("Need to find vpdmfId=32106: ", lvi.getName().equals("vpdmfId=26785"));
			    
	}
	
	@Test @Ignore("Unsure if this functionality is working")
	public final void testExecuteFullQuery() throws Exception {

		ViewDefinition vd = top.getViews().get("ArticleCitation");

		ViewInstance vi = new ViewInstance(vd);
		
	    vhf.connectToDB(this.login, this.password, this.dbName);
		 	    
	    List<ViewInstance> viewList = vhf.executeFullQuery(vi);
	    
	    vhf.closeDbConnection();
	    	    	    
	    assertTrue("Should return all 4 complete articles: ", viewList.size() == 4);
			    
	}
	
	@Test
	public final void testExecuteUIDQuery() throws Exception {
		
	    vhf.connectToDB(this.login, this.password, this.dbName);
		 	    
	    ViewInstance vi = vhf.executeUIDQuery("ArticleCitation", 26785L);
		
	    vhf.closeDbConnection();
	  
	    assertTrue("Need to find id=26785: ", vi.getName().equals("vpdmfId=26785"));
	    	    
	} 
		
	@Test 
	public final void testViewInstanceToObjectGraph() throws Exception {

	    vhf.connectToDB(this.login, this.password, this.dbName);
		 	    
	    ViewInstance vi = vhf.executeUIDQuery("ArticleCitation", 26785L);
	    
	    String viewName = "ArticleCitation";
	    
	    ViewBasedObjectGraph og = new ViewBasedObjectGraph(top, cl, viewName);
	    
	    Map<String, Object> map = og.viewToObjectGraph(vi);
		
	    vhf.closeDbConnection();
	  
	    assertTrue("Should have 12 mapped objects here: ", (map.size() == 12) );

	} 

	@Test 
	public final void testAndBackAgain() throws Exception {

	    vhf.connectToDB(this.login, this.password, this.dbName);
	 	 	    
	    ViewInstance vi = vhf.executeUIDQuery("ArticleCitation", 26785L);
	    
	    log.debug(vi.readDebugString());
	    
	    String viewName = "ArticleCitation";
	    
	    ViewBasedObjectGraph og = new ViewBasedObjectGraph(top, cl, viewName);
	    
	    Map<String, Object> map = og.viewToObjectGraph(vi);
		
	    vhf.closeDbConnection();
	    
	    Object o = map.get(vi.getPrimaryPrimitive().getName());

	    ViewInstance rebuiltVi = og.objectGraphToView(o);	    
	    
	    System.out.print(rebuiltVi.readDebugString());
	       
	    String testString = "A Kawano, Y Hayashi, S Noguchi, H Handa, M Horikoshi, Y Yamaguchi (2011), " +
	    		"`Global analysis for functional residues of histone variant Htz1 using the comprehensive " +
	    		"point mutant library.` Genes Cells 16:590-607 [21470346]";
	    assertTrue("Indexstring should be generated appropriately for the rebuilt view instance:" + testString
	    		+ "\n != " +rebuiltVi.getVpdmfLabel() + "\n", (rebuiltVi.getVpdmfLabel().equals(testString)) );

	}
	
	@Test 
	public final void testPagingListQuery() throws Exception {

		ViewDefinition vd = top.getViews().get("ArticleCitation");

		ViewInstance vi = new ViewInstance(vd);
		
	    vhf.connectToDB(this.login, this.password, this.dbName);
		 
	    List<LightViewInstance> viewList = vhf.executeListQuery(vi, true, 2, 2);
	    
	    vhf.closeDbConnection();
	    	    	    
	    assertTrue("Viewlist needs to have 5 views: ", viewList.size() == 2);
	}	
	

	@Test 
	public final void testSortedListQuery() throws Exception {

		Object tsObject = Class.forName("edu.isi.bmkeg.triage.model.qo.TriageScore_qo", true, cl).newInstance();
		tsObject.getClass().getDeclaredMethod("setInScore", String.class).invoke(tsObject, "<vpdmf-sort-0>");
	    
		String viewName = "TriageScore";
	    ViewBasedObjectGraph og = new ViewBasedObjectGraph(top, cl, viewName);	    
	    ViewInstance vi = og.objectGraphToView(tsObject, false);
	    
	    vhf.connectToDB(this.login, this.password, this.dbName);
		 
	    List<LightViewInstance> viewList = vhf.executeListQuery(vi, og.getSortAddr());
	    
	    vhf.closeDbConnection();
	    	    	    
	    assertTrue("Viewlist needs to have 4 views: ", viewList.size() == 4);
	}
	
}
