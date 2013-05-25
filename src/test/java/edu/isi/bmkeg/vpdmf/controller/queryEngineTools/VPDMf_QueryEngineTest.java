package edu.isi.bmkeg.vpdmf.controller.queryEngineTools;

import static org.junit.Assert.assertTrue;

import java.io.File;
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

	@Before
	public void setUp() throws Exception {

		ctx = AppContext.getApplicationContext();

		BmkegProperties prop = (BmkegProperties) ctx.getBean("bmkegProperties");
		
		login = prop.getDbUser();
		password =  prop.getDbPassword();
		dbName = "triage_vpdmf_test";
		
	    buildFile = ctx.getResource("classpath:edu/isi/bmkeg/vpdmf/triage_data_VPDMf.zip")
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
		
	    File jarLocation = new File(buildFile.getParent() + "/tempResource.jar" );
		
		builder.buildDatabaseFromArchive();
		
		vhf = new QueryEngine(this.login, this.password, dbName);
		vhf.connectToDB(this.login, this.password, this.dbName);
	 
	    top = vhf.readTop();
	    
	    cl = vhf.readClassLoader(jarLocation);
		
	    vhf.closeDbConnection();

	}

	@After
	public void tearDown() throws Exception {

		builder.destroyDatabase(dbName);

	}

	@Test @Ignore("Outdated")
	public final void testExecuteListQuery() throws Exception {

		ViewDefinition vd = top.getViews().get("ArticleCitation");

		ViewInstance vi = new ViewInstance(vd);

		AttributeInstance ai = vi.readAttributeInstance("]Author|Person.fullName", 0);
		ai.writeValueString("JF Chen");
		
	    vhf.connectToDB(this.login, this.password, this.dbName);
	    
	    List<LightViewInstance> viewList = vhf.executeListQuery(vi);
	    
	    vhf.closeDbConnection();
	    
	    LightViewInstance lvi = viewList.get(0);
	    	    
	    assertTrue("Need to find vpdmfId=32106: ", lvi.getName().equals("vpdmfId=26768"));
			    
	}
	
	@Test @Ignore("Outdated")
	public final void testExecuteFullQuery() throws Exception {

		ViewDefinition vd = top.getViews().get("Article");

		ViewInstance vi = new ViewInstance(vd);
		
	    vhf.connectToDB(this.login, this.password, this.dbName);
		 	    
	    List<ViewInstance> viewList = vhf.executeFullQuery(vi);
	    
	    vhf.closeDbConnection();
	    	    	    
	    assertTrue("Should return all 50 complete articles: ", viewList.size() == 50);
			    
	}
	
	@Test @Ignore("Outdated")
	public final void testExecuteUIDQuery() throws Exception {
		
	    vhf.connectToDB(this.login, this.password, this.dbName);
		 	    
	    ViewInstance vi = vhf.executeUIDQuery("Article", 32106L);
		
	    vhf.closeDbConnection();
	  
	    assertTrue("Need to find id=32106: ", vi.getName().equals("vpdmfId=32106"));
	    	    
	} 
		
	@Test @Ignore("Outdated")
	public final void testViewInstanceToObjectGraph() throws Exception {

	    vhf.connectToDB(this.login, this.password, this.dbName);
		 	    
	    ViewInstance vi = vhf.executeUIDQuery("Article", 32106L);
	    
	    String viewName = "Article";
	    
	    ViewBasedObjectGraph og = new ViewBasedObjectGraph(top, cl, viewName);
	    
	    Map<String, Object> map = og.viewToObjectGraph(vi);
		
	    vhf.closeDbConnection();
	  
	    assertTrue("Should have 18 mapped objects here: ", (map.size() == 18) );

	} 

	@Test @Ignore("Outdated")
	public final void testAndBackAgain() throws Exception {

	    vhf.connectToDB(this.login, this.password, this.dbName);
	 	 	    
	    ViewInstance vi = vhf.executeUIDQuery("ArticleCitation", 32106L);
	    
	    log.debug(vi.readDebugString());
	    
	    String viewName = "Article";
	    
	    ViewBasedObjectGraph og = new ViewBasedObjectGraph(top, cl, viewName);
	    
	    Map<String, Object> map = og.viewToObjectGraph(vi);
		
	    vhf.closeDbConnection();
	    
	    Object o = map.get(vi.getPrimaryPrimitive().getName());

	    ViewInstance rebuiltVi = og.objectGraphToView(o);	    
	    
	    System.out.print(rebuiltVi.readDebugString());
	       
	    String testString = "Hagg, Ha (1970), `Cervicothalamic tract in the dog.` J Comp Neurol 139:357-74";
	    assertTrue("Indexstring should be generated appropriately for the rebuilt view instance:" + testString
	    		+ "\n != " +rebuiltVi.getVpdmfLabel() + "\n", (rebuiltVi.getVpdmfLabel().equals(testString)) );

	}
	
	@Test @Ignore("Outdated")
	public final void pagingListQuery() throws Exception {

		ViewDefinition vd = top.getViews().get("Resource");

		ViewInstance vi = new ViewInstance(vd);
		
	    vhf.connectToDB(this.login, this.password, this.dbName);
		 
	    List<LightViewInstance> viewList = vhf.executeListQuery(vi, true, 2, 5);
	    
	    vhf.closeDbConnection();
	    	    	    
	    assertTrue("Viewlist needs to have 5 views: ", viewList.size() == 5);
	}
	
	@Test @Ignore("Outdated")
	public final void pagingFullQuery() throws Exception {

		ViewDefinition vd = top.getViews().get("Resource");

		ViewInstance vi = new ViewInstance(vd);
		
	    vhf.connectToDB(this.login, this.password, this.dbName);
		 
	    List<ViewInstance> viewList = vhf.executeFullQuery(vi, true, 2, 5);
	    
	    vhf.closeDbConnection();
	    	    	    
	    assertTrue("Viewlist needs to have 5 views: ", viewList.size() == 5);
	}
	
/*	@Test
	public final void testQueryClass() throws Exception {

		List<ClassInstance> cis = null;
		
		QueryEngine qe = new QueryEngine(this.login, this.password, dbName);

		try {
					
			qe.connectToDB();
		    qe.stat.execute("set autocommit=0;");
			 
		    qe.local_state = VPDMfController.DISPLAY;
		    qe.local_sourceViewInstance = null;
		    qe.local_doPagingInQuery = false;
		    qe.local_listOffset = 0;
		    qe.vpdmf = top;
		    
	        String query = "[Article]Article|ViewTable?indexString=AAA%";
		    
		    cis = qe.executeClassQuery(query);
		    
		
		} finally {

			qe.closeDbConnection();
			
		}

		assertTrue("Should have removed view data with ViewTableId = 32106.", true);

	}*/
	
	

	
}
