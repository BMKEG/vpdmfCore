package edu.isi.bmkeg.vpdmf;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.isi.bmkeg.uml.interfaces.JavaUmlInterface;
import edu.isi.bmkeg.uml.model.UMLmodel;
import edu.isi.bmkeg.uml.sources.UMLModelSimpleParser;
import edu.isi.bmkeg.utils.mvnRunner.LocalMavenInstall;
import edu.isi.bmkeg.utils.springContext.AppContext;
import edu.isi.bmkeg.vpdmf.controller.archiveBuilder.VPDMfArchiveFileBuilder;
import edu.isi.bmkeg.vpdmf.model.definitions.VPDMf;
import edu.isi.bmkeg.vpdmf.utils.VPDMfParser;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/edu/isi/bmkeg/vpdmf/appCtx-VPDMfTest.xml" })
public class VPDMfMavenBuildsTest {

	ApplicationContext ctx;

	VPDMfArchiveFileBuilder vafb;
	
	VPDMf top;
	UMLmodel m;
	UMLModelSimpleParser p;

	File vpdmfUserUmlFile;
	
	File resourceUmlFile;
	File resourceSpecsDir;
	File resourcesSrcZip;
	File heavyArchiveFile;

	File resourceXlFile;
	File resourceSheetDir;

	File ooevvUmlFile;
	File ooevvSrcZip;
	File ooevvMavenZip;
	File ooevvViewDir;

	
	File diglibUmlFile;
	File diglibSpecsDir;
	File diglibArchiveFile;

	String sql;


	@Before
	public void setUp() throws Exception {

		ctx = AppContext.getApplicationContext();

		vafb = new VPDMfArchiveFileBuilder();

		resourceUmlFile = ctx.getResource(
				"classpath:edu/isi/bmkeg/vpdmf/resource/resource.xml").getFile();

		resourceSpecsDir = ctx.getResource("classpath:edu/isi/bmkeg/vpdmf/resource/specs")
				.getFile();
						
		resourcesSrcZip = new File( resourceSpecsDir.getParentFile().getParentFile()
				.getAbsolutePath() + "/resource-0.0.1.zip");
		
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		
		ooevvUmlFile = ctx.getResource(
				"classpath:edu/isi/bmkeg/vpdmf/ooevv/ooevv.xml").getFile();

		ooevvViewDir = ctx.getResource(
				"classpath:edu/isi/bmkeg/vpdmf/ooevv/specs").getFile();

		ooevvSrcZip = new File( resourceSpecsDir.getParentFile().getParentFile()
				.getAbsolutePath() + "/ooevv-0.0.1.zip");
		
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		
		diglibUmlFile = ctx.getResource("classpath:edu/isi/bmkeg/vpdmf/digitalLibrary/digitalLibrary.xml")
				.getFile();

		diglibArchiveFile = new File( diglibUmlFile.getParentFile().getParentFile()
				.getAbsolutePath() + "/digitalLibrary_VPDMf.zip");

		
		LocalMavenInstall.installMavenLocally();
		
	}

	@After
	public void tearDown() throws Exception {
		LocalMavenInstall.removeLocalMaven();
	}

	/*@Test
	public final void testBuildResourceArchiveFile() throws Exception {

		p = new UMLModelSimpleParser(UMLmodel.XMI_MAGICDRAW);
		p.parseUMLModelFile(resourceUmlFile);
		
		UMLmodel m = p.getUmlModels().get(0);
					
		VPDMfParser vpdmfP = new VPDMfParser();
		vpdmfP.setModel(m);
		vpdmfP.setViewSpecDir(resourceSpecsDir);
		top = vpdmfP.buildAllViews2();			
		
		JavaUmlInterface java = new JavaUmlInterface();
		java.setUmlModel(m);
		
		java.buildJpaMavenProject(ooevvSrcZip, null, 
				".model.", top.getGroupId(), top.getArtifactId(), top.getVersion());
		
		//
		// TODO: Write checks for the unit test here.
		//
		
	}*/

	
	/*@Test
	public final void testBuildOoevvArchiveFile() throws Exception {		

		p = new UMLModelSimpleParser(UMLmodel.XMI_MAGICDRAW);
		p.parseUMLModelFile(ooevvUmlFile);
		
		UMLmodel m = p.getUmlModels().get(0);
					
		VPDMfParser vpdmfP = new VPDMfParser();
		vpdmfP.setModel(m);
		vpdmfP.setViewSpecDir(ooevvViewDir);
		top = vpdmfP.buildAllViews2();			

		JavaUmlInterface java = new JavaUmlInterface();
		java.setUmlModel(m);
		
		java.buildJpaMavenProject(resourcesSrcZip, null, ".model.", 
				top.getGroupId(), top.getArtifactId(), top.getVersion());
		//
		// TODO: Write checks for the unit test here.
		//
		
	}*/

	@Test
	public final void fakeTest() throws Exception {		

		assert(true);
		
	}
	
	
}
