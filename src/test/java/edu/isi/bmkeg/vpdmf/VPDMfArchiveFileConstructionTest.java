package edu.isi.bmkeg.vpdmf;

import java.io.File;
import java.util.ArrayList;
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
import edu.isi.bmkeg.utils.mvnRunner.LocalMavenInstall;
import edu.isi.bmkeg.utils.springContext.AppContext;
import edu.isi.bmkeg.vpdmf.controller.archiveBuilder.VPDMfArchiveFileBuilder;
import edu.isi.bmkeg.vpdmf.model.definitions.VPDMf;
import edu.isi.bmkeg.vpdmf.model.definitions.specs.VpdmfSpec;
import edu.isi.bmkeg.vpdmf.utils.VPDMfConverters;
import edu.isi.bmkeg.vpdmf.utils.VPDMfParser;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/edu/isi/bmkeg/vpdmf/appCtx-VPDMfTest.xml" })
public class VPDMfArchiveFileConstructionTest {

	ApplicationContext ctx;

	VPDMfArchiveFileBuilder vafb;

	VPDMf top;

	VpdmfSpec vpdmfSpec;
	UMLmodel m;
	List<File> views; 
	UMLModelSimpleParser p;

	File vpdmfUserUmlFile;
	
	File lightResourceVpdmfFile;
	File heavyResourceVpdmfFile;
	File resourceUmlFile;
	File resourceSpecsDir;
	File lightArchiveFile;
	File heavyArchiveFile;

	File resourceXlFile;
	File resourceSheetDir;

	File ooevvVpdmfFile;
	File ooevvUmlFile;
	File ooevvArchiveFile;
	File ooevvMavenZip;
	File ooevvViewDir;

	File diglibVpdmfFile;
	File diglibUmlFile;
	File diglibSpecsDir;
	File diglibArchiveFile;
	File diglibData;

	File triageVpdmfFile;
	File triageUmlFile;
	File triageSpecsDir;
	File triageArchiveFile;
	
	String sql;
	
	@Before
	public void setUp() throws Exception {

		ctx = AppContext.getApplicationContext();

		vafb = new VPDMfArchiveFileBuilder();

		lightResourceVpdmfFile = ctx.getResource("classpath:edu/isi/bmkeg/vpdmf/resource/xlsResource_vpdmf.xml")
				.getFile();

		heavyResourceVpdmfFile = ctx.getResource("classpath:edu/isi/bmkeg/vpdmf/resource/altNameResource_vpdmf.xml")
				.getFile();

		resourceUmlFile = ctx.getResource("classpath:edu/isi/bmkeg/vpdmf/resource/resource.xml")
				.getFile();

		resourceSpecsDir = ctx.getResource("classpath:edu/isi/bmkeg/vpdmf/resource/specs")
				.getFile();
				
		resourceXlFile = ctx.getResource("classpath:edu/isi/bmkeg/vpdmf/resource/resource-data.xls")
				.getFile();
		
		resourceSheetDir = ctx.getResource("classpath:edu/isi/bmkeg/vpdmf/resource/sheets")
				.getFile();
		
		lightArchiveFile = new File( resourceSpecsDir.getParentFile().getParentFile()
				.getAbsolutePath() + "/resource_excel_VPDMf.zip");
		
		heavyArchiveFile = new File( resourceSpecsDir.getParentFile().getParentFile()
				.getAbsolutePath() + "/resource_sheets_VPDMf.zip");

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		
		ooevvVpdmfFile = ctx.getResource(
				"classpath:edu/isi/bmkeg/vpdmf/ooevv/ooevv_vpdmf.xml").getFile();

		ooevvUmlFile = ctx.getResource(
				"classpath:edu/isi/bmkeg/vpdmf/ooevv/ooevv.xml").getFile();

		ooevvViewDir = ctx.getResource(
				"classpath:edu/isi/bmkeg/vpdmf/ooevv/specs").getFile();

		ooevvArchiveFile = new File( resourceSpecsDir.getParentFile().getParentFile()
				.getAbsolutePath() + "/ooevv_VPDMf.zip");

		ooevvMavenZip = new File( resourceSpecsDir.getParentFile().getParentFile()
				.getAbsolutePath() + "/ooevv_jpa.zip");

		
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		
		diglibVpdmfFile = ctx.getResource("classpath:edu/isi/bmkeg/vpdmf/digitalLibrary/digitalLibrary_vpdmf.xml")
				.getFile();

		diglibUmlFile = ctx.getResource("classpath:edu/isi/bmkeg/vpdmf/digitalLibrary/digitalLibrary.xml")
				.getFile();

		diglibSpecsDir = ctx.getResource(
				"classpath:edu/isi/bmkeg/vpdmf/digitalLibrary/specs").getFile();

		diglibArchiveFile = new File( diglibUmlFile.getParentFile().getParentFile()
				.getAbsolutePath() + "/digitalLibrary_VPDMf.zip");

		diglibData = ctx.getResource("classpath:edu/isi/bmkeg/vpdmf/digitalLibrary/sheets")
				.getFile();

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		triageVpdmfFile = ctx.getResource("classpath:edu/isi/bmkeg/vpdmf/triage/triage_vpdmfSpecs.xml")
				.getFile();

		triageUmlFile = ctx.getResource("classpath:edu/isi/bmkeg/vpdmf/triage/triage.xml")
				.getFile();

		triageSpecsDir = ctx.getResource(
				"classpath:edu/isi/bmkeg/vpdmf/triage/specs").getFile();

		triageArchiveFile = new File( triageUmlFile.getParentFile().getParentFile()
				.getAbsolutePath() + "/triage_VPDMf.zip");

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		LocalMavenInstall.installMavenLocally();
		
	}

