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
import edu.isi.bmkeg.vpdmf.model.instances.viewGraphInstance;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/edu/isi/bmkeg/vpdmf/appCtx-VPDMfTest.xml" })
public class VPDMf_ChangeEngineTest {

	ApplicationContext ctx;

	VPDMf top;
	UMLmodel m;
	UMLModelSimpleParser p;

	String dbName;
	String login;
	String password;

	VPDMfChangeEngineInterface ce;
	
	VPDMfKnowledgeBaseBuilder builder;
	File buildFile;

	ViewDefinition vd;
	ViewInstance vi;
	viewGraphInstance vgi;

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
		builder.setLogin(login);
		builder.setPassword(password);
		
		try {
			builder.destroyDatabase(dbName);
		} catch (SQLException sqlE) {
			if( !sqlE.getMessage().contains("database doesn't exist") ) {
				sqlE.printStackTrace();
				// Gully, avoids unnecessary isssues.
				// throw sqlE;
			}
		}
		
		builder.buildDatabaseFromArchive();
				
		ce = new ChangeEngine(this.login, this.password, dbName);
		ce.connectToDB();

		top = ce.readTop();

		ce.closeDbConnection();

	}

	@After
	public void tearDown() throws Exception {

		builder.destroyDatabase(dbName);	

	}

	@Test @Ignore("Outdated")
	public final void testExecuteUIDQuery() throws Exception {

		ce.connectToDB(this.login, this.password, dbName);
		ce.turnOffAutoCommit();

		try {
			
			ViewInstance vi = ce.executeUIDQuery("Article", 32106L);
			
			ce.storeViewInstanceForUpdate(vi);

			AttributeInstance ai = vi.readAttributeInstance("]Scientist|Person.lastName", 0);
			ai.writeValueString("Watts");

			ai = vi.readAttributeInstance("]Scientist|Person.initials", 0);
			ai.writeValueString("AG");
		
			ce.executeUpdateQuery(vi);
			ce.commitTransaction();
						
			vi = ce.executeUIDQuery("Article", 32106L);

			ai = vi.readAttributeInstance("]Scientist|Person.vpdmfId", 0);

		} catch (Exception e) {

			e.printStackTrace();			
			ce.rollbackTransaction();
			
			throw e;

		} finally {

			ce.closeDbConnection();

		}
	
	}
	
	
	@Test @Ignore("Outdated")
	public final void testExecuteInsertQuery() throws Exception {

		try {
			
			vd = top.getViews().get("Article");
			vi = new ViewInstance(vd);

			vi.readAttributeInstance("]Scientist|Person.initials", 0)
					.writeValueString("TEST");
			vi.readAttributeInstance("]Scientist|Person.lastName", 0)
					.writeValueString("TEST");
			vi.readAttributeInstance("]Scientist|Person.affiliation", 0)
					.writeValueString("TEST");
			vi.readAttributeInstance("]Resource|Resource.abstractText", 0)
					.writeValueString("TEST");
			vi.readAttributeInstance("]Resource|Resource.title", 0).writeValueString(
					"TEST");
			vi.readAttributeInstance("]Resource|Resource.pubYear", 0)
					.writeValueString("0000");
			vi.readAttributeInstance("]Resource|Resource.checksum", 0)
					.writeValueString("TEST");
			vi.readAttributeInstance("]Resource|Resource.pages", 0).writeValueString(
					"TEST");
			vi.readAttributeInstance("]Resource|Article.volume", 0).writeValueString(
					"0");
			vi.readAttributeInstance("]JournalLU|Journal.abbr", 0).writeValueString(
					"J Comp Neurol");
			vi.readAttributeInstance("]JournalLU|Journal.ISSN", 0).writeValueString(
					"0021-9967");
			vi.readAttributeInstance("]ResourceType|CV.context", 0).writeValueString(
					"Resource.ResourceType");
			vi.readAttributeInstance("]ResourceType|CV.name", 0).writeValueString(
					"Bibliographic");
			vi.readAttributeInstance("]Keyword|Keyword.value", 0).writeValueString(
					"TEST");
			vi.readAttributeInstance("]URI|URI.uri", 0).writeValueString("TEST");
			vi.readAttributeInstance("]UID|UID.uidValue", 0).writeValueString("TEST");

			ce.connectToDB(this.login, this.password, dbName);
			ce.turnOffAutoCommit();
			ce.executeInsertQuery(vi);
			ce.commitTransaction();
			ce.turnOnAutoCommit();
			
		} catch (Exception e) {

			e.printStackTrace();
			ce.rollbackTransaction();
			
			System.out.println("        *** transaction rolled back ***");
			throw e;

		} finally {

			ce.closeDbConnection();

		}

		// Need to run a query to check if the insert actually worked. 
		vi = new ViewInstance(vd);
		vi.readAttributeInstance("]Resource|Resource.pubYear", 0).writeValueString("0000");

		ce.connectToDB(this.login, this.password, dbName);
		 
		List<LightViewInstance> viewList = ce.executeListQuery(vi);
	    
	    ce.closeDbConnection();
		
	    LightViewInstance newVi = viewList.get(0);

		assertTrue("Inserted UID value should be vpdmfId=32320, ",
				newVi.getUIDString().equals("vpdmfId=32320"));

	}
	
	
	@Test @Ignore("Outdated")
	public final void testRollback() throws Exception {

		try {
			
			vd = top.getViews().get("Article");
			vi = new ViewInstance(vd);

			vi.readAttributeInstance("]Scientist|Person.initials", 0)
					.writeValueString("TEST");
			vi.readAttributeInstance("]Scientist|Person.lastName", 0)
					.writeValueString("TEST");
			vi.readAttributeInstance("]Scientist|Person.affiliation", 0)
					.writeValueString("TEST");
			vi.readAttributeInstance("]Resource|Resource.abstractText", 0)
					.writeValueString("TEST");
			vi.readAttributeInstance("]Resource|Resource.title", 0).writeValueString(
					"TEST");
			vi.readAttributeInstance("]Resource|Resource.pubYear", 0)
					.writeValueString("0000");
			vi.readAttributeInstance("]Resource|Resource.checksum", 0)
					.writeValueString("TEST");
			vi.readAttributeInstance("]Resource|Resource.pages", 0).writeValueString(
					"TEST");
			vi.readAttributeInstance("]Resource|Article.volume", 0).writeValueString(
					"0");
			vi.readAttributeInstance("]JournalLU|Journal.abbr", 0).writeValueString(
					"J Comp Neurol");
			vi.readAttributeInstance("]JournalLU|Journal.ISSN", 0).writeValueString(
					"0021-9967");
			vi.readAttributeInstance("]ResourceType|CV.context", 0).writeValueString(
					"Resource.ResourceType");
			vi.readAttributeInstance("]ResourceType|CV.name", 0).writeValueString(
					"Bibliographic");
			vi.readAttributeInstance("]Keyword|Keyword.value", 0).writeValueString(
					"TEST");
			vi.readAttributeInstance("]URI|URI.uri", 0).writeValueString("TEST");
			vi.readAttributeInstance("]UID|UID.uidValue", 0).writeValueString("TEST");

			ce.connectToDB(this.login, this.password, dbName);
			ce.turnOffAutoCommit();
			ce.executeInsertQuery(vi);
			ce.rollbackTransaction();
			ce.turnOnAutoCommit();
			
		} catch (Exception e) {

			e.printStackTrace();
			ce.rollbackTransaction();
			
			System.out.println("        *** transaction rolled back ***");
			throw e;

		} finally {

			ce.closeDbConnection();

		}

		// Need to run a query to check if the insert actually worked. 
		vi = new ViewInstance(vd);
		vi.readAttributeInstance("]Resource|Resource.pubYear", 0).writeValueString("0000");

		ce.connectToDB(this.login, this.password, dbName);
		 
		List<LightViewInstance> viewList = ce.executeListQuery(vi);
	    
	    ce.closeDbConnection();
		
		assertTrue("There should be no entry in the database after the rollback, list size = " 
				+ viewList.size(), viewList.size() == 0);

	}
	
	@Test @Ignore("Outdated")
	public final void testExecuteUpdateQuery() throws Exception {

		ce.connectToDB(this.login, this.password, dbName);
		ce.turnOffAutoCommit();

		try {
			
			ViewInstance vi = ce.executeUIDQuery("Article", 32106L);
			
			ce.storeViewInstanceForUpdate(vi);

			AttributeInstance ai = vi.readAttributeInstance("]Scientist|Person.lastName", 0);
			ai.writeValueString("Watts");

			ai = vi.readAttributeInstance("]Scientist|Person.initials", 0);
			ai.writeValueString("AG");
		
			ce.executeUpdateQuery(vi);
			
			System.out.println("        *** transaction committed ***");
			
			vi = ce.executeUIDQuery("Article", 32106L);

			ai = vi.readAttributeInstance("]Scientist|Person.vpdmfId", 0);
			
			int i=0;
			i++;

		} catch (Exception e) {

			e.printStackTrace();
						
			System.out.println("        *** transaction rolled back ***");
			
			throw e;

		} finally {

			ce.closeDbConnection();

		}
	
	}

	@Test @Ignore("Outdated")
	public final void testDeleteView() throws Exception {

		try {
			
			ViewDefinition vd = top.getViews().get("Article");
			ViewInstance vi = new ViewInstance(vd);
			AttributeInstance ai = vi.readAttributeInstance("]Resource|ViewTable.vpdmfId", 0);
			ai.writeValueString("32106");
			
			QueryEngine vhf = new QueryEngine(this.login, this.password, dbName);

			vhf.connectToDB();
		    vhf.stat.execute("set autocommit=0;");
			 		
			//
			// Then delete the data
			//
			ce.connectToDB(this.login, this.password, dbName);
			ce.turnOffAutoCommit();
			ce.deleteView("Article", 32106L);
			ce.commitTransaction();

		} catch (Exception e) {
			
			e.printStackTrace();
			ce.rollbackTransaction();

		} finally {

			ce.closeDbConnection();
			
		}

		ViewDefinition vd = top.getViews().get("Article");
		ViewInstance vi = new ViewInstance(vd);
		AttributeInstance ai = vi.readAttributeInstance("]Resource|Resource.vpdmfId", 0);
		ai.writeValueString("32106");
						
	    ce.connectToDB(this.login, this.password, dbName);		 
	    
	    List<LightViewInstance> viewList = ce.executeListQuery(vi);

		assertTrue("Should have removed view data with ViewTableId = 32106.", viewList.size() == 0);

	}
	
}
