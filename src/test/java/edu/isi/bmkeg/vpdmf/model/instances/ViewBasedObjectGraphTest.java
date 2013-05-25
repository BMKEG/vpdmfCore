package edu.isi.bmkeg.vpdmf.model.instances;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.isi.bmkeg.uml.model.UMLmodel;
import edu.isi.bmkeg.utils.springContext.AppContext;
import edu.isi.bmkeg.utils.springContext.BmkegProperties;
import edu.isi.bmkeg.vpdmf.controller.VPDMfKnowledgeBaseBuilder;
import edu.isi.bmkeg.vpdmf.controller.queryEngineTools.VPDMfChangeEngineInterface;
import edu.isi.bmkeg.vpdmf.model.definitions.VPDMf;
import edu.isi.bmkeg.vpdmf.model.definitions.ViewDefinition;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/edu/isi/bmkeg/vpdmf/appCtx-VPDMfTest.xml" })
public class ViewBasedObjectGraphTest {

	ApplicationContext ctx;

	VPDMf top;
	UMLmodel m;

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
		password = prop.getDbPassword();
		dbName = "resource_vpdmf_test";

		buildFile = ctx.getResource(
				"classpath:edu/isi/bmkeg/vpdmf/resource_excel_VPDMf.zip")
				.getFile();

		builder = new VPDMfKnowledgeBaseBuilder(
				buildFile, login, password, dbName);

		builder.destroyDatabase(dbName);

		builder.buildDatabaseFromArchive();

	}

	@After
	public void tearDown() throws Exception {

		builder.destroyDatabase(dbName);

	}

	@Test @Ignore("Outdated")
	public void test() throws Exception {

		/*
		VPDMfQueryEngineInterface engine = new QueryEngine(login, password,
				dbName);

		engine.connectToDB();

		VPDMf top = engine.readTop();

		ClassLoader cl = engine.provideClassLoaderForModel();
		ViewInstance vi = engine.executeUIDQuery("Article", "32106");

		ViewBasedObjectGraph vbog = new ViewBasedObjectGraph(top, cl, "Article");

		Map<String, Object> objMap = vbog.viewToObjectGraph(vi);

		//ViewInstance vi2 = vbog.objectGraphToView(objMap);*/

	}

}