	@After
	public void tearDown() throws Exception {
		LocalMavenInstall.removeLocalMaven();
	}

	@Test @Ignore("Outdated")
	public final void testBuildArchiveFileFromExcelFile() throws Exception {

		vpdmfSpec = VPDMfConverters.readVpdmfSpecFromFile(lightResourceVpdmfFile);
		
		p = new UMLModelSimpleParser(UMLmodel.XMI_MAGICDRAW);
		p.parseUMLModelFile(resourceUmlFile);
		
		UMLmodel m = p.getUmlModels().get(0);
					
		views = VPDMfParser.getAllSpecFiles(resourceSpecsDir);
		
		VPDMfParser vpdmfP = new VPDMfParser();
		top = vpdmfP.buildAllViews(vpdmfSpec, m, views);	
		
		List<File> dataFiles = new ArrayList<File>();
		dataFiles.add(resourceXlFile);
				
		//vafb.buildArchiveFile(vpdmfSpec, top, dataFiles, lightArchiveFile);
		
		//
		// TODO: Write checks for the unit test here.
		//
		
	}

	@Test @Ignore("Outdated")
	public final void testBuildArchiveFileFromSheetDir() throws Exception {		

		vpdmfSpec = VPDMfConverters.readVpdmfSpecFromFile(lightResourceVpdmfFile);

		p = new UMLModelSimpleParser(UMLmodel.XMI_MAGICDRAW);
		p.parseUMLModelFile(resourceUmlFile);
		
		UMLmodel m = p.getUmlModels().get(0);

		views = VPDMfParser.getAllSpecFiles(resourceSpecsDir);

		VPDMfParser vpdmfP = new VPDMfParser();
		top = vpdmfP.buildAllViews(vpdmfSpec, m, views);			
		
		List<File> dataFiles = new ArrayList<File>();
		dataFiles.add(resourceSheetDir);

		//vafb.buildArchiveFile(vpdmfSpec, top, dataFiles, heavyArchiveFile);

		//
		// TODO: Write checks for the unit test here.
		//
		
	}
	
	@Test @Ignore("Outdated")
	public final void testBuildArchiveFileForOoevvModel() throws Exception {		

		vpdmfSpec = VPDMfConverters.readVpdmfSpecFromFile(ooevvVpdmfFile);

		p = new UMLModelSimpleParser(UMLmodel.XMI_MAGICDRAW);
		p.parseUMLModelFile(ooevvUmlFile);
		
		UMLmodel m = p.getUmlModels().get(0);

		views = VPDMfParser.getAllSpecFiles(ooevvViewDir);

		VPDMfParser vpdmfP = new VPDMfParser();
		top = vpdmfP.buildAllViews(vpdmfSpec, m, views);			
				
		//vafb.buildArchiveFile(vpdmfSpec, top, null, this.ooevvArchiveFile);

		//
		// TODO: Write checks for the unit test here.
		//
		
	}
	
	@Test @Ignore("Outdated")
	public final void testBuildArchiveFileForDigitalLibrary() throws Exception {		

		vpdmfSpec = VPDMfConverters.readVpdmfSpecFromFile(diglibVpdmfFile);

		p = new UMLModelSimpleParser(UMLmodel.XMI_MAGICDRAW);
		p.parseUMLModelFile(this.diglibUmlFile);
		
		UMLmodel m = p.getUmlModels().get(0);
		
		views = VPDMfParser.getAllSpecFiles(diglibSpecsDir);

		VPDMfParser vpdmfP = new VPDMfParser();
		top = vpdmfP.buildAllViews(vpdmfSpec, m, views);			
		
		List<File> dataFiles = new ArrayList<File>();
		dataFiles.add(diglibData);

		//vafb.buildArchiveFile(vpdmfSpec, top, dataFiles, diglibArchiveFile);

		
		//
		// TODO: Write checks for the unit test here.
		//
		
	} 	

	
}
